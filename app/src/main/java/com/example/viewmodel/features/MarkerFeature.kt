package com.example.viewmodel.features

import com.example.model.TimelineMarker
import com.example.viewmodel.EditorViewModel

/** Timeline markers: create, delete, move to the playhead and jump between. */
internal object MarkerFeature {

    /** Adds a marker at the playhead. */
    fun addMarker(vm: EditorViewModel, label: String = "") {
        vm.updateProject { proj ->
            val at = vm._currentPositionMs.value.coerceAtLeast(0L)
            proj.copy(markers = proj.markers + TimelineMarker(atMs = at, label = label))
        }
    }

    fun removeMarker(vm: EditorViewModel, markerId: String) {
        vm.updateProject { proj ->
            proj.copy(markers = proj.markers.filterNot { it.id == markerId })
        }
    }

    /** Renames a marker (the label is drawn next to the pin on the ruler). */
    fun renameMarker(vm: EditorViewModel, markerId: String, label: String) {
        vm.updateProject(coalesceKey = "markerLabel:$markerId") { proj ->
            proj.copy(
                markers = proj.markers.map { marker ->
                    if (marker.id == markerId) marker.copy(label = label.trim().take(24)) else marker
                }
            )
        }
    }

    /** Recolours a marker pin. */
    fun setMarkerColor(vm: EditorViewModel, markerId: String, colorArgb: Long) {
        vm.updateProject { proj ->
            proj.copy(
                markers = proj.markers.map { marker ->
                    if (marker.id == markerId) marker.copy(colorArgb = colorArgb) else marker
                }
            )
        }
    }

    fun moveMarkerToPlayhead(vm: EditorViewModel, markerId: String) {
        vm.updateProject { proj ->
            val at = vm._currentPositionMs.value.coerceAtLeast(0L)
            proj.copy(
                markers = proj.markers.map { marker ->
                    if (marker.id == markerId) marker.copy(atMs = at) else marker
                }
            )
        }
    }

    fun seekToMarker(vm: EditorViewModel, markerId: String) {
        val marker = vm._currentProject.value?.markers?.firstOrNull { it.id == markerId } ?: return
        vm.seekTo(marker.atMs)
    }

    /** Jumps to the marker before/after the playhead. Returns the new position. */
    fun jumpToMarker(vm: EditorViewModel, direction: Int): Long? {
        val markers = vm._currentProject.value?.markers?.sortedBy { it.atMs } ?: return null
        if (markers.isEmpty()) return null
        val here = vm._currentPositionMs.value
        val target = if (direction >= 0) {
            markers.firstOrNull { it.atMs > here } ?: markers.last()
        } else {
            markers.lastOrNull { it.atMs < here } ?: markers.first()
        }
        vm.seekTo(target.atMs)
        return target.atMs
    }
}
