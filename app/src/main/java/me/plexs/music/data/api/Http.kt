package me.plexs.music.data.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

object Http {
    const val CONFIG_URL = "https://plex-meta.urdonkey6.workers.dev/api/app/config"
    const val BOOTSTRAP_URL = "https://music.plexs.me/api/app/bootstrap"

    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    val contentType = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun get(url: String, cookie: String? = null): okhttp3.Response {
        val b = Request.Builder().url(url).get()
        if (cookie != null) b.header("Cookie", cookie)
        return client.newCall(b.build()).execute()
    }

    inline fun <reified T> post(url: String, body: T, cookie: String? = null): okhttp3.Response {
        val b = Request.Builder().url(url)
            .post(json.encodeToString(body).toRequestBody(contentType))
        if (cookie != null) b.header("Cookie", cookie)
        return client.newCall(b.build()).execute()
    }

    inline fun <reified T> parse(body: String): T = json.decodeFromString(body)
}
