package me.plexs.music.data.offline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.plexs.music.data.api.Http
import me.plexs.music.data.api.Song
import java.io.File

@Serializable
data class OfflineSong(
    val song: Song,
    val fileName: String,
    val sizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val autoCache: Boolean = false,
)

@Serializable
data class OfflinePlayList(
    val id: String,
    val title: String,
    val songIds: List<String> = emptyList(),
    val downloadedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class OfflineIndex(
    var songs: List<OfflineSong> = emptyList(),
    var playLists: List<OfflinePlayList> = emptyList(),
    var playCounts: Map<String, Int> = emptyMap(),
    var lastPlayedAt: Map<String, Long> = emptyMap(),
)

/**
 * Manages the app's offline library in private app storage so downloaded audio
 * never shows up in the gallery, file managers, or other media apps. Files live
 * under `filesDir/offline` (app-private) with a deduplicated-by-id index.
 *
 * Threading: the index is kept decoded in an in-memory cache guarded by a Mutex,
 * so repeated reads are cheap and never hit disk on the calling (often main)
 * thread. Every mutation runs on Dispatchers.IO via the suspend helpers, which
 * removes the main-thread JSON parse + file write churn that caused jank.
 *
 * - [download]: streams a super-compressed (`?low=1`) copy into storage.
 * - [isDownloaded]: offline-first check before any network play.
 * - [playCount]: used for the "auto-save after 3 genuine plays" rule. A play
 *   only increments when a track is actually heard (>= LISTEN_SECONDS of
 *   continuous playback), so reloads/restarts can never count as a play.
 */
class OfflineRepository(context: Context) {

    private val dir: File = File(context.filesDir, "offline").apply { mkdirs() }
    private val indexFile: File = File(context.filesDir, "offline_index.json")

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _version = kotlinx.coroutines.flow.MutableStateFlow(0)
    val version: kotlinx.coroutines.flow.StateFlow<Int> = _version

    private val mutex = Mutex()

    @Volatile
    private var cached: OfflineIndex? = null

    private fun bump() { _version.value += 1 }

    fun offlineDir(): File = dir

    fun fileFor(id: String): File = File(dir, id + ".m4a")

    private fun index(): OfflineIndex {
        cached?.let { return it }
        val loaded = try {
            if (indexFile.exists()) json.decodeFromString<OfflineIndex>(indexFile.readText()) else OfflineIndex()
        } catch (e: SerializationException) {
            OfflineIndex()
        } catch (e: Exception) {
            OfflineIndex()
        }
        cached = loaded
        return loaded
    }

    /** Runs [block] against the in-memory index under the mutex (off calling thread). */
    private fun <T> withIndex0(block: (OfflineIndex) -> T): T = block(index())

    private fun mutate(transform: (OfflineIndex) -> OfflineIndex) {
        val next = transform(index())
        cached = next
        try { indexFile.writeText(json.encodeToString(OfflineIndex.serializer(), next)) } catch (_: Exception) {}
    }

    /** Records a finished download into the index (used by DownloadManager). */
    suspend fun commitDownload(song: Song, file: File, autoCache: Boolean = false) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (index().songs.any { it.song.id == song.id }) return@withLock
            mutate { it.copy(songs = it.songs + OfflineSong(song, file.name, file.length(), autoCache = autoCache)) }
            bump()
        }
    }

    /** Records (or updates) a playlist reference without re-resolving already-stored songs. */
    suspend fun recordPlaylistRef(playListId: String, title: String, songIds: List<String>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val existing = index().playLists.firstOrNull { it.id == playListId }
                val merged = (existing?.songIds ?: emptyList()).toMutableList().apply { addAll(songIds) }.distinct()
                val pl = OfflinePlayList(playListId, title, merged, System.currentTimeMillis())
                mutate { it.copy(playLists =
                    if (existing == null) it.playLists + pl
                    else it.playLists.map { old -> if (old.id == playListId) pl else old }) }
                bump()
            }
        }

    fun list(): List<OfflineSong> = withIndex0 { it.songs }

    fun isDownloaded(id: String): Boolean = withIndex0 { it.songs.any { s -> s.song.id == id } }

    fun find(id: String): OfflineSong? = withIndex0 { it.songs.firstOrNull { s -> s.song.id == id } }

    fun size(): String = withIndex0 { idx ->
        val total = idx.songs.sumOf { it.sizeBytes }
        if (total <= 0) return@withIndex0 "0"
        val kb = total / 1024.0
        val mb = kb / 1024.0
        if (mb >= 1) String.format("%.1f MB", mb) else "${kb.toInt()} KB"
    }

    fun playListFor(id: String): String? = withIndex0 { it.playLists.firstOrNull { p -> p.id == id }?.title }

    fun playLists(): List<OfflinePlayList> = withIndex0 { it.playLists }

    /** Downloads a song to private storage as a super-compressed copy (lowest bitrate). */
    suspend fun download(song: Song, autoCache: Boolean = false): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (mutex.withLock { index().songs.any { it.song.id == song.id } }) {
                return@withContext Result.success(fileFor(song.id))
            }
            val url = "https://plex-meta.urdonkey6.workers.dev/api/stream/" + song.id + "?low=1"
            val file = fileFor(song.id)
            Http.client.newCall(
                okhttp3.Request.Builder().url(url).get().build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Download failed (${resp.code})")
                val body = resp.body ?: throw IllegalStateException("Empty response")
                val input = body.byteStream()
                val output = file.outputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    total += read
                }
                output.flush()
                output.close()
                input.close()
            }
            // Corruption guard: a valid audio download is never tiny.
            if (!file.exists() || file.length() < 30_000L) {
                file.delete()
                throw IllegalStateException("Empty download")
            }
            mutex.withLock {
                mutate { it.copy(songs = it.songs + OfflineSong(song, file.name, file.length(), autoCache = autoCache)) }
                bump()
            }
            file
        }
    }

    /** Records (or updates) a playlist reference without re-resolving already-stored songs. */
    suspend fun recordPlaylist(playListId: String, title: String, songIds: List<String>) =
        recordPlaylistRef(playListId, title, songIds)

    /** Downloads an entire playlist's track list, skipping already-stored ids */
    suspend fun downloadPlayList(playListId: String, title: String, songs: List<Song>): Result<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val existingPlay = mutex.withLock { index().playLists.firstOrNull { it.id == playListId } }
                val savedIds = (existingPlay?.songIds ?: emptyList()).toMutableList()
                var added = 0
                for (song in songs) {
                    if (!isDownloaded(song.id)) {
                        val r = download(song).getOrNull()
                        if (r != null) { added++; savedIds.add(song.id) }
                    } else if (!savedIds.contains(song.id)) {
                        savedIds.add(song.id)
                    }
                }
                mutex.withLock {
                    mutate { it.copy(playLists =
                        if (existingPlay == null) it.playLists + OfflinePlayList(playListId, title, savedIds.distinct())
                        else it.playLists.map { old -> if (old.id == playListId) OfflinePlayList(playListId, title, savedIds.distinct()) else old }) }
                    bump()
                }
                added to savedIds.distinct().size
            }
        }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val song = index().songs.firstOrNull { it.song.id == id } ?: return@withLock
                fileFor(id).delete()
                mutate { it.copy(songs = it.songs.filterNot { s -> s.song.id == id }) }
                bump()
            }
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                dir.listFiles()?.forEach { it.delete() }
                mutate { OfflineIndex() }
                bump()
            }
        }
    }

    /** A "play" counts once per genuine listen (30s threshold in the controller). */
    suspend fun recordPlay(id: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val count = (index().playCounts[id] ?: 0) + 1
                mutate { it.copy(
                    playCounts = it.playCounts + (id to count),
                    lastPlayedAt = it.lastPlayedAt + (id to System.currentTimeMillis()),
                ) }
            }
        }
    }

    /** True when the stored file is missing or clearly corrupt (tiny/empty). */
    fun isCorrupt(id: String): Boolean {
        return withIndex0 { idx ->
            val e = idx.songs.firstOrNull { s -> s.song.id == id } ?: return@withIndex0 false
            !File(dir, e.fileName).exists() || File(dir, e.fileName).length() < 30_000L
        }
    }

    /** Deletes auto-cache songs not played for [ttlMs] (default 24h). User downloads stay forever. */
    fun purgeExpiredAutoCache(now: Long = System.currentTimeMillis(), ttlMs: Long = 24 * 3600_000L) {
        withIndex0 { idx ->
            val stale = idx.songs.filter { it.autoCache && (now - (idx.lastPlayedAt[it.song.id] ?: it.downloadedAt)) > ttlMs }
            if (stale.isEmpty()) return@withIndex0
            stale.forEach { runCatching { File(dir, it.fileName).delete() } }
            val ids = stale.map { it.song.id }.toSet()
            mutate { it.copy(songs = it.songs.filterNot { ids.contains(it.song.id) }) }
            bump()
        }
    }

    /** Downloads completed (every song id present) → used for the playlist checkmark. */
    fun completedPlaylistLocked(pl: OfflinePlayList): Boolean {
        return withIndex0 { idx ->
            pl.songIds.isNotEmpty() && pl.songIds.all { id -> idx.songs.any { it.song.id == id } }
        }
    }
    fun completedPlaylist(pl: OfflinePlayList): Boolean = completedPlaylistLocked(pl)
    fun downloadedCount(pl: OfflinePlayList): Int = withIndex0 { idx ->
        pl.songIds.count { id -> idx.songs.any { it.song.id == id } }
    }

    fun playCount(id: String): Int = withIndex0 { it.playCounts[id] ?: 0 }
}