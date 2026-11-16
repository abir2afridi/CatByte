package com.example.viewmodel.features

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.MediaAvailability
import com.example.data.MediaMetadata
import com.example.data.MediaVideoInfo
import com.example.model.VideoProject
import com.example.engine.RelinkMath
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.withUpdatedClip
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Detects project media that can no longer be opened and lets the user relink a
 * replacement file without losing the clip's edits.
 *
 * Relinking keeps trims, speed, filters, keyframes and effects; only the source
 * URI (and the probed dimensions/duration) changes. The timeline duration is
 * clamped when the replacement file is shorter than the part that was used.
 */
internal object RelinkFeature {

    private var refreshJob: Job? = null

    /** Re-scans the open project. Safe to call often; the previous scan is cancelled. */
    fun refresh(vm: EditorViewModel) {
        val context = vm.context ?: return
        val project = vm._currentProject.value ?: run {
            vm._missingMedia.value = emptyList()
            return
        }
        refreshJob?.cancel()
        refreshJob = vm.viewModelScope.launch {
            val missing = runCatching { MediaAvailability.findMissing(context, project) }
                .onFailure { AppLog.warn("Relink", "Could not check media availability", it) }
                .getOrDefault(emptyList())
            vm._missingMedia.value = missing
            if (missing.isNotEmpty()) {
                AppLog.info("Relink", "${missing.size} media file(s) are missing from project ${project.id}")
            }
        }
    }

    /** Replaces the source of [clipId] with [uriString], keeping the edit intact. */
    fun relinkClip(vm: EditorViewModel, clipId: String, uriString: String) {
        val context = vm.context ?: return
        val project = vm._currentProject.value ?: return
        val isAudio = project.audioTracks.any { it.id == clipId }

        vm.viewModelScope.launch {
            val uri = Uri.parse(uriString)
            val probe = runCatching { MediaMetadata.videoInfo(context, uri) }.getOrNull()
            val sourceMs = probe?.durationMs?.takeIf { it > 0L }
                ?: runCatching { MediaMetadata.durationMs(context, uri) }.getOrDefault(0L)

            if (sourceMs <= 0L) {
                // Not a readable media file: leave the clip untouched and tell
                // the user instead of silently breaking the clip.
                AppLog.warn("Relink", "Relinked file has no readable duration: $uriString")
                return@launch
            }

            vm.updateProject { current ->
                if (isAudio) current.withRelinkedAudio(clipId, uriString, sourceMs)
                else current.withRelinkedClip(clipId, uriString, sourceMs, probe)
            }
            AppLog.info("Relink", "Relinked clip $clipId to $uriString (${sourceMs}ms)")

            // Re-check so the banner disappears as soon as everything resolves.
            vm._missingMedia.value = vm._missingMedia.value.filterNot { it.id == clipId }
            refresh(vm)
        }
    }

    /** Drops a stale warning without touching the project (e.g. the clip was deleted). */
    fun forget(vm: EditorViewModel, clipId: String) {
        vm._missingMedia.value = vm._missingMedia.value.filterNot { it.id == clipId }
    }

    private fun VideoProject.withRelinkedClip(
        clipId: String,
        uriString: String,
        sourceMs: Long,
        probe: MediaVideoInfo?
    ): VideoProject = withUpdatedClip(clipId) { clip ->
        val timing = RelinkMath.relinkedTiming(
            durationMs = clip.durationMs,
            speed = clip.speed,
            trimStartMs = clip.trimStartMs,
            newSourceMs = sourceMs
        )
        clip.copy(
            uriString = uriString,
            originalDurationMs = sourceMs,
            trimStartMs = timing.trimStartMs,
            // A shorter replacement shortens the clip instead of reading past EOF.
            durationMs = timing.durationMs,
            sourceWidth = probe?.width?.takeIf { it > 0 } ?: clip.sourceWidth,
            sourceHeight = probe?.height?.takeIf { it > 0 } ?: clip.sourceHeight
        )
    }

    private fun VideoProject.withRelinkedAudio(
        clipId: String,
        uriString: String,
        sourceMs: Long
    ): VideoProject {
        val updated = audioTracks.map { clip ->
            if (clip.id != clipId) return@map clip
            val timing = RelinkMath.relinkedTiming(
                durationMs = clip.durationMs,
                speed = clip.speed,
                trimStartMs = clip.trimStartMs,
                newSourceMs = sourceMs
            )
            clip.copy(
                uriString = uriString,
                trimStartMs = timing.trimStartMs,
                durationMs = timing.durationMs
            )
        }
        return copy(audioTracks = updated)
    }
}
