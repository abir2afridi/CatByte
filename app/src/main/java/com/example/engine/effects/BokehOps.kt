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
 * "BOKEH" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Large soft translucent circles drifting upward. */
fun bokeh(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val count = (6 + (spec.intensity * 14).toInt())
    repeat(count) {
        val x = rng.nextFloat() * w
        val drift = (localMs / 1000f) * (10f + spec.speed * 60f)
        val y = ((rng.nextFloat() * h - drift) % (h + 120f))
        val radius = (18f + rng.nextFloat() * 60f) * (0.6f + spec.intensity * 0.8f)
        val alpha = (26 + spec.intensity * 44).toInt().coerceIn(0, 120)
        ops += EffectOp(
            EffectOpKind.OVAL_FILLED,
            cx = x, cy = if (y < 0f) y + h + 120f else y, radius = radius,
            colorA = withAlpha(if (it % 2 == 0) spec.colorA else spec.colorB, alpha)
        )
    }
    return ops
}
