package com.example.data.export.legacy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.sin

/**
 * "RGB Split" - the hand-drawn renderer for this ONE effect, isolated in its
 * own file so tuning or debugging it cannot affect any other effect.
 * Called from the export canvas; the Compose preview draws its own matching
 * approximation so the live view stays cheap.
 */
fun drawRgbSplit(canvas: Canvas, effectPaint: Paint, localMs: Long, width: Float, height: Float) {
    val shift = width * 0.008f
    effectPaint.shader = LinearGradient(
        0f, 0f, width, 0f,
        intArrayOf(AndroidColor.argb(52, 255, 0, 80), AndroidColor.TRANSPARENT, AndroidColor.argb(52, 0, 220, 255)),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width, height, effectPaint)
    effectPaint.shader = null
    effectPaint.color = AndroidColor.argb(38, 255, 0, 40)
    canvas.drawRect(0f, 0f, shift, height, effectPaint)
    effectPaint.color = AndroidColor.argb(38, 0, 220, 255)
    canvas.drawRect(width - shift, 0f, width, height, effectPaint)
}
