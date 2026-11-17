package com.example.ui.audio

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CapCutBorder
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutRed
import com.example.ui.theme.CapCutSurfaceElevated
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The voice recorder tab (plus the TTS voiceover entry). */
@Composable
internal fun AudioVoiceTab(
    isRecordingVoice: Boolean,
    voiceDurationMs: Long,
    onStartVoiceRecord: () -> Unit,
    onStopVoiceRecord: () -> Unit,
    onTtsVoiceover: (() -> Unit)? = null
) {
    val colors = PencilTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        val sec = voiceDurationMs / 1000f
        Text(
            text = if (isRecordingVoice) String.format("Recording: %02d:%04.1f", (sec / 60).toInt(), sec % 60) else "Hold or Tap to Record Voiceover",
            color = if (isRecordingVoice) CapCutRed else Color.LightGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (isRecordingVoice) {
            Spacer(modifier = Modifier.height(8.dp))
            // Animated Audio Level dB Meter (8 Animated Level Bars)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val barHeights = remember { listOf(12.dp, 20.dp, 16.dp, 24.dp, 18.dp, 22.dp, 14.dp, 19.dp) }
                barHeights.forEachIndexed { index, defaultHeight ->
                    var activeHeight by remember { mutableStateOf(defaultHeight) }
                    LaunchedEffect(isRecordingVoice, voiceDurationMs) {
                        // Vary bar height dynamically to simulate live mic input
                        val factor = ((voiceDurationMs + index * 150) % 500) / 500f
                        activeHeight = (10 + (factor * 14)).dp
                    }
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(activeHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    activeHeight > 20.dp -> CapCutRed
                                    activeHeight > 15.dp -> Color(0xFFFFC107)
                                    else -> Color(0xFF26E5D7)
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val pulseScale: MutableState<Float> = remember { mutableStateOf(1f) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(isRecordingVoice) {
            if (isRecordingVoice) {
                scope.launch {
                    var growing = true
                    while (isRecordingVoice) {
                        val target = if (growing) 1.25f else 1.0f
                        growing = !growing
                        // Simple linear interpolation over 600ms
                        val start = pulseScale.value
                        val duration = 600L
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < duration) {
                            if (!isRecordingVoice) break
                            val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
                            pulseScale.value = start + (target - start) * progress
                            delay(16) // ~60fps
                        }
                        if (!isRecordingVoice) break
                        pulseScale.value = target
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(if (isRecordingVoice) pulseScale.value else 1.0f)
                .clip(CircleShape)
                .background(if (isRecordingVoice) CapCutRed else CapCutSurfaceElevated)
                .border(2.dp, if (isRecordingVoice) Color.White else CapCutBorder, CircleShape)
                .clickable {
                    if (isRecordingVoice) onStopVoiceRecord() else onStartVoiceRecord()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Voice Record",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Audio Ducking", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Music dips automatically under every voice-over",
                    color = CapCutTextSecondary,
                    fontSize = 9.sp
                )
            }
            Text(
                "Always on",
                color = CapCutTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI Voice Noise Reduction", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Neural denoise needs the online AI engine",
                    color = CapCutTextSecondary,
                    fontSize = 9.sp
                )
            }
            androidx.compose.material3.Switch(
                checked = false,
                onCheckedChange = null,
                enabled = false
            )
        }

        // TTS Voiceover Button
        if (onTtsVoiceover != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onTtsVoiceover() },
                colors = ButtonDefaults.buttonColors(containerColor = CapCutPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "TTS",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Voiceover", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isRecordingVoice) "Tap to finish and add to timeline" else "Speak clearly into your phone's microphone",
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}
