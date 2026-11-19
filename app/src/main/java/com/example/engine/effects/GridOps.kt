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
 * "GRID" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** A pulsing perspective-style grid. */
fun grid(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val cell = max(16f, w / (8f + spec.intensity * 16f))
    val pulse = 0.6f + 0.4f * sin((localMs / 1000f) * (0.7f + spec.speed * 2.6f) * 2f * PI).toFloat()
    val alpha = (34 + spec.intensity * 80 * pulse).toInt().coerceIn(0, 170)
    val stroke = max(1f, w * 0.0018f)
    var x = 0f
    while (x <= w) {
        ops += EffectOp(
            EffectOpKind.LINE, cx = x, cy = 0f, left = x, top = h,
            colorA = withAlpha(spec.colorA, alpha), strokeWidth = stroke
        )
        x += cell
    }
    var y = ((localMs / 1000f) * (10f + spec.speed * 50f)) % cell
    while (y <= h) {
        ops += EffectOp(
            EffectOpKind.LINE, cx = 0f, cy = y, left = w, top = y,
            colorA = withAlpha(spec.colorA, alpha), strokeWidth = stroke
        )
        y += cell
    }
    return ops
}
