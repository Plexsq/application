package me.plexs.music.updater

import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val toVersion: String = "",
    val toCode: Long = 0,
    val fromVersion: String = "",
    val fromCode: Long = 0,
    val apkSize: Long = 0,
    val apkSha256: String = "",
    val apkUrl: String = "",
    val patchSize: Long = 0,
    val patchSha256: String = "",
    val patchUrl: String = "",
)