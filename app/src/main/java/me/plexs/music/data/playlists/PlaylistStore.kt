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
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PlaylistTombstone(
    val id: String = "",
    val name: String? = null,
    val deletedAt: Long = 0,
)

@Serializable
private data class PlaylistData(
    var playlists: List<UserPlaylist> = emptyList(),
    var playlistDeletes: List<PlaylistTombstone> = emptyList(),
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
        val now = System.currentTimeMillis()
        val pl = UserPlaylist("pl_${now}", trimmed, createdAt = now, updatedAt = now)
        write(data.copy(playlists = data.playlists + pl))
        return pl
    }

    fun find(id: String): UserPlaylist? = read().playlists.firstOrNull { it.id == id }

    fun rename(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val data = read()
        write(data.copy(playlists = data.playlists.map { pl ->
            if (pl.id == id) pl.copy(name = trimmed, updatedAt = System.currentTimeMillis()) else pl
        }))
    }

    /** Adds a song to a playlist, ignoring duplicates by song id. */
    fun addSong(playlistId: String, song: Song) {
        val data = read()
        val updated = data.playlists.map { pl ->
            if (pl.id == playlistId && pl.songs.none { it.id == song.id }) {
                pl.copy(songs = pl.songs + song, updatedAt = System.currentTimeMillis())
            } else pl
        }
        write(data.copy(playlists = updated))
    }

    fun removeSong(playlistId: String, songId: String) {
        val data = read()
        val updated = data.playlists.map { pl ->
            if (pl.id == playlistId) pl.copy(songs = pl.songs.filterNot { it.id == songId }, updatedAt = System.currentTimeMillis()) else pl
        }
        write(data.copy(playlists = updated))
    }

    fun delete(playlistId: String) {
        val data = read()
        val name = data.playlists.firstOrNull { it.id == playlistId }?.name
        write(data.copy(
            playlists = data.playlists.filterNot { it.id == playlistId },
            playlistDeletes = data.playlistDeletes + PlaylistTombstone(playlistId, name, System.currentTimeMillis()),
        ))
    }

    /** Local deletion tombstones to push with the next save (so the account stays deleted). */
    fun deletes(): List<PlaylistTombstone> = read().playlistDeletes

    /** Clears a tombstone once the server has acknowledged the delete via its own list. */
    fun clearDelete(id: String) {
        val data = read()
        if (data.playlistDeletes.none { it.id == id }) return
        write(data.copy(playlistDeletes = data.playlistDeletes.filterNot { it.id == id }))
    }

    /**
     * Applies the account's authoritative playlist state (server = truth):
     *  - drops any local playlist that is tombstoned on the server (deleted on PC)
     *  - adopts server playlists whose updatedAt is newer (renames, add/remove songs)
     *  - keeps local playlists the server hasn't seen yet (will push on next save)
     *  - clears our own tombstone once the server list reflects the delete
     */
    fun syncFromServer(server: List<me.plexs.music.data.api.UserPlaylist>, serverDeletes: List<me.plexs.music.data.api.PlaylistTombstone>) {
        val data = read()
        val deleted = serverDeletes.map { it.id }.toSet()
        // Drop locally-tombstoned playlists the server hasn't removed yet only when they
        // are absent from the server list too; otherwise the server merge removed them.
        val serverById = server.associateBy { it.id }

        val local = data.playlists.filter { p ->
            !deleted.contains(p.id) || serverById.containsKey(p.id)
        }
        val merged = local.toMutableList()
        val byId = merged.associateBy { it.id }.toMutableMap()
        for (sp in server) {
            val existing = byId[sp.id]
            if (existing == null) {
                merged.add(sp.toLocal())
                byId[sp.id] = sp.toLocal()
            } else {
                val sv = sp.updatedAt
                val lv = existing.updatedAt
                if (sv > lv) {
                    val idx = merged.indexOf(existing)
                    val adopted = sp.toLocal()
                    merged[idx] = adopted
                    byId[sp.id] = adopted
                }
                // else keep local (it's newer; it'll push on next save)
            }
        }
        // Clear tombstones the server has now applied (their ids are in serverDeletes).
        val keeps = data.playlistDeletes.filter { !serverDeletes.any { d -> d.id == it.id } }
        write(PlaylistData(playlists = merged, playlistDeletes = keeps))
    }

    private fun me.plexs.music.data.api.UserPlaylist.toLocal(): UserPlaylist =
        UserPlaylist(
            id = id,
            name = name,
            image = image,
            songs = songs,
            createdAt = createdAt,
            updatedAt = updatedAt,
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