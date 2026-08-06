package me.plexs.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    downloading: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PlexMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (downloaded) {
                DropdownMenuItem(
                    text = { Text("Delete offline copy") },
                    onClick = { open = false; onDelete() },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(if (downloading) "Downloading…" else "Download offline") },
                    enabled = !downloading,
                    onClick = { open = false; onDownload() },
                )
            }
        }
    }
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