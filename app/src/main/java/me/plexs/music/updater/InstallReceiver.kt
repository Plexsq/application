package me.plexs.music.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.plexs.music.MainActivity
import me.plexs.music.R

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let { confirm ->
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirm)
            }
            return
        }
        val launcher = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (status == PackageInstaller.STATUS_SUCCESS) {
            "PLEX updated"
        } else {
            "Update failed"
        }
        val text = if (status == PackageInstaller.STATUS_SUCCESS) {
            "Tap to open the new version."
        } else {
            "The install didn't complete. Open Settings to retry."
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel("plex_updates", "Updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notif = NotificationCompat.Builder(context, "plex_updates")
            .setSmallIcon(R.drawable.ic_stat_plex)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(launcher)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1002, notif)
        } catch (_: SecurityException) {
        }
    }
}