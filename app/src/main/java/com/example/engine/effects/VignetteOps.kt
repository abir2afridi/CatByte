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
 * "VIGNETTE" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Pulsing dark vignette pulling the eye to the centre. */
fun vignette(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val t = localMs / 1000f
    val pulse = 0.6f + 0.4f * sin((t * (0.5f + spec.speed * 2f) * 2f * PI).toFloat())
    val alpha = (spec.intensity * 190 * pulse).toInt().coerceIn(0, 255)
    return listOf(
        EffectOp(
            EffectOpKind.RADIAL,
            left = 0f, top = 0f, right = w, bottom = h,
            colorA = withAlpha(0x00000000, 0),
            colorB = withAlpha(spec.colorA, alpha)
        )
    )
}
