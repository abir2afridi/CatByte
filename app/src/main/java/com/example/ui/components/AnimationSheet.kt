package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ClipAnimationMath
import com.example.model.ClipAnimation
import com.example.ui.theme.PencilTheme
import java.util.Locale

/**
 * Clip entry animations.
 *
 * The chosen animation is stored on the clip and applied by the shared render
 * graph (zoom settle, swipe-in, damped bounce, spin and fade from black), so
 * the preview and the export stay in sync. The duration slider writes the
 * clip's real animation length, which the render graph samples the curve
 * against.
 */
@Composable
fun AnimationSheet(
    current: ClipAnimation,
    /** Length of the entry animation in milliseconds. */
    currentDurationMs: Long,
    onSelect: (ClipAnimation) -> Unit,
    /** Receives the new length of the entry animation, in milliseconds. */
    onDurationChanged: (Long) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("animation_sheet")
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
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The panel edits the clip's entry animation - that is the only
            // animation the model stores - so it says so instead of showing
            // tabs that would not change anything.
            Column {
                Text(
                    text = "Entry animation",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "How the clip arrives on the timeline",
                    color = colors.textTertiary,
                    fontSize = 9.sp
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Close", tint = colors.accentCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClipAnimation.entries.forEach { animation ->
                val selected = animation == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) colors.accentCyan.copy(alpha = 0.18f) else colors.surfaceElevated)
                        .border(
                            1.2.dp,
                            if (selected) colors.accentCyan else colors.borderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(animation) }
                        .padding(vertical = 10.dp)
                        .testTag("animation_${animation.name.lowercase()}")
                ) {
                    Text(
                        text = animation.displayName,
                        color = if (selected) colors.accentCyan else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (animation == ClipAnimation.NONE) {
                            "Off"
                        } else {
                            String.format(Locale.US, "%.1fs", currentDurationMs / 1000f)
                        },
                        color = colors.textTertiary,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimationDurationRow(
            durationMs = currentDurationMs,
            onDurationChanged = onDurationChanged
        )
    }
}

/**
 * The duration slider of the animation panel.
 *
 * The draft value follows the committed one while the sheet is idle, and the
 * gesture itself is the source of truth - the ViewModel coalesces the whole
 * drag into a single undo step.
 */
@Composable
private fun AnimationDurationRow(
    durationMs: Long,
    onDurationChanged: (Long) -> Unit
) {
    val colors = PencilTheme.colors
    val range = ClipAnimationMath.MIN_DURATION_MS.toFloat()..ClipAnimationMath.MAX_DURATION_MS.toFloat()
    val committed = durationMs.toFloat().coerceIn(range.start, range.endInclusive)
    var draft by remember { mutableFloatStateOf(committed) }

    LaunchedEffect(committed) {
        draft = committed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Duration:", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = draft.coerceIn(range.start, range.endInclusive),
            onValueChange = {
                draft = it.coerceIn(range.start, range.endInclusive)
                onDurationChanged(draft.toLong())
            },
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .testTag("animation_duration"),
            colors = SliderDefaults.colors(
                thumbColor = colors.accentCyan,
                activeTrackColor = colors.accentCyan
            )
        )
        Text(
            text = String.format(Locale.US, "%.1fs", draft / 1000f),
            color = colors.accentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
