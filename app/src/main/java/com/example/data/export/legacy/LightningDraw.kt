package com.example.data.export.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor

import kotlin.math.max
import kotlin.math.sin

/**
 * "Lightning" - the hand-drawn renderer for this ONE effect, isolated in its
 * own file so tuning or debugging it cannot affect any other effect.
 * Called from the export canvas; the Compose preview draws its own matching
 * approximation so the live view stays cheap.
 */
fun drawLightning(canvas: Canvas, effectPaint: Paint, localMs: Long, width: Float, height: Float) {
    val phase = localMs % 1150L
    if (phase < 75L) {
        effectPaint.color = AndroidColor.argb(120, 255, 255, 255)
        canvas.drawRect(0f, 0f, width, height, effectPaint)
    }
}
