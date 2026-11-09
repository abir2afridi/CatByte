package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/**
 * AI tools shown in the reference clip action bar. These need cloud models
 * (or an on-device model pack) that this offline build does not ship, so the
 * sheet lists them with an explicit status instead of hiding or faking them.
 */
@Composable
fun AiToolsSheet(
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val tools = listOf(
        "AI remix" to "Rewrites the clip with a generative model",
        "Eye contact" to "Re-aims the subject's gaze in every frame",
        "Relight" to "Relights the scene with a virtual light rig",
        "Generate media" to "Text-to-video / image generation",
        "AI avatar" to "Talking-avatar generation",
        "AI remove" to "Object removal with inpainting",
        "AI expand" to "Outpainting beyond the source frame",
        "Remove BG" to "Segmentation-based background removal",
        "Video translator" to "Speech translation with lip sync",
        "Enhance voice" to "Neural speech restoration",
        "Reduce noise" to "Neural denoise for audio",
        "Isolate voice" to "Voice/music source separation",
        "Motion blur" to "Optical-flow motion blur",
        "Stabilize" to "Gyro/optical stabilisation",
        "Auto reframe" to "Subject-aware reframing",
        "Camera tracking" to "Tracks a subject across frames",
        "Transform" to "Perspective and 3D transform"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("ai_tools_sheet")
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
                "AI Tools",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
        }

        Text(
            text = "Cloud AI: needs an online model and an account. This offline build cannot run these yet.",
            color = colors.accentYellow,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            items(tools) { (name, description) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated.copy(alpha = 0.6f))
                        .clickable { onSelect(name) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("ai_tool_$name"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(description, color = colors.textTertiary, fontSize = 9.sp, maxLines = 1)
                    }
                    Text("☁", color = colors.accentCyan, fontSize = 12.sp)
                }
            }
        }
    }
}
