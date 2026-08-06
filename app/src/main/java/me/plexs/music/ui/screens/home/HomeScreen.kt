package me.plexs.music.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.plexs.music.PlexApp
import me.plexs.music.data.api.Song
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.screens.search.CardRow
import me.plexs.music.ui.screens.search.SongRow
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

@Composable
fun HomeScreen(services: PlexApp.Services, vm: AuthViewModel, onSignedOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by PlaybackController.favorites.collectAsState()
    val recent by PlaybackController.recentlyPlayed.collectAsState()
    val user = services.session.user

    val offline = services.offline
    val offlineVersion by offline.version.collectAsState()
    var offlineQuery by remember { mutableStateOf("") }
    val allOffline = remember(offlineVersion) { offline.list() }
    val offlineSongs = allOffline
        .filter {
            offlineQuery.isBlank() ||
                it.song.title.contains(offlineQuery, true) ||
                it.song.artist.contains(offlineQuery, true)
        }
        .map { it.song }
    val offlinePlayLists = remember(offlineVersion) { offline.playLists() }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(28.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "PLEX",
                color = PlexAccent,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Good ${timeGreeting()}, ${user?.username?.let { "@$it" } ?: "you"}",
                color = PlexMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(20.dp))

        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                LikedSongsHeader(favorites)
            }
            if (allOffline.isNotEmpty() || offlinePlayLists.isNotEmpty()) {
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            "Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${allOffline.size} song${if (allOffline.size != 1) "s" else ""} · ${offline.size()} stored",
                            color = PlexMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
                if (allOffline.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = offlineQuery,
                            onValueChange = { offlineQuery = it },
                            placeholder = { Text("Search offline") },
                            leadingIcon = {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.Search,
                                    contentDescription = null,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PlexAccent,
                                unfocusedBorderColor = PlexSurfaceVariant,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                if (offlineSongs.isNotEmpty()) {
                    itemsIndexed(offlineSongs) { i, song ->
                        SongRow(
                            song = song,
                            onPlay = {
                                PlaybackController.playSongs(context, offlineSongs, i)
                            },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                            downloaded = true,
                            onDelete = { scope.launch { offline.delete(song.id) } },
                        )
                    }
                }
            }
            if (offlinePlayLists.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Downloaded Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(offlinePlayLists) { pl ->
                    val plSongs = allOffline.filter { it.song.id in pl.songIds }.map { it.song }
                    CardRow(
                        thumbnail = plSongs.firstOrNull()?.thumbnail,
                        title = pl.title,
                        subtitle = "${pl.songIds.size} track${if (pl.songIds.size != 1) "s" else ""}",
                        onClick = {
                            if (plSongs.isNotEmpty()) PlaybackController.playSongs(context, plSongs, 0)
                        },
                    )
                }
            }
            if (recent.isNotEmpty()) {
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Text(
                        "Recently Played",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(recent) { i, song ->
                    SongRow(
                        song = song,
                        onPlay = {
                            PlaybackController.playSongs(context, recent, i)
                        },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                        downloaded = song.id in allOffline.map { it.song.id },
                        onDelete = { scope.launch { offline.delete(song.id) } },
                    )
                }
            }
            if (favorites.isNotEmpty()) {
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Text(
                        "Liked Songs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(favorites) { i, song ->
                    SongRow(
                        song = song,
                        onPlay = {
                            PlaybackController.playSongs(context, favorites, i)
                        },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                        downloaded = song.id in allOffline.map { it.song.id },
                        onDelete = { scope.launch { offline.delete(song.id) } },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            item {
                OutlinedButton(
                    onClick = { vm.signOut(onSignedOut) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) { Text("Sign out") }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LikedSongsHeader(favorites: List<Song>, onPlay: (() -> Unit)? = null) {
    val context = LocalContext.current
    val playBlock = onPlay ?: { PlaybackController.playSongs(context, favorites, 0) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PlexAccent)
            .clickable { playBlock() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = "Liked",
            tint = Color.White,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.width(14.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(
                "Liked Songs",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${favorites.size} song${if (favorites.size != 1) "s" else ""}",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
        )
    }
    if (favorites.isEmpty()) Spacer(Modifier.height(12.dp))
}

private fun timeGreeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h in 5..11 -> "morning"
        h in 12..17 -> "afternoon"
        else -> "evening"
    }
}