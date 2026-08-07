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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.plexs.music.BuildConfig
import me.plexs.music.PlexApp
import me.plexs.music.data.api.StatsData
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.components.QrScanner
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted

val discordLogo: androidx.compose.ui.graphics.vector.ImageVector by lazy {
    androidx.compose.ui.graphics.vector.ImageVector.Builder(
        name = "Discord", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
        moveTo(20.317f, 4.369f)
        lineTo(18.05f, 3.0f)
        lineToRelative(-0.373f, 0.735f)
        lineTo(6.323f, 3.735f)
        lineTo(5.95f, 3.0f)
        lineTo(3.683f, 4.369f)
        lineTo(0.0f, 3.0f)
        verticalLineTo(22.0f)
        horizontalLineTo(5.45f)
        lineTo(6.94f, 20.2f)
        horizontalLineTo(17.06f)
        lineTo(18.55f, 22.0f)
        horizontalLineTo(24.0f)
        verticalLineTo(3.0f)
        close()
        moveTo(8.14f, 15.5f)
        curveTo(7.32f, 15.5f, 6.66f, 14.84f, 6.66f, 14.03f)
        curveTo(6.66f, 13.22f, 7.32f, 12.56f, 8.14f, 12.56f)
        curveTo(8.96f, 12.56f, 9.62f, 13.22f, 9.62f, 14.03f)
        curveTo(9.62f, 14.84f, 8.96f, 15.5f, 8.14f, 15.5f)
        close()
        moveTo(15.86f, 15.5f)
        curveTo(15.04f, 15.5f, 14.38f, 14.84f, 14.38f, 14.03f)
        curveTo(14.38f, 13.22f, 15.04f, 12.56f, 15.86f, 12.56f)
        curveTo(16.68f, 12.56f, 17.34f, 13.22f, 17.34f, 14.03f)
        curveTo(17.34f, 14.84f, 16.68f, 15.5f, 15.86f, 15.5f)
        close()
        moveTo(18.09f, 9.66f)
        curveTo(17.26f, 9.36f, 16.41f, 9.12f, 15.56f, 8.94f)
        curveTo(15.45f, 9.18f, 15.32f, 9.48f, 15.23f, 9.72f)
        curveTo(14.17f, 9.54f, 13.05f, 9.54f, 11.99f, 9.72f)
        curveTo(11.9f, 9.48f, 11.77f, 9.18f, 11.66f, 8.94f)
        curveTo(10.8f, 9.12f, 9.96f, 9.36f, 9.13f, 9.66f)
        curveTo(8.99f, 9.88f, 8.84f, 10.16f, 8.73f, 10.42f)
        curveTo(7.62f, 10.81f, 6.62f, 11.3f, 5.75f, 11.86f)
        curveTo(6.87f, 13.88f, 8.13f, 14.94f, 9.32f, 14.44f)
        curveTo(9.51f, 14.14f, 9.68f, 13.84f, 9.84f, 13.52f)
        curveTo(9.5f, 13.39f, 9.18f, 13.24f, 8.87f, 13.06f)
        curveTo(8.97f, 12.99f, 9.06f, 12.9f, 9.16f, 12.83f)
        curveTo(10.6f, 13.53f, 12.1f, 13.76f, 13.62f, 13.5f)
        curveTo(14.42f, 13.36f, 15.17f, 13.12f, 15.88f, 12.77f)
        curveTo(15.97f, 12.84f, 16.06f, 12.93f, 16.16f, 13.0f)
        curveTo(15.85f, 13.18f, 15.53f, 13.33f, 15.2f, 13.46f)
        curveTo(15.36f, 13.78f, 15.53f, 14.08f, 15.72f, 14.38f)
        curveTo(16.92f, 14.87f, 18.17f, 13.82f, 19.31f, 11.79f)
        curveTo(18.42f, 11.23f, 17.42f, 10.74f, 16.31f, 10.35f)
        curveTo(16.21f, 10.1f, 16.12f, 9.88f, 16.0f, 9.66f)
        lineTo(18.09f, 9.66f)
        close()
    }.build()
}

@Composable
fun SettingsScreen(services: PlexApp.Services, vm: AuthViewModel, onSignedOut: () -> Unit, onOpenOffline: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val offlineVersion by services.offline.version.collectAsState()
    val offlineSongs = remember(offlineVersion) { services.offline.list() }
    val user = services.session.user

    var showProfile by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(services.session.themeMode != "light") }
    var accent by remember { mutableStateOf(services.session.accentHex) }

    fun setTheme(newDark: Boolean, accentHex: String) {
        services.session.themeMode = if (newDark) "dark" else "light"
        services.session.accentHex = accentHex
        // Push to reactive state so the theme recomposes immediately (no restart).
        services.themeMode.value = if (newDark) "dark" else "light"
        services.accentHex.value = accentHex
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
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showPicker = true }) {
                    Text("Custom", color = PlexAccent)
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

        Button(
            onClick = {
                val i = android.content.Intent(android.content.Intent.ACTION_VIEW)
                i.data = android.net.Uri.parse("https://discord.gg/AQEUbdPX6p")
                runCatching { context.startActivity(i) }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5865F2),
                contentColor = Color.White,
            ),
        ) {
            Icon(discordLogo, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Contact Support", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        Text("Offline downloads", fontWeight = FontWeight.Bold, color = PlexMuted)
        Text(
            "${offlineSongs.size} song${if (offlineSongs.size != 1) "s" else ""} · ${services.offline.size()} stored",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onOpenOffline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) { Text("Manage offline downloads") }
        if (offlineSongs.isNotEmpty()) {
            OutlinedButton(
                onClick = { scope.launch { services.offline.deleteAll() } },
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
    if (showPicker) {
        me.plexs.music.ui.components.ColorPickerDialog(
            initialColor = me.plexs.music.ui.theme.accentColor(services.session.accentHex),
            onPick = { hex ->
                setTheme(dark, hex)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
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
    val linking = vm.loading
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Link to desktop") },
        text = {
            when {
                linking -> Text("Linking…", color = PlexMuted)
                else -> {
                    (vm.error ?: msg)?.let { Text(it, color = PlexMuted); Spacer(Modifier.height(8.dp)) }
                    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))) {
                        QrScanner(
                            onToken = { token ->
                                msg = null
                                vm.qrScan(token) {
                                    // Link succeeded — pull the desktop account's playlists/stats now.
                                    PlaybackController.refresh(ctx)
                                    onDismiss()
                                }
                            },
                            onError = { err -> msg = err },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(enabled = !linking, onClick = onDismiss) { Text("Close") } },
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