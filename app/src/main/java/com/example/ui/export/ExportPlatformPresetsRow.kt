package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ExportPresets
import com.example.engine.ExportQuality
import com.example.export.ExportSettings
import com.example.model.AspectRatioType
import com.example.ui.theme.PencilTheme
import kotlin.math.roundToInt

/**
 * One-tap platform presets at the top of the export sheet: "Recommended"
 * (the project's own canvas at a balanced bitrate), TikTok/Reels/Shorts,
 * YouTube and Feed. Each chip sets resolution, fps and bitrate together, so
 * the default export "just works" for the target platform.
 */
@Composable
internal fun ExportPlatformPresetsRow(
    settings: ExportSettings,
    currentAspectRatio: AspectRatioType,
    onUpdateSettings: (ExportSettings) -> Unit
) {
    val colors = PencilTheme.colors
    val portrait = when (currentAspectRatio) {
        AspectRatioType.RATIO_16_9,
        AspectRatioType.RATIO_2_35_1,
        AspectRatioType.RATIO_1_85_1,
        AspectRatioType.RATIO_128_27 -> false
        else -> true
    }

    data class PresetChip(val label: String, val note: String, val apply: () -> ExportSettings)

    val chips = buildList {
            add(
                PresetChip("Recommended", "Best for this project") {
                    settings.copy(
                        resolution = if (portrait) "1080P" else "1080P",
                        fps = 30,
                        bitrateMbps = 10
                    )
                }
            )
            if (portrait) {
                add(
                    PresetChip("TikTok / Reels", "1080p · 30 fps · sharp") {
                        val preset = ExportPresets.portrait(quality = ExportQuality.HIGH)
                        settings.copy(
                            resolution = "1080P",
                            fps = 30,
                            bitrateMbps = bitrateToStop(preset.videoBitrateKbps)
                        )
                    }
                )
                add(
                    PresetChip("Shorts 60", "1080p · 60 fps · smooth") {
                        val preset = ExportPresets.portrait(
                            quality = ExportQuality.MAX,
                            fps = 60
                        )
                        settings.copy(
                            resolution = "1080P",
                            fps = 60,
                            bitrateMbps = bitrateToStop(preset.videoBitrateKbps)
                        )
                    }
                )
                add(
                    PresetChip("Data saver", "720p · small upload") {
                        settings.copy(resolution = "720P", fps = 30, bitrateMbps = 5)
                    }
                )
            } else {
                add(
                    PresetChip("YouTube", "1080p · 30 fps · 16:9") {
                        val preset = ExportPresets.landscape(quality = ExportQuality.HIGH)
                        settings.copy(
                            resolution = "1080P",
                            fps = 30,
                            bitrateMbps = bitrateToStop(preset.videoBitrateKbps)
                        )
                    }
                )
                add(
                    PresetChip("YouTube 2K", "1440p-class · crisp") {
                        settings.copy(resolution = "2K", fps = 30, bitrateMbps = 50)
                    }
                )
                add(
                    PresetChip("Data saver", "720p · small upload") {
                        settings.copy(resolution = "720P", fps = 30, bitrateMbps = 5)
                    }
                )
            }
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Platform presets",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { chip ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(118.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .clickable { onUpdateSettings(chip.apply()) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("export_preset_${chip.label.replace(" ", "_").lowercase()}")
                ) {
                    Text(
                        text = chip.label,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = chip.note,
                        color = colors.textTertiary,
                        fontSize = 9.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        Text(
            text = "Chips set resolution, frame rate and bitrate together - fine-tune below.",
            color = colors.textTertiary,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

/** Maps a kbps value onto the bitrate selector's stops (5/10/20/50/100 Mbps). */
internal fun bitrateToStop(videoBitrateKbps: Int): Int {
    val stops = listOf(5, 10, 20, 50, 100)
    val mbps = (videoBitrateKbps / 1_000.0).roundToInt().coerceAtLeast(1)
    return stops.firstOrNull { mbps <= it } ?: stops.last()
}
