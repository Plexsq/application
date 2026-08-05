package me.plexs.music.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.plexs.music.data.session.SessionStore

class CatalogRepository(private val session: SessionStore) {

    private val base = "https://music.plexs.me"

    suspend fun search(query: String, filter: String = "all"): SearchResults = withContext(Dispatchers.IO) {
        val url = "$base/api/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=$filter&_cache=0"
        Http.get(url, session.cookie()).use { resp ->
            if (!resp.isSuccessful) throw ApiException("Search failed (${resp.code})")
            Http.json.decodeFromString<SearchResults>(resp.body!!.string())
        }
    }

    suspend fun album(id: String): AlbumPage = withContext(Dispatchers.IO) {
        val url = "$base/api/album?id=${java.net.URLEncoder.encode(id, "UTF-8")}&_cache=0"
        Http.get(url, session.cookie()).use { resp ->
            if (!resp.isSuccessful) throw ApiException("Album failed (${resp.code})")
            Http.json.decodeFromString<AlbumPage>(resp.body!!.string())
        }
    }

    suspend fun playlist(id: String): PlaylistPage = withContext(Dispatchers.IO) {
        val url = "$base/api/playlist?id=${java.net.URLEncoder.encode(id, "UTF-8")}"
        Http.get(url, session.cookie()).use { resp ->
            if (!resp.isSuccessful) throw ApiException("Playlist failed (${resp.code})")
            Http.json.decodeFromString<PlaylistPage>(resp.body!!.string())
        }
    }

    suspend fun artist(id: String): ArtistPage = withContext(Dispatchers.IO) {
        val url = "$base/api/artist?id=${java.net.URLEncoder.encode(id, "UTF-8")}&_cache=0"
        Http.get(url, session.cookie()).use { resp ->
            if (!resp.isSuccessful) throw ApiException("Artist failed (${resp.code})")
            Http.json.decodeFromString<ArtistPage>(resp.body!!.string())
        }
    }

    suspend fun lyrics(videoId: String, title: String, artist: String): LyricsResult? = withContext(Dispatchers.IO) {
        val url = "$base/api/lyrics?id=${java.net.URLEncoder.encode(videoId, "UTF-8")}" +
            "&title=${java.net.URLEncoder.encode(title, "UTF-8")}" +
            "&artist=${java.net.URLEncoder.encode(artist, "UTF-8")}"
        runCatching {
            Http.get(url, session.cookie()).use { resp ->
                if (!resp.isSuccessful) null else Http.json.decodeFromString<LyricsResult>(resp.body!!.string())
            }
        }.getOrNull()
    }
}