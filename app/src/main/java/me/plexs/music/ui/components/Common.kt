package me.plexs.music.ui.components

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
            DropdownMenuItem(
                text = { Text(if (downloaded) "Delete offline copy" else if (downloading) "Downloading…" else "Download offline") },
                enabled = !downloading || downloaded,
                onClick = { onExpandedChange(false); if (downloaded) onDelete() else onDownload() },
            )
            DropdownMenuItem(
                text = { Text("Add to playlist") },
                onClick = { onExpandedChange(false); onAddToPlaylist() },
            )
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