package com.example.data.export

import com.example.model.VideoFilter

/** Maps the CatByte filters onto real colour grading values. */
object FilterMapper {

    data class Grade(
        val redGain: Float,
        val greenGain: Float,
        val blueGain: Float,
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    )

    fun gradeFor(filter: VideoFilter): Grade = when (filter) {
        VideoFilter.NONE -> Grade(1f, 1f, 1f, 0f, 0f, 1f)
        VideoFilter.SKETCH_GRAPHITE -> Grade(0.94f, 0.96f, 1f, 0.06f, 0.22f, 0.15f)
        VideoFilter.BLUEPRINT -> Grade(0.42f, 0.78f, 1.25f, -0.04f, 0.18f, 0.85f)
        VideoFilter.SEPIA_NOTEBOOK -> Grade(1.22f, 1.02f, 0.72f, 0.05f, 0.05f, 0.7f)
        VideoFilter.CHARCOAL_SMUDGE -> Grade(1f, 1f, 1f, -0.1f, 0.42f, 0f)
        VideoFilter.HALFTONE_MANGA -> Grade(1f, 1f, 1f, 0.08f, 0.3f, 0.25f)
        VideoFilter.CYBERPUNK -> Grade(0.88f, 0.95f, 1.3f, -0.04f, 0.26f, 1.35f)
        VideoFilter.VINTAGE -> Grade(1.14f, 1.02f, 0.86f, 0.04f, -0.06f, 0.8f)
        VideoFilter.GOLDEN_HOUR -> Grade(1.2f, 1.04f, 0.78f, 0.08f, 0.08f, 1.15f)
        VideoFilter.NOIR -> Grade(1f, 1f, 1f, 0f, 0.45f, 0f)
        VideoFilter.TEAL_ORANGE -> Grade(0.92f, 1.03f, 1.12f, 0.04f, 0.18f, 1.2f)
        VideoFilter.MOODY -> Grade(0.92f, 0.97f, 1.06f, -0.08f, 0.12f, 0.55f)
        VideoFilter.GLITCH_ART -> Grade(1.12f, 0.94f, 1.18f, 0.02f, 0.32f, 1.45f)
    }
}
