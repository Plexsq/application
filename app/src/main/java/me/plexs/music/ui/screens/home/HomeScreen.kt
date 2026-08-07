package me.plexs.music.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import me.plexs.music.ui.components.PlaylistContextMenu
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
    var menuPl by remember { mutableStateOf<UserPlaylist?>(null) }
    var addTarget by remember { mutableStateOf<Song?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
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
                LibraryGridCard(
                    thumbnail = recent.firstOrNull()?.thumbnail,
                    icon = Icons.Default.History,
                    label = "Recently Played",
                    sub = "${recent.size} song${if (recent.size != 1) "s" else ""}",
                    onClick = { onOpenSection(Destinations.RECENTS) },
                )
            }
            item {
                LibraryGridCard(
                    thumbnail = favorites.firstOrNull()?.thumbnail,
                    icon = Icons.Default.Favorite,
                    label = "Liked Songs",
                    sub = "${favorites.size} song${if (favorites.size != 1) "s" else ""}",
                    onClick = { onOpenSection(Destinations.LIKED) },
                )
            }
            item {
                LibraryGridCard(
                    thumbnail = null,
                    icon = Icons.Default.Download,
                    label = "Offline",
                    sub = "${allOffline.size} song${if (allOffline.size != 1) "s" else ""}",
                    onClick = { },
                    accent = false,
                )
            }
            items(playlists, key = { it.id }) { pl ->
                LibraryGridCard(
                    thumbnail = pl.songs.firstOrNull()?.thumbnail ?: "",
                    icon = if (pl.songs.isEmpty()) Icons.Default.PlaylistAdd else null,
                    label = pl.name,
                    sub = "${pl.songs.size} song${if (pl.songs.size != 1) "s" else ""}",
                    onClick = {
                        if (pl.songs.isNotEmpty()) {
                            PlaybackController.playSongs(context, pl.songs, 0)
                        }
                    },
                    onLongClick = { menuPl = pl },
                )
            }
            if (allOffline.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
if (offlineSongs.isNotEmpty()) {
                    items(
                        count = offlineSongs.size,
                        span = { GridItemSpan(maxLineSpan) },
                    ) { i ->
                        SongRow(
                            song = offlineSongs[i],
                            onPlay = {
                                PlaybackController.playSongs(context, offlineSongs, i)
                            },
                            contentPadding = PaddingValues(vertical = 6.dp),
                            downloaded = true,
                            onDownload = {},
                            onDelete = { scope.launch { offline.delete(offlineSongs[i].id) } },
                            onAddToPlaylist = { addTarget = offlineSongs[i] },
                            onPlayNext = { PlaybackController.playNext(context, listOf(offlineSongs[i])) },
                            onAddToQueue = { PlaybackController.addToQueue(context, listOf(offlineSongs[i])) },
                            onShare = { me.plexs.music.ui.components.shareSong(context, offlineSongs[i]) },
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
    menuPl?.let { pl ->
        PlaylistContextMenu(
            playlist = pl,
            onPlay = { if (pl.songs.isNotEmpty()) PlaybackController.playSongs(context, pl.songs, 0); menuPl = null },
            onPlayNext = { PlaybackController.playNext(context, pl.songs); menuPl = null },
            onAddToQueue = { PlaybackController.addToQueue(context, pl.songs); menuPl = null },
            onRename = {
                services.playlists.rename(pl.id, it)
                menuPl = null
            },
            onDelete = {
                services.playlists.delete(pl.id)
                menuPl = null
            },
            onDismiss = { menuPl = null },
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
private fun LibraryGridCard(
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
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail?.isNotBlank() == true) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            } else {
                Icon(
                    icon ?: Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(sub, color = PlexMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}