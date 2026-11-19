package com.example.ui.canvas

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
 * Preview colour filter for a clip's filter. The numbers come from the
 * filter's OWN file (engine/filters/<Name>Filter.kt) through [FilterGrade],
 * exactly like the export path - one definition, no duplicate matrices.
 */
internal fun clipFilterColorMatrix(filter: VideoFilter): ColorFilter? {
    val grade: FilterGrade = when (filter) {
        VideoFilter.NONE -> return null
        VideoFilter.SKETCH_GRAPHITE -> SketchGraphiteFilter.grade
        VideoFilter.BLUEPRINT -> BlueprintFilter.grade
        VideoFilter.SEPIA_NOTEBOOK -> SepiaNotebookFilter.grade
        VideoFilter.CHARCOAL_SMUDGE -> CharcoalSmudgeFilter.grade
        VideoFilter.HALFTONE_MANGA -> HalftoneMangaFilter.grade
        VideoFilter.CYBERPUNK -> CyberpunkFilter.grade
        VideoFilter.VINTAGE -> VintageFilter.grade
        VideoFilter.GOLDEN_HOUR -> GoldenHourFilter.grade
        VideoFilter.NOIR -> NoirFilter.grade
        VideoFilter.TEAL_ORANGE -> TealOrangeFilter.grade
        VideoFilter.MOODY -> MoodyFilter.grade
        VideoFilter.GLITCH_ART -> GlitchArtFilter.grade
    }
    if (grade.isIdentity) return null
    return ColorFilter.colorMatrix(ColorMatrix(grade.toPreviewMatrix()))
}
