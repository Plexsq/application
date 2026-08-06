package me.plexs.music.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
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
import me.plexs.music.ui.components.DownloadIconButton
import me.plexs.music.ui.components.SongOverflowMenu
import me.plexs.music.ui.components.SongPlaylistPickerDialog
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
    var history by remember { mutableStateOf<List<me.plexs.music.data.api.HistoryItem>>(emptyList()) }

    val offline = services.offline
    val offlineVersion by offline.version.collectAsState()
    var downloadedIds by remember { mutableStateOf(offline.list().map { it.song.id }.toSet()) }
    var downloadedPlaylists by remember { mutableStateOf(offline.playLists().map { it.id }.toSet()) }
    var downloading by remember { mutableStateOf<Set<String>>(emptySet()) }
    var addTarget by remember { mutableStateOf<Song?>(null) }
    val userPlaylists by services.playlists.version.collectAsState()

    LaunchedEffect(offlineVersion) {
        downloadedIds = offline.list().map { it.song.id }.toSet()
        downloadedPlaylists = offline.playLists().map { it.id }.toSet()
    }

    suspend fun downloadSong(song: Song) {
        if (song.id in downloading) return
        downloading = downloading + song.id
        offline.download(song)
        downloading = downloading - song.id
    }

    suspend fun downloadPlaylist(pl: me.plexs.music.data.api.SearchCard) {
        if (pl.id in downloading) return
        downloading = downloading + pl.id
        runCatching {
            val page = services.catalog.playlist(pl.id)
            val title = page.title.ifBlank { pl.title ?: pl.name ?: pl.id }
            offline.downloadPlayList(pl.id, title, page.items)
        }
        downloading = downloading - pl.id
    }

    LaunchedEffect(Unit) {
        if (services.session.isSignedIn) {
            services.userData.fetchHistory()?.let { history = it }
        }
    }

    suspend fun runSearch(q: String) {
        val g = ++gen
        loading = true
        error = null
        if (services.session.isSignedIn) {
            services.userData.addHistory(q)?.let { history = it }
        }
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
        Spacer(Modifier.height(16.dp))
        Text(
            text = "PLEX",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = PlexAccent,
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
            results == null && !searched && history.isNotEmpty() -> {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Recent searches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Clear",
                            color = PlexMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    services.userData.clearHistory()
                                    history = emptyList()
                                }
                            },
                        )
                    }
                    history.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    query = item.query
                                    searchJob?.cancel()
                                    scope.launch { runSearch(item.query) }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = PlexMuted,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = item.query,
                                color = PlexMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
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
                        itemsIndexed(songs) { i, song ->
                            SongRow(
                                song = song,
                                onPlay = {
                                    PlaybackController.playSongs(context, songs, i)
                                },
                                downloaded = song.id in downloadedIds,
                                downloading = song.id in downloading,
                                onDownload = { scope.launch { downloadSong(song) } },
                                onDelete = { scope.launch { offline.delete(song.id) } },
                                onAddToPlaylist = { addTarget = song },
                            )
                        }
                    }
                    if (r.videos.isNotEmpty()) {
                        item {
                            SectionTitle("Related")
                        }
                        itemsIndexed(r.videos) { i, song ->
                            SongRow(
                                song = song,
                                onPlay = {
                                    val all = songs + r.videos
                                    PlaybackController.playSongs(context, all, songs.size + i)
                                },
                                downloaded = song.id in downloadedIds,
                                downloading = song.id in downloading,
                                onDownload = { scope.launch { downloadSong(song) } },
                                onDelete = { scope.launch { offline.delete(song.id) } },
                                onAddToPlaylist = { addTarget = song },
                            )
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
                                downloaded = pl.id in downloadedPlaylists,
                                downloading = pl.id in downloading,
                                onDownload = { scope.launch { downloadPlaylist(pl) } },
                            )
                        }
                    }
                }
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
    downloaded: Boolean = false,
    downloading: Boolean = false,
    onDownload: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
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
        if (onDownload != null) {
            DownloadIconButton(
                downloaded = downloaded,
                downloading = downloading,
                onDownload = onDownload,
            )
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
    downloaded: Boolean = false,
    downloading: Boolean = false,
    onDownload: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onPlay,
                onLongClick = { menuOpen = true },
            )
            .padding(contentPadding),
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
        if (onDownload != null || onDelete != null || onAddToPlaylist != null) {
            SongOverflowMenu(
                downloaded = downloaded,
                downloading = downloading,
                onDownload = onDownload ?: {},
                onDelete = onDelete ?: {},
                onAddToPlaylist = onAddToPlaylist ?: {},
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = PlexMuted,
            )
        }
    }
}
