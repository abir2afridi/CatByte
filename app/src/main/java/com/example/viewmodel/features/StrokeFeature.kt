package com.example.viewmodel.features

import com.example.model.StrokeOverlay
import com.example.viewmodel.EditorViewModel

/**
 * Freehand drawing strokes ("Text → Draw").
 *
 * A stroke is stored as normalised canvas points so it renders at the right
 * place in the preview and in the exported file, whatever the output size.
 */
internal object StrokeFeature {

    /** Adds a stroke that starts at the playhead and lasts 3 seconds. */
    fun addStroke(
        vm: EditorViewModel,
        points: List<Float>,
        colorArgb: Long,
        widthFraction: Float
    ): Boolean {
        if (points.size < 4) return false
        val stroke = StrokeOverlay(
            points = points.map { it.coerceIn(0f, 1f) },
            colorArgb = colorArgb,
            widthFraction = widthFraction.coerceIn(0.002f, 0.06f),
            startTimelineMs = vm._currentPositionMs.value.coerceAtLeast(0L),
            durationMs = 3_000L
        )
        vm.updateProject { project -> project.copy(strokes = project.strokes + stroke) }
        return true
    }

    /** Removes every stroke (the sheet's Clear button). */
    fun clearStrokes(vm: EditorViewModel) {
        if (vm._currentProject.value?.strokes.isNullOrEmpty()) return
        vm.updateProject { project -> project.copy(strokes = emptyList()) }
    }
}
