package me.plexs.music.data.offline

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.plexs.music.data.api.Song
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Done : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

/**
 * App-lifetime download coordinator. Holds its OWN long-timeout OkHttp client
 * (the shared Http client's 20s read timeout is too short for a full-song proxy
 * download) and an application-scoped coroutine scope, so downloads keep running
 * and reporting progress across navigation and screen teardown.
 */
class DownloadManager(private val offline: OfflineRepository) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states: StateFlow<Map<String, DownloadState>> = _states

    private val active = ConcurrentHashMap.newKeySet<String>()

    fun state(id: String): DownloadState = _states.value[id] ?: DownloadState.Idle

    fun isActive(id: String): Boolean = _states.value[id] is DownloadState.Downloading

    fun download(song: Song) {
        val id = song.id
        if (id.isEmpty()) return
        if (!active.add(id)) return
        _states.value = _states.value + (id to DownloadState.Downloading(0f))
        appScope.launch {
            val result = runCatching { doDownload(song) }
            active.remove(id)
            val state = result.fold(
                onSuccess = { DownloadState.Done },
                onFailure = { e ->
                    deletePartial(id)
                    DownloadState.Failed(e.message ?: "Download failed")
                },
            )
            _states.value = _states.value + (id to state)
        }
    }

    fun cancel(id: String) {
        if (!active.remove(id)) return
        deletePartial(id)
        _states.value = _states.value + (id to DownloadState.Idle)
    }

    fun clear(id: String) {
        _states.value = _states.value - id
    }

    /** Queues every song in a playlist through the manager and records the playlist ref. */
    fun downloadPlaylist(playlistId: String, title: String, songs: List<Song>) {
        val ids = songs.map { it.id }.filter { it.isNotEmpty() }
        appScope.launch { offline.recordPlaylistRef(playlistId, title, ids) }
        songs.forEach { download(it) }
    }

    fun stateOf(song: Song): DownloadState = state(song.id)

    private fun deletePartial(id: String) {
        runCatching { File(offline.offlineDir(), id + ".m4a").delete() }
    }

    private suspend fun doDownload(song: Song): File {
        if (offline.isDownloaded(song.id)) return offline.fileFor(song.id)

        val id = song.id
        val url = "https://music.plexs.me/api/embed/stream/" + id + "?low=1"
        val file = offline.fileFor(id)
        client.newCall(
            okhttp3.Request.Builder().url(url).get().build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Download failed (${resp.code})")
            val body = resp.body ?: throw IllegalStateException("Empty response")
            val total = body.contentLength()
            val input = body.byteStream()
            val output = file.outputStream()
            val buffer = ByteArray(64 * 1024)
            var written = 0L
            while (true) {
                if (!active.contains(id)) {
                    output.close()
                    input.close()
                    throw IllegalStateException("Cancelled")
                }
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                written += read
                if (total > 0) {
                    val p = (written.toFloat() / total).coerceIn(0f, 1f)
                    _states.value = _states.value + (id to DownloadState.Downloading(p))
                }
            }
            output.flush()
            output.close()
            input.close()
        }
        if (!file.exists() || file.length() == 0L) throw IllegalStateException("Empty download")
        offline.commitDownload(song, file)
        return file
    }
}