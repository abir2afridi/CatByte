package com.example.viewmodel.features

import com.example.engine.AudioTiming
import com.example.model.AudioClip
import com.example.viewmodel.EditorViewModel

/**
 * Per-audio-clip mixer operations.
 *
 * Every value is stored on [AudioClip], so the normal preview and export
 * composition consume exactly the same mix. Slider gestures use a coalescing
 * key, which keeps one continuous drag to one undo step.
 */
internal object AudioMixerFeature {

    /** Linear track gain in the range 0..2 (0..200%). */
    fun setAudioVolume(vm: EditorViewModel, audioId: String, volume: Float) {
        val clipped = volume.coerceIn(0f, 2f)
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        if (current.volume == clipped) return
        vm.updateProject("audio-volume:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) audio.copy(volume = clipped) else audio
                }
            )
        }
    }

    /** Fade durations on the output timeline. */
    fun setAudioFades(
        vm: EditorViewModel,
        audioId: String,
        fadeInMs: Long,
        fadeOutMs: Long
    ) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val duration = AudioTiming.presentationDurationMs(current)
        val fades = AudioTiming.clampFades(duration, fadeInMs, fadeOutMs)
        if (current.fadeInMs == fades.first && current.fadeOutMs == fades.second) return
        vm.updateProject("audio-fades:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) {
                        audio.copy(fadeInMs = fades.first, fadeOutMs = fades.second)
                    } else {
                        audio
                    }
                }
            )
        }
    }

    /** Changes playback speed while preserving the selected source window. */
    fun setAudioSpeed(vm: EditorViewModel, audioId: String, speed: Float) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val normalized = AudioTiming.normalizedSpeed(speed)
        if (current.speed == normalized) return
        vm.updateProject("audio-speed:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id != audioId) {
                        audio
                    } else {
                        val retimed = audio.copy(speed = normalized)
                        val duration = AudioTiming.presentationDurationMs(retimed)
                        val fades = AudioTiming.clampFades(
                            duration,
                            retimed.fadeInMs,
                            retimed.fadeOutMs
                        )
                        retimed.copy(
                            durationMs = duration,
                            fadeInMs = fades.first,
                            fadeOutMs = fades.second
                        )
                    }
                }
            )
        }
    }

    /** Changes the absolute source in/out points and retimes the clip. */
    fun setAudioTrim(
        vm: EditorViewModel,
        audioId: String,
        trimStartMs: Long,
        trimEndMs: Long
    ) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val trim = AudioTiming.clampTrim(current, trimStartMs, trimEndMs)
        if (current.trimStartMs == trim.first && current.trimEndMs == trim.second) return
        vm.updateProject("audio-trim:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id != audioId) {
                        audio
                    } else {
                        val trimmed = audio.copy(
                            trimStartMs = trim.first,
                            trimEndMs = trim.second
                        )
                        val duration = AudioTiming.presentationDurationMs(trimmed)
                        val fades = AudioTiming.clampFades(
                            duration,
                            trimmed.fadeInMs,
                            trimmed.fadeOutMs
                        )
                        trimmed.copy(
                            durationMs = duration,
                            fadeInMs = fades.first,
                            fadeOutMs = fades.second
                        )
                    }
                }
            )
        }
    }

    /** Moves the clip on the project timeline. */
    fun setAudioTimelineStart(vm: EditorViewModel, audioId: String, startMs: Long) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val project = vm._currentProject.value ?: return
        val maxStart = project.totalDurationMs.coerceAtLeast(0L)
        val clipped = startMs.coerceIn(0L, maxStart)
        if (current.startTimelineMs == clipped) return
        vm.updateProject("audio-start:$audioId") { updated ->
            updated.copy(
                audioTracks = updated.audioTracks.map { audio ->
                    if (audio.id == audioId) audio.copy(startTimelineMs = clipped) else audio
                }
            )
        }
    }

    /** Restores gain, mute, fades, speed and trim without moving the clip. */
    fun resetAudioMixer(vm: EditorViewModel, audioId: String) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        vm.updateProject { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id != audioId) {
                        audio
                    } else {
                        val reset = audio.copy(
                            volume = 1f,
                            isMuted = false,
                            fadeInMs = 0L,
                            fadeOutMs = 0L,
                            speed = 1f,
                            trimStartMs = 0L,
                            trimEndMs = 0L
                        )
                        reset.copy(durationMs = AudioTiming.presentationDurationMs(reset))
                    }
                }
            )
        }
    }
}
