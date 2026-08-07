package me.plexs.music.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the mobile models can parse the server's user-data + stats payloads
 * (the web stores float durations like 196.101 — Song.duration is an Int).
 * This is the regression gate for "stats stuck on Loading" and
 * "playlists/liked/recents don't sync after QR". Shapes mirror the live data.
 */
class SyncDecodeTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun userDataWithFloatDurationsDecodes() {
        val data = """
        {"data": {
          "queue": [
            {"id":"KRRoQkkwuJE","type":"song","title":"Du hast","artist":"Rammstein","artistId":"464","album":"Sehnsucht","duration":235,"durationText":"3:55","thumbnail":"https://x"},
            {"id":"V9PVRfjEBTI","type":"song","title":"BIRDS OF A FEATHER","artist":"Billie Eilish","duration":196.821,"durationText":"3:50","thumbnail":"https://x"}
          ],
          "queueIndex": 0,
          "favorites": [
            {"id":"ZXjNFRjLOi_k","title":"Sehnsucht","artist":"Rammstein","thumbnail":"https://x","duration":244.441,"durationText":"4:04"},
            {"id":"AeF4cXJcUjY","title":"FEAR","artist":"Billie Eilish","thumbnail":"https://x","duration":268.541,"durationText":"4:28"}
          ],
          "recentlyPlayed": [
            {"id":"Z15LgaXxPQs","title":"Its A Dream","artist":"Snow Strippers","thumbnail":"https://x","duration":215.0,"durationText":"3:35"}
          ],
          "playlists": [
            {"id":"p-55be3fa8","shortId":"55be3","name":"Billie","createdAt":1785365878438,
             "songs":[{"id":"fDcrRJI_H04","title":"when the party's over","artist":"Billie Eilish","thumbnail":"https://x","duration":196.101,"durationText":"3:16"}]}
          ],
          "playtime": {"daily":19880,"monthly":85270},
          "play_counts": {"jIRLTIpIe4o":5,"GqX7p3fBzu4":4}
        }}
        """.trimIndent()
        val d = json.decodeFromString<UserDataResp>(data).data
        assertEquals(2, d.queue.size)
        assertEquals(2, d.favorites.size)
        assertEquals(1, d.recentlyPlayed.size)
        assertEquals(1, d.playlists.size)
        assertEquals(235, d.queue[0].duration)
        assertEquals(196, d.queue[1].duration)
        assertEquals(244, d.favorites[0].duration)
        assertEquals(268, d.favorites[1].duration)
        assertEquals(215, d.recentlyPlayed[0].duration)
        assertEquals(196, d.playlists[0].songs[0].duration)
        assertEquals(19880L, d.playtime.daily)
        assertEquals(85270L, d.playtime.monthly)
        assertEquals(5, d.play_counts["jIRLTIpIe4o"])
        assertTrue(d.playlists[0].id.startsWith("p"))
        assertEquals("Billie", d.playlists[0].name)
    }

    @Test
    fun statsPayloadDecodes() {
        val data = """
        {"stats": {
          "daily": 19880,
          "monthly": 85270,
          "top": [
            {"song":{"id":"fDcrRJI_H04","title":"when the party's over","artist":"Billie Eilish","thumbnail":"https://x","duration":196.101,"durationText":"3:16"},"plays":32},
            {"song":null,"plays":4}
          ]
        }}
        """.trimIndent()
        val s = json.decodeFromString<StatsResp>(data).stats
        assertEquals(19880L, s.daily)
        assertEquals(85270L, s.monthly)
        assertEquals(2, s.top.size)
        assertEquals(196, s.top[0].song?.duration)
        assertEquals(32, s.top[0].plays)
        assertNull(s.top[1].song)
    }
}