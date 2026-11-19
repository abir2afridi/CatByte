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
 * "STROBE" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Full-frame flash at a fixed period, [spec.speed] sets the tempo. */
fun strobe(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val periodMs = (1400f - spec.speed * 1150f).coerceAtLeast(120f)
    val phase = (localMs % periodMs.toLong()) / periodMs
    val on = phase < 0.18f
    val alpha = if (on) (spec.intensity * 165).toInt() else 0
    if (alpha <= 0) return emptyList()
    return listOf(
        EffectOp(EffectOpKind.RECT, 0f, 0f, w, h, colorA = withAlpha(spec.colorA, alpha)),
        EffectOp(
            EffectOpKind.RECT, 0f, 0f, w, h,
            colorA = withAlpha(0xFFFFFFFF.toInt(), (alpha * 0.35f).toInt())
        )
    )
}
