package com.example.viewmodel.features

import com.example.engine.AudioLoudness
import com.example.engine.AudioTiming
import com.example.model.AudioClip
import com.example.viewmodel.EditorViewModel
import kotlin.math.abs

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
        val clipped = if (volume.isFinite()) volume.coerceIn(0f, 2f) else 1f
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val canonical = canonical(current)
        if (canonical.volume == clipped) return
        vm.updateProject("audio-volume:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) canonical.copy(volume = clipped) else audio
                }
            )
        }
    }

    /**
     * Loudness normalisation for independent audio tracks.
     *
     * The gain is derived from the track's decoded waveform - the same maths
     * the per-clip Volume panel uses - and written through the normal volume
     * field, so preview and export apply exactly the corrected level. Tracks
     * without a decoded waveform return false: the UI then explains that the
     * file must be analysed first instead of applying a bogus guess.
     */
    fun normaliseAudioVolume(vm: EditorViewModel, audioId: String): Boolean {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return false
        val base = canonical(current)
        if (!AudioLoudness.canNormalise(base.waveform)) return false
        val target = AudioLoudness.gainFor(base.waveform).coerceIn(0f, 2f)
        if (abs(target - base.volume) < 0.01f) return true
        vm.updateProject("audio-volume:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) base.copy(volume = target) else audio
                }
            )
        }
        return true
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
        val canonical = canonical(current)
        val duration = AudioTiming.presentationDurationMs(canonical)
        val fades = AudioTiming.clampFades(duration, fadeInMs, fadeOutMs)
        if (canonical.fadeInMs == fades.first && canonical.fadeOutMs == fades.second) return
        vm.updateProject("audio-fades:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) {
                        canonical.copy(fadeInMs = fades.first, fadeOutMs = fades.second)
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
        // Materialise a legacy source duration BEFORE changing speed. Otherwise
        // sourceDurationMs would infer the new speed from the old output time.
        val base = canonical(current)
        if (base.speed == normalized) return
        vm.updateProject("audio-speed:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id != audioId) {
                        audio
                    } else {
                        canonical(base.copy(speed = normalized))
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
        val base = canonical(current)
        val trim = AudioTiming.clampTrim(base, trimStartMs, trimEndMs)
        if (base.trimStartMs == trim.first && base.trimEndMs == trim.second) return
        vm.updateProject("audio-trim:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id != audioId) {
                        audio
                    } else {
                        canonical(
                            base.copy(
                                trimStartMs = trim.first,
                                trimEndMs = trim.second
                            )
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
        val base = canonical(current)
        // The project duration already contains this clip. Excluding its own
        // length prevents moving it to the end from growing the project again.
        val maxStart = (project.totalDurationMs - base.durationMs).coerceAtLeast(0L)
        val clipped = startMs.coerceIn(0L, maxStart)
        if (base.startTimelineMs == clipped) return
        vm.updateProject("audio-start:$audioId") { updated ->
            updated.copy(
                audioTracks = updated.audioTracks.map { audio ->
                    if (audio.id == audioId) base.copy(startTimelineMs = clipped) else audio
                }
            )
        }
    }

    /** Restores gain, mute, fades, speed and trim without moving the clip. */
    fun resetAudioMixer(vm: EditorViewModel, audioId: String) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val base = canonical(current)
        vm.updateProject { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id != audioId) {
                        audio
                    } else {
                        canonical(
                            base.copy(
                                volume = 1f,
                                isMuted = false,
                                fadeInMs = 0L,
                                fadeOutMs = 0L,
                                speed = 1f,
                                trimStartMs = 0L,
                                trimEndMs = 0L
                            )
                        )
                    }
                }
            )
        }
    }

    /** Canonicalises both legacy and malformed clips before every mutation. */
    private fun canonical(audio: AudioClip): AudioClip = AudioTiming.normalize(audio)
}
