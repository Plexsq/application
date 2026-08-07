package me.plexs.music.ui.screens.offline

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
fun OfflineScreen(services: PlexApp.Services) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offlineVersion by services.offline.version.collectAsState()
    val allOffline = remember(offlineVersion) { services.offline.list() }
    val downloadStates by services.downloads.states.collectAsState()
    var addTarget by remember { mutableStateOf<Song?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Offline downloads",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
        Text(
            "${allOffline.size} song${if (allOffline.size != 1) "s" else ""} · ${services.offline.size()} stored",
            style = MaterialTheme.typography.bodyMedium,
            color = PlexMuted,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        if (allOffline.isEmpty()) {
            Text(
                "Songs you download are saved here and play from your device without a connection.",
                color = PlexMuted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(allOffline) { i, entry ->
                val song = entry.song
                SongRow(
                    song = song,
                    onPlay = {
                        val songs = allOffline.map { it.song }
                        PlaybackController.playSongs(context, songs, i)
                    },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    downloaded = true,
                    downloading = downloadStates[song.id] is DownloadState.Downloading,
                    onDownload = {},
                    onDelete = { scope.launch { services.offline.delete(song.id) } },
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
