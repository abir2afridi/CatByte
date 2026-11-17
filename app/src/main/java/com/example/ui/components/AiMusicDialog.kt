package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MusicGenerator
import com.example.ui.theme.AppColors
import com.example.ui.theme.PencilTheme

/**
 * "Audio → AI music": pick a mood and the app composes a track on-device
 * (procedural synthesis, no network) and adds it at the playhead.
 */
@Composable
fun AiMusicDialog(
    onGenerate: (MusicGenerator.Mood) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PencilTheme.colors
    var selected by remember { mutableStateOf(MusicGenerator.Mood.CHILL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("AI music", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Composed offline on this device. It lands at the playhead and can be trimmed, mixed and exported like any track.",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                MusicGenerator.Mood.entries.forEach { mood ->
                    val active = mood == selected
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(selectedBackgroundColor(colors, active))
                            .border(
                                1.dp,
                                if (active) colors.accentCyan else colors.borderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selected = mood }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("ai_music_mood_${mood.name.lowercase()}")
                    ) {
                        Text(
                            mood.displayName,
                            color = if (active) colors.accentCyan else colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(mood.description, color = colors.textSecondary, fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(selected) },
                modifier = Modifier.testTag("ai_music_generate")
            ) {
                Text("Generate", color = colors.accentCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

@Composable
private fun selectedBackgroundColor(colors: AppColors, active: Boolean): Color =
    if (active) colors.accentCyan.copy(alpha = 0.10f) else colors.surfaceElevated
