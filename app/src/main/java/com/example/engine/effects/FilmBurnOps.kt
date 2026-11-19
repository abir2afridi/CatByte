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
 * "FILM_BURN" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Warm blotchy burn patches drifting with a flicker. */
fun filmBurn(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val flicker = 0.75f + 0.25f * sin(localMs / 120.0).toFloat()
    repeat((5 + (spec.intensity * 9).toInt())) {
        val drift = (localMs / 1000f) * (14f + spec.speed * 70f)
        val x = rng.nextFloat() * w
        val y = ((rng.nextFloat() * h + drift) % (h + 80f)) - 40f
        val radius = (24f + rng.nextFloat() * 90f) * (0.6f + spec.intensity * 0.7f)
        val alpha = (34 * flicker * (0.7f + spec.intensity * 0.6f)).toInt().coerceIn(0, 150)
        ops += EffectOp(
            EffectOpKind.RADIAL,
            left = x - radius, top = y - radius,
            right = x + radius, bottom = y + radius,
            colorA = withAlpha(spec.colorA, alpha),
            colorB = withAlpha(spec.colorB, 0)
        )
    }
    return ops
}
