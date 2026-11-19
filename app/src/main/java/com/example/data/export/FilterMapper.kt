package com.example.data.export

import com.example.engine.filters.BlueprintFilter
import com.example.engine.filters.CharcoalSmudgeFilter
import com.example.engine.filters.CyberpunkFilter
import com.example.engine.filters.FilterGrade
import com.example.engine.filters.GlitchArtFilter
import com.example.engine.filters.GoldenHourFilter
import com.example.engine.filters.HalftoneMangaFilter
import com.example.engine.filters.MoodyFilter
import com.example.engine.filters.NoirFilter
import com.example.engine.filters.SepiaNotebookFilter
import com.example.engine.filters.SketchGraphiteFilter
import com.example.engine.filters.TealOrangeFilter
import com.example.engine.filters.VintageFilter
import com.example.model.VideoFilter

/**
 * Maps a [VideoFilter] onto its grade. Numbers live in the filter's OWN file
 * under engine/filters/ - this dispatcher only points at them.
 */
object FilterMapper {

    fun gradeFor(filter: VideoFilter): Grade = when (filter) {
        VideoFilter.NONE -> Grade(1f, 1f, 1f, 0f, 0f, 1f)
        VideoFilter.SKETCH_GRAPHITE -> SketchGraphiteFilter.grade.toExportGrade()
        VideoFilter.BLUEPRINT -> BlueprintFilter.grade.toExportGrade()
        VideoFilter.SEPIA_NOTEBOOK -> SepiaNotebookFilter.grade.toExportGrade()
        VideoFilter.CHARCOAL_SMUDGE -> CharcoalSmudgeFilter.grade.toExportGrade()
        VideoFilter.HALFTONE_MANGA -> HalftoneMangaFilter.grade.toExportGrade()
        VideoFilter.CYBERPUNK -> CyberpunkFilter.grade.toExportGrade()
        VideoFilter.VINTAGE -> VintageFilter.grade.toExportGrade()
        VideoFilter.GOLDEN_HOUR -> GoldenHourFilter.grade.toExportGrade()
        VideoFilter.NOIR -> NoirFilter.grade.toExportGrade()
        VideoFilter.TEAL_ORANGE -> TealOrangeFilter.grade.toExportGrade()
        VideoFilter.MOODY -> MoodyFilter.grade.toExportGrade()
        VideoFilter.GLITCH_ART -> GlitchArtFilter.grade.toExportGrade()
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

    private fun FilterGrade.toExportGrade() = Grade(
        redGain = redGain,
        greenGain = greenGain,
        blueGain = blueGain,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation
    )
}
