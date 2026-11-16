package com.example.engine

import com.example.model.AspectRatioType
import kotlin.math.abs

/**
 * Chooses the closest canvas ratio for imported footage.
 *
 * The editor ships a set of fixed ratios (9:16, 16:9, 1:1, 4:5, 4:3, 3:4,
 * 5:8, 128:27, 2:1, 2.35:1 and 1.85:1), so an unusual source (21:9, 5:4, ...)
 * maps to the nearest one instead of being forced into 9:16. The bottom
 * navigation's "Original" chip uses the same mapping, which is what keeps the
 * canvas close to the imported footage.
 */
object AspectMath {

    fun fromVideo(width: Int, height: Int, rotation: Int = 0): AspectRatioType {
        if (width <= 0 || height <= 0) return AspectRatioType.RATIO_9_16
        val swapped = rotation % 180 == 90 || rotation % 180 == -90
        val w = if (swapped) height else width
        val h = if (swapped) width else height
        return closestTo(w.toFloat() / h.toFloat())
    }

    /** The preset whose width/height ratio is closest to [ratio]. */
    fun closestTo(ratio: Float): AspectRatioType {
        if (ratio <= 0f) return AspectRatioType.RATIO_9_16
        return AspectRatioType.entries.minByOrNull { candidate ->
            abs(candidate.ratioWidth / candidate.ratioHeight - ratio)
        } ?: AspectRatioType.RATIO_9_16
    }
}
