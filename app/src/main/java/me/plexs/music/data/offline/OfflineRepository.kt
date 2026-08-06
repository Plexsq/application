package me.plexs.music.data.offline

import android.content.Context
import kotlinx.coroutines.Dispatchers
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
)

/**
 * Manages the app's offline library in private app storage so downloaded audio
 * never shows up in the gallery, file managers, or other media apps. Files live
 * under `filesDir/offline` (app-private) with a deduplicated-by-id index.
 *
 * - [download]: streams a super-compressed (`?low=1`) copy into storage.
 * - [isDownloaded]: offline-first check before any network play.
 * - [playCount]: used for the "auto-save after 3 genuine plays" rule. A play
 *   only increments when a track is actually heard (>= LISTEN_SECONDS of
 *   continuous playback), so reloads/restarts can never count as a play.
 */
class OfflineRepository(context: Context) {

    private val dir: File = File(context.filesDir, "offline").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "offline_index.json")

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _version = kotlinx.coroutines.flow.MutableStateFlow(0)
    val version: kotlinx.coroutines.flow.StateFlow<Int> = _version

    private fun bump() { _version.value += 1 }

    fun offlineDir(): File = dir

    fun list(): List<OfflineSong> = withContext0 { readIndex().songs }

    fun isDownloaded(id: String): Boolean = withContext0 {
        readIndex().songs.any { it.song.id == id }
    }

    fun find(id: String): OfflineSong? = withContext0 {
        readIndex().songs.firstOrNull { it.song.id == id }
    }

    fun size(): String {
        var total = readIndex().songs.sumOf { it.sizeBytes }
        if (total <= 0) return "0"
        val kb = total / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) String.format("%.1f MB", mb) else "${kb.toInt()} KB"
    }

    fun playListFor(id: String): String? = withContext0 {
        readIndex().playLists.firstOrNull { it.id == id }?.title
    }

    fun playLists(): List<OfflinePlayList> = withContext0 { readIndex().playLists }

    /**
     * Downloads a song to private storage as a super-compressed copy (lowest
     * bitrate the stream endpoint can give). Deduplicates by song id so the same
     * song is never stored twice, even when it appears in multiple playlists.
     */
    suspend fun download(song: Song): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val idx = readIndex()
            if (idx.songs.any { it.song.id == song.id }) {
                return@withContext Result.success(fileFor(song.id))
            }
            val streamBase = "https://music.plexs.me/api/embed/stream/" + song.id
            val url = streamBase + "?low=1"
            val ext = "m4a"
            val file = fileFor(song.id, ext)
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
            if (!file.exists() || file.length() == 0L) {
                file.delete()
                throw IllegalStateException("Empty download")
            }
            val updated = idx.copy(songs = idx.songs + OfflineSong(song, file.name, file.length()))
            writeIndex(updated)
            bump()
            file
        }
    }

    /**
     * Downloads an entire playlist's track list, skipping already-stored ids so
     * a song in two playlists (or a repeated track) is only ever saved once.
     */
    suspend fun downloadPlayList(playListId: String, title: String, songs: List<Song>): Result<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val idx = readIndex()
                var added = 0
                val already = idx.songs.map { it.song.id }.toMutableSet()
                val existingPlay = idx.playLists.firstOrNull { it.id == playListId }
                val savedIds = (existingPlay?.songIds ?: emptyList()).toMutableList()
                for (song in songs) {
                    if (!already.contains(song.id)) {
                        val r = download(song).getOrNull()
                        if (r != null) { added++; savedIds.add(song.id); already.add(song.id) }
                    } else {
                        // Song already downloaded; still record it under this playlist ref.
                        if (!savedIds.contains(song.id)) savedIds.add(song.id)
                    }
                }
                val newPl = OfflinePlayList(playListId, title, savedIds, System.currentTimeMillis())
                val pls = if (existingPlay == null) idx.playLists + newPl
                    else idx.playLists.map { if (it.id == playListId) newPl else it }
                writeIndex(idx.copy(playLists = pls))
                bump()
                added to savedIds.size
            }
        }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            val idx = readIndex()
            val song = idx.songs.firstOrNull { it.song.id == id } ?: return@withContext
            fileFor(id).delete()
            writeIndex(idx.copy(songs = idx.songs.filterNot { it.song.id == id }))
            bump()
        }
    }

    fun deleteAll() {
        try {
            dir.listFiles()?.forEach { it.delete() }
            writeIndex(OfflineIndex())
            bump()
        } catch (_: Exception) {}
    }

    /**
     * A "play" only counts once per genuine listen: called when playback has
     * reached the 30s listen threshold in the playback controller. Because it is
     * fired only on real progress, a page/app reload or restart never bumps it.
     */
    fun recordPlay(id: String) {
        val idx = readIndex()
        val count = (idx.playCounts[id] ?: 0) + 1
        writeIndex(idx.copy(playCounts = idx.playCounts + (id to count)))
    }

    fun playCount(id: String): Int = readIndex().playCounts[id] ?: 0

    private fun fileFor(id: String): File = File(dir, id + ".m4a")

    private fun <T> withContext0(block: () -> T): T = block()

    private fun readIndex(): OfflineIndex {
        return try {
            if (indexFile.exists()) json.decodeFromString<OfflineIndex>(indexFile.readText())
            else OfflineIndex()
        } catch (e: SerializationException) {
            OfflineIndex()
        } catch (e: Exception) {
            OfflineIndex()
        }
    }

    private fun writeIndex(idx: OfflineIndex) {
        try {
            indexFile.writeText(json.encodeToString(OfflineIndex.serializer(), idx))
        } catch (e: Exception) {
            // never let a failed index write break the UI
        }
    }
}