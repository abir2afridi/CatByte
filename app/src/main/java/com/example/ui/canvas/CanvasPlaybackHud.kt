package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.player.PlaybackAudioMeter
import com.example.model.MediaClip
import com.example.model.VideoFilter
import com.example.ui.components.PlaybackLevelBar
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.sketchPencilBorder

/**
 * The canvas HUD: the paused play button, the active-filter pill and the
 * render-fidelity notice.
 */

/** The centered play button shown while the preview is paused. */
@Composable
internal fun CanvasPlayHud() {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(colors.surface.copy(alpha = 0.90f))
            .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 29.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = colors.textPrimary,
            modifier = Modifier
                .size(34.dp)
                .offset(x = 2.dp)
        )
    }
}

/** The pill naming the filter that is currently applied to the active clip. */
@Composable
internal fun CanvasFilterPill(activeClip: MediaClip?) {
    val colors = PencilTheme.colors
    if (activeClip == null || activeClip.filter == VideoFilter.NONE) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.accentCyan)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = activeClip.filter.displayName,
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * The render-fidelity notice.
 *
 * Warnings are never hidden: if a mask or blend mode is approximated, the
 * user sees it before exporting. A stored player error is hidden while the
 * extracted scrub frame is on screen: that frame is decoded from the source
 * and proves the preview is working, so the pill would be stale noise.
 */
@Composable
internal fun BoxScope.CanvasPreviewNotice(
    previewError: String?,
    previewWarnings: List<String>,
    hasScrubFrame: Boolean
) {
    val colors = PencilTheme.colors
    val shownError = previewError.takeIf { !hasScrubFrame }
    val previewNotice = shownError?.let { "Preview error: $it" }
        ?: previewWarnings.firstOrNull()
    if (previewNotice == null) return

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 36.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (shownError != null) {
                    colors.accentRed.copy(alpha = 0.92f)
                } else {
                    colors.accentYellow.copy(alpha = 0.92f)
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (previewWarnings.size > 1) {
                "$previewNotice (+${previewWarnings.size - 1} more)"
            } else {
                previewNotice
            },
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
    }
}

/**
 * The real output peak meter (L / R), fed by the audio that is actually
 * playing. Lives ABOVE the preview canvas - it never covers the user's
 * picture (moved out of the video overlay on user request).
 */
@Composable
internal fun PlaybackMeterStrip(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) {
        Box(modifier = modifier.height(0.dp))
        return
    }
    val levels by PlaybackAudioMeter.levels.collectAsState()
    // Volume-style pill: speaker icon + one labelled channel bar per side.
    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Output volume meter",
            tint = Color(0xFFA5A8B2),
            modifier = Modifier.size(12.dp)
        )
        MeterChannel("L", levels.left, Color(0xFF26E5D7))
        MeterChannel("R", levels.right, Color(0xFFFFC107))
    }
}

/** One labelled channel bar of the output meter, with progress semantics. */
@Composable
private fun MeterChannel(label: String, level: Float, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color(0xFFA5A8B2),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
        val clamped = level.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(color, color.copy(alpha = 0.45f))
                        )
                    )
            )
        }
    }
}

/** The top status row of the video overlay: only the active-filter pill now. */
@Composable
internal fun BoxScope.CanvasTopStatusRow(activeClip: MediaClip?, isPlaying: Boolean = false) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CanvasFilterPill(activeClip)
    }
}
