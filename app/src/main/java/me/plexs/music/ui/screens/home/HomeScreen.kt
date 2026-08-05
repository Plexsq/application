package me.plexs.music.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.plexs.music.PlexApp
import me.plexs.music.data.api.Song
import me.plexs.music.playback.PlaybackController
import me.plexs.music.ui.auth.AuthViewModel
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexSurfaceVariant

@Composable
fun HomeScreen(services: PlexApp.Services, vm: AuthViewModel, onSignedOut: () -> Unit) {
    val context = LocalContext.current
    val favorites by PlaybackController.favorites.collectAsState()
    val user = services.session.user

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(28.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "PLEX",
                color = PlexAccent,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Good ${timeGreeting()}, ${user?.username?.let { "@$it" } ?: "you"}",
                color = PlexMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(20.dp))

        if (favorites.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No liked songs yet.\nTap the heart on any song.",
                        color = PlexMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = { vm.signOut(onSignedOut) }) { Text("Sign out") }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PlexAccent)
                            .clickable {
                                PlaybackController.playSongs(context, favorites, 0)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Liked",
                            tint = Color.White,
                            modifier = Modifier.width(28.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(
                                "Liked Songs",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${favorites.size} song${if (favorites.size != 1) "s" else ""}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                            )
                        }
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Text(
                        "Liked Songs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(favorites) { i, song ->
                    LikedRow(song, index = i, onPlay = {
                        PlaybackController.playSongs(context, favorites, i)
                    })
                }
                item { Spacer(Modifier.height(24.dp)) }
                if (favorites.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = { vm.signOut(onSignedOut) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                        ) { Text("Sign out") }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private fun timeGreeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h in 5..11 -> "morning"
        h in 12..17 -> "afternoon"
        else -> "evening"
    }
}

@Composable
fun LikedRow(song: Song, index: Int, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(44.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PlexSurfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = PlexMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}