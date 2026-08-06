package me.plexs.music.ui.screens.liked

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.plexs.music.PlexApp
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.screens.search.SongRow
import me.plexs.music.ui.theme.PlexMuted

@Composable
fun LikedScreen(services: PlexApp.Services) {
    val context = LocalContext.current
    val favorites by PlaybackController.favorites.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Liked Songs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
        if (favorites.isEmpty()) {
            Text(
                "Tap the heart on any song to build your library.",
                color = PlexMuted,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(favorites) { i, song ->
                SongRow(
                    song = song,
                    onPlay = { PlaybackController.playSongs(context, favorites, i) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}