package me.plexs.music.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.plexs.music.data.session.SessionStore

class StatsRepository(private val session: SessionStore) {

    private val base = "https://music.plexs.me"

    suspend fun reportSeconds(seconds: Long) {
        withContext(Dispatchers.IO) {
            runCatching {
                Http.post("$base/api/stats/play", mapOf("seconds" to seconds), session.cookie()).use { }
            }
        }
    }

    suspend fun reportPlay(song: Song) {
        if (song.id.isEmpty()) return
        withContext(Dispatchers.IO) {
            runCatching {
                Http.post("$base/api/stats/play", mapOf("song" to song), session.cookie()).use { }
            }
        }
    }

    suspend fun fetch(): StatsData? = withContext(Dispatchers.IO) {
        runCatching {
            Http.get("$base/api/stats", session.cookie()).use { resp ->
                if (!resp.isSuccessful) return@use null
                Http.json.decodeFromString<StatsResp>(resp.body!!.string()).stats
            }
        }.getOrNull()
    }
}
