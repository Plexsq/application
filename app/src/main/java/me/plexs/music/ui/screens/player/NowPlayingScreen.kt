package me.plexs.music.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.plexs.music.PlexApp
import me.plexs.music.data.api.LyricsResult
import me.plexs.music.data.api.Song
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

private fun fmt(ms: Long): String {
    if (ms <= 0 || ms == Long.MIN_VALUE) return "0:00"
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "$m:${sec.toString().padStart(2, '0')}"
}

@Composable
fun NowPlayingScreen(onClose: () -> Unit) {
    val song = PlaybackController.currentSong
    val playing by PlaybackController.playing.collectAsState()
    val queue by PlaybackController.queue.collectAsState()
    val queueIndex by PlaybackController.queueIndex.collectAsState()
    val currentTime by PlaybackController.currentTime.collectAsState()
    val duration by PlaybackController.duration.collectAsState()
    val shuffle by PlaybackController.shuffle.collectAsState()
    val repeat by PlaybackController.repeat.collectAsState()
    val favorites by PlaybackController.favorites.collectAsState()
    var showQueue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<LyricsResult?>(null) }
    val fav = song?.let { favorites.any { f -> f.id == it.id } } ?: false
    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember { (context.applicationContext as me.plexs.music.PlexApp).services.catalog }

    LaunchedEffect(song?.id, showLyrics) {
        lyrics = null
        if (showLyrics && song != null) {
            lyrics = catalog.lyrics(song.id, song.title, song.artist ?: "")
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 10 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 10 },
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close")
                }
                Spacer(Modifier.weight(1f))
                Text("Now Playing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showLyrics = !showLyrics; showQueue = false }) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) PlexAccent else PlexMuted,
                    )
                }
                IconButton(onClick = { showQueue = !showQueue; showLyrics = false }) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueue) PlexAccent else PlexMuted,
                    )
                }
            }

            if (showLyrics) {
                LyricsView(song = song, lyrics = lyrics, currentTimeMs = currentTime)
            } else if (showQueue) {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Text(
                        "Up next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        itemsIndexed(queue) { i, qs ->
                            QueueRow(
                                song = qs,
                                active = i == queueIndex,
                                onClick = { PlaybackController.playAtIndex(i) },
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                ) {
                    Spacer(Modifier.height(16.dp))
                    if (song != null) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PlexSurfaceVariant),
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlexSurfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Nothing playing", color = PlexMuted)
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = song?.title ?: "Nothing playing",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = song?.artist ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PlexMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { song?.let { PlaybackController.toggleFavorite(it) } }) {
                            Icon(
                                if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (fav) PlexAccent else PlexMuted,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Slider(
                        value = if (duration > 0) currentTime.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                        onValueChange = { PlaybackController.seekTo(it.toLong()) },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        colors = SliderDefaults.colors(
                            thumbColor = PlexAccent,
                            activeTrackColor = PlexAccent,
                            inactiveTrackColor = PlexSurfaceVariant,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt(currentTime), style = MaterialTheme.typography.labelSmall, color = PlexMuted)
                        Spacer(Modifier.weight(1f))
                        Text(fmt(duration), style = MaterialTheme.typography.labelSmall, color = PlexMuted)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { PlaybackController.toggleShuffle() }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffle) PlexAccent else PlexMuted,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        IconButton(onClick = { PlaybackController.previous() }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        IconButton(onClick = { PlaybackController.playPause() }) {
                            val scale by animateFloatAsState(if (playing) 1f else 0.92f)
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(PlexAccent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        }
                        IconButton(onClick = { PlaybackController.next() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        IconButton(onClick = { PlaybackController.cycleRepeat() }) {
                            Icon(
                                if (repeat == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeat > 0) PlexAccent else PlexMuted,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun QueueRow(song: Song, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) PlexSurfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(PlexSurfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
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
    }
}

@Composable
private fun LyricsView(song: Song?, lyrics: LyricsResult?, currentTimeMs: Long) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text(
            "Lyrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (song == null) {
            Text("Nothing playing", color = PlexMuted)
        } else if (lyrics == null) {
            Text("No lyrics available", color = PlexMuted)
        } else {
            val lr = lyrics
            val scrollState = rememberScrollState()
            // Active line index derived from the current playback position against each timestamp.
            val activeIndex = remember(currentTimeMs, lr) {
                lr.synced?.let { synced ->
                    var idx = 0
                    for ((i, l) in synced.withIndex()) {
                        if (l.time <= currentTimeMs / 1000.0) idx = i else break
                    }
                    idx
                } ?: -1
            }
            LaunchedEffect(activeIndex) {
                if (activeIndex > 0) {
                    scrollState.animateScrollTo(scrollState.value + 56)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
            ) {
                if (lr.synced != null) {
                    lr.synced.forEachIndexed { i, line ->
                        val active = i == activeIndex
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) PlexAccent else PlexMuted,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                } else {
                    lr.plain?.split("\n")?.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyLarge,
                            color = PlexMuted,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}