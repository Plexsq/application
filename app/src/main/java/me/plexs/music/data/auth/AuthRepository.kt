package me.plexs.music.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import me.plexs.music.data.api.ApiError
import me.plexs.music.data.api.ApiException
import me.plexs.music.data.api.AuthResponse
import me.plexs.music.data.api.AuthState
import me.plexs.music.data.api.Http
import me.plexs.music.data.session.SessionStore

class AuthRepository(private val store: SessionStore) {

    private val base = "https://music.plexs.me"

    suspend fun getSession(): AuthState = withContext(Dispatchers.IO) {
        if (!store.isSignedIn) return@withContext AuthState()
        Http.get("$base/api/auth/get-session", store.cookie()).use { resp ->
            if (!resp.isSuccessful) {
                if (resp.code == 401) {
                    // Token is invalid/expired — drop it so we don't keep trusting a stale account.
                    store.clear()
                }
                AuthState()
            } else {
                val state = Http.json.decodeFromString<AuthState>(resp.body!!.string())
                // Keep the cached user profile in sync so cold-start reflects the real account.
                state.user?.let { store.user = it }
                state
            }
        }
    }

    suspend fun signIn(email: String, password: String): AuthResponse =
        post("$base/api/auth/sign-in/email", SignInRequest(email.trim(), password))

    suspend fun signUp(name: String, username: String, email: String, password: String): AuthResponse =
        post("$base/api/auth/sign-up/email", SignUpRequest(name.trim(), username.trim(), email.trim(), password))

    suspend fun verifySignup(email: String, otp: String): AuthResponse =
        post("$base/api/auth/verify-signup", VerifySignupRequest(email.trim(), otp.trim()))

    suspend fun forgotPassword(email: String): AuthResponse =
        post("$base/api/auth/forgot-password", ForgotPasswordRequest(email.trim()))

    suspend fun verifyOtp(email: String, otp: String, newPassword: String): AuthResponse =
        post("$base/api/auth/verify-otp", VerifyOtpRequest(email.trim(), otp.trim(), newPassword))

    suspend fun google(idToken: String): AuthResponse =
        post("$base/api/auth/google", GoogleSignInRequest(idToken))

    suspend fun qrSwap(token: String): AuthResponse =
        post("$base/api/qr/swap", QrSwapRequest(token))

    suspend fun updateProfile(name: String, username: String): AuthResponse =
        withContext(Dispatchers.IO) {
            Http.post("$base/api/profile", mapOf("name" to name.trim(), "username" to username.trim()), store.cookie()).use { r ->
                if (!r.isSuccessful) {
                    val msg = runCatching { Http.json.decodeFromString<ApiError>(r.body!!.string()) }.getOrNull()?.error
                    throw ApiException(msg ?: "Profile update failed")
                }
                val res = Http.json.decodeFromString<AuthResponse>(r.body!!.string())
                res.user?.let { store.user = it }
                res
            }
        }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            runCatching { Http.post("$base/api/auth/sign-out", mapOf("ok" to true), store.cookie()).close() }
            store.clear()
        }
    }

    private suspend inline fun <reified T> post(url: String, body: T): AuthResponse =
        withContext(Dispatchers.IO) {
            Http.post(url, body).use { r ->
                if (!r.isSuccessful) {
                    val msg = runCatching { Http.json.decodeFromString<ApiError>(r.body!!.string()) }.getOrNull()?.error
                    throw ApiException(msg ?: "Something went wrong")
                }
                val res = Http.json.decodeFromString<AuthResponse>(r.body!!.string())
                r.headers("Set-Cookie").firstOrNull { it.startsWith("p_token=") }?.let { c ->
                    store.token = c.substringAfter("p_token=").substringBefore(";")
                }
                res.user?.let { store.user = it }
                res
            }
        }
}
