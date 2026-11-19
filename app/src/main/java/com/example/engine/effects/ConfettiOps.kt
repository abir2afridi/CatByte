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
 * "CONFETTI" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Rotating confetti squares falling. */
fun confetti(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val count = (16 + spec.intensity * 50).toInt()
    repeat(count) {
        val x = rng.nextFloat() * w
        val fall = (h * 0.5f) * (0.4f + spec.speed * 2.4f)
        val y = ((rng.nextFloat() * h + localMs / 1000f * fall) % (h + 40f)) - 20f
        val size = (4f + rng.nextFloat() * 9f) * (0.7f + spec.intensity * 0.6f)
        val alpha = (90 + spec.intensity * 120).toInt().coerceIn(0, 255)
        val color = when (it % 3) {
            0 -> spec.colorA
            1 -> spec.colorB
            else -> 0xFFFFFFFF.toInt()
        }
        ops += EffectOp(
            EffectOpKind.RECT,
            left = x, top = y, right = x + size, bottom = y + size,
            colorA = withAlpha(color, alpha)
        )
    }
    return ops
}
