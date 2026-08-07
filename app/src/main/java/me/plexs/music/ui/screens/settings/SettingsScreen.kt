package me.plexs.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.plexs.music.BuildConfig
import me.plexs.music.PlexApp
import me.plexs.music.data.api.StatsData
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.components.QrScanner
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted

@Composable
fun SettingsScreen(services: PlexApp.Services, vm: AuthViewModel, onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val offlineVersion by services.offline.version.collectAsState()
    val offlineSongs = remember(offlineVersion) { services.offline.list() }
    val user = services.session.user

    var showProfile by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(services.session.themeMode != "light") }
    var accent by remember { mutableStateOf(services.session.accentHex) }

    fun setTheme(newDark: Boolean, accentHex: String) {
        services.session.themeMode = if (newDark) "dark" else "light"
        services.session.accentHex = accentHex
        dark = newDark
        accent = accentHex
    }

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
        user?.let {
            Text(it.name ?: it.email ?: "", style = MaterialTheme.typography.bodyMedium, color = PlexMuted)
        }
        OutlinedButton(
            onClick = { showProfile = true },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { Text("Edit profile") }

        Spacer(Modifier.height(24.dp))

        Text("Appearance", fontWeight = FontWeight.Bold, color = PlexMuted)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Theme", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { setTheme(true, accent) }) { Text("Dark", fontWeight = if (dark) FontWeight.Bold else FontWeight.Normal) }
            TextButton(onClick = { setTheme(false, accent) }) { Text("Light", fontWeight = if (!dark) FontWeight.Bold else FontWeight.Normal) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Accent", color = PlexMuted)
            Spacer(Modifier.weight(1f))
            val presets = listOf("#A855F7", "#60A5FA", "#34D399", "#F472B6", "#FBBF24", "#22D3EE")
            Row(verticalAlignment = Alignment.CenterVertically) {
                presets.forEach { hex ->
                    val c = me.plexs.music.ui.theme.accentColor(hex)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c)
                            .clickable { setTheme(dark, hex) },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Link to desktop", fontWeight = FontWeight.Bold, color = PlexMuted)
        Text(
            "Scan a QR from your desktop Plex app to link this device to your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = PlexMuted,
        )
        OutlinedButton(
            onClick = { showQr = true },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { Text("Scan QR") }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                val i = android.content.Intent(android.content.Intent.ACTION_VIEW)
                i.data = android.net.Uri.parse("https://discord.gg/AQEUbdPX6p")
                runCatching { context.startActivity(i) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Join the Discord") }

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

        StatisticsSection(services)

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

    if (showProfile) {
        ProfileEditDialog(services, vm) { showProfile = false }
    }
    if (showQr) {
        QrLinkDialog(services, vm) { showQr = false }
    }
}

@Composable
private fun ProfileEditDialog(services: PlexApp.Services, vm: AuthViewModel, onDismiss: () -> Unit) {
    val user = services.session.user
    var name by remember { mutableStateOf(user?.name ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                saving = true
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    runCatching { services.auth.updateProfile(name, username) }
                        .onSuccess { onDismiss() }
                        .onFailure { error = it.message ?: "Update failed" }
                    saving = false
                }
            }) { Text(if (saving) "Saving…" else "Save", color = PlexAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QrLinkDialog(services: PlexApp.Services, vm: AuthViewModel, onDismiss: () -> Unit) {
    var linking by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!linking) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Link to desktop") },
        text = {
            if (linking) {
                Text("Linking…", color = PlexMuted)
            } else {
                msg?.let { Text(it, color = PlexMuted); Spacer(Modifier.height(8.dp)) }
                Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))) {
                    QrScanner(onToken = { token ->
                        linking = true
                        vm.qrScan(token) { onDismiss() }
                    }, onError = { err -> msg = err })
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { if (!linking) onDismiss() }) { Text("Close") } },
    )
}

private fun fmtMinutes(m: Long): String {
    val min = m / 60
    return if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"
}

@Composable
private fun StatisticsSection(services: PlexApp.Services) {
    var stats by remember { mutableStateOf<StatsData?>(null) }
    LaunchedEffect(Unit) {
        stats = services.stats.fetch()
    }
    Text("Statistics", fontWeight = FontWeight.Bold, color = PlexMuted)
    val s = stats
    if (s == null) {
        Text("Loading…", style = MaterialTheme.typography.bodyMedium, color = PlexMuted)
    } else {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    fmtMinutes(s.daily),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("Today", style = MaterialTheme.typography.bodySmall, color = PlexMuted)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    fmtMinutes(s.monthly),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("This month", style = MaterialTheme.typography.bodySmall, color = PlexMuted)
            }
        }
        if (s.top.isNotEmpty()) {
            Text("Top songs", fontWeight = FontWeight.Bold, color = PlexMuted, modifier = Modifier.padding(top = 12.dp))
            s.top.forEachIndexed { i, e ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlexMuted,
                        modifier = Modifier.width(20.dp),
                    )
                    coil.compose.AsyncImage(
                        model = e.song?.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .width(28.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        e.song?.title ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${e.plays} play${if (e.plays != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PlexMuted,
                    )
                }
            }
        } else {
            Text(
                "Keep listening to see your top songs",
                style = MaterialTheme.typography.bodyMedium,
                color = PlexMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}