package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaClip
import com.example.model.VideoFilter
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

/** The top status row (the active-filter pill). */
@Composable
internal fun BoxScope.CanvasTopStatusRow(activeClip: MediaClip?) {
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
