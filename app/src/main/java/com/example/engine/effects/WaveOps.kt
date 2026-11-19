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
 * "WAVE" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Translucent sine bands sliding sideways. */
fun wave(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val bands = (4 + (spec.intensity * 8).toInt())
    val speed = 0.5f + spec.speed * 2.5f
    for (i in 0 until bands) {
        val phase = sin((localMs / 1000f) * speed * 2f * PI + i * 1.1f).toFloat()
        val bandH = h * 0.05f * (1f + 0.4f * phase)
        val y = h * (i + 0.5f) / bands + phase * h * 0.04f
        val alpha = (20 + spec.intensity * 55 * (0.6f + 0.4f * phase)).toInt().coerceIn(0, 160)
        ops += EffectOp(
            EffectOpKind.RECT, 0f, y - bandH / 2f, w, y + bandH / 2f,
            colorA = withAlpha(if (i % 2 == 0) spec.colorA else spec.colorB, alpha)
        )
    }
    return ops
}
