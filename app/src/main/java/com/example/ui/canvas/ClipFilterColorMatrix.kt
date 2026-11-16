package com.example.ui.canvas

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.VideoFilter

/**
 * Colour-matrix preset for a clip's selected filter, or null for
 * [VideoFilter.NONE]. Kept in its own file so the preset table can be found
 * and tuned without reading the whole preview renderer.
 */
internal fun clipFilterColorMatrix(filter: VideoFilter): ColorFilter? = when (filter) {
    VideoFilter.SKETCH_GRAPHITE -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 15f,
            0.33f, 0.33f, 0.33f, 0f, 12f,
            0.33f, 0.33f, 0.33f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.BLUEPRINT -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            0.1f, 0.2f, 0.4f, 0f, -30f,
            0.2f, 0.5f, 0.8f, 0f, 10f,
            0.4f, 0.7f, 1.4f, 0f, 50f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.SEPIA_NOTEBOOK -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 25f,
            0.349f, 0.686f, 0.168f, 0f, 15f,
            0.272f, 0.534f, 0.131f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.CHARCOAL_SMUDGE -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            1.3f, 1.3f, 1.3f, 0f, -50f,
            1.3f, 1.3f, 1.3f, 0f, -50f,
            1.3f, 1.3f, 1.3f, 0f, -50f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.HALFTONE_MANGA -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            1.4f, 0f, 0f, 0f, -20f,
            0f, 1.4f, 0f, 0f, -20f,
            0f, 0f, 1.4f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.CYBERPUNK -> ColorFilter.colorMatrix(ColorMatrix().apply {
        setToSaturation(1.4f)
    })
    VideoFilter.VINTAGE -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            1.2f, 0.1f, 0.1f, 0f, 15f,
            0.1f, 1.0f, 0.1f, 0f, 10f,
            0.1f, 0.1f, 0.8f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.GOLDEN_HOUR -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            1.3f, 0.2f, 0f, 0f, 20f,
            0.1f, 1.1f, 0f, 0f, 10f,
            0f, 0f, 0.7f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.NOIR -> ColorFilter.colorMatrix(ColorMatrix().apply {
        setToSaturation(0f)
    })
    VideoFilter.TEAL_ORANGE -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            1.25f, 0f, 0f, 0f, 10f,
            0f, 1.15f, 0.1f, 0f, 0f,
            0f, 0.1f, 1.35f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.MOODY -> ColorFilter.colorMatrix(ColorMatrix().apply {
        setToSaturation(0.6f)
    })
    VideoFilter.GLITCH_ART -> ColorFilter.colorMatrix(ColorMatrix(
        floatArrayOf(
            1.5f, 0f, 0f, 0f, 30f,
            0f, 0.8f, 0f, 0f, -10f,
            0f, 0f, 1.5f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        )
    ))
    VideoFilter.NONE -> null
}
