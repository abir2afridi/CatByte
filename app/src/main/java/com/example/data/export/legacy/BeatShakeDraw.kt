package com.example.data.export.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor

import kotlin.math.max
import kotlin.math.sin

/**
 * "Beat Shake" - the hand-drawn renderer for this ONE effect, isolated in its
 * own file so tuning or debugging it cannot affect any other effect.
 * Called from the export canvas; the Compose preview draws its own matching
 * approximation so the live view stays cheap.
 */
fun drawBeatShake(canvas: Canvas, effectPaint: Paint, localMs: Long, width: Float, height: Float) {
    val jitter = sin(localMs / 38.0).toFloat() * width * 0.006f
    effectPaint.color = AndroidColor.argb(26, 255, 255, 255)
    canvas.drawRect(jitter, 0f, width + jitter, height, effectPaint)
    effectPaint.color = AndroidColor.argb(18, 0, 0, 0)
    canvas.drawRect(-jitter, 0f, width - jitter, height, effectPaint)
}
