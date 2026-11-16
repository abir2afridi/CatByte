package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** Backgrounds understood by [com.example.engine.RenderGraph]. */
data class BackgroundPreset(
    val paperStyle: String,
    val label: String,
    val colour: Color
)

val BackgroundPresets: List<BackgroundPreset> = listOf(
    BackgroundPreset("Standard", "Black", Color(0xFF000000)),
    BackgroundPreset("Drafting Grid", "Slate", Color(0xFF1A1A1E)),
    BackgroundPreset("Blueprint", "Blue", Color(0xFF0B1E33)),
    BackgroundPreset("Parchment Sketchbook", "Sepia", Color(0xFF2A2118)),
    BackgroundPreset("Dark Slate Chalkboard", "Charcoal", Color(0xFF141414)),
    BackgroundPreset("Architect Blueprint", "Deep blue", Color(0xFF10243D))
)

/**
 * Canvas background picker. Each preset maps to a real RenderGraph background
 * colour, so the choice survives save/load and reaches the export canvas.
 */
@Composable
fun BackgroundSheet(
    currentPaperStyle: String,
    onSelectBackground: (String) -> Unit,
    /** Current background image (uri string); empty means colour only. */
    currentBackgroundImageUri: String = "",
    onPickBackgroundImage: ((String) -> Unit)? = null,
    onClearBackgroundImage: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickBackgroundImage?.invoke(uri.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("background_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textTertiary.copy(alpha = 0.4f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Background",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Done",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackgroundPresets.forEach { preset ->
                val selected = preset.paperStyle.equals(currentPaperStyle, ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectBackground(preset.paperStyle) }
                        .testTag("background_${preset.paperStyle.lowercase().replace(' ', '_')}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(preset.colour)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) colors.accentCyan else colors.borderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        preset.label,
                        color = if (selected) colors.accentCyan else colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onPickBackgroundImage != null) {
                Text(
                    text = if (currentBackgroundImageUri.isBlank()) "Pick background image" else "Change background image",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("background_pick_image")
                )
            }
            if (currentBackgroundImageUri.isNotBlank() && onClearBackgroundImage != null) {
                Text(
                    text = "Remove image",
                    color = colors.accentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClearBackgroundImage() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("background_clear_image")
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (currentBackgroundImageUri.isBlank()) "Colour" else "Image",
                color = colors.textTertiary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "The background fills the bars around a clip whose aspect does not match the canvas, in both the preview and the exported file.",
            color = colors.textTertiary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
