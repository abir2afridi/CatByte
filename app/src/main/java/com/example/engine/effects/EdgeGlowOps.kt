package com.example.engine.effects

import com.example.engine.EffectOp
import com.example.engine.EffectOpKind
import com.example.engine.withAlpha
import com.example.model.EffectSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * "EDGE_GLOW" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** A pulsing coloured border (neon frame). */
fun edgeGlow(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val t = localMs / 1000f
    val pulse = 0.5f + 0.5f * sin((t * (0.8f + spec.speed * 3f) * 2f * PI).toFloat())
    val alpha = (60 + spec.intensity * 150 * pulse).toInt().coerceIn(0, 255)
    val stroke = max(2f, w * (0.008f + spec.intensity * 0.010f))
    return listOf(
        EffectOp(
            EffectOpKind.RECT_STROKE,
            left = 2f, top = 2f, right = w - 2f, bottom = h - 2f,
            colorA = withAlpha(spec.colorA, alpha),
            strokeWidth = stroke
        ),
        EffectOp(
            EffectOpKind.RECT_STROKE,
            left = 6f, top = 6f, right = w - 6f, bottom = h - 6f,
            colorA = withAlpha(spec.colorB, (alpha * 0.7f).toInt()),
            strokeWidth = stroke * 0.5f
        )
    )
}
