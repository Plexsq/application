package me.plexs.music.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import me.plexs.music.data.api.Http
import me.plexs.music.data.api.User

/**
 * Session storage backed by EncryptedSharedPreferences, with a safe fallback to
 * plain SharedPreferences if the encrypted store ever becomes unreadable (for
 * example after an in-place update where the Android Keystore key can no longer
 * decrypt, which previously made the whole app appear corrupt until data was
 * cleared). Reads and writes never throw — a broken store degrades to the plain
 * fallback instead of crashing the app.
 */
class SessionStore(context: Context) {

    private val plainPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("plex_session_plain", Context.MODE_PRIVATE)
    }

    private val encryptedPrefs: SharedPreferences? by lazy {
        runCatching {
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
        }.getOrNull()
    }

    private fun read(key: String): String? {
        encryptedPrefs?.let { enc ->
            runCatching { enc.getString(key, null)?.let { return it } }
        }
        return runCatching { plainPrefs.getString(key, null) }.getOrNull()
    }

    private fun write(key: String, value: String?) {
        encryptedPrefs?.let { enc ->
            runCatching { enc.edit().putString(key, value).apply(); return }
        }
        runCatching { plainPrefs.edit().putString(key, value).apply() }
    }

    var token: String?
        get() = read(KEY_TOKEN)
        set(v) = write(KEY_TOKEN, v)

    var user: User?
        get() = read(KEY_USER)?.let { runCatching { Http.json.decodeFromString<User>(it) }.getOrNull() }
        set(v) = write(KEY_USER, v?.let { Http.json.encodeToString(User.serializer(), it) })

    var googleAccount: String?
        get() = read(KEY_GOOGLE)
        set(v) = write(KEY_GOOGLE, v)

    var themeMode: String
        get() = read(KEY_THEME) ?: "dark"
        set(v) = write(KEY_THEME, v)

    var accentHex: String
        get() = read(KEY_ACCENT) ?: "#A855F7"
        set(v) = write(KEY_ACCENT, v)

    val isSignedIn: Boolean get() = !token.isNullOrBlank()

    fun clear() {
        runCatching { encryptedPrefs?.edit()?.clear()?.apply() }
        runCatching { plainPrefs.edit().clear().apply() }
    }

    fun cookie(): String? = token?.let { "p_token=$it" }

    private companion object {
        const val KEY_TOKEN = "p_token"
        const val KEY_USER = "user"
        const val KEY_GOOGLE = "google_account"
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent_hex"
    }
}