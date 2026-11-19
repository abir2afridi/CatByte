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
 * "BEAMS" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Rotating club beams from the top centre. */
fun beams(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val count = 5
    val spin = (localMs / 1000f) * (0.35f + spec.speed * 1.6f)
    for (i in 0 until count) {
        val angle = spin + i * (2f * PI.toFloat() / count)
        val length = max(w, h)
        val endX = w / 2f + cos(angle) * length
        val endY = h / 2f + sin(angle) * length
        val alpha = (spec.intensity * 90 * (0.55f + 0.45f * sin(angle * 3f))).toInt()
            .coerceIn(0, 255)
        ops += EffectOp(
            EffectOpKind.LINE,
            cx = w / 2f, cy = h / 2f, left = endX, top = endY,
            colorA = withAlpha(spec.colorA, alpha),
            strokeWidth = max(2f, w * (0.006f + 0.004f * rng.nextFloat()))
        )
    }
    return ops
}
