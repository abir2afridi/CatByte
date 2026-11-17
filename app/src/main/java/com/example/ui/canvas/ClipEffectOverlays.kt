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
    pulseAlpha: Float,
    /** Clip-local time driving the library effects' animation. */
    localMs: Long = 0L
) {
    // Library presets: the same EffectOps the export renderer consumes,
    // replayed here with Compose so preview and output match.
    activeEffects
        .filter { it.effectType !in LEGACY_PRESETS }
        .forEach { effect ->
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val spec = effect.effectType.spec.let {
                    it.copy(intensity = (it.intensity * effect.intensityScale).coerceIn(0.05f, 1.6f))
                }
                com.example.engine.EffectOps.compute(
                    effect.effectType,
                    spec,
                    localMs,
                    size.width,
                    size.height
                ).forEach { op -> drawOp(op) }
            }
        }

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
            else -> Unit // rendered by the EffectOps canvas above
        }
    }
}

/** The six original presets keep their hand-drawn preview boxes. */
private val LEGACY_PRESETS = setOf<EffectType>(
    EffectType.RGB_SPLIT,
    EffectType.SHAKE,
    EffectType.VHS_GRAIN,
    EffectType.NEON_HALO,
    EffectType.LIGHTNING,
    EffectType.ZOOM_BLUR
)

/** Draws one [com.example.engine.EffectOp] with Compose DrawScope primitives. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOp(
    op: com.example.engine.EffectOp
) {
    val colorA = androidx.compose.ui.graphics.Color(op.colorA)
    val colorB = androidx.compose.ui.graphics.Color(op.colorB)
    when (op.kind) {
        com.example.engine.EffectOpKind.RECT -> drawRect(
            color = colorA,
            topLeft = androidx.compose.ui.geometry.Offset(op.left, op.top),
            size = androidx.compose.ui.geometry.Size(
                (op.right - op.left).coerceAtLeast(0.5f),
                (op.bottom - op.top).coerceAtLeast(0.5f)
            )
        )
        com.example.engine.EffectOpKind.GRADIENT_H -> drawRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(colorA, colorB),
                startX = op.left,
                endX = op.right
            ),
            topLeft = androidx.compose.ui.geometry.Offset(op.left, op.top),
            size = androidx.compose.ui.geometry.Size(
                (op.right - op.left).coerceAtLeast(0.5f),
                (op.bottom - op.top).coerceAtLeast(0.5f)
            )
        )
        com.example.engine.EffectOpKind.RADIAL -> drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(colorA, colorB),
                center = androidx.compose.ui.geometry.Offset(
                    (op.left + op.right) / 2f,
                    (op.top + op.bottom) / 2f
                ),
                radius = maxOf(op.right - op.left, op.bottom - op.top) / 2f
            ),
            topLeft = androidx.compose.ui.geometry.Offset(op.left, op.top),
            size = androidx.compose.ui.geometry.Size(
                (op.right - op.left).coerceAtLeast(0.5f),
                (op.bottom - op.top).coerceAtLeast(0.5f)
            )
        )
        com.example.engine.EffectOpKind.OVAL_FILLED -> drawCircle(
            color = colorA,
            radius = op.radius.coerceAtLeast(0.5f),
            center = androidx.compose.ui.geometry.Offset(op.cx, op.cy)
        )
        com.example.engine.EffectOpKind.CIRCLE_STROKE -> drawCircle(
            color = colorA,
            radius = op.radius.coerceAtLeast(1f),
            center = androidx.compose.ui.geometry.Offset(op.cx, op.cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = op.strokeWidth.coerceAtLeast(1f))
        )
        com.example.engine.EffectOpKind.RECT_STROKE -> drawRect(
            color = colorA,
            topLeft = androidx.compose.ui.geometry.Offset(op.left, op.top),
            size = androidx.compose.ui.geometry.Size(
                (op.right - op.left).coerceAtLeast(1f),
                (op.bottom - op.top).coerceAtLeast(1f)
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = op.strokeWidth.coerceAtLeast(1f))
        )
        com.example.engine.EffectOpKind.LINE -> drawLine(
            color = colorA,
            start = androidx.compose.ui.geometry.Offset(op.cx, op.cy),
            end = androidx.compose.ui.geometry.Offset(op.left, op.top),
            strokeWidth = op.strokeWidth.coerceAtLeast(1f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
