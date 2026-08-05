package me.plexs.music

import android.app.Application
import android.content.Context
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
        services = Services(this)
    }

    class Services(context: Context) {
        val session = SessionStore(context)
        val config = ConfigRepository()
        val auth = AuthRepository(session)
        val bootstrap = BootstrapRepository()
        val catalog = CatalogRepository(session)
    }
}