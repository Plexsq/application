package me.plexs.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import me.plexs.music.data.bootstrap.UpdateChecker

class UpgradeNotifier(context: Context) {

    private val appContext = context.applicationContext
    val updateChecker = UpdateChecker()

    fun notifyUpgrade(apkUrl: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "plex_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openUrl = PendingIntent.getActivity(
            appContext,
            0,
            Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl.ifBlank { "https://github.com/Plexsq/plex-app/releases/latest" })),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_plex)
            .setContentTitle("Plex has an update")
            .setContentText("A new version is available. Tap to download it.")
            .setContentIntent(openUrl)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(1001, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; the in-app banner covers it.
        }
    }
}