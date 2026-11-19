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
 * "CINEMA_BARS" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Soft cinematic bars sliding slightly with a warm tint wash. */
fun cinemaBars(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val breathe = sin((localMs / 1000f) * (0.4f + spec.speed * 1.4f) * 2f * PI.toFloat()).toFloat()
    val barH = h * (0.10f + spec.intensity * 0.07f) * (1f + breathe * 0.06f)
    return listOf(
        EffectOp(EffectOpKind.RECT, 0f, 0f, w, barH, colorA = spec.colorA),
        EffectOp(EffectOpKind.RECT, 0f, h - barH, w, h, colorA = spec.colorA),
        EffectOp(
            EffectOpKind.RECT, 0f, 0f, w, h,
            colorA = withAlpha(spec.colorB, (22 + spec.intensity * 22).toInt())
        )
    )
}
