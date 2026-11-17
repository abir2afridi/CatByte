package com.example.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioClip
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.PencilTheme

/**
 * The Audio & Foley Studio sheet.
 *
 * This file owns the sheet chrome (header, tab row) only; each tab lives in
 * its own file under `ui/audio/`:
 *  - `AudioFoleyTab.kt`: the sketchbook Foley / ASMR library.
 *  - `AudioMusicTab.kt`: the music library.
 *  - `AudioSfxTab.kt`: the sound-effect library.
 *  - `AudioVoiceTab.kt`: the voice recorder and the TTS voiceover entry.
 */
@Composable
fun AudioSheet(
    isRecordingVoice: Boolean,
    voiceDurationMs: Long,
    onAddAudio: (AudioClip) -> Unit,
    onImportAudio: (() -> Unit)? = null,
    onExtractAudio: (() -> Unit)? = null,
    onStartVoiceRecord: () -> Unit,
    onStopVoiceRecord: () -> Unit,
    onTtsVoiceover: (() -> Unit)? = null,
    /** Whether music dips automatically under a voice-over. */
    autoDuckEnabled: Boolean = true,
    /** How deep the music dips (0 = full level, 1 = silent). */
    autoDuckStrength: Float = 0.35f,
    onAutoDuckEnabledChange: (Boolean) -> Unit = {},
    onAutoDuckStrengthChange: (Float) -> Unit = {},
    initialTab: Int = 0,
    onClose: () -> Unit
) {
    // The tab follows the entry point that opened the sheet (Sounds, Sound FX
    // or Record in the bottom navigation), but the user can still switch tabs.
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf("✏️ Foley", "🎵 Music", "🔔 SFX", "🎙️ Mic")

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 460.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("audio_sheet")
    ) {
        // Drag handle indicator
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

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎵", fontSize = 16.sp)
                    Text("Audio & Foley Studio", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    if (onImportAudio != null) {
                        Button(
                            onClick = { onImportAudio.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentCyan),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("audio_import_local")
                        ) {
                            Text("Import", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onExtractAudio != null) {
                        Button(
                            onClick = { onExtractAudio.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentPurple),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Extract Audio 🔊", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CapCutSurface,
                contentColor = CapCutPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CapCutPurple
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> AudioFoleyTab(onAddAudio = onAddAudio)
                1 -> AudioMusicTab(onAddAudio = onAddAudio)
                2 -> AudioSfxTab(onAddAudio = onAddAudio)
                3 -> AudioVoiceTab(
                    isRecordingVoice = isRecordingVoice,
                    voiceDurationMs = voiceDurationMs,
                    onStartVoiceRecord = onStartVoiceRecord,
                    onStopVoiceRecord = onStopVoiceRecord,
                    onTtsVoiceover = onTtsVoiceover,
                    autoDuckEnabled = autoDuckEnabled,
                    autoDuckStrength = autoDuckStrength,
                    onAutoDuckEnabledChange = onAutoDuckEnabledChange,
                    onAutoDuckStrengthChange = onAutoDuckStrengthChange
                )
            }
        }
    }
}
