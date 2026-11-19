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
 * "PIXEL_BLOCKS" effect family renderer - the ONLY place this family's drawing maths
 * live. Editing this file cannot affect any other effect family.
 */
/** Translucent mosaic squares sliding across the frame. */
fun pixelBlocks(
    spec: EffectSpec,
    localMs: Long,
    rng: Random,
    w: Float,
    h: Float
): List<EffectOp> {
    val ops = mutableListOf<EffectOp>()
    val cell = max(14f, w / (10f + spec.intensity * 22f))
    val cols = (w / cell).toInt() + 1
    val rows = (h / cell).toInt() + 1
    val shift = ((localMs / 1000f) * (12f + spec.speed * 90f)) % cell
    repeat((cols * rows * 0.16f).toInt().coerceAtLeast(6)) {
        val col = rng.nextInt(cols)
        val row = rng.nextInt(rows)
        val alpha = (24 + spec.intensity * 70).toInt().coerceIn(0, 150)
        ops += EffectOp(
            EffectOpKind.RECT,
            left = col * cell + shift * (if (row % 2 == 0) 1f else -1f),
            top = row * cell,
            right = col * cell + shift * (if (row % 2 == 0) 1f else -1f) + cell * 0.92f,
            bottom = row * cell + cell * 0.92f,
            colorA = withAlpha(if (rng.nextBoolean()) spec.colorA else spec.colorB, alpha)
        )
    }
    return ops
}
