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
 * "GRAIN" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Speckle noise shimmering per noise-frame. */
fun grain(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val count = (30 + spec.intensity * 170).toInt()
    val alpha = (26 + spec.intensity * 66).toInt().coerceIn(0, 140)
    val ops = mutableListOf<EffectOp>()
    repeat(count) {
        val x = rng.nextFloat() * w
        val y = rng.nextFloat() * h
        val size = 1.5f + rng.nextFloat() * 4.5f
        val bright = rng.nextFloat() > 0.45f
        ops += EffectOp(
            EffectOpKind.RECT, x, y,
            min(w, x + size), min(h, y + size),
            colorA = withAlpha(if (bright) spec.colorA else spec.colorB, alpha)
        )
    }
    return ops
}
