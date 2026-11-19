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
 * "PARTICLES" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Falling particles (snow / rain / dust by parameters). */
fun particles(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val count = (14 + spec.intensity * 60).toInt()
    val fall = (h * 0.35f) * (0.3f + spec.speed * 2.2f)
    repeat(count) {
        val seedX = rng.nextFloat()
        val x = seedX * w + sin(localMs / 900f + it * 1.7f) * w * 0.02f
        val y = ((rng.nextFloat() * h + localMs / 1000f * fall) % (h + 30f)) - 15f
        val size = (2f + rng.nextFloat() * 6f) * (0.7f + spec.intensity * 0.7f)
        val alpha = (60 + spec.intensity * 120).toInt().coerceIn(0, 255)
        ops += EffectOp(
            EffectOpKind.OVAL_FILLED,
            cx = x, cy = y, radius = size,
            colorA = withAlpha(spec.colorA, alpha)
        )
    }
    return ops
}
