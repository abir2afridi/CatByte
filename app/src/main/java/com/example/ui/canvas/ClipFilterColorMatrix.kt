package com.example.ui.canvas

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.engine.filters.FilterCatalog
import com.example.model.VideoFilter

/**
 * Preview colour filter for a clip's filter. Numbers come from the filter's
 * OWN file via [FilterCatalog], the same source the export path uses - one
 * definition per filter, no duplicate matrices.
 */
internal fun clipFilterColorMatrix(filter: VideoFilter): ColorFilter? {
    val grade = FilterCatalog.gradeOf(filter) ?: return null
    if (grade.isIdentity) return null
    return ColorFilter.colorMatrix(ColorMatrix(grade.toPreviewMatrix()))
}
