package me.plexs.music.ui.screens.detail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.plexs.music.PlexApp
import me.plexs.music.data.api.ArtistPage
import me.plexs.music.data.api.Song
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

/** One shared screen for album / playlist / artist detail opened from search results. */
@Composable
fun CatalogDetailScreen(
    services: PlexApp.Services,
    type: String,
    id: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var sections by remember { mutableStateOf<List<Pair<String, List<Song>>>>(emptyList()) }
    var cover by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(type, id) {
        loading = true; error = null
        try {
            when (type) {
                "artist" -> {
                    val artist = withContext(Dispatchers.IO) { services.catalog.artist(id) }
                    title = artist.name
                    cover = artist.thumbnail
                    subtitle = artist.followers?.let { "$it followers" } ?: ""
                    sections = artist.sections.map { it.title to it.items }
                }
                "album" -> {
                    val album = withContext(Dispatchers.IO) { services.catalog.album(id) }
                    title = album.title
                    subtitle = album.artist?.let { it + (album.year?.let { y -> " · $y" } ?: "") } ?: ""
                    cover = album.thumbnail
                    sections = listOf("Tracks" to album.items)
                }
                else -> {
                    val page = withContext(Dispatchers.IO) { services.catalog.playlist(id) }
                    title = page.title
                    subtitle = page.author ?: ""
                    sections = listOf("Tracks" to page.items)
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "PLEX",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = PlexAccent,
            )
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Text(
                text = error ?: "Failed to load", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PlexSurfaceVariant),
                        ) {
                            if (!cover.isNullOrBlank()) {
                                AsyncImage(
                                    model = cover,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                title, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            if (subtitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(subtitle, color = PlexMuted)
                            }
                        }
                    }
                }
                sections.forEach { (secTitle, songs) ->
                    if (songs.isNotEmpty()) {
                        item {
                            Text(
                                secTitle, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(songs.size) { i ->
                            val song = songs[i]
                            DetailSongRow(song = song, onClick = {
                                PlaybackController.playSongs(context, songs, i)
                            })
                        }
                    }
                }
                if (sections.none { it.second.isNotEmpty() }) {
                    item { Text("Nothing here yet.", color = PlexMuted, modifier = Modifier.padding(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DetailSongRow(song: Song, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 20.dp, top = 8.dp, bottom = 8.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PlexSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!song.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = song.thumbnail, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(song.artist ?: "", color = PlexMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        song.durationText?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, color = PlexMuted, fontSize = 13.sp)
        }
    }
}