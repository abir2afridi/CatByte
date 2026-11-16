package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.EffectType
import com.example.model.TimelineEffect

/** Preview overlays for the clip's active visual effects. */
@Composable
internal fun ClipEffectOverlays(
    activeEffects: List<TimelineEffect>,
    pulseAlpha: Float
) {
    activeEffects.forEach { effect ->
        when (effect.effectType) {
            EffectType.RGB_SPLIT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Cyan.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
            }
            EffectType.SHAKE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((pulseAlpha * 6).toInt(), 0) }
                )
            }
            EffectType.VHS_GRAIN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.07f))
                )
            }
            EffectType.NEON_HALO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(5.dp, Color(0xFF38BDF8).copy(alpha = pulseAlpha * 0.7f))
                )
            }
            EffectType.LIGHTNING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = if (pulseAlpha > 0.95f) 0.35f else 0f))
                )
            }
            EffectType.ZOOM_BLUR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.04f)
                )
            }
        }
    }
}
