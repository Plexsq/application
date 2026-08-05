package me.plexs.music.data.bootstrap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import me.plexs.music.data.api.ApiException
import me.plexs.music.data.api.Bootstrap
import me.plexs.music.data.api.BootstrapApp
import me.plexs.music.data.api.Http

class BootstrapRepository {
    suspend fun latest(): BootstrapApp? = withContext(Dispatchers.IO) {
        try {
            Http.get(Http.BOOTSTRAP_URL).use { resp ->
                if (!resp.isSuccessful) return@withContext null
                Http.json.decodeFromString<Bootstrap>(resp.body!!.string()).app
            }
        } catch (_: Exception) {
            null
        }
    }
}

class UpdateChecker {
    fun isNewer(current: String, latest: String): Boolean {
        val a = current.split(".").mapNotNull { it.toIntOrNull() }
        val b = latest.split(".").mapNotNull { it.toIntOrNull() }
        if (a.isEmpty() || b.isEmpty()) return false
        val len = maxOf(a.size, b.size)
        for (i in 0 until len) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }
}
