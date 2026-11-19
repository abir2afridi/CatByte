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
 * "SPOTLIGHT" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** A soft light spot wandering across the frame. */
fun spotlight(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val t = (localMs / 1000f) * (0.3f + spec.speed * 1.5f)
    val x = w / 2f + sin(t * 2f * PI.toFloat() * 0.7f) * w * 0.3f
    val y = h / 2f + cos(t * 2f * PI.toFloat() * 0.5f) * h * 0.3f
    val radius = min(w, h) * (0.24f + spec.intensity * 0.18f)
    return listOf(
        EffectOp(
            EffectOpKind.RADIAL,
            left = x - radius * 2f, top = y - radius * 2f,
            right = x + radius * 2f, bottom = y + radius * 2f,
            colorA = withAlpha(spec.colorA, (70 + spec.intensity * 110).toInt()),
            colorB = withAlpha(spec.colorB, 0)
        )
    )
}
