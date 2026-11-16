package com.example.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.model.TextFontStyle
import kotlin.math.max

/** Draws the text / sticker / watermark layers that are burned into the export. */
object OverlayRenderer {

    /**
     * Renders a transparent canvas the same aspect ratio as the output frame and
     * draws [text] at the requested position. The canvas is scaled down to keep
     * memory usage reasonable while still covering the whole frame.
     */
    fun renderTextLayer(
        frameWidth: Int,
        frameHeight: Int,
        text: String,
        color: Long,
        fontStyle: TextFontStyle,
        posX: Float,
        posY: Float,
        scale: Float,
        withShadow: Boolean = false
    ): Bitmap? {
        if (text.isBlank()) return null
        val factor = max(1f, (max(frameWidth, frameHeight) / 960f))
        val canvasWidth = max(2, (frameWidth / factor).toInt())
        val canvasHeight = max(2, (frameHeight / factor).toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toInt()
            textSize = canvasWidth * 0.085f * scale.coerceAtLeast(0.2f)
            typeface = typefaceFor(fontStyle)
            textAlign = Paint.Align.CENTER
            if (withShadow) {
                setShadowLayer(canvasWidth * 0.02f, 0f, 0f, Color.BLACK)
            }
        }
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(110, 0, 0, 0)
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = bounds.width() + canvasWidth / 20
        val textHeight = bounds.height() + canvasWidth / 24

        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = (posX.coerceIn(0f, 1f) * canvasWidth).toInt()
        val centerY = (posY.coerceIn(0f, 1f) * canvasHeight).toInt()

        val left = centerX - textWidth / 2
        val top = centerY - textHeight / 2
        canvas.drawRoundRect(
            left.toFloat(),
            top.toFloat(),
            (left + textWidth).toFloat(),
            (top + textHeight).toFloat(),
            canvasWidth * 0.02f,
            canvasWidth * 0.02f,
            backgroundPaint
        )
        canvas.drawText(text, centerX.toFloat(), (centerY + bounds.height() / 2f), paint)
        return bitmap
    }

    /** Renders a sticker (emoji) onto a transparent full-frame canvas. */
    fun renderStickerLayer(
        frameWidth: Int,
        frameHeight: Int,
        symbol: String,
        posX: Float,
        posY: Float,
        scale: Float
    ): Bitmap? {
        if (symbol.isBlank()) return null
        val factor = max(1f, (max(frameWidth, frameHeight) / 960f))
        val canvasWidth = max(2, (frameWidth / factor).toInt())
        val canvasHeight = max(2, (frameHeight / factor).toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = canvasWidth * 0.22f * scale.coerceAtLeast(0.2f)
            textAlign = Paint.Align.CENTER
        }
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = (posX.coerceIn(0f, 1f) * canvasWidth).toInt()
        val centerY = (posY.coerceIn(0f, 1f) * canvasHeight).toInt()
        canvas.drawText(symbol, centerX.toFloat(), centerY.toFloat(), paint)
        return bitmap
    }

    /** Renders the small "CatByte" watermark in the bottom-right corner. */
    fun renderWatermarkLayer(frameWidth: Int, frameHeight: Int): Bitmap? {
        val factor = max(1f, (max(frameWidth, frameHeight) / 960f))
        val canvasWidth = max(2, (frameWidth / factor).toInt())
        val canvasHeight = max(2, (frameHeight / factor).toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = canvasWidth * 0.045f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(canvasWidth * 0.012f, 0f, 0f, Color.argb(160, 0, 0, 0))
        }
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText("CatByte", (canvasWidth * 0.97f).toInt().toFloat(), (canvasHeight * 0.96f).toInt().toFloat(), paint)
        return bitmap
    }

    private fun typefaceFor(style: TextFontStyle): Typeface = when (style) {
        TextFontStyle.CAPCUT_BOLD -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        TextFontStyle.CLEAN_SANS, TextFontStyle.BANGLA_NOTO_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        TextFontStyle.NEON_GLOW -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
        TextFontStyle.SUBTITLE, TextFontStyle.BANGLA_HIND_SILIGURI -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        else -> Typeface.DEFAULT
    }
}
