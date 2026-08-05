package me.plexs.music.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

class ConfigRepository {
    private var cached: AppConfig? = null

    suspend fun config(): AppConfig = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        Http.get(Http.CONFIG_URL).use { resp ->
            if (!resp.isSuccessful) throw ApiException("Config unavailable")
            val cfg = Http.json.decodeFromString<AppConfig>(resp.body!!.string())
            cached = cfg
            cfg
        }
    }

    fun clear() { cached = null }
}
