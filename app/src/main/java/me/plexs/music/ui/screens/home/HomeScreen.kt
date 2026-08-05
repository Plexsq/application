package me.plexs.music.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import me.plexs.music.playback.PlaybackController
import me.plexs.music.innertube.InnertubeResolver
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.theme.PlexAccent

private const val SAMPLE_VIDEO_ID = "9bZkp7q19f0"

@Composable
fun HomeScreen(services: PlexApp.Services, vm: AuthViewModel, onSignedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var updateUrl by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var playError by remember { mutableStateOf<String?>(null) }
    val playing by PlaybackController.playing.collectAsState()
    val user = services.session.user

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        runCatching {
            val app = services.bootstrap.latest() ?: return@runCatching
            val latest = app.latestVersion ?: return@runCatching
            if (services.notifier.updateChecker.isNewer(BuildConfig.VERSION_NAME, latest)) {
                updateUrl = app.apkUrl ?: "https://github.com/Plexsq/plex-app/releases/latest"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("PLEX", color = PlexAccent, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        Text("Signed in as @${user?.username ?: "you"}", fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))

        updateUrl?.let { url ->
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("A new version of Plex is available.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }) { Text("Update") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text("Playback", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        playError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                resolving = true
                playError = null
                scope.launch {
                    val stream = runCatching {
                        val cfg = services.config.config()
                        InnertubeResolver().resolve(SAMPLE_VIDEO_ID, cfg.innertubeKey, cfg.innertubeClients)
                    }.getOrNull()
                    if (stream != null) {
                        PlaybackController.play(context, stream.url, "Sample track", "Plex")
                    } else {
                        playError = "Could not resolve a stream for this track."
                    }
                    resolving = false
                }
            },
            enabled = !resolving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (resolving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
            else Text(if (playing) "Playing… tap to pause" else "Play a sample track")
        }
        Spacer(Modifier.height(40.dp))

        OutlinedButton(
            onClick = { vm.signOut(onSignedOut) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign out") }
    }
}