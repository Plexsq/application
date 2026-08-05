package me.plexs.music.playback

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import me.plexs.music.R

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        mediaSession = PlaybackController.ensureSession(this)
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("playback")
            .setChannelName(R.string.playback_channel_name)
            .build()
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { if (it.playWhenReady) it.pause() }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession = null
        super.onDestroy()
    }
}