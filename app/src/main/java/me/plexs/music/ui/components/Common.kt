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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.plexs.music.ui.theme.PlexAccent
import me.plexs.music.ui.theme.PlexMuted
import me.plexs.music.ui.theme.PlexOnAccent
import me.plexs.music.ui.theme.PlexSurfaceVariant

@Composable
fun PlexTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (isPassword) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
            else androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PlexAccent,
            unfocusedBorderColor = PlexSurfaceVariant,
        ),
    )
}

@Composable
fun PlexButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PlexAccent, contentColor = PlexOnAccent),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp,
                color = PlexOnAccent,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun SongOverflowMenu(
    downloaded: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    downloading: Boolean = false,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PlexMuted)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            if (onPlayNext != null) {
                DropdownMenuItem(
                    text = { Text("Play next") },
                    onClick = { onExpandedChange(false); onPlayNext() },
                )
            }
            if (onAddToQueue != null) {
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    onClick = { onExpandedChange(false); onAddToQueue() },
                )
            }
            if (onPlayNext != null || onAddToQueue != null) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(if (downloaded) "Delete offline copy" else if (downloading) "Downloading…" else "Download offline") },
                enabled = !downloading || downloaded,
                onClick = { onExpandedChange(false); if (downloaded) onDelete() else onDownload() },
            )
            DropdownMenuItem(
                text = { Text("Add to playlist") },
                onClick = { onExpandedChange(false); onAddToPlaylist() },
            )
            if (onShare != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { onExpandedChange(false); onShare() },
                )
            }
        }
    }
}

@Composable
fun SongPlaylistPickerDialog(
    playlists: List<me.plexs.music.data.playlists.UserPlaylist>,
    onPick: (me.plexs.music.data.playlists.UserPlaylist) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var createMode by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { Text("Add to playlist") },
        text = {
            if (createMode) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlexAccent,
                        unfocusedBorderColor = PlexSurfaceVariant,
                    ),
                )
            } else {
                Column {
                    if (playlists.isNotEmpty()) {
                        playlists.forEach { pl ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(pl) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = PlexMuted,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    pl.name,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${pl.songs.size}",
                                    color = PlexMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    } else {
                        Text(
                            "No playlists yet. Create one to start adding songs.",
                            color = PlexMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (createMode) {
                Button(
                    onClick = { if (newName.trim().isNotEmpty()) onCreate(newName.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = PlexAccent, contentColor = PlexOnAccent),
                ) { Text("Create") }
            } else {
                Button(
                    onClick = { createMode = true },
                ) { Text("New playlist") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Create playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlexAccent,
                    unfocusedBorderColor = PlexSurfaceVariant,
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = PlexAccent, contentColor = PlexOnAccent),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun DownloadIconButton(
    downloaded: Boolean,
    downloading: Boolean,
    onDownload: () -> Unit,
) {
    if (downloaded) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Downloaded",
            tint = PlexAccent,
            modifier = Modifier.width(28.dp).height(28.dp),
        )
    } else if (downloading) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
    } else {
        IconButton(onClick = onDownload) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = PlexMuted)
        }
    }
}

/** Shares a song's public track link via the Android share sheet. */
fun shareSong(context: android.content.Context, song: me.plexs.music.data.api.Song) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, "https://music.plexs.me/track/${song.id}")
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share song"))
}

/**
 * Long-press context menu for a user playlist — mirrors the desktop playlist menu
 * (Open / Play / Play next / Add to queue / Rename / Delete). Open shows the song
 * list. Rename opens a small dialog.
 */
@Composable
fun PlaylistContextMenu(
    playlist: me.plexs.music.data.playlists.UserPlaylist,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showRename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val options = @Composable {
        DropdownMenuItem(
            text = { Text("Open playlist") },
            onClick = { onDismiss(); onOpen() },
        )
        DropdownMenuItem(
            text = { Text("Play") },
            onClick = { onDismiss(); onPlay() },
        )
        DropdownMenuItem(
            text = { Text("Play next") },
            onClick = { onDismiss(); onPlayNext() },
        )
        DropdownMenuItem(
            text = { Text("Add to queue") },
            onClick = { onDismiss(); onAddToQueue() },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = { showRename = true },
        )
        DropdownMenuItem(
            text = { Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error) },
            onClick = { confirmDelete = true },
        )
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
    ) {
        options()
    }

    if (showRename) {
        var newName by remember { mutableStateOf(playlist.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Rename playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    placeholder = { Text("Playlist name") },
                    shape = RoundedCornerShape(10.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { showRename = false; onRename(newName) }) { Text("Save", color = PlexAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            },
        )
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete playlist") },
            text = { Text("Remove \"${playlist.name}\"?") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDismiss(); onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * A popup that shows the songs inside a playlist (mirrors the Now Playing sheet
 * style). Tapping a song plays it from the playlist. Not used for editing.
 */
@Composable
fun PlaylistSheet(
    playlist: me.plexs.music.data.playlists.UserPlaylist,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                playlist.name,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        text = {
            if (playlist.songs.isEmpty()) {
                Text("This playlist is empty. Add songs to it from any track's menu.", color = PlexMuted)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.height(320.dp),
                ) {
                    items(playlist.songs.size) { i ->
                        val s = playlist.songs[i]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    me.plexs.music.playback.PlaybackController.playSongs(context, playlist.songs, i)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            coil.compose.AsyncImage(
                                model = s.thumbnail,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PlexSurfaceVariant),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(s.artist ?: "", color = PlexMuted, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        confirmButton = {},
    )
}