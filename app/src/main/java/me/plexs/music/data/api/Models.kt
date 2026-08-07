package me.plexs.music.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.longOrNull

private object FlexibleLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long? {
        val json = decoder as? kotlinx.serialization.json.JsonDecoder
            ?: return decoder.decodeLong()
        return when (val el = json.decodeJsonElement()) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (el.isString) {
                    el.content.trim().replace(",", "")
                        .replace("M", "000000").replace("K", "000")
                        .toLongOrNull()
                } else {
                    el.longOrNull
                }
            }
            is kotlinx.serialization.json.JsonNull -> null
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

private object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val json = decoder as? kotlinx.serialization.json.JsonDecoder
            ?: return decoder.decodeInt()
        return when (val el = json.decodeJsonElement()) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                val s = el.content.trim().replace(",", "").replace("M", "000000").replace("K", "000")
                // Accept int / float / numeric-string (the web stores float durations like 196.101).
                s.toFloatOrNull()?.toInt() ?: s.toIntOrNull() ?: 0
            }
            else -> 0
        }
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

@Serializable
data class InnertubeClient(
    val name: String,
    val version: String,
    val androidSdkVersion: Int,
)

@Serializable
data class AppFlag(
    val streamHost: String? = null,
    val useWorkerStream: Boolean? = null,
    val bufferMs: Int? = null,
)

@Serializable
data class AppConfig(
    val baseUrl: String,
    val googleClientId: String,
    val innertubeKey: String,
    val innertubeClients: List<InnertubeClient>,
    val latestVersion: String? = null,
    val flags: AppFlag? = null,
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
    @Serializable(with = FlexibleIntSerializer::class)
    val duration: Int = 0,
    val durationText: String? = null,
    val thumbnail: String? = null,
    @Serializable(with = FlexibleLongSerializer::class)
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
data class PlaytimeData(
    val daily: Long = 0,
    val monthly: Long = 0,
)

@Serializable
data class UserData(
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val favorites: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val playtime: PlaytimeData = PlaytimeData(),
    val play_counts: Map<String, Int> = emptyMap(),
)

@Serializable
data class UserPlaylist(
    val id: String = "",
    val name: String = "",
    val image: String? = null,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = 0,
)

@Serializable
data class UserDataResp(val data: UserData = UserData())

@Serializable
data class StatsTopSong(val song: Song? = null, val plays: Int = 0)

@Serializable
data class StatsData(
    val daily: Long = 0,
    val monthly: Long = 0,
    val top: List<StatsTopSong> = emptyList(),
)

@Serializable
data class StatsResp(val stats: StatsData = StatsData())

@Serializable
data class HistoryItem(val query: String = "", val searchedAt: String = "")

@Serializable
data class HistoryResp(val history: List<HistoryItem> = emptyList())

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
