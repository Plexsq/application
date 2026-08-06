package me.plexs.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.plexs.music.BuildConfig
import me.plexs.music.PlexApp
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted

@Composable
fun SettingsScreen(services: PlexApp.Services, vm: AuthViewModel, onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offlineVersion by services.offline.version.collectAsState()
    val offlineSongs = remember(offlineVersion) { services.offline.list() }
    val user = services.session.user

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 20.dp),
        )

        Text("Account", fontWeight = FontWeight.Bold, color = PlexMuted, modifier = Modifier.padding(top = 8.dp))
        val display = user?.let { "@${it.username}" } ?: "Signed out"
        Text(display, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(24.dp))

        Text("Offline downloads", fontWeight = FontWeight.Bold, color = PlexMuted)
        Text(
            "${offlineSongs.size} song${if (offlineSongs.size != 1) "s" else ""} · ${services.offline.size()} stored",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (offlineSongs.isNotEmpty()) {
            OutlinedButton(
                onClick = { scope.launch { runCatching { services.offline.deleteAll() } } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) { Text("Clear offline downloads") }
        }

        Spacer(Modifier.height(24.dp))

        Text("About", fontWeight = FontWeight.Bold, color = PlexMuted)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Version", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Text(BuildConfig.VERSION_NAME, color = PlexMuted)
        }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = { vm.signOut(onSignedOut) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign out", color = PlexAccent, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(24.dp))
    }
}