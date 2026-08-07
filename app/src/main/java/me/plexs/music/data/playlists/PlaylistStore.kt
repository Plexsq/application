package me.plexs.music.data.playlists

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.plexs.music.data.api.Song
import java.io.File

@Serializable
data class UserPlaylist(
    val id: String,
    val name: String,
    val image: String? = null,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
private data class PlaylistData(
    var playlists: List<UserPlaylist> = emptyList(),
)

/**
 * Local custom playlists (created by the user, like the web app's "New Playlist").
 * Stored as a JSON file under filesDir so it survives restarts. Adding a song is
 * always guarded against duplicates by song id, so a track can never appear twice
 * in the same playlist.
 */
class PlaylistStore(context: Context) {

    private val file = java.io.File(context.filesDir, "playlists.json")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    fun list(): List<UserPlaylist> = read().playlists

    fun create(name: String): UserPlaylist {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return UserPlaylist("pl_${System.currentTimeMillis()}", "Untitled")
        val data = read()
        val pl = UserPlaylist("pl_${System.currentTimeMillis()}", trimmed)
        write(data.copy(playlists = data.playlists + pl))
        return pl
    }

    fun find(id: String): UserPlaylist? = read().playlists.firstOrNull { it.id == id }

    fun rename(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val data = read()
        write(data.copy(playlists = data.playlists.map { pl ->
            if (pl.id == id) pl.copy(name = trimmed) else pl
        }))
    }

    /** Adds a song to a playlist, ignoring duplicates by song id. */
    fun addSong(playlistId: String, song: Song) {
        val data = read()
        val updated = data.playlists.map { pl ->
            if (pl.id == playlistId && pl.songs.none { it.id == song.id }) {
                pl.copy(songs = pl.songs + song)
            } else pl
        }
        write(data.copy(playlists = updated))
    }

    fun removeSong(playlistId: String, songId: String) {
        val data = read()
        val updated = data.playlists.map { pl ->
            if (pl.id == playlistId) pl.copy(songs = pl.songs.filterNot { it.id == songId }) else pl
        }
        write(data.copy(playlists = updated))
    }

    fun delete(playlistId: String) {
        val data = read()
        write(data.copy(playlists = data.playlists.filterNot { it.id == playlistId }))
    }

    /**
     * Merges the server-side playlist set into local storage: adopt the server
     * set wholesale when local is empty, otherwise union by id (keep the local
     * copy for matches so unsaved edits win, add server-only entries).
     */
    fun syncFromServer(server: List<me.plexs.music.data.api.UserPlaylist>) {
        if (server.isEmpty()) return
        val local = read().playlists
        val merged = if (local.isEmpty()) {
            server.map { it.toLocal() }
        } else {
            val byId = local.associateBy { it.id }
            val out = local.toMutableList()
            for (sp in server) {
                if (byId[sp.id] == null) out.add(sp.toLocal())
            }
            out
        }
        if (merged.map { it.id } != local.map { it.id }) {
            write(PlaylistData(playlists = merged))
        }
    }

    private fun me.plexs.music.data.api.UserPlaylist.toLocal(): UserPlaylist =
        UserPlaylist(
            id = id,
            name = name,
            image = image,
            songs = songs,
            createdAt = createdAt,
        )

    private fun read(): PlaylistData {
        return try {
            if (file.exists()) json.decodeFromString<PlaylistData>(file.readText()) else PlaylistData()
        } catch (e: Exception) {
            PlaylistData()
        }
    }

    private fun write(data: PlaylistData) {
        try {
            file.writeText(json.encodeToString(PlaylistData.serializer(), data))
            _version.value += 1
        } catch (e: Exception) {
            // never let a failed write break the UI
        }
    }
}