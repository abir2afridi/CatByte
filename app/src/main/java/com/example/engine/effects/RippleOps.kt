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
 * "RIPPLE" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Concentric ripples expanding from the bottom centre. */
fun ripple(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val period = 1500f - spec.speed * 1000f
    repeat(3) { i ->
        val phase = ((localMs / period.toLong()) + i.toFloat() / 3f) % 1f
        val radius = phase * w * 0.6f
        val alpha = ((1f - phase) * spec.intensity * 120).toInt().coerceIn(0, 255)
        ops += EffectOp(
            EffectOpKind.CIRCLE_STROKE,
            cx = w / 2f, cy = h * 0.86f, radius = radius.coerceAtLeast(3f),
            colorA = withAlpha(spec.colorA, alpha),
            strokeWidth = max(1.5f, w * 0.005f)
        )
    }
    return ops
}
