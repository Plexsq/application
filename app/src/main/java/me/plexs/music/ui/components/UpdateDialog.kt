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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import me.plexs.music.PlexApp
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.updater.AppUpdater

@Composable
fun UpdateDialog(services: PlexApp.Services) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var waitingForPermission by remember { mutableStateOf(false) }

    // Code-based check: prompt when the release's built version code is NEWER than the
    // installed build. Works from any older build (betas included) regardless of names.
    suspend fun checkAvailable(): Boolean {
        val manifest = AppUpdater.latestManifest() ?: return false
        if (manifest.toCode > AppUpdater.installedVersionCode(context)) {
            label = "A new version of Plex is available — it installs in-app."
            return true
        }
        return false
    }
    LaunchedEffect(Unit) { if (checkAvailable()) show = true }

    fun start() {
        if (waitingForPermission && !AppUpdater.canInstallPermission(context)) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            )
            waitingForPermission = true
            return
        }
        busy = true
        failed = false
        progress = 0f
        label = "Checking for update…"
        AppUpdater.start(
            context,
            onProgress = { l, done, total ->
                label = l
                if (total > 0) progress = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            },
            onDone = { result ->
                busy = false
                when (result) {
                    AppUpdater.Result.AlreadyCurrent -> { if (waitingForPermission) waitingForPermission = false; show = false }
                    AppUpdater.Result.NeedsInstallPermission -> {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        )
                        waitingForPermission = true
                        show = false
                    }
                    AppUpdater.Result.InstallComplete -> { label = "Done — installing…"; progress = 1f }
                    is AppUpdater.Result.Failed -> { label = result.message ?: "Update failed"; failed = true }
                }
            },
        )
    }

    // If the user was sent to the "install unknown apps" settings page, auto-continue
    // the update the moment they come back (no need to find the dialog again).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !busy && waitingForPermission) {
                if (AppUpdater.canInstallPermission(context)) {
                    waitingForPermission = false
                    scope.launch { if (checkAvailable()) { show = true; start() } }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { if (!busy) show = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Update Plex", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(if (busy || failed) label else "A new version of Plex is available. It downloads in-app — no manual APK.")
                    if (busy && (progress > 0f || label.startsWith("Applying") || label.startsWith("Downloading"))) {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(0.5f),
                            color = PlexAccent,
                            strokeWidth = 4.dp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${(progress * 100).toInt()}%", color = PlexMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { start() },
                ) { Text(if (failed) "Retry" else "Update", color = PlexAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { show = false }) { Text("Later") }
            },
        )
    }
}