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
 * "LIGHT_LEAK" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** A pulsing diagonal light wash with a bright streak along one edge. */
fun lightLeak(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val t = localMs / 1000f
    val pulse = 0.45f + 0.55f * sin((t * (0.6f + spec.speed * 2.4f) * 2f * PI).toFloat())
    val alpha = (spec.intensity * 110 * pulse).toInt().coerceIn(0, 255)
    return listOf(
        EffectOp(
            kind = EffectOpKind.GRADIENT_H,
            left = 0f, top = 0f, right = w, bottom = h,
            colorA = withAlpha(spec.colorA, alpha),
            colorB = withAlpha(spec.colorB, (alpha * 0.25f).toInt())
        ),
        EffectOp(
            kind = EffectOpKind.GRADIENT_H,
            left = 0f, top = h * 0.35f, right = w * 0.35f, bottom = h,
            colorA = withAlpha(spec.colorA, (alpha * 0.9f).toInt()),
            colorB = withAlpha(spec.colorB, 0)
        )
    )
}
