package me.plexs.music.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.plexs.music.data.session.SessionStore

class UserDataRepository(private val session: SessionStore) {

    private val base = "https://music.plexs.me"

    suspend fun fetch(): UserData? = withContext(Dispatchers.IO) {
        runCatching {
            Http.get("$base/api/user-data", session.cookie()).use { resp ->
                if (!resp.isSuccessful) return@use null
                Http.json.decodeFromString<UserDataResp>(resp.body!!.string()).data
            }
        }.getOrNull()
    }

    suspend fun save(data: UserData) {
        withContext(Dispatchers.IO) {
            runCatching {
                Http.post("$base/api/user-data", mapOf("data" to data), session.cookie())
                    .use { }
            }
        }
    }

    suspend fun fetchHistory(): List<HistoryItem>? = withContext(Dispatchers.IO) {
        runCatching {
            Http.get("$base/api/history", session.cookie()).use { resp ->
                if (!resp.isSuccessful) return@use null
                Http.json.decodeFromString<HistoryResp>(resp.body!!.string()).history
            }
        }.getOrNull()
    }

    suspend fun addHistory(query: String): List<HistoryItem>? = withContext(Dispatchers.IO) {
        runCatching {
            Http.post("$base/api/history", mapOf("query" to query), session.cookie())
                .use { resp ->
                    if (!resp.isSuccessful) return@use null
                    Http.json.decodeFromString<HistoryResp>(resp.body!!.string()).history
                }
        }.getOrNull()
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            runCatching {
                Http.delete("$base/api/history", session.cookie()).use { }
            }
        }
    }
}