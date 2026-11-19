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
 * "SCANLINES" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** CRT scanlines scrolling down plus a faint colour cast. */
fun scanlines(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val spacing = max(6f, h / (8f + spec.intensity * 26f))
    val scroll = (localMs / 1000f) * (8f + spec.speed * 46f)
    val alpha = (30 + spec.intensity * 60).toInt().coerceIn(0, 120)
    var y = (scroll % spacing) - spacing
    while (y < h) {
        ops += EffectOp(
            EffectOpKind.RECT, 0f, y, w, y + max(1.2f, spacing * 0.22f),
            colorA = withAlpha(spec.colorA, alpha)
        )
        y += spacing
    }
    ops += EffectOp(
        EffectOpKind.RECT, 0f, 0f, w, h,
        colorA = withAlpha(spec.colorB, (18 + spec.intensity * 20).toInt())
    )
    return ops
}
