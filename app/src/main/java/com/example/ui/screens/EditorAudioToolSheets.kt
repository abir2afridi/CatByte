package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.model.AudioEffectSettings
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.ui.audio.AudioEffectsSheet
import com.example.ui.audio.AudioEffectsSheetEmpty
import com.example.ui.audio.AudioMixerSheet
import com.example.ui.audio.AudioSheet
import com.example.ui.components.VolumeSheet
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * The audio tool sheets of the editor: the Audio & Foley studio (music, SFX
 * and the voice recorder) and the per-clip volume boost.
 */

@Composable
internal fun AudioToolSheet(
    viewModel: EditorViewModel,
    isRecordingVoice: Boolean,
    voiceRecordingDurationMs: Long,
    initialTab: Int = 0,
    onShowTts: () -> Unit
) {
    val context = LocalContext.current
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // AudioFeature copies the provider content into app-private storage (or
        // verifies a persisted grant) before reporting success. Keep the sheet
        // open until that work finishes so a failed import is never presented
        // as a completed one.
        viewModel.addAudioTrackFromUri(uri.toString(), "", "Imported") { imported ->
            if (imported) {
                viewModel.setActiveSheet(ActiveToolSheet.NONE)
            } else {
                Toast.makeText(
                    context,
                    "Could not import audio. The file could not be copied or kept with persistent access.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    AudioSheet(
        isRecordingVoice = isRecordingVoice,
        voiceDurationMs = voiceRecordingDurationMs,
        initialTab = initialTab,
        onAddAudio = { track ->
            // Library metadata has no media URI. Generate a real WAV first;
            // keep the sheet open and explain the failure when that does not
            // complete instead of presenting a fake successful track.
            viewModel.addSampleAudioTrack(track) { added ->
                if (added) {
                    viewModel.setActiveSheet(ActiveToolSheet.NONE)
                } else {
                    Toast.makeText(
                        context,
                        "Could not prepare the sample audio. No track was added.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        },
        onImportAudio = { audioPicker.launch(arrayOf("audio/*")) },
        onExtractAudio = { viewModel.extractAudioFromSelectedClip() },
        onStartVoiceRecord = { viewModel.startVoiceRecording() },
        onStopVoiceRecord = { viewModel.stopAndSaveVoiceRecording() },
        onTtsVoiceover = {
            onShowTts()
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        autoDuckEnabled = viewModel.currentProject.value?.autoDuckEnabled ?: true,
        autoDuckStrength = viewModel.currentProject.value?.autoDuckStrength ?: 0.35f,
        onAutoDuckEnabledChange = { enabled -> viewModel.setAutoDuckEnabled(enabled) },
        onAutoDuckStrengthChange = { strength -> viewModel.setAutoDuckStrength(strength) },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun AudioMixerToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject,
    selectedAudioId: String?
) {
    val context = LocalContext.current
    AudioMixerSheet(
        audioTracks = project.audioTracks,
        selectedAudioId = selectedAudioId,
        maxTimelineStartMs = project.totalDurationMs,
        onSelectAudio = { viewModel.selectAudioClip(it) },
        onVolumeChanged = { id, value -> viewModel.setAudioVolume(id, value) },
        onNormaliseVolume = { id -> viewModel.normaliseAudioVolume(id) },
        onMuteChanged = { id, muted -> viewModel.setAudioClipMuted(id, muted) },
        onFadesChanged = { id, fadeIn, fadeOut ->
            viewModel.setAudioFades(id, fadeIn, fadeOut)
        },
        onSpeedChanged = { id, speed -> viewModel.setAudioSpeed(id, speed) },
        onTrimChanged = { id, start, end -> viewModel.setAudioTrim(id, start, end) },
        onTimelineStartChanged = { id, start -> viewModel.setAudioTimelineStart(id, start) },
        onEffects = { id ->
            viewModel.selectAudioClip(id)
            viewModel.setActiveSheet(ActiveToolSheet.AUDIO_EFFECTS)
        },
        onLoopCountChanged = { id, repeats -> viewModel.setAudioLoopCount(id, repeats) },
        onSplit = { id ->
            viewModel.selectAudioClip(id)
            if (!viewModel.splitAudioAtPlayhead()) {
                Toast.makeText(
                    context,
                    "Move the playhead inside the selected track to split it",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onReset = { viewModel.resetAudioMixer(it) },
        onDelete = { viewModel.deleteAudioTrack(it) },
        onClose = { viewModel.closeToolSheet() }
    )
}

/**
 * The audio-effects sheet. It edits whichever the user has selected: an
 * independent audio track (from the mixer) or the selected video clip (from
 * the clip toolbar). Selection is mutually exclusive, so at most one target
 * exists; otherwise the sheet explains that honestly.
 */
@Composable
internal fun AudioEffectsToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject,
    currentClip: MediaClip?,
    selectedAudioId: String?
) {
    val audio = project.audioTracks.firstOrNull { it.id == selectedAudioId }
    when {
        audio != null -> AudioEffectsSheet(
            targetTitle = audio.title,
            settings = audio.audioEffects,
            onSettingsChange = { viewModel.setAudioEffects(audio.id, it) },
            onClose = { viewModel.closeToolSheet() }
        )
        currentClip != null -> AudioEffectsSheet(
            targetTitle = currentClip.title,
            settings = currentClip.audioEffects,
            onSettingsChange = { settings: AudioEffectSettings ->
                viewModel.setClipAudioEffects(settings)
            },
            onClose = { viewModel.closeToolSheet() }
        )
        else -> AudioEffectsSheetEmpty(onClose = { viewModel.closeToolSheet() })
    }
}

@Composable
internal fun VolumeToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    val clip = currentClip
    VolumeSheet(
        currentVolume = clip?.volume ?: 1.0f,
        isNoiseReductionEnabled = clip?.audioEffects?.reduceNoise == true,
        isLoudnessAdjusted = clip?.isLoudnessAdjusted == true,
        canNormaliseLoudness = clip?.waveform?.isNotEmpty() == true,
        // The on-device peak gate is part of the clip's effect chain, so the
        // switch really does something now.
        canReduceNoise = clip != null,
        audioFadeInMs = clip?.audioFadeInMs ?: 0L,
        audioFadeOutMs = clip?.audioFadeOutMs ?: 0L,
        maxFadeMs = clip?.let { com.example.data.ClipTiming.presentationDurationMs(it) } ?: 0L,
        onVolumeChange = { vol ->
            viewModel.setClipVolume(vol)
        },
        onToggleNoiseReduction = { enabled ->
            val current = currentClip ?: return@VolumeSheet
            viewModel.setClipAudioEffects(current.audioEffects.copy(reduceNoise = enabled))
        },
        onToggleLoudnessAdjustment = { enabled ->
            viewModel.setClipLoudnessAdjustment(enabled)
        },
        onAudioFadesChange = { fadeIn, fadeOut ->
            viewModel.setClipAudioFades(fadeIn, fadeOut)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}
