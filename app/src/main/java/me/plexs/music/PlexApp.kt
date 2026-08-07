package me.plexs.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import me.plexs.music.data.api.ConfigRepository
import me.plexs.music.data.api.CatalogRepository
import me.plexs.music.data.auth.AuthRepository
import me.plexs.music.data.bootstrap.BootstrapRepository
import me.plexs.music.data.session.SessionStore

class PlexApp : Application() {

    lateinit var services: Services
        private set

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashLogger.capture(this, "uncaught:${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "playback",
                "Playback",
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
        services = Services(this)
    }

    class Services(context: Context) {
        val session = SessionStore(context)
        val config = ConfigRepository()
        val auth = AuthRepository(session)
        val bootstrap = BootstrapRepository()
        val catalog = CatalogRepository(session)
        val userData = me.plexs.music.data.api.UserDataRepository(session)
        val stats = me.plexs.music.data.api.StatsRepository(session)
        val offline = me.plexs.music.data.offline.OfflineRepository(context)
        val downloads = me.plexs.music.data.offline.DownloadManager(offline)
        val playlists = me.plexs.music.data.playlists.PlaylistStore(context)

        // Reactive theme state so Light/Dark + accent apply live without a restart.
        val themeMode = kotlinx.coroutines.flow.MutableStateFlow(session.themeMode)
        val accentHex = kotlinx.coroutines.flow.MutableStateFlow(session.accentHex)
    }
}