package com.example.data.export.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.sin

/**
 * "VHS Grain" - the hand-drawn renderer for this ONE effect, isolated in its
 * own file so tuning or debugging it cannot affect any other effect.
 * Called from the export canvas; the Compose preview draws its own matching
 * approximation so the live view stays cheap.
 */
fun drawVhsGrain(canvas: Canvas, effectPaint: Paint, localMs: Long, width: Float, height: Float) {
    val random = Random(localMs / 33L + 7L)
    effectPaint.color = AndroidColor.argb(34, 255, 255, 255)
    repeat(46) {
        val y = random.nextFloat() * height
        val thickness = 1f + random.nextFloat() * 2.2f
        canvas.drawRect(0f, y, width, y + thickness, effectPaint)
    }
    effectPaint.color = AndroidColor.argb(18, 0, 0, 0)
    repeat(18) {
        val y = random.nextFloat() * height
        canvas.drawRect(0f, y, width, y + 3f, effectPaint)
    }
}
