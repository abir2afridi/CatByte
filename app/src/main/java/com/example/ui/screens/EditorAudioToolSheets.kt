package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.model.MediaClip
import com.example.model.VideoProject
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
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun AudioMixerToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject,
    selectedAudioId: String?
) {
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
        onReset = { viewModel.resetAudioMixer(it) },
        onDelete = { viewModel.deleteAudioTrack(it) },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun VolumeToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    VolumeSheet(
        currentVolume = currentClip?.volume ?: 1.0f,
        isLoudnessAdjusted = currentClip?.isLoudnessAdjusted == true,
        canNormaliseLoudness = currentClip?.waveform?.isNotEmpty() == true,
        onVolumeChange = { vol ->
            viewModel.setClipVolume(vol)
        },
        onToggleLoudnessAdjustment = { enabled ->
            viewModel.setClipLoudnessAdjustment(enabled)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}
