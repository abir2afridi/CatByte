package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.example.model.MediaClip
import com.example.model.TextOverlay
import com.example.model.VideoProject
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * Hosts the editor's bottom tool sheet with smooth slide-up / slide-down animations.
 */
@Composable
internal fun BoxScope.EditorToolSheetHost(
    viewModel: EditorViewModel,
    project: VideoProject,
    activeSheet: ActiveToolSheet,
    currentClip: MediaClip?,
    currentText: TextOverlay?,
    captionExportCount: Int,
    selectedClipId: String?,
    selectedAudioId: String?,
    isEyedropperActive: Boolean,
    isMultiSelect: Boolean,
    onMultiSelectChange: (Boolean) -> Unit,
    activeTransitionClipId: String?,
    magneticSnap: Boolean,
    timelineZoom: Float,
    splitAtPlayhead: () -> Unit,
    isRecordingVoice: Boolean,
    voiceRecordingDurationMs: Long,
    audioSheetTab: Int,
    onShowTts: () -> Unit,
    onLaunchCaptionImport: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val visible = activeSheet != ActiveToolSheet.NONE && activeSheet != ActiveToolSheet.EXPORT_MODAL

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 280)
        ) + fadeIn(animationSpec = tween(280)),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 220)
        ) + fadeOut(animationSpec = tween(220)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .zIndex(120f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when (activeSheet) {
                ActiveToolSheet.TTS_VOICEOVER -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                ActiveToolSheet.EDIT_CLIP -> {
                EditClipToolSheet(
                    viewModel = viewModel,
                    currentClip = currentClip,
                    splitAtPlayhead = splitAtPlayhead
                )
            }
            ActiveToolSheet.SPEED -> {
                SpeedToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.AUDIO_LIBRARY, ActiveToolSheet.VOICE_RECORDER -> {
                AudioToolSheet(
                    viewModel = viewModel,
                    isRecordingVoice = isRecordingVoice,
                    voiceRecordingDurationMs = voiceRecordingDurationMs,
                    initialTab = audioSheetTab,
                    onShowTts = onShowTts
                )
            }
            ActiveToolSheet.AUDIO_MIXER -> {
                AudioMixerToolSheet(
                    viewModel = viewModel,
                    project = project,
                    selectedAudioId = selectedAudioId
                )
            }
            ActiveToolSheet.AUDIO_EFFECTS -> {
                AudioEffectsToolSheet(
                    viewModel = viewModel,
                    project = project,
                    currentClip = currentClip,
                    selectedAudioId = selectedAudioId
                )
            }
            ActiveToolSheet.TEXT_EDITOR -> {
                TextEditorToolSheet(
                    viewModel = viewModel,
                    project = project,
                    currentText = currentText,
                    captionExportCount = captionExportCount,
                    onLaunchCaptionImport = onLaunchCaptionImport
                )
            }
            ActiveToolSheet.EFFECTS_LIBRARY -> {
                EffectsToolSheet(viewModel = viewModel)
            }
            ActiveToolSheet.BODY_EFFECTS -> {
                BodyEffectsToolSheet(viewModel = viewModel)
            }
            ActiveToolSheet.FILTERS_LIBRARY -> {
                FiltersToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.LUT_IMPORT -> {
                LutImportToolSheet(viewModel = viewModel)
            }
            ActiveToolSheet.ADJUSTMENTS -> {
                AdjustmentsToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.CANVAS_RATIO -> {
                CanvasRatioToolSheet(viewModel = viewModel, project = project)
            }
            ActiveToolSheet.STICKERS_PICKER -> {
                StickersToolSheet(viewModel = viewModel)
            }
            ActiveToolSheet.TRANSITIONS_PICKER -> {
                TransitionsToolSheet(
                    viewModel = viewModel,
                    activeTransitionClipId = activeTransitionClipId
                )
            }

            ActiveToolSheet.KEYFRAME -> {
                KeyframeToolSheet(
                    viewModel = viewModel,
                    currentClip = currentClip,
                    selectedClipId = selectedClipId
                )
            }
            ActiveToolSheet.BLEND_MODE -> {
                BlendModeToolSheet(
                    viewModel = viewModel,
                    currentClip = currentClip,
                    selectedClipId = selectedClipId
                )
            }
            ActiveToolSheet.MASK -> {
                MaskToolSheet(
                    viewModel = viewModel,
                    currentClip = currentClip,
                    selectedClipId = selectedClipId
                )
            }
            ActiveToolSheet.CHROMA_KEY -> {
                ChromaKeyToolSheet(
                    viewModel = viewModel,
                    currentClip = currentClip,
                    selectedClipId = selectedClipId,
                    isEyedropperActive = isEyedropperActive
                )
            }
            ActiveToolSheet.VOLUME_BOOST -> {
                VolumeToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.CAPTIONS -> {
                CaptionsToolSheet(
                    viewModel = viewModel,
                    project = project,
                    onLaunchCaptionImport = onLaunchCaptionImport
                )
            }
            ActiveToolSheet.ANIMATIONS -> {
                AnimationToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.CROP -> {
                CropToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.OPACITY -> {
                OpacityToolSheet(viewModel = viewModel, currentClip = currentClip)
            }
            ActiveToolSheet.AI_TOOLS -> {
                AiToolsToolSheet(viewModel = viewModel, onOpenSettings = onOpenSettings)
            }
            ActiveToolSheet.TIMELINE_TOOLS -> {
                TimelineToolsToolSheet(
                    viewModel = viewModel,
                    project = project,
                    isMultiSelect = isMultiSelect,
                    magneticSnap = magneticSnap,
                    timelineZoom = timelineZoom,
                    selectedClipId = selectedClipId,
                    onMultiSelectChange = onMultiSelectChange
                )
            }
            ActiveToolSheet.DRAW -> {
                DrawToolSheet(viewModel = viewModel)
            }
            ActiveToolSheet.MARKERS -> {
                MarkerToolSheet(viewModel = viewModel, project = project)
            }
            ActiveToolSheet.BACKGROUND -> {
                BackgroundToolSheet(viewModel = viewModel, project = project)
            }
            ActiveToolSheet.COVER_EDITOR -> {
                CoverEditorToolSheet(viewModel = viewModel, project = project)
            }
            ActiveToolSheet.TEMPLATES -> {
                TemplateToolSheet(viewModel = viewModel)
            }
            ActiveToolSheet.EXPORT_MODAL -> {}
            ActiveToolSheet.NONE -> {}
        }
      }
    }
}


