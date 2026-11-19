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
 * "STARFIELD" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Twinkling star dots. */
fun starfield(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val count = (24 + spec.intensity * 80).toInt()
    repeat(count) {
        val x = rng.nextFloat() * w
        val y = rng.nextFloat() * h
        val twinkle = 0.4f + 0.6f * abs(sin(localMs / 260f + it * 2.4f))
        val radius = (1f + rng.nextFloat() * 2.6f) * (0.7f + spec.intensity * 0.6f)
        val alpha = (70 + spec.intensity * 130 * twinkle).toInt().coerceIn(0, 255)
        ops += EffectOp(
            EffectOpKind.OVAL_FILLED,
            cx = x, cy = y, radius = radius,
            colorA = withAlpha(spec.colorA, alpha)
        )
    }
    return ops
}
