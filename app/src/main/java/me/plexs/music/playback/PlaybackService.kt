package me.plexs.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.plexs.music.MainActivity
import me.plexs.music.R

class PlaybackService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        PlaybackController.attachNotificationCallback { startForeground(NOTIFICATION_ID, buildNotification()) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlaybackController.playPause()
            ACTION_NEXT -> PlaybackController.next()
            ACTION_PREV -> PlaybackController.previous()
            ACTION_STOP -> {
                PlaybackController.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val song = PlaybackController.currentSong
        val playing = PlaybackController.playing.value

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val playPause = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = PendingIntent.getService(
            this, 2,
            Intent(this, PlaybackService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val prev = PendingIntent.getService(
            this, 3,
            Intent(this, PlaybackService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_plex)
            .setContentTitle(song?.title ?: "Plex")
            .setContentText(song?.artist ?: "")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(R.drawable.ic_stat_plex, "Previous", prev)
            .addAction(R.drawable.ic_stat_plex, if (playing) "Pause" else "Play", playPause)
            .addAction(R.drawable.ic_stat_plex, "Next", next)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

        largeIcon = null
        val thumb = song?.thumbnail
        if (thumb != null) {
            val nm = NotificationManagerCompat.from(this)
            Thread {
                val bmp = loadBitmap(thumb)
                if (bmp != null) {
                    largeIcon = bmp
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotifPermission()) {
                        nm.notify(NOTIFICATION_ID, builder.setLargeIcon(bmp).build())
                    }
                }
            }.start()
        }
        return builder.build()
    }

    @Volatile
    private var largeIcon: android.graphics.Bitmap? = null

    private fun hasNotifPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun loadBitmap(url: String): android.graphics.Bitmap? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doInput = true
            conn.connect()
            val stream = conn.inputStream
            val bmp = android.graphics.BitmapFactory.decodeStream(stream)
            stream.close()
            conn.disconnect()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackController.pause()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        PlaybackController.detachNotificationCallback()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_PAUSE = "me.plexs.music.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "me.plexs.music.ACTION_NEXT"
        const val ACTION_PREV = "me.plexs.music.ACTION_PREV"
        const val ACTION_STOP = "me.plexs.music.ACTION_STOP"
    }
}