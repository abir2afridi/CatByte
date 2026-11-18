package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * One channel of a live peak meter.
 *
 * [level] comes from [com.example.data.player.PlaybackAudioMeter], i.e. the
 * audio that is actually leaving the preview sink. A new peak jumps the bar up
 * instantly; when the audio falls the bar glides down, which is what a VU
 * meter does. The background is the unlit track, supplied by the caller's
 * modifier.
 */
@Composable
internal fun PlaybackLevelBar(
    level: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated = remember { Animatable(0f) }
    val target = level.coerceIn(0f, 1f)
    LaunchedEffect(target) {
        if (target >= animated.value) {
            animated.snapTo(target)
        } else {
            animated.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 320, easing = LinearEasing)
            )
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(1.5.dp))
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(animated.value, 0f..1f)
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(maxWidth * animated.value)
                .fillMaxHeight()
                .background(color)
        )
    }
}
