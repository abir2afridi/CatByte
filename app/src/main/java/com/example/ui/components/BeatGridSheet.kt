package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import com.example.engine.BeatGrid
import com.example.ui.theme.PencilTheme

/**
 * Beat-grid sheet: the detected magnets plus the beats pinned by hand.
 *
 * Detection cannot hear every hit, so the user can park the playhead and pin a
 * beat there. The pinned beats are saved with the project and merged into the
 * same ruler lines and snap magnets, and the whole grid is what "Cut to beats"
 * uses. The list only shows the hand-pinned beats: the detected ones are
 * already visible on the ruler and can be numerous.
 */
@Composable
fun BeatGridSheet(
    detectedBeatCount: Int,
    pinnedBeatsMs: List<Long>,
    playheadMs: Long,
    onCutToBeats: () -> Unit,
    onPinAtPlayhead: () -> Unit,
    onRemoveAtPlayhead: () -> Unit,
    onClearPinned: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onRemovePin: (Long) -> Unit = {}
) {
    val colors = PencilTheme.colors
    val pinned = pinnedBeatsMs.filter { it >= 0L }.distinct().sorted()
    val canCut = detectedBeatCount + pinned.size >= 2
    val hasPins = pinned.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(
                1.dp,
                colors.borderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("beat_grid_sheet")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Beat grid",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$detectedBeatCount detected · ${pinned.size} pinned",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("beat_grid_status")
                )
            }
            Text(
                "Done",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .testTag("beat_grid_close")
            )
        }

        Text(
            "Playhead ${BeatGrid.timecode(playheadMs)}",
            color = colors.accentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("beat_grid_playhead")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BeatAction(
                label = "Cut to beats",
                tag = "beat_grid_cut",
                enabled = canCut,
                primary = true,
                onClick = onCutToBeats
            )
            BeatAction(
                label = "Pin at playhead",
                tag = "beat_grid_pin",
                enabled = true,
                onClick = onPinAtPlayhead
            )
            BeatAction(
                label = "Remove at playhead",
                tag = "beat_grid_unpin",
                enabled = hasPins,
                onClick = onRemoveAtPlayhead
            )
            BeatAction(
                label = "Clear pins",
                tag = "beat_grid_clear",
                enabled = hasPins,
                onClick = onClearPinned
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasPins) {
            Text(
                "Pin a beat where detection missed one - pins snap, cut and save with the project.",
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(pinned, key = { it }) { at ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pinned beat",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        BeatGrid.timecode(at),
                        color = colors.accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRemovePin(at) }
                            .padding(8.dp)
                            .testTag("beat_grid_pin_remove_$at"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove pinned beat at ${BeatGrid.timecode(at)}",
                            tint = colors.accentRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BeatAction(
    label: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val colors = PencilTheme.colors
    val background = when {
        !enabled -> colors.surfaceElevated.copy(alpha = 0.4f)
        primary -> colors.accentCyan
        else -> colors.surfaceElevated
    }
    val content = when {
        !enabled -> colors.textTertiary.copy(alpha = 0.5f)
        primary -> Color.Black
        else -> colors.textPrimary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(
                1.dp,
                if (primary && enabled) colors.accentCyan else colors.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 11.dp, vertical = 7.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = content,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
