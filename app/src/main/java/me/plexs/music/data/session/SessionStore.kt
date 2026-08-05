package me.plexs.music.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import me.plexs.music.data.api.Http
import me.plexs.music.data.api.User

class SessionStore(context: Context) {

    private val prefs: android.content.SharedPreferences by lazy {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "plex_session",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(v) = prefs.edit().putString(KEY_TOKEN, v).apply()

    var user: User?
        get() = prefs.getString(KEY_USER, null)?.let { runCatching { Http.json.decodeFromString<User>(it) }.getOrNull() }
        set(v) = prefs.edit().putString(KEY_USER, v?.let { Http.json.encodeToString(User.serializer(), it) }).apply()

    var googleAccount: String?
        get() = prefs.getString(KEY_GOOGLE, null)
        set(v) = prefs.edit().putString(KEY_GOOGLE, v).apply()

    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun cookie(): String? = token?.let { "p_token=$it" }

    private companion object {
        const val KEY_TOKEN = "p_token"
        const val KEY_USER = "user"
        const val KEY_GOOGLE = "google_account"
    }
}
