package me.plexs.music.updater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import me.plexs.music.data.api.Http
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

object AppUpdater {

    private const val MANIFEST_URL =
        "https://github.com/Plexsq/application/releases/latest/download/update-manifest.json"

    sealed class Result {
        object AlreadyCurrent : Result()
        object NeedsInstallPermission : Result()
        object InstallComplete : Result()
        data class Failed(val message: String) : Result()
    }

    fun installedVersionCode(context: Context): Long =
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()

    /** Fetches the latest update manifest (callers decide whether to act on it). */
    suspend fun latestManifest(): UpdateManifest? = withContext(Dispatchers.IO) {
        runCatching { fetchManifest() }.getOrNull()
    }

    fun canInstallPermission(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    suspend fun run(
        context: Context,
        onProgress: (label: String, done: Long, total: Long) -> Unit,
        onDone: (Result) -> Unit,
    ) {
        val main = Handler(Looper.getMainLooper())
        fun progress(label: String, done: Long, total: Long) =
            main.post { onProgress(label, done, total) }
        fun done(r: Result) = main.post { onDone(r) }

        withContext(Dispatchers.IO) {
            try {
                val code = installedVersionCode(context)
                val manifest = fetchManifest()
                if (manifest.toCode <= code) {
                    done(Result.AlreadyCurrent)
                    return@withContext
                }
                if (!canInstallPermission(context)) {
                    done(Result.NeedsInstallPermission)
                    return@withContext
                }

                val dir = File(context.filesDir, "update").apply { mkdirs() }
                var apk: File? = null
                var usedPatch = false

                // Prefer delta when this exact installed version has a patch available.
                val deltaAllowed = manifest.patchUrl.isNotBlank() && manifest.fromCode == code
                if (deltaAllowed) {
                    val patchFile = File(dir, "patch.bin")
                    download(
                        manifest.patchUrl,
                        patchFile,
                        manifest.patchSize,
                    ) { d, t -> progress("Downloading update (patch)", d, maxOf(t, 1)) }
                    progress("Applying update", 0, manifest.apkSize)
                    val oldPath = context.applicationInfo.sourceDir
                    if (oldPath != null) {
                        val rebuilt = File(dir, "rebuilt.apk")
                        if (runCatching { Bspatch.apply(File(oldPath), patchFile, rebuilt) }.isSuccess) {
                            apk = rebuilt
                            usedPatch = true
                        }
                    }
                }

                if (apk == null) {
                    val full = File(dir, "Plex.apk")
                    download(manifest.apkUrl, full, manifest.apkSize) { c, t ->
                        progress("Downloading update", maxOf(c, 0), maxOf(t, 1))
                    }
                    apk = full
                }

                val target = apk ?: return@withContext
                if (!verify(context, target, manifest)) {
                    // Delta produced an invalid artifact; fall back to the full APK.
                    if (usedPatch) {
                        target.delete()
                        val full = File(dir, "Plex.apk")
                        download(manifest.apkUrl, full, manifest.apkSize) { c, b ->
                            progress("Downloading update", maxOf(c, 0), maxOf(b, 1))
                        }
                        if (!verify(context, full, manifest)) {
                            done(Result.Failed("Downloaded update failed verification"))
                            return@withContext
                        }
                        install(context, full, manifest)
                    } else {
                        done(Result.Failed("Downloaded update failed verification"))
                        return@withContext
                    }
                } else {
                    install(context, target, manifest)
                }
                done(Result.InstallComplete)
            } catch (e: Exception) {
                done(Result.Failed(e.message ?: "Update failed"))
            }
        }
    }

    private fun verify(context: Context, apk: File, manifest: UpdateManifest): Boolean {
        if (sha256(apk) != manifest.apkSha256) return false
        return signaturesMatch(context, apk)
    }

    private fun fetchManifest(): UpdateManifest {
        Http.client.newCall(Request.Builder().url(MANIFEST_URL).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("update manifest: HTTP ${resp.code}")
            return Http.json.decodeFromString<UpdateManifest>(resp.body!!.string())
        }
    }

    private fun download(
        url: String,
        target: File,
        expected: Long,
        progress: (done: Long, total: Long) -> Unit,
    ) {
        Http.client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("download: HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("empty body")
            val total = if (expected > 0) expected else body.contentLength()
            target.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                var last = 0L
                body.byteStream().use { input ->
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (done - last >= 128 * 1024 || done == total) {
                            last = done
                            progress(done, total)
                        }
                    }
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { i ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = i.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun signaturesMatch(context: Context, candidateApk: File): Boolean {
        val pm = context.packageManager
        return try {
            val current = signerOf(pm.getPackageInfo(context.packageName, signFlags()))
            val archive = signerOf(pm.getPackageArchiveInfo(candidateApk.absolutePath, signFlags()))
            current != null && archive != null && java.util.Arrays.equals(current, archive)
        } catch (_: Exception) {
            true
        }
    }

    private fun signFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun signerOf(info: PackageInfo?): ByteArray? {
        if (info == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()?.toByteArray()
        }
    }

    private fun install(context: Context, apk: File, manifest: UpdateManifest) {
        val pm = context.packageManager
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        val sessionId = pm.packageInstaller.createSession(params)
        val session = pm.packageInstaller.openSession(sessionId)
        val stream = session.openWrite("apk", 0, apk.length())
        try {
            apk.inputStream().use { it.copyTo(stream) }
            session.fsync(stream)
        } finally {
            stream.close()
            session.close()
        }
        val intent = Intent(context, InstallReceiver::class.java)
            .setAction("me.plexs.music.$sessionId")
            .setPackage(context.packageName)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session.commit(pending.intentSender)
    }
}