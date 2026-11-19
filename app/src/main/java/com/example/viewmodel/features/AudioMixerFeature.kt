package com.example.viewmodel.features

import com.example.engine.AudioLoudness
import com.example.engine.AudioTiming
import com.example.engine.StereoPan
import com.example.model.AudioClip
import com.example.model.AudioCompressor
import com.example.model.AudioEffectSettings
import com.example.model.AudioEqualizer
import com.example.viewmodel.EditorViewModel
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

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

    /**
     * Stereo balance of one track: -1 is hard left, 0 is centre and +1 is hard
     * right. The value lives on the clip, so the shared composition balances
     * the track the same way in the preview and in the exported file.
     */
    fun setAudioPan(vm: EditorViewModel, audioId: String, pan: Float) {
        val clipped = StereoPan.clamp(pan)
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val canonical = canonical(current)
        if (canonical.pan == clipped) return
        vm.updateProject("audio-pan:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) canonical.copy(pan = clipped) else audio
                }
            )
        }
    }

    /**
     * Three-band EQ of one track. The value lives on the clip and flows into
     * the shared composition factory, so the preview and the export run the
     * same low-shelf / mid-peak / high-shelf filters. Slider drags use one
     * coalescing key and therefore collapse into a single undo step.
     */
    fun setAudioEqualizer(vm: EditorViewModel, audioId: String, equalizer: AudioEqualizer) {
        val canonicalEq = AudioEqualizer.clamp(equalizer)
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val canonical = canonical(current)
        if (canonical.equalizer == canonicalEq) return
        vm.updateProject("audio-eq:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) canonical.copy(equalizer = canonicalEq) else audio
                }
            )
        }
    }

    /**
     * Dynamics compressor of one track. The value lives on the clip and flows
     * into the shared composition factory, so the preview and the export run
     * the same envelope follower and soft-knee curve. Slider drags use one
     * coalescing key and therefore collapse into a single undo step.
     */
    fun setAudioCompressor(vm: EditorViewModel, audioId: String, compressor: AudioCompressor) {
        val canonicalCompressor = AudioCompressor.clamp(compressor)
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val canonical = canonical(current)
        if (canonical.compressor == canonicalCompressor) return
        vm.updateProject("audio-compressor:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) canonical.copy(compressor = canonicalCompressor) else audio
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

    /**
     * Sets one track's audio-effect chain (voice preset, enhance voice, noise
     * gate). The stored [AudioEffectSettings] flows into the shared composition
     * factory, so preview and export run exactly the same DSP chain.
     */
    fun setAudioEffects(vm: EditorViewModel, audioId: String, settings: AudioEffectSettings) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val canonical = canonical(current)
        if (canonical.audioEffects == settings) return
        vm.updateProject("audio-effects:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) canonical.copy(audioEffects = settings) else audio
                }
            )
        }
    }

    /**
     * Sets how often the track's source window repeats. A looping track covers
     * `count` times the window on the timeline, and the composition renders
     * exactly that many repeats, so preview and export stay in sync.
     */
    fun setAudioLoopCount(vm: EditorViewModel, audioId: String, count: Int) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        val canonical = canonical(current)
        val repeats = count.coerceIn(1, AudioTiming.MAX_LOOP_COUNT)
        if (canonical.loopCount == repeats) return
        vm.updateProject("audio-loop:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) {
                        canonical(canonical.copy(loopCount = repeats))
                    } else {
                        audio
                    }
                }
            )
        }
    }

    /**
     * Turns the automatic ducking of music under a voice-over on or off.
     *
     * The flag is a project setting: the shared composition only collects the
     * audible voice-over spans when it is on, so preview and export duck
     * exactly the same way (or not at all).
     */
    fun setAutoDuckEnabled(vm: EditorViewModel, enabled: Boolean) {
        val project = vm._currentProject.value ?: return
        if (project.autoDuckEnabled == enabled) return
        vm.updateProject("auto-duck") { updated ->
            updated.copy(autoDuckEnabled = enabled)
        }
    }

    /**
     * Sets how deep the music dips under a voice-over. 0 keeps the music at
     * full level, 1 mutes it completely.
     */
    fun setAutoDuckStrength(vm: EditorViewModel, strength: Float) {
        val project = vm._currentProject.value ?: return
        val clamped = if (strength.isFinite()) strength.coerceIn(0f, 1f) else 0.35f
        if (project.autoDuckStrength == clamped) return
        vm.updateProject("auto-duck-strength") { updated ->
            updated.copy(autoDuckStrength = clamped)
        }
    }

    /**
     * Splits the selected independent audio track at the playhead, keeping the
     * playhead exactly on the cut. Returns false when the playhead is outside
     * the clip or too close to an edge, so the UI can explain instead of
     * silently doing nothing.
     *
     * [AudioClip.durationMs] lives on the output timeline, so a local cut at
     * `localMs` maps back to source time through the clip speed. The first
     * half keeps the original in-point and loses the outgoing fade; the second
     * half gets a fresh id and loses the incoming fade, so no fade spans the
     * cut edge.
     */
    fun splitAudioAtPlayhead(vm: EditorViewModel): Boolean {
        val project = vm._currentProject.value ?: return false
        val audioId = vm._selectedAudioId.value ?: return false
        if (vm._isPlaying.value) {
            vm.pausePlayback()
            vm.playerManager?.syncPosition()
            vm.playerManager?.currentPositionMs?.value?.let { vm._currentPositionMs.value = it }
        }
        val playhead = vm._currentPositionMs.value
        val selected = project.audioTracks.firstOrNull { it.id == audioId } ?: return false
        val clip = AudioTiming.normalize(selected)
        val min = AudioTiming.MIN_AUDIO_DURATION_MS
        val localMs = playhead - clip.startTimelineMs
        if (localMs < min || localMs > clip.durationMs - min) return false

        val speed = AudioTiming.normalizedSpeed(clip.speed)
        val splitSrcMs = clip.trimStartMs + (localMs * speed).roundToLong()
        val sourceEnd = AudioTiming.sourceEndMs(clip)
        if (splitSrcMs < clip.trimStartMs + min || sourceEnd - splitSrcMs < min) return false

        val first = AudioTiming.normalize(
            clip.copy(trimEndMs = splitSrcMs, fadeOutMs = 0L)
        )
        val second = AudioTiming.normalize(
            clip.copy(
                id = UUID.randomUUID().toString(),
                startTimelineMs = clip.startTimelineMs + localMs,
                trimStartMs = splitSrcMs,
                fadeInMs = 0L
            )
        )
        vm.updateProject { current ->
            current.copy(
                audioTracks = current.audioTracks.flatMap { audio ->
                    if (audio.id == audioId) listOf(first, second) else listOf(audio)
                }
            )
        }
        vm._selectedAudioId.value = second.id
        return true
    }

    /**
     * Soloes one audio track.
     *
     * While a track is soloed, every other audio track drops out of the mix.
     * The rule lives in the render graph, so the preview and the exported file
     * always agree - the mixer's "S" button is not a decorative toggle.
     */
    fun setAudioSolo(vm: EditorViewModel, audioId: String, soloed: Boolean) {
        val current = vm._currentProject.value?.audioTracks?.firstOrNull { it.id == audioId }
            ?: return
        if (current.isSoloed == soloed) return
        vm.updateProject("audio-solo:$audioId") { project ->
            project.copy(
                audioTracks = project.audioTracks.map { audio ->
                    if (audio.id == audioId) audio.copy(isSoloed = soloed) else audio
                }
            )
        }
    }

    /** Restores gain, mute, fades, speed, trim, EQ and compressor without moving the clip. */
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
                                pan = 0f,
                                isMuted = false,
                                isSoloed = false,
                                fadeInMs = 0L,
                                fadeOutMs = 0L,
                                speed = 1f,
                                trimStartMs = 0L,
                                trimEndMs = 0L,
                                equalizer = AudioEqualizer(),
                                compressor = AudioCompressor()
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
