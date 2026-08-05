package me.plexs.music.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.plexs.music.CrashLogger
import me.plexs.music.PlexApp
import me.plexs.music.data.api.SearchResults
import me.plexs.music.data.api.Song
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.components.UpdateDialog
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

@Composable
fun SearchScreen(services: PlexApp.Services) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<SearchResults?>(null) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var gen by remember { mutableStateOf(0) }
    var lastCrash by remember { mutableStateOf(CrashLogger.readLatest(context)) }

    suspend fun runSearch(q: String) {
        val g = ++gen
        loading = true
        error = null
        var outcome: Result<SearchResults> = runCatching { services.catalog.search(q) }
        if (outcome.isFailure && outcome.exceptionOrNull() !is CancellationException) {
            outcome = runCatching { services.catalog.search(q) }
        }
        outcome
            .onSuccess {
                if (g != gen) return@onSuccess
                results = it
                searched = true
            }
            .onFailure { e ->
                if (g != gen) return@onFailure
                if (e is CancellationException) return@onFailure
                val msg = e.message ?: e.javaClass.simpleName
                error = when {
                    msg.startsWith("Search failed (") -> msg
                    msg.contains("timeout", ignoreCase = true) -> "Search timed out. Try again."
                    msg.contains("Unable to resolve host", ignoreCase = true) -> "No connection."
                    e.javaClass.simpleName.contains("Serialization", ignoreCase = true) ||
                        e.javaClass.simpleName.contains("Parser", ignoreCase = true) -> "Unexpected response. Try again."
                    else -> "Search failed ($msg)"
                }
            }
        if (g == gen) loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { q ->
                query = q
                error = null
                searchJob?.cancel()
                if (q.isBlank()) {
                    results = null
                    searched = false
                    return@OutlinedTextField
                }
                loading = true
                searchJob = scope.launch {
                    delay(450)
                    runSearch(q)
                }
            },
            placeholder = { Text("What do you want to listen to?") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isBlank()) return@KeyboardActions
                searchJob?.cancel()
                scope.launch { runSearch(query) }
            }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlexAccent,
                unfocusedBorderColor = PlexSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        lastCrash?.let { crash ->
            androidx.compose.material3.Card(
                onClick = {
                    CrashLogger.clear(context)
                    lastCrash = null
                },
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Previous crash detected — tap to dismiss",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = crash.lines().take(8).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            results == null && searched -> {
                Text(
                    text = "No results found.",
                    color = PlexMuted,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            results != null -> {
                val r = results!!
                val songs = r.songs
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (r.artists.isNotEmpty()) {
                        item {
                            SectionTitle("Artists")
                        }
                        items(r.artists) { artist ->
                            CardRow(
                                thumbnail = artist.thumbnail,
                                title = artist.name ?: artist.title ?: "",
                                subtitle = artist.subscribers?.let { "$it subscribers" },
                                onClick = {},
                            )
                        }
                    }
                    if (songs.isNotEmpty()) {
                        item {
                            SectionTitle("Songs")
                        }
                        items(songs) { song ->
                            SongRow(song = song, onPlay = {
                                PlaybackController.playSongs(context, songs, songs.indexOf(song))
                            })
                        }
                    }
                    if (r.videos.isNotEmpty()) {
                        item {
                            SectionTitle("Related")
                        }
                        items(r.videos) { song ->
                            SongRow(song = song, onPlay = {
                                val all = songs + r.videos
                                PlaybackController.playSongs(context, all, songs.size + r.videos.indexOf(song))
                            })
                        }
                    }
                    if (r.playlists.isNotEmpty()) {
                        item {
                            SectionTitle("Playlists")
                        }
                        items(r.playlists) { pl ->
                            CardRow(
                                thumbnail = pl.thumbnail,
                                title = pl.title ?: pl.name ?: "",
                                subtitle = pl.author?.let { "$it · ${pl.itemCount ?: "?"} tracks" },
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
        UpdateDialog(services)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
fun CardRow(
    thumbnail: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PlexSurfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlexMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SongRow(song: Song, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PlexSurfaceVariant),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = PlexMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.durationText ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = PlexMuted,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = PlexMuted,
        )
    }
}
