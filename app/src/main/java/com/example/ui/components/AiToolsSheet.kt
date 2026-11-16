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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** What an AI tool needs from the user's endpoint. */
enum class AiToolKind {
    /** Chat-completions model (text). */
    TEXT,

    /** Image-generation model. */
    IMAGE,

    /** Whisper-compatible speech-to-text model (auto captions). */
    SPEECH,

    /** Needs a specialised model/API that a chat endpoint cannot provide. */
    SPECIALISED
}

data class AiToolEntry(
    val id: String,
    val name: String,
    val description: String,
    val kind: AiToolKind
)

/**
 * Every AI action the editor knows about.
 *
 * Text, image and speech tools run on the OpenAI-compatible endpoint the user
 * configures in Settings → AI. Everything else genuinely needs a different kind
 * of model (segmentation, lip sync, optical flow, avatars); those rows stay
 * disabled and say why instead of pretending to work.
 */
val DefaultAiTools: List<AiToolEntry> = listOf(
    AiToolEntry("remix", "AI remix", "Rewrite the captions into punchy lines", AiToolKind.TEXT),
    AiToolEntry("translate", "AI translate", "Translate captions to the other language", AiToolKind.TEXT),
    AiToolEntry("packaging", "AI packaging", "Three titles for this video", AiToolKind.TEXT),
    AiToolEntry("captions", "AI auto captions", "Transcribe the clip's audio with timing", AiToolKind.SPEECH),
    AiToolEntry("image", "AI image", "Generate a background image from a prompt", AiToolKind.IMAGE),
    AiToolEntry("overlay", "AI overlay", "Insert a generated image into the video", AiToolKind.IMAGE),
    AiToolEntry("cover", "AI cover", "Generate the project's cover image", AiToolKind.IMAGE),
    AiToolEntry("eye_contact", "Eye contact", "Re-aims the subject's gaze in every frame", AiToolKind.SPECIALISED),
    AiToolEntry("relight", "Relight", "Relights the scene with a virtual light rig", AiToolKind.SPECIALISED),
    AiToolEntry("ai_remove", "AI remove", "Object removal with inpainting", AiToolKind.SPECIALISED),
    AiToolEntry("ai_expand", "AI expand", "Outpainting beyond the source frame", AiToolKind.SPECIALISED),
    AiToolEntry("remove_bg", "Remove BG", "Segmentation-based background removal", AiToolKind.SPECIALISED),
    AiToolEntry("video_translator", "Video translator", "Speech translation with lip sync", AiToolKind.SPECIALISED),
    AiToolEntry("enhance_voice", "Enhance voice", "Neural speech restoration", AiToolKind.SPECIALISED),
    AiToolEntry("reduce_noise", "Reduce noise", "Neural denoise for audio", AiToolKind.SPECIALISED),
    AiToolEntry("isolate_voice", "Isolate voice", "Voice/music source separation", AiToolKind.SPECIALISED),
    AiToolEntry("motion_blur", "Motion blur", "Optical-flow motion blur", AiToolKind.SPECIALISED),
    AiToolEntry("stabilize", "Stabilize", "Gyro/optical stabilisation", AiToolKind.SPECIALISED),
    AiToolEntry("auto_reframe", "Auto reframe", "Subject-aware reframing", AiToolKind.SPECIALISED),
    AiToolEntry("camera_tracking", "Camera tracking", "Tracks a subject across frames", AiToolKind.SPECIALISED),
    AiToolEntry("transform", "Transform", "Perspective and 3D transform", AiToolKind.SPECIALISED),
    AiToolEntry("avatar", "AI avatar", "Talking-avatar generation", AiToolKind.SPECIALISED)
)

@Composable
fun AiToolsSheet(
    configured: Boolean,
    /** Host shown in the status line, e.g. "api.openai.com". */
    endpointLabel: String = "",
    tools: List<AiToolEntry> = DefaultAiTools,
    onRunTool: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val readyCount = tools.count { it.kind != AiToolKind.SPECIALISED }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("ai_open_settings")
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "AI settings",
                        tint = colors.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (configured) Color(0xFF3FE0D0) else Color(0xFFFFC94A))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    configured && endpointLabel.isNotBlank() ->
                        "Connected to $endpointLabel — $readyCount tools ready"
                    configured -> "AI endpoint configured — $readyCount tools ready"
                    else -> "No AI endpoint. Add a base URL in Settings → AI."
                },
                color = if (configured) colors.accentCyan else colors.accentYellow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            items(tools, key = { it.id }) { tool ->
                val available = configured && tool.kind != AiToolKind.SPECIALISED
                val status = when {
                    tool.kind == AiToolKind.SPECIALISED -> "Needs a specialised model"
                    !configured -> "Add an endpoint first"
                    else -> "Ready"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated.copy(alpha = if (available) 0.8f else 0.4f))
                        .clickable(enabled = available) { onRunTool(tool.id) }
                        .alpha(if (available) 1f else 0.55f)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("ai_tool_${tool.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.name, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            tool.description,
                            color = colors.textTertiary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        status,
                        color = if (available) colors.accentCyan else colors.textTertiary,
                        fontSize = 9.sp,
                        fontWeight = if (available) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
