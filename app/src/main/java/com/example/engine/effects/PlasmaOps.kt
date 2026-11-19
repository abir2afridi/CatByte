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
 * "PLASMA" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Slow drifting colour blobs. */
fun plasma(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val t = (localMs / 1000f) * (0.2f + spec.speed * 1.1f)
    repeat(3) { i ->
        val x = w * (0.5f + 0.36f * sin(t * 2f * PI.toFloat() + i * 2.1f))
        val y = h * (0.5f + 0.36f * cos(t * 2f * PI.toFloat() + i * 1.3f))
        val radius = min(w, h) * (0.42f + spec.intensity * 0.2f)
        val alpha = (44 + spec.intensity * 50).toInt().coerceIn(0, 140)
        ops += EffectOp(
            EffectOpKind.RADIAL,
            left = x - radius, top = y - radius,
            right = x + radius, bottom = y + radius,
            colorA = withAlpha(if (i % 2 == 0) spec.colorA else spec.colorB, alpha),
            colorB = withAlpha(if (i % 2 == 0) spec.colorB else spec.colorA, 0)
        )
    }
    return ops
}
