package me.plexs.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
    var h = 0f
    if (d > 0f) {
        when (max) {
            r -> h = 60f * (((g - b) / d) % 6f)
            g -> h = 60f * (((b - r) / d) + 2f)
            b -> h = 60f * (((r - g) / d) + 4f)
        }
        if (h < 0f) h += 360f
    }
    val s = if (max == 0f) 0f else d / max
    return Triple(h, s, max)
}

private fun Color.Companion.plexFromHsv(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = r + m, green = g + m, blue = b + m, alpha = 1f)
}

private fun Color.plexHex(): String =
    "#%06X".format(toArgb() and 0xFFFFFF)

private val hueBandColors: List<Color> =
    (0 until 360 step 15).map { Color.Companion.plexFromHsv(it.toFloat(), 1f, 1f) }

/**
 * A small HSV color-picker dialog (no external dependency). Hue is a horizontal
 * band; saturation/value is a square fading to black. [onPick] is called with the
 * selected hex when the user taps Select.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val (initH, initS, initV) = initialColor.toHsv()
    var h by remember { mutableFloatStateOf(initH) }
    var s by remember { mutableFloatStateOf(initS) }
    var v by remember { mutableFloatStateOf(initV) }
    val current by remember(h, s, v) { mutableStateOf(Color.Companion.plexFromHsv(h, s, v)) }
    val svGradient = remember(h) {
        Brush.verticalGradient(listOf(Color.Companion.plexFromHsv(h, 1f, 1f), Color.Black))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Accent color", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(svGradient)
                        .pointerInput(Unit) {
                            detectTapGestures { off ->
                                s = (off.x / size.width).coerceIn(0f, 1f)
                                v = (1f - off.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                s = (change.position.x / size.width).coerceIn(0f, 1f)
                                v = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.Black,
                            radius = 8.dp.toPx(),
                            center = Offset(s * size.width, (1f - v) * size.height),
                            style = Stroke(4.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(s * size.width, (1f - v) * size.height),
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(hueBandColors))
                        .pointerInput(Unit) {
                            detectTapGestures { off -> h = (off.x / size.width).coerceIn(0f, 1f) * 360f }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ -> h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White,
                            radius = 11.dp.toPx(),
                            center = Offset(h / 360f * size.width, size.height / 2f),
                            style = Stroke(3.dp.toPx()),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(current))
                    Spacer(Modifier.width(12.dp))
                    Text(current.plexHex(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(current.plexHex()) }) {
                Text("Select", color = me.plexs.music.ui.theme.PlexAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}