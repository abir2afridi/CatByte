package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.MediaClip
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
    AudioSheet(
        isRecordingVoice = isRecordingVoice,
        voiceDurationMs = voiceRecordingDurationMs,
        initialTab = initialTab,
        onAddAudio = { track ->
            viewModel.addAudioTrack(track)
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
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
