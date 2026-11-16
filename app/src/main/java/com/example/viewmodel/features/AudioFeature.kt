package com.example.viewmodel.features

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.ClipTiming
import com.example.data.MediaDurationFallback
import com.example.data.MediaMetadata
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Audio-track import, extraction, microphone recording and TTS voice-over. */
internal object AudioFeature {

    /** Adds an audio track from a URI (real duration + waveform). */
    fun addAudioTrackFromUri(
        vm: EditorViewModel,
        uriString: String,
        title: String,
        artist: String = "Unknown"
    ) {
        val ctx = vm.context ?: return
        val uri = Uri.parse(uriString)
        vm.viewModelScope.launch {
            val durationMs = MediaMetadata.durationMs(ctx, uri)
            val waveform = MediaMetadata.waveform(ctx, uri, 48) ?: emptyList()
            val realTitle = title.takeIf { it.isNotBlank() }
                ?: MediaMetadata.displayName(ctx, uri)
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = realTitle,
                artist = artist,
                uriString = uriString,
                startTimelineMs = vm._currentPositionMs.value,
                durationMs = if (durationMs > 0) durationMs else 6000L,
                waveform = waveform.ifEmpty { emptyList() }
            )
            vm.updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
        }
    }

    fun addAudioTrack(vm: EditorViewModel, audioClip: AudioClip) {
        vm.updateProject { proj ->
            val track = audioClip.copy(
                id = UUID.randomUUID().toString(),
                startTimelineMs = vm._currentPositionMs.value
            )
            proj.copy(audioTracks = proj.audioTracks + track)
        }
    }

    fun deleteAudioTrack(vm: EditorViewModel, id: String) {
        vm.updateProject { proj ->
            proj.copy(audioTracks = proj.audioTracks.filterNot { it.id == id })
        }
    }

    /**
     * Extracts the selected clip's audio as a real timeline audio track. The
     * source trim and speed are carried over, so the extracted track lines up
     * with the picture in both preview and export instead of starting from the
     * beginning of the original recording.
     */
    fun extractAudioFromSelectedClip(vm: EditorViewModel) {
        val ctx = vm.context ?: return
        val project = vm._currentProject.value ?: return
        val selectedId = vm._selectedClipId.value

        var found: MediaClip? = null
        var startMs = 0L
        var cursor = 0L
        for (clip in project.clips) {
            if (clip.id == selectedId) {
                found = clip
                startMs = cursor
                break
            }
            cursor += clip.durationMs
        }
        if (found == null) {
            project.overlayTracks.forEach { track ->
                track.clips.firstOrNull { it.id == selectedId }?.let { clip ->
                    found = clip
                    startMs = clip.startTimelineMs
                }
            }
        }

        val clip = found ?: return
        val uriString = clip.uriString
        if (uriString.isNullOrBlank()) {
            AppLog.warn(
                "EditorViewModel",
                "Cannot extract audio: '${clip.title}' has no media source"
            )
            return
        }

        val sourceLength = ClipTiming.sourceLengthMs(clip)
        val trimStart = clip.trimStartMs.coerceAtLeast(0L)
        val trimEnd = if (clip.trimEndMs > trimStart) clip.trimEndMs else trimStart + sourceLength
        val effectiveSpeed = if (ClipTiming.hasSpeedCurve(clip)) {
            (sourceLength.toFloat() / clip.durationMs.coerceAtLeast(1L)).coerceIn(0.1f, 16f)
        } else {
            clip.speed.coerceIn(0.1f, 16f)
        }

        vm.viewModelScope.launch {
            val waveform = withContext(Dispatchers.IO) {
                MediaMetadata.waveform(ctx, Uri.parse(uriString), 48)
            } ?: emptyList()
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "${clip.title} (Audio)",
                artist = "Extracted",
                uriString = uriString,
                startTimelineMs = startMs,
                durationMs = clip.durationMs.coerceAtLeast(300L),
                trimStartMs = trimStart,
                trimEndMs = trimEnd,
                speed = effectiveSpeed,
                volume = clip.volume.coerceIn(0f, 2f),
                waveform = waveform
            )
            vm.updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
            AppLog.info(
                "EditorViewModel",
                "Extracted audio from '${clip.title}': start=${startMs}ms, duration=${audio.durationMs}ms"
            )
        }
    }

    /**
     * Detaches (unlinks) the selected clip's audio into its own audio track -
     * CapCut's "Unlink": the video keeps playing silently and the sound becomes
     * an independent clip you can move, trim, fade or delete. Both halves are
     * written in ONE project update, so a single undo restores the original
     * clip with its sound.
     *
     * @return false when there is nothing to detach (no selection, no media
     *   source, or the audio was already detached) so the UI can explain why
     *   instead of silently doing nothing.
     */
    fun detachAudioFromSelectedClip(vm: EditorViewModel): Boolean {
        val project = vm._currentProject.value ?: return false
        val selectedId = vm._selectedClipId.value ?: return false
        val clip = project.allVideoClips.firstOrNull { it.id == selectedId } ?: return false

        val uriString = clip.uriString
        if (uriString.isNullOrBlank()) {
            AppLog.warn(
                "EditorViewModel",
                "Cannot detach audio: '${clip.title}' has no media source"
            )
            return false
        }
        if (clip.isMuted) {
            AppLog.info("EditorViewModel", "Audio of '${clip.title}' is already detached")
            return false
        }

        val startMs = timelineStartMsOf(project, clip)
        val sourceLength = ClipTiming.sourceLengthMs(clip)
        val trimStart = clip.trimStartMs.coerceAtLeast(0L)
        val trimEnd = if (clip.trimEndMs > trimStart) clip.trimEndMs else trimStart + sourceLength
        val effectiveSpeed = if (ClipTiming.hasSpeedCurve(clip)) {
            (sourceLength.toFloat() / clip.durationMs.coerceAtLeast(1L)).coerceIn(0.1f, 16f)
        } else {
            clip.speed.coerceIn(0.1f, 16f)
        }

        val detached = AudioClip(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (Audio)",
            artist = "Detached",
            uriString = uriString,
            startTimelineMs = startMs,
            durationMs = clip.durationMs.coerceAtLeast(300L),
            trimStartMs = trimStart,
            trimEndMs = trimEnd,
            speed = effectiveSpeed,
            volume = clip.volume.coerceIn(0f, 2f),
            waveform = clip.waveform
        )

        vm.updateProject { proj ->
            proj.copy(
                audioTracks = proj.audioTracks + detached,
                clips = proj.clips.map { if (it.id == clip.id) it.copy(isMuted = true) else it },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map {
                            if (it.id == clip.id) it.copy(isMuted = true) else it
                        }
                    )
                }
            )
        }
        AppLog.info(
            "EditorViewModel",
            "Detached audio from '${clip.title}': start=${startMs}ms, duration=${detached.durationMs}ms"
        )
        return true
    }

    /** Absolute timeline start of a clip (main track = sum of the clips before it). */
    private fun timelineStartMsOf(
        project: com.example.model.VideoProject,
        clip: MediaClip
    ): Long {
        var cursor = 0L
        for (main in project.clips) {
            if (main.id == clip.id) return cursor
            cursor += main.durationMs
        }
        return clip.startTimelineMs
    }

    /** Starts a real microphone recording. */
    fun startVoiceRecording(vm: EditorViewModel) {
        val recorder = vm.voiceRecorder ?: return
        val ok = recorder.start()
        if (ok) {
            vm._isRecordingVoice.value = true
            vm._voiceRecordingDurationMs.value = 0L
            vm.voiceRecordJob?.cancel()
            vm.voiceRecordJob = vm.viewModelScope.launch {
                while (isActive && vm._isRecordingVoice.value) {
                    delay(100L)
                    vm._voiceRecordingDurationMs.value += 100L
                }
            }
        }
    }

    /** Stops the recording and adds the clip to the timeline. */
    fun stopAndSaveVoiceRecording(vm: EditorViewModel) {
        val recorder = vm.voiceRecorder ?: return
        val file = recorder.stop()
        vm._isRecordingVoice.value = false
        vm.voiceRecordJob?.cancel()
        vm.voiceRecordJob = null

        if (file != null && file.exists()) {
            val duration = vm._voiceRecordingDurationMs.value
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Voiceover",
                artist = "You",
                uriString = file.absolutePath,
                startTimelineMs = vm._currentPositionMs.value,
                durationMs = duration.coerceAtLeast(500L),
                isVoiceover = true,
                waveform = emptyList() // could compute later
            )
            vm.updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
        }
    }

    fun cancelVoiceRecording(vm: EditorViewModel) {
        vm.voiceRecorder?.cancel()
        vm._isRecordingVoice.value = false
        vm._voiceRecordingDurationMs.value = 0L
        vm.voiceRecordJob?.cancel()
        vm.voiceRecordJob = null
    }

    /** Generates a voice-over from text using the device TTS engine. */
    fun startTtsVoiceover(vm: EditorViewModel, text: String) {
        val tts = vm.speechSynthesizer ?: return
        vm._activeSheet.value = ActiveToolSheet.TTS_VOICEOVER
        tts.synthesize(text) { file, error ->
            if (file != null) {
                val audio = AudioClip(
                    id = UUID.randomUUID().toString(),
                    title = "AI Voiceover",
                    artist = "CatByte TTS",
                    uriString = file.absolutePath,
                    startTimelineMs = vm._currentPositionMs.value,
                    durationMs = MediaDurationFallback.durationMs(vm.context!!, Uri.fromFile(file)),
                    isVoiceover = true,
                    waveform = emptyList()
                )
                vm.updateProject { proj ->
                    proj.copy(audioTracks = proj.audioTracks + audio)
                }
            }
            // The UI can show error if needed
        }
    }
}
