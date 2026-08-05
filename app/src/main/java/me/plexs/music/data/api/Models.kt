package me.plexs.music.data.api

import kotlinx.serialization.Serializable

@Serializable
data class InnertubeClient(
    val name: String,
    val version: String,
    val androidSdkVersion: Int,
)

@Serializable
data class AppConfig(
    val baseUrl: String,
    val googleClientId: String,
    val innertubeKey: String,
    val innertubeClients: List<InnertubeClient>,
    val latestVersion: String? = null,
)

@Serializable
data class BootstrapApp(val latestVersion: String? = null, val apkUrl: String? = null)

@Serializable
data class Bootstrap(val app: BootstrapApp? = null)

@Serializable
data class User(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val avatar: String? = null,
    val emailVerified: Boolean? = null,
)

@Serializable
data class Session(val id: String, val token: String? = null)

@Serializable
data class AuthResponse(
    val user: User? = null,
    val session: Session? = null,
    val pending: Boolean? = null,
    val email: String? = null,
)

@Serializable
data class AuthState(val user: User? = null, val session: Session? = null)

@Serializable
data class ApiError(val error: String? = null)
