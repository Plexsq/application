package me.plexs.music.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media3 [MediaSessionService]. Handing the [MediaSession] to the media3 framework
 * makes it publish a proper MediaStyle media notification for us: real transport
 * icons (play/pause/previous/next), a seek bar (API 29+) and, on Android 13+,
 * system-wide media controls — all kept in sync automatically. Shuffle & repeat
 * are exposed as extra media buttons matching the in-app Now Playing controls.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var currentSession: androidx.media3.session.MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Reuse the session the controller owns (same ExoPlayer instance).
        currentSession = PlaybackController.session
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        currentSession ?: PlaybackController.session

    override fun onBind(intent: android.content.Intent?): android.os.IBinder? =
        super.onBind(intent)

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        // Add the controller's session to this service the moment it exists. media3 only
        // foregrounds the media notification for sessions that are ADDED to the service;
        // onCreate() may run before the session is created (null), so (re)attach here on
        // every start so the MediaStyle notification with transport controls appears.
        PlaybackController.session?.let { s ->
            if (!isSessionAdded(s)) addSession(s)
        }
        // Shuffle/repeat/more came through as the ACTION_* intents before the MediaSession
        // takeover; now they ride the session's media buttons instead. Keep them for forward
        // compatibility in case an old PendingIntent still pokes the service.
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlaybackController.playPause()
            ACTION_NEXT -> PlaybackController.next()
            ACTION_PREV -> PlaybackController.previous()
            ACTION_SHUFFLE -> PlaybackController.toggleShuffle()
            ACTION_REPEAT -> PlaybackController.cycleRepeat()
            ACTION_STOP -> {
                PlaybackController.pause()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        PlaybackController.pause()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        currentSession = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "playback"
        const val ACTION_PLAY_PAUSE = "me.plexs.music.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "me.plexs.music.ACTION_NEXT"
        const val ACTION_PREV = "me.plexs.music.ACTION_PREV"
        const val ACTION_SHUFFLE = "me.plexs.music.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "me.plexs.music.ACTION_REPEAT"
        const val ACTION_STOP = "me.plexs.music.ACTION_STOP"

        const val CMD_SHUFFLE = "me.plexs.music.cmd.shuffle"
        const val CMD_REPEAT = "me.plexs.music.cmd.repeat"
    }
}