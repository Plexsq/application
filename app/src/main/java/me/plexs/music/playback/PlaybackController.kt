package me.plexs.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.plexs.music.MainActivity

object PlaybackController {

    @Volatile
    var session: MediaSession? = null
        private set

    @Volatile
    private var player: ExoPlayer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    private val _hasItem = MutableStateFlow(false)
    val hasItem: StateFlow<Boolean> = _hasItem

    fun ensureSession(context: Context): MediaSession {
        session?.let { return it }
        val exo = ExoPlayer.Builder(context).build()
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
            }
        })
        player = exo

        val activityIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val s = MediaSession.Builder(context, exo)
            .setSessionActivity(activityIntent)
            .build()
        session = s
        startService(context)
        return s
    }

    fun play(context: Context, url: String, title: String, artist: String?) {
        val exo = player ?: run {
            ensureSession(context)
            player ?: return
        }
        startService(context)
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
        exo.setMediaItem(item)
        exo.prepare()
        exo.play()
        _hasItem.value = true
    }

    fun playPause() {
        val exo = player ?: return
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    fun release() {
        scope.coroutineContext[Job]?.cancel()
        session?.release()
        player?.release()
        session = null
        player = null
        _playing.value = false
        _hasItem.value = false
    }

    private fun startService(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
}
