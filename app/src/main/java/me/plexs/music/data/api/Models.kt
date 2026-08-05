package me.plexs.music.data.api

import kotlinx.serialization.Serializable

@Serializable
data class InnertubeClient(
    val name: String,
    val version: String,
    val androidSdkVersion: Int,
)

@Serializable
data class AppConfig(
    val baseUrl: String,
    val googleClientId: String,
    val innertubeKey: String,
    val innertubeClients: List<InnertubeClient>,
    val latestVersion: String? = null,
)

@Serializable
data class BootstrapApp(val latestVersion: String? = null, val apkUrl: String? = null)

@Serializable
data class Bootstrap(val app: BootstrapApp? = null)

@Serializable
data class User(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val avatar: String? = null,
    val emailVerified: Boolean? = null,
)

@Serializable
data class Session(val id: String, val token: String? = null)

@Serializable
data class AuthResponse(
    val user: User? = null,
    val session: Session? = null,
    val pending: Boolean? = null,
    val email: String? = null,
)

@Serializable
data class AuthState(val user: User? = null, val session: Session? = null)

@Serializable
data class ApiError(val error: String? = null)

@Serializable
data class Song(
    val id: String = "",
    val type: String? = null,
    val title: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val album: String? = null,
    val duration: Int = 0,
    val durationText: String? = null,
    val thumbnail: String? = null,
    val plays: Long? = null,
    val year: Int? = null,
)

@Serializable
data class ArtistCard(
    val type: String? = null,
    val id: String = "",
    val name: String = "",
    val subscribers: String? = null,
    val thumbnail: String? = null,
)

@Serializable
data class AlbumCard(
    val type: String? = null,
    val id: String = "",
    val title: String = "",
    val artist: String? = null,
    val year: Int? = null,
    val thumbnail: String? = null,
)

@Serializable
data class PlaylistCard(
    val type: String? = null,
    val id: String = "",
    val title: String = "",
    val author: String? = null,
    val itemCount: Int? = null,
    val thumbnail: String? = null,
)

@Serializable
data class SearchResults(
    val artists: List<SearchCard> = emptyList(),
    val songs: List<Song> = emptyList(),
    val videos: List<Song> = emptyList(),
    val albums: List<SearchCard> = emptyList(),
    val playlists: List<SearchCard> = emptyList(),
)

@Serializable
data class SearchCard(
    val type: String? = null,
    val id: String = "",
    val name: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val author: String? = null,
    val subscribers: String? = null,
    val itemCount: Int? = null,
    val year: Int? = null,
    val thumbnail: String? = null,
)

@Serializable
data class AlbumPage(
    val title: String = "",
    val artist: String? = null,
    val thumbnail: String? = null,
    val year: Int? = null,
    val items: List<Song> = emptyList(),
)

@Serializable
data class PlaylistPage(
    val title: String = "",
    val author: String? = null,
    val items: List<Song> = emptyList(),
)

@Serializable
data class ArtistSection(
    val title: String = "",
    val items: List<Song> = emptyList(),
)

@Serializable
data class ArtistPage(
    val name: String = "",
    val description: String? = null,
    val thumbnail: String? = null,
    val banner: String? = null,
    val sections: List<ArtistSection> = emptyList(),
    val followers: Long? = null,
)

@Serializable
data class LyricsLine(val time: Double, val text: String)

@Serializable
data class LyricsResult(
    val synced: List<LyricsLine>? = null,
    val plain: String? = null,
    val source: String? = null,
)
