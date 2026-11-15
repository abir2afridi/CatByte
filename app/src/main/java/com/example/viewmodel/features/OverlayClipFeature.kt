package com.example.viewmodel.features

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.MediaMetadata
import com.example.model.MediaClip
import com.example.model.VideoTrack
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.withUpdatedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** PIP/overlay clips: add from a URI and update their transform. */
internal object OverlayClipFeature {

    /**
     * Adds a PIP/overlay clip. The source duration is probed from the real
     * media file instead of assuming four seconds, otherwise the overlay would
     * be cut short (or stretched into black) in both preview and export.
     */
    fun addOverlayClipFromUri(vm: EditorViewModel, uriString: String, title: String) {
        val currentPos = vm._currentPositionMs.value
        vm.viewModelScope.launch {
            val ctx = vm.context
            val probedMs = if (ctx != null) {
                withContext(Dispatchers.IO) {
                    MediaMetadata.durationMs(ctx, Uri.parse(uriString))
                }
            } else {
                0L
            }
            val info = if (ctx != null) {
                runCatching { MediaMetadata.videoInfo(ctx, Uri.parse(uriString)) }.getOrNull()
            } else {
                null
            }
            val durationMs = if (probedMs > 0L) probedMs else 4000L
            val newClip = MediaClip(
                id = "pip_${UUID.randomUUID().toString().take(8)}",
                title = title,
                uriString = uriString,
                startTimelineMs = currentPos,
                durationMs = durationMs,
                originalDurationMs = durationMs,
                sourceWidth = info?.width ?: 0,
                sourceHeight = info?.height ?: 0,
                scale = 0.5f,
                posX = 0.5f,
                posY = 0.5f
            )
            vm.updateProject { proj ->
                val currentTracks = proj.overlayTracks.toMutableList()
                if (currentTracks.isEmpty()) {
                    currentTracks.add(
                        VideoTrack(
                            name = "PIP Track 1",
                            isMainTrack = false,
                            clips = listOf(newClip)
                        )
                    )
                } else {
                    val track = currentTracks.first()
                    currentTracks[0] = track.copy(clips = track.clips + newClip)
                }
                proj.copy(overlayTracks = currentTracks)
            }
            vm._selectedClipId.value = newClip.id
            vm.refreshPreviewNow()
        }
    }

    /** Updates the PIP transform used by the compositor and keyframe sheet. */
    fun updateOverlayTransform(
        vm: EditorViewModel,
        clipId: String,
        scale: Float? = null,
        posX: Float? = null,
        posY: Float? = null,
        opacity: Float? = null
    ) {
        vm.updateProject(coalesceKey = "pipTransform:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    scale = scale?.coerceIn(0.05f, 4f) ?: clip.scale,
                    posX = posX?.coerceIn(0f, 1f) ?: clip.posX,
                    posY = posY?.coerceIn(0f, 1f) ?: clip.posY,
                    opacity = opacity?.coerceIn(0f, 1f) ?: clip.opacity
                )
            }
        }
    }
}
