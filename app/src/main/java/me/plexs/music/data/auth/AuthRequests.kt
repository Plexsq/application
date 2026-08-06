package me.plexs.music.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class SignUpRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
)

@Serializable
data class VerifySignupRequest(val email: String, val otp: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class VerifyOtpRequest(val email: String, val otp: String, val newPassword: String)

@Serializable
data class GoogleSignInRequest(val idToken: String)

@Serializable
data class QrSwapRequest(val token: String)
