package me.plexs.music.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.plexs.music.BuildConfig
import me.plexs.music.PlexApp
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.updater.AppUpdater

@Composable
fun UpdateDialog(services: PlexApp.Services) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var waitingForPermission by remember { mutableStateOf(false) }

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
            label = "A new version of Plex is available — it installs in-app."
            show = true
        }
    }

    fun start() {
        busy = true
        failed = false
        progress = 0f
        label = "Checking for update…"
        scope.launch {
            AppUpdater.run(context,
                onProgress = { l, done, total ->
                    label = l
                    if (total > 0) progress = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                },
                onDone = { result ->
                    busy = false
                    when (result) {
                        AppUpdater.Result.AlreadyCurrent -> { show = false }
                        AppUpdater.Result.NeedsInstallPermission -> {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}"),
                                )
                            )
                            waitingForPermission = true
                            show = false
                        }
                        AppUpdater.Result.InstallComplete -> { label = "Done"; progress = 1f }
                        is AppUpdater.Result.Failed -> {
                            label = result.message
                            failed = true
                        }
                    }
                },
            )
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { if (!busy) show = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Update Plex", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(if (busy || failed) label else "A new version of Plex is available. Updating in-app downloads a small patch instead of the full app when possible.")
                    if (busy && (progress > 0f || label.startsWith("Applying"))) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = PlexAccent,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${(progress * 100).toInt()}%",
                            color = PlexMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { if (failed) start() else show = false },
                ) { Text(if (failed) "Retry" else "Update", color = PlexAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { show = false }) { Text("Later") }
            },
        )
    }
}