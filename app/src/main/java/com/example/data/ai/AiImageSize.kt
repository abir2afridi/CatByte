package com.example.data.ai

/**
 * Requested output size for image generation, chosen from the project canvas.
 *
 * DALL·E 3 (and compatible gateways) accept square, portrait and landscape
 * sizes. Asking for a square image for a 9:16 project meant the background had
 * to be cropped hard; matching the canvas keeps the composition intact.
 */
object AiImageSize {

    const val SQUARE = "1024x1024"
    const val PORTRAIT = "1024x1792"
    const val LANDSCAPE = "1792x1024"

    /** Picks the closest supported shape for a width/height [ratio]. */
    fun forAspect(ratio: Float): String {
        if (!ratio.isFinite() || ratio <= 0f) return SQUARE
        return when {
            ratio < 0.9f -> PORTRAIT
            ratio > 1.1f -> LANDSCAPE
            else -> SQUARE
        }
    }
}
