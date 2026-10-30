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
}
