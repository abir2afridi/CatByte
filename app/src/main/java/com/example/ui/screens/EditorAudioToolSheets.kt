package com.example.ui.screens

import android.content.Intent
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
        // Keep access after a process restart so the mixer/export can reopen
        // the same source. Some document providers do not grant persistence.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        // AudioFeature resolves the provider's display name asynchronously.
        viewModel.addAudioTrackFromUri(uri.toString(), "", "Imported")
        viewModel.setActiveSheet(ActiveToolSheet.NONE)
    }

    AudioSheet(
        isRecordingVoice = isRecordingVoice,
        voiceDurationMs = voiceRecordingDurationMs,
        initialTab = initialTab,
        onAddAudio = { track ->
            viewModel.addAudioTrack(track)
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
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
