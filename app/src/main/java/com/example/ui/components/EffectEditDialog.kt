package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimelineEffect
import com.example.ui.theme.PencilTheme

/**
 * Edits a placed effect: tap its chip on the effects track to open this.
 * Duration and intensity commit live (the same pattern as the audio sheet),
 * so the preview updates while dragging.
 */
@Composable
fun EffectEditDialog(
    effect: TimelineEffect,
    onEdit: (durationMs: Long, intensityScale: Float) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PencilTheme.colors
    var duration by remember(effect.id) {
        mutableFloatStateOf(effect.durationMs.coerceIn(300L, 30_000L).toFloat())
    }
    var intensity by remember(effect.id) {
        mutableFloatStateOf(effect.intensityScale.coerceIn(0.2f, 2f))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Edit effect", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(effect.name, color = colors.textSecondary, fontSize = 11.sp)
            }
        },
        text = {
            Column {
                Text("Duration: ${(duration / 1000f).format1()}s", color = colors.textPrimary, fontSize = 12.sp)
                Slider(
                    value = duration,
                    onValueChange = {
                        duration = it
                        onEdit(duration.toLong(), intensity)
                    },
                    valueRange = 300f..10_000f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("effect_edit_duration")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Intensity: ${(intensity * 100).toInt()}%",
                    color = colors.textPrimary,
                    fontSize = 12.sp
                )
                Slider(
                    value = intensity,
                    onValueChange = {
                        intensity = it
                        onEdit(duration.toLong(), intensity)
                    },
                    valueRange = 0.2f..2f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("effect_edit_intensity")
                )
                Text(
                    "Changes apply to the preview and the exported file.",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("effect_edit_done")) {
                Text("Done", color = colors.accentCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDelete()
            }, modifier = Modifier.testTag("effect_edit_delete")) {
                Text("Delete", color = colors.accentRed)
            }
        },
        containerColor = colors.surface
    )
}

private fun Float.format1(): String {
    val rounded = (this * 10).toInt() / 10f
    return if (rounded == rounded.toInt().toFloat()) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
