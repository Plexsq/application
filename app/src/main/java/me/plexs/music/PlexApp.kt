package me.plexs.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.plexs.music.data.api.ConfigRepository
import me.plexs.music.data.auth.AuthRepository
import me.plexs.music.data.bootstrap.BootstrapRepository
import me.plexs.music.data.session.SessionStore

class PlexApp : Application() {

    lateinit var services: Services
        private set

    override fun onCreate() {
        super.onCreate()
        services = Services(this)
        services.checkForUpdate()
    }

    class Services(context: Context) {
        val session = SessionStore(context)
        val config = ConfigRepository()
        val auth = AuthRepository(session)
        val bootstrap = BootstrapRepository()
        val notifier = UpgradeNotifier(context)
        private val scope = CoroutineScope(Dispatchers.IO)

        fun checkForUpdate() {
            val current = BuildConfig.VERSION_NAME
            scope.launch {
                runCatching {
                    val app = bootstrap.latest() ?: return@runCatching
                    val latest = app.latestVersion ?: return@runCatching
                    if (notifier.updateChecker.isNewer(current, latest)) {
                        notifier.notifyUpgrade(apkUrl = app.apkUrl ?: "")
                    }
                }
            }
        }
    }
}