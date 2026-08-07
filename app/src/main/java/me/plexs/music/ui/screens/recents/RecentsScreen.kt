package me.plexs.music.ui.screens.recents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import me.plexs.music.PlexApp
import me.plexs.music.data.api.Song
import me.plexs.music.data.offline.DownloadState
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.components.SongPlaylistPickerDialog
import me.plexs.music.ui.screens.search.SongRow
import me.plexs.music.ui.theme.PlexMuted

@Composable
fun RecentsScreen(services: PlexApp.Services) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recent by PlaybackController.recentlyPlayed.collectAsState()

    val offline = services.offline
    val offlineVersion by offline.version.collectAsState()
    var downloadedIds by remember { mutableStateOf(offline.list().map { it.song.id }.toSet()) }
    val downloadStates by services.downloads.states.collectAsState()
    var addTarget by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(offlineVersion) {
        downloadedIds = offline.list().map { it.song.id }.toSet()
    }

    fun downloadSong(song: Song) {
        services.downloads.download(song)
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Recently Played",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
        if (recent.isEmpty()) {
            Text(
                "Songs you play will show up here.",
                color = PlexMuted,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(recent) { i, song ->
                SongRow(
                    song = song,
                    onPlay = { PlaybackController.playSongs(context, recent, i) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    downloaded = song.id in downloadedIds,
                    downloading = downloadStates[song.id] is DownloadState.Downloading,
                    onDownload = { downloadSong(song) },
                    onDelete = { scope.launch { offline.delete(song.id) } },
                    onAddToPlaylist = { addTarget = song },
                    onPlayNext = { PlaybackController.playNext(context, listOf(song)) },
                    onAddToQueue = { PlaybackController.addToQueue(context, listOf(song)) },
                    onShare = { me.plexs.music.ui.components.shareSong(context, song) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
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