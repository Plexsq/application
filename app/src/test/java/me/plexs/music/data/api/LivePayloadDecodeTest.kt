package me.plexs.music.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes the ACTUAL live /api/user-data payload (captured from the desktop account
 * on the production server) through the real mobile models. This is the proof gate
 * for playback-statistics/sync: if this passes, decoding is NOT the blocker.
 */
class LivePayloadDecodeTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun fullLiveUserDataDecodes() {
        val text = javaClass.getResourceAsStream("/live_user_data.json")!!
            .readBytes().toString(Charsets.UTF_8)
        val resp = json.decodeFromString<UserDataResp>(text)
        val d = resp.data
        assertTrue("favorites should exist", d.favorites.isNotEmpty())
        assertTrue("recentlyPlayed should exist", d.recentlyPlayed.isNotEmpty())
        assertTrue("playlists should exist", d.playlists.isNotEmpty())
        assertEquals(100, d.recentlyPlayed.size)
        assertTrue("floats truncated", d.favorites.all { it.duration in 0..7200 })
        d.playlists.forEach { p ->
            assertTrue(p.name.isNotBlank())
            assertTrue(p.songs.all { it.duration in 0..7200 })
        }
    }
}