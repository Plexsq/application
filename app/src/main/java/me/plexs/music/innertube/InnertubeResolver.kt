package me.plexs.music.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.selects.select
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import me.plexs.music.data.api.Http
import me.plexs.music.data.api.InnertubeClient

data class Stream(val url: String, val mimeType: String, val bitrate: Long, val itag: Int)

class InnertubeResolver {

    suspend fun resolve(videoId: String, innertubeKey: String, clients: List<InnertubeClient>): Stream? =
        withContext(Dispatchers.IO) {
            // Resolve every client in parallel (async) so a slow/banned first client
            // doesn't burn the whole budget sequentially — whichever succeeds first wins.
            val deferreds = clients.map { client ->
                async { runCatching { resolveWith(client, videoId, innertubeKey) }.getOrNull() }
            }
            val pending = deferreds.toMutableList()
            while (pending.isNotEmpty()) {
                val result = select<Stream?> {
                    pending.forEach { d -> d.onAwait { it } }
                }
                if (result != null) return@withContext result
                pending.removeAll { it.isCompleted }
            }
            null
        }

    private fun resolveWith(client: InnertubeClient, videoId: String, key: String): Stream? {
        val url = "https://www.youtube.com/youtubei/v1/player?key=$key&prettyPrint=false"
        val body = Http.json.encodeToString(
            buildJsonClient(client, videoId)
        ).toRequestBody("application/json".toMediaType())

        val req = Request.Builder().url(url).post(body).build()
        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val root = Json.parseToJsonElement(resp.body!!.string()).jsonObject
            val status = root["playabilityStatus"]?.jsonObject?.get("status")?.jsonPrimitive?.content
            if (status != "OK") return null
            val adaptive = root["streamingData"]?.jsonObject?.get("adaptiveFormats")?.jsonArray ?: return null
            return bestAudio(adaptive)
        }
    }

    private fun bestAudio(array: kotlinx.serialization.json.JsonArray): Stream? {
        var best: Stream? = null
        for (el in array) {
            val o = el.jsonObject
            val mime = o["mimeType"]?.jsonPrimitive?.content ?: continue
            if (!mime.startsWith("audio/")) continue
            val streamUrl = o["url"]?.jsonPrimitive?.content ?: continue
            val bitrate = o["bitrate"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val itag = o["itag"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            if (best == null || bitrate > best.bitrate) {
                best = Stream(streamUrl, mime, bitrate, itag)
            }
        }
        return best
    }

    private fun buildJsonClient(client: InnertubeClient, videoId: String): JsonObject {
        val clientObj = buildString {
            append("{\"clientName\":\"").append(client.name)
                .append("\",\"clientVersion\":\"").append(client.version)
                .append("\",\"androidSdkVersion\":").append(client.androidSdkVersion)
                .append(",\"hl\":\"en\",\"gl\":\"US\"}")
        }
        val json = """
            {
              "context": { "client": $clientObj },
              "videoId": "$videoId",
              "contentCheckOk": true,
              "racyCheckOk": true
            }
        """.trimIndent()
        return Json.parseToJsonElement(json).jsonObject
    }
}