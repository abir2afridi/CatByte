package com.example.data.export.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor

import kotlin.math.max
import kotlin.math.sin

/**
 * "Zoom Blur" - the hand-drawn renderer for this ONE effect, isolated in its
 * own file so tuning or debugging it cannot affect any other effect.
 * Called from the export canvas; the Compose preview draws its own matching
 * approximation so the live view stays cheap.
 */
fun drawZoomBlur(canvas: Canvas, effectPaint: Paint, localMs: Long, width: Float, height: Float) {
    effectPaint.style = Paint.Style.STROKE
    effectPaint.strokeWidth = max(1f, width * 0.003f)
    val rings = 5
    for (i in 0 until rings) {
        val fraction = (i + 1f) / rings
        val alpha = (18 * (1f - fraction)).toInt().coerceAtLeast(0)
        effectPaint.color = AndroidColor.argb(alpha, 255, 255, 255)
        val inset = width * 0.06f * fraction
        canvas.drawRect(inset, inset, width - inset, height - inset, effectPaint)
    }
    effectPaint.style = Paint.Style.FILL
}
