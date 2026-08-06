package me.plexs.music.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import me.plexs.music.PlexApp
import me.plexs.music.data.api.Song
import me.plexs.music.data.playlists.UserPlaylist
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.components.CreatePlaylistDialog
import me.plexs.music.ui.components.SongPlaylistPickerDialog
import me.plexs.music.ui.navigation.Destinations
import me.plexs.music.ui.screens.search.SongRow
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

@Composable
fun HomeScreen(services: PlexApp.Services, onOpenSection: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by PlaybackController.favorites.collectAsState()
    val recent by PlaybackController.recentlyPlayed.collectAsState()
    val playlistsVersion by services.playlists.version.collectAsState()
    val playlists = remember(playlistsVersion) { services.playlists.list() }

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

    var showCreate by remember { mutableStateOf(false) }
    var deletePl by remember { mutableStateOf<UserPlaylist?>(null) }
    var addTarget by remember { mutableStateOf<Song?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Your Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PlexAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Playlist", color = PlexAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        LibraryTile(
                            thumbnail = recent.firstOrNull()?.thumbnail,
                            icon = Icons.Default.History,
                            label = "Recently Played",
                            sub = "${recent.size} song${if (recent.size != 1) "s" else ""}",
                            onClick = { onOpenSection(Destinations.RECENTS) },
                        )
                    }
                    item {
                        LibraryTile(
                            thumbnail = favorites.firstOrNull()?.thumbnail,
                            icon = Icons.Default.Favorite,
                            label = "Liked Songs",
                            sub = "${favorites.size} song${if (favorites.size != 1) "s" else ""}",
                            onClick = { onOpenSection(Destinations.LIKED) },
                        )
                    }
                    item {
                        LibraryTile(
                            thumbnail = null,
                            icon = Icons.Default.Download,
                            label = "Offline",
                            sub = "${allOffline.size} song${if (allOffline.size != 1) "s" else ""}",
                            onClick = { },
                            accent = false,
                        )
                    }
                    items(playlists) { pl ->
                        LibraryTile(
                            thumbnail = pl.songs.firstOrNull()?.thumbnail ?: "",
                            icon = if (pl.songs.isEmpty()) Icons.Default.PlaylistAdd else null,
                            label = pl.name,
                            sub = "${pl.songs.size} song${if (pl.songs.size != 1) "s" else ""}",
                            onClick = {
                                if (pl.songs.isNotEmpty()) {
                                    PlaybackController.playSongs(context, pl.songs, 0)
                                }
                            },
                            onLongClick = { deletePl = pl },
                        )
                    }
                    item {
                        NewPlaylistTile(onClick = { showCreate = true })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (allOffline.isNotEmpty()) {
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
                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = offlineQuery,
                        onValueChange = { offlineQuery = it },
                        placeholder = { Text("Search offline") },
                        leadingIcon = {
                            Icon(Icons.Default.History, contentDescription = null)
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
                if (offlineSongs.isNotEmpty()) {
                    itemsIndexed(offlineSongs) { i, song ->
                        SongRow(
                            song = song,
                            onPlay = {
                                PlaybackController.playSongs(context, offlineSongs, i)
                            },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                            downloaded = true,
                            onDownload = {},
                            onDelete = { scope.launch { offline.delete(song.id) } },
                            onAddToPlaylist = { addTarget = song },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onCreate = { services.playlists.create(it); showCreate = false },
            onDismiss = { showCreate = false },
        )
    }
    deletePl?.let { pl ->
        AlertDialog(
            onDismissRequest = { deletePl = null },
            title = { Text("Delete playlist") },
            text = { Text("Remove \"${pl.name}\"?") },
            confirmButton = {
                TextButton(onClick = { services.playlists.delete(pl.id); deletePl = null }) {
                    Text("Delete", color = PlexAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletePl = null }) { Text("Cancel") }
            },
        )
    }
    addTarget?.let { target ->
        SongPlaylistPickerDialog(
            playlists = services.playlists.list(),
            onPick = { pl ->
                services.playlists.addSong(pl.id, target)
                addTarget = null
            },
            onCreate = { name ->
                val pl = services.playlists.create(name)
                services.playlists.addSong(pl.id, target)
                addTarget = null
            },
            onDismiss = { addTarget = null },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LibraryTile(
    thumbnail: String?,
    icon: ImageVector?,
    label: String,
    sub: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    accent: Boolean = true,
) {
    val gradient = Brush.linearGradient(
        if (accent) listOf(PlexAccent, Color(0xFF60A5FA)) else listOf(PlexSurfaceVariant, PlexSurfaceVariant),
    )
    Row(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail?.isNotBlank() == true) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(58.dp),
                )
            } else {
                Icon(
                    icon ?: Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(sub, color = PlexMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NewPlaylistTile(onClick: () -> Unit) {
    Box(
        Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, PlexSurfaceVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New playlist",
                tint = PlexMuted,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text("New Playlist", color = PlexMuted, fontSize = 12.sp)
        }
    }
}