package me.plexs.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Repeat
import androidx.compose.material.icons.automirrored.filled.Shuffle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

@Composable
fun PlayerBar(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playing by PlaybackController.playing.collectAsState()
    val hasItem by PlaybackController.hasItem.collectAsState()
    val currentTime by PlaybackController.currentTime.collectAsState()
    val duration by PlaybackController.duration.collectAsState()
    val shuffle by PlaybackController.shuffle.collectAsState()
    val repeat by PlaybackController.repeat.collectAsState()
    val song = PlaybackController.currentSong
    if (!hasItem || song == null) return

    val progress = if (duration > 0) (currentTime.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onTap),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PlexSurfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(PlexAccent),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PlexSurfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                song.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = PlexMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { PlaybackController.toggleShuffle() }) {
                Icon(
                    Icons.AutoMirrored.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffle) PlexAccent else PlexMuted,
                )
            }
            IconButton(onClick = { PlaybackController.previous() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = PlexMuted)
            }
            IconButton(onClick = { PlaybackController.playPause() }) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = PlexAccent,
                )
            }
            IconButton(onClick = { PlaybackController.next() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = PlexMuted)
            }
            IconButton(onClick = { PlaybackController.cycleRepeat() }) {
                Icon(
                    Icons.AutoMirrored.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeat != 0) PlexAccent else PlexMuted,
                )
            }
        }
    }
}