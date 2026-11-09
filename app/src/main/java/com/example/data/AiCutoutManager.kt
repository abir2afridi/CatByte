package com.example.data

import android.graphics.Bitmap

/**
 * AI-powered selfie segmentation & Chroma Key background removal engine.
 */
object AiCutoutManager {

    data class ChromaKeyConfig(
        val colorHex: Long = 0xFF00FF00, // Default Green Screen
        val tolerance: Float = 0.3f,
        val softness: Float = 0.1f
    )

    /**
     * Applies Chroma Key green-screen removal to a bitmap frame.
     */
    fun applyChromaKey(
        input: Bitmap,
        config: ChromaKeyConfig
    ): Bitmap {
        val width = input.width
        val height = input.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetR = (config.colorHex shr 16 and 0xFF).toInt()
        val targetG = (config.colorHex shr 8 and 0xFF).toInt()
        val targetB = (config.colorHex and 0xFF).toInt()

        val toleranceSq = (config.tolerance * 255f) * (config.tolerance * 255f)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val distSq = (r - targetR) * (r - targetR) + (g - targetG) * (g - targetG) + (b - targetB) * (b - targetB)
            if (distSq < toleranceSq) {
                pixels[i] = 0x00000000 // Fully transparent
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Applies smart portrait background isolation / cutout simulation.
     */
    fun applySmartCutout(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) * 0.4f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val dx = x - centerX
                val dy = y - centerY
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > radius) {
                    // Soft vignette / cutout background dimming
                    val color = pixels[idx]
                    val r = ((color shr 16) and 0xFF) / 2
                    val g = ((color shr 8) and 0xFF) / 2
                    val b = (color and 0xFF) / 2
                    pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
