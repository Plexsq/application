package me.plexs.music.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.plexs.music.data.api.ApiException
import me.plexs.music.data.api.User
import me.plexs.music.data.auth.AuthRepository
import me.plexs.music.data.session.SessionStore

class AuthViewModel(
    private val auth: AuthRepository,
    private val store: SessionStore,
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pendingEmail by mutableStateOf<String?>(null)
        private set

    private fun clearError() { error = null }

    fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: ApiException) {
                error = e.message
            } catch (_: Exception) {
                error = "Something went wrong. Try again."
            } finally {
                loading = false
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: (User) -> Unit) = run {
        val res = auth.signIn(email, password)
        res.user?.let(onSuccess) ?: throw ApiException("Sign-in failed")
    }

    fun signUp(name: String, username: String, email: String, password: String, onStep2: () -> Unit) = run {
        auth.signUp(name, username, email, password)
        pendingEmail = email
        onStep2()
    }

    fun verifySignup(email: String, otp: String, onSuccess: (User) -> Unit) = run {
        val res = auth.verifySignup(email, otp)
        res.user?.let(onSuccess) ?: throw ApiException("Verification failed")
    }

    fun forgot(email: String, onStep2: () -> Unit) = run {
        auth.forgotPassword(email)
        pendingEmail = email
        onStep2()
    }

    fun verifyOtp(email: String, otp: String, newPassword: String, onSuccess: () -> Unit) = run {
        auth.verifyOtp(email, otp, newPassword)
        onSuccess()
    }

    fun google(idToken: String, onSuccess: (User) -> Unit) = run {
        store.googleAccount = ""
        val res = auth.google(idToken)
        res.user?.let(onSuccess) ?: throw ApiException("Google sign-in failed")
    }

    fun signOut(onDone: () -> Unit) = run {
        auth.signOut()
        onDone()
    }
}