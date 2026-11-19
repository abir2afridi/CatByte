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
 * "RINGS" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Expanding concentric rings from the centre. */
fun rings(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val period = 1600f - spec.speed * 1100f
    val ops = mutableListOf<EffectOp>()
    val count = 4
    for (i in 0 until count) {
        val phase = (((localMs / period.toLong()) + i.toFloat() / count) % 1f)
        val radius = (phase * max(w, h) * 0.75f).coerceAtLeast(2f)
        val alpha = ((1f - phase) * spec.intensity * 150).toInt().coerceIn(0, 255)
        ops += EffectOp(
            EffectOpKind.CIRCLE_STROKE,
            cx = w / 2f, cy = h / 2f, radius = radius,
            colorA = withAlpha(spec.colorA, alpha),
            strokeWidth = max(1.5f, w * 0.004f)
        )
    }
    return ops
}
