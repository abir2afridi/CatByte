package com.example.data.export

import com.example.engine.filters.FilterCatalog
import com.example.engine.filters.FilterGrade
import com.example.model.VideoFilter

/**
 * Maps a [VideoFilter] onto its grade. The numbers live in the filter's OWN
 * file under engine/filters/ (and engine/filters/hit/ for the Hit tab); this
 * is only a lookup, so every new filter works in both renderers instantly.
 */
object FilterMapper {

    fun gradeFor(filter: VideoFilter): Grade {
        val grade = FilterCatalog.gradeOf(filter) ?: return Grade(1f, 1f, 1f, 0f, 0f, 1f)
        return Grade(
            redGain = grade.redGain,
            greenGain = grade.greenGain,
            blueGain = grade.blueGain,
            brightness = grade.brightness,
            contrast = grade.contrast,
            saturation = grade.saturation
        )
    }

    /** The export path's own grade type (kept for binary compatibility). */
    data class Grade(
        val redGain: Float,
        val greenGain: Float,
        val blueGain: Float,
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    )
}
