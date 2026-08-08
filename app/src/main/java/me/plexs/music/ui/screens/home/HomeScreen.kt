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
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import me.plexs.music.data.playlists.UserPlaylist
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.components.CreatePlaylistDialog
import me.plexs.music.ui.components.PlaylistContextMenu
import me.plexs.music.ui.components.PlaylistSheet
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(services: PlexApp.Services, onOpenSection: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlistsVersion by services.playlists.version.collectAsState()
    val playlists = remember(playlistsVersion) { services.playlists.list() }
    var refreshing by remember { mutableStateOf(false) }
    val offlineVersion by services.offline.version.collectAsState()
    val offlineCount = services.offline.list().size

    var showCreate by remember { mutableStateOf(false) }
    var menuPl by remember { mutableStateOf<UserPlaylist?>(null) }
    var openPl by remember { mutableStateOf<UserPlaylist?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                PlaybackController.refresh(context)
                scope.launch { kotlinx.coroutines.delay(900); refreshing = false }
            },
        ) {
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
                // Pinned "Offline downloads" entry — always the first library cell, fixed.
                item {
                    LibraryGridCard(
                        thumbnail = null,
                        icon = Icons.Default.OfflinePin,
                        label = "Offline",
                        sub = if (offlineCount > 0) "$offlineCount song${if (offlineCount != 1) "s" else ""} · ${services.offline.size()}" else "Download songs to play without data",
                        onClick = { onOpenSection("offline") },
                        onLongClick = null,
                        accent = false,
                    )
                }
                items(playlists, key = { it.id }) { pl ->
                    LibraryGridCard(
                        thumbnail = pl.songs.firstOrNull()?.thumbnail ?: "",
                        icon = if (pl.songs.isEmpty()) Icons.Default.PlaylistAdd else null,
                        label = pl.name,
                        sub = "${pl.songs.size} song${if (pl.songs.size != 1) "s" else ""}",
                        onClick = { openPl = pl },
                        onLongClick = { menuPl = pl },
                    )
                }
                if (playlists.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Tap New Playlist to create your first collection.",
                            color = PlexMuted,
                            modifier = Modifier.padding(top = 8.dp),
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
    openPl?.let { pl ->
        PlaylistSheet(
            playlist = pl,
            onDismiss = { openPl = null },
        )
    }
    menuPl?.let { pl ->
        PlaylistContextMenu(
            playlist = pl,
            onOpen = { openPl = pl },
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