package com.example.data.export.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor

import kotlin.math.max
import kotlin.math.sin

/**
 * "Neon Halo" - the hand-drawn renderer for this ONE effect, isolated in its
 * own file so tuning or debugging it cannot affect any other effect.
 * Called from the export canvas; the Compose preview draws its own matching
 * approximation so the live view stays cheap.
 */
fun drawNeonHalo(canvas: Canvas, effectPaint: Paint, localMs: Long, width: Float, height: Float) {
    val pulse = 0.55f + 0.45f * sin(localMs / 180.0).toFloat()
    effectPaint.style = Paint.Style.STROKE
    effectPaint.strokeWidth = max(2f, width * 0.012f)
    effectPaint.color = AndroidColor.argb((70 * pulse).toInt(), 56, 189, 248)
    canvas.drawRect(2f, 2f, width - 2f, height - 2f, effectPaint)
    effectPaint.strokeWidth = max(1f, width * 0.005f)
    effectPaint.color = AndroidColor.argb((110 * pulse).toInt(), 125, 211, 252)
    canvas.drawRect(6f, 6f, width - 6f, height - 6f, effectPaint)
    effectPaint.style = Paint.Style.FILL
}
