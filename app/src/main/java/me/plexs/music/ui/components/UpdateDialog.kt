package me.plexs.music.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.plexs.music.BuildConfig
import me.plexs.music.PlexApp
import me.plexs.music.ui.theme.PlexAccent

@Composable
fun UpdateDialog(services: PlexApp.Services) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var apkUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val app = runCatching { services.bootstrap.latest() }.getOrNull() ?: return@LaunchedEffect
        val latest = app.latestVersion ?: return@LaunchedEffect
        val current = BuildConfig.VERSION_NAME
        val a = current.split(".").mapNotNull { it.toIntOrNull() }
        val b = latest.split(".").mapNotNull { it.toIntOrNull() }
        var newer = false
        if (a.isNotEmpty() && b.isNotEmpty()) {
            val len = maxOf(a.size, b.size)
            for (i in 0 until len) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) { newer = x < y; break }
            }
        }
        if (newer) {
            apkUrl = app.apkUrl ?: "https://github.com/Plexsq/application/releases/latest"
            show = true
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("A new version of Plex is available", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Update to the latest build to get new features and fixes.")
            },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
                }) {
                    Text("Update", color = PlexAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) {
                    Text("Later")
                }
            },
        )
    }
}
