package com.example.engine

import com.example.model.AspectRatioType

/**
 * Chooses the closest canvas ratio for imported footage.
 *
 * The project only ships 9:16/16:9/1:1/4:5 presets, so an unusual source (3:4,
 * 21:9) maps to the nearest one instead of being forced into 9:16.
 */
object AspectMath {

    fun fromVideo(width: Int, height: Int, rotation: Int = 0): AspectRatioType {
        if (width <= 0 || height <= 0) return AspectRatioType.RATIO_9_16
        val swapped = rotation % 180 == 90 || rotation % 180 == -90
        val w = if (swapped) height else width
        val h = if (swapped) width else height
        val ratio = w.toFloat() / h.toFloat()
        return when {
            ratio >= 1.75f -> AspectRatioType.RATIO_16_9
            ratio <= 0.65f -> AspectRatioType.RATIO_9_16
            ratio <= 0.88f -> AspectRatioType.RATIO_4_5
            else -> AspectRatioType.RATIO_1_1
        }
    }
}
