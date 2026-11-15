package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.example.model.MediaClip
import com.example.model.TextOverlay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.engine.Shortcuts
import com.example.model.VideoProject
import com.example.ui.components.AdjustmentsSheet
import com.example.ui.components.AiToolsSheet
import com.example.ui.components.AnimationSheet
import com.example.ui.audio.AudioSheet
import com.example.ui.components.BackgroundSheet
import com.example.ui.components.BlendModeSheet
import com.example.ui.components.CanvasRatioSheet
import com.example.ui.components.CaptionsSheet
import com.example.ui.components.CoverEditorSheet
import com.example.ui.components.CropSheet
import com.example.ui.components.EditorSearchEntry
import com.example.ui.export.ExportDropdownPanel
import com.example.ui.components.FeatureSearchDialog
import com.example.ui.components.OpacitySheet
import com.example.ui.components.SelectionControlsBar
import com.example.ui.components.EditClipSheet
import com.example.ui.components.EffectsSheet
import com.example.export.ExportSettings
import com.example.export.ExportUiState
import com.example.ui.export.ExportDialog
import com.example.ui.export.ExportOptionsBottomSheet
import com.example.ui.export.QuitExportingDialog
import com.example.ui.components.FiltersSheet
import com.example.ui.components.KeyframeSheet
import com.example.ui.components.MaskSheet
import com.example.ui.components.ChromaKeySheet
import com.example.ui.components.TimelineToolsSheet
import com.example.ui.components.VolumeSheet
import com.example.ui.components.MultiTrackTimeline
import com.example.ui.components.SpeedSheet
import com.example.ui.components.StickersSheet
import com.example.ui.text.TextEditorSheet
import androidx.compose.foundation.layout.BoxScope
import com.example.ui.components.TransitionsPickerSheet
import com.example.ui.canvas.VideoCanvas
import com.example.ui.theme.PencilTheme
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.data.MediaDurationFallback
import com.example.data.ScrubPreviewFrame

@Composable
internal fun BoxScope.EditorToolSheetHost(
    viewModel: EditorViewModel,
    project: VideoProject,
    activeSheet: ActiveToolSheet,
    currentClip: MediaClip?,
    currentText: TextOverlay?,
    captionExportCount: Int,
    selectedClipId: String?,
    isEyedropperActive: Boolean,
    isMultiSelect: Boolean,
    onMultiSelectChange: (Boolean) -> Unit,
    activeTransitionClipId: String?,
    magneticSnap: Boolean,
    timelineZoom: Float,
    splitAtPlayhead: () -> Unit,
    isRecordingVoice: Boolean,
    voiceRecordingDurationMs: Long,
    onShowTts: () -> Unit,
    onLaunchCaptionImport: () -> Unit
) {
    if (activeSheet == ActiveToolSheet.NONE || activeSheet == ActiveToolSheet.EXPORT_MODAL) return
    val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                when (activeSheet) {
                    ActiveToolSheet.TTS_VOICEOVER -> {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    ActiveToolSheet.EDIT_CLIP -> {
                    EditClipSheet(
                        clip = currentClip,
                        onSplit = splitAtPlayhead,
                        onDelete = { viewModel.deleteSelectedClip() },
                        onDuplicate = { viewModel.duplicateSelectedClip() },
                        onRotate = { viewModel.rotateSelectedClip() },
                        onMirror = { viewModel.mirrorSelectedClip() },
                        onOpenSpeed = { viewModel.setActiveSheet(ActiveToolSheet.SPEED) },
                        onOpenAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                        onOpenKeyframe = { viewModel.setActiveSheet(ActiveToolSheet.KEYFRAME) },
                        onOpenBlendMode = { viewModel.setActiveSheet(ActiveToolSheet.BLEND_MODE) },
                        onOpenMask = { viewModel.setActiveSheet(ActiveToolSheet.MASK) },
                        onExtractAudio = { viewModel.extractAudioFromSelectedClip() },
                        onFreeze = { viewModel.freezeFrameAtPlayhead() },
                        onReverse = { viewModel.reverseSelectedClip() },
                        onOpenVolume = { viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST) },
                        onOpenChroma = { viewModel.setActiveSheet(ActiveToolSheet.CHROMA_KEY) },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.SPEED -> {
                    SpeedSheet(
                        currentSpeed = currentClip?.speed ?: 1.0f,
                        clipDurationMs = currentClip?.durationMs ?: 0L,
                        isChangePitch = currentClip?.maintainPitch == false,
                        onSpeedSelected = { viewModel.setClipSpeed(it) },
                        onSelectSpeedCurve = { viewModel.setClipSpeedCurve(it) },
                        onChangePitch = { changePitch -> viewModel.setClipMaintainPitch(!changePitch) },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.AUDIO_LIBRARY, ActiveToolSheet.VOICE_RECORDER -> {
                    AudioSheet(
                        isRecordingVoice = isRecordingVoice,
                        voiceDurationMs = voiceRecordingDurationMs,
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
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.TEXT_EDITOR -> {
                    TextEditorSheet(
                        textOverlay = currentText,
                        onAddOrUpdateText = { text, font, color, anim ->
                            if (currentText != null) {
                                viewModel.updateSelectedText(text, font, color, anim)
                            } else {
                                viewModel.addTextOverlay(text, font, color, anim)
                            }
                        },
                        onDeleteText = {
                            viewModel.deleteSelectedText()
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        },
                        onImportCaptions = { onLaunchCaptionImport() },
                        onExportCaptions = {
                            val content = viewModel.exportSubtitles(toVtt = false)
                            if (content == null) {
                                Toast.makeText(context, "No text overlays to export yet", Toast.LENGTH_LONG).show()
                            } else {
                                shareSubtitleExport(context, project.id, content)
                            }
                        },
                        captionExportCount = captionExportCount,
                        onAutoCaptions = { viewModel.addAutoCaptions() },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.EFFECTS_LIBRARY -> {
                    EffectsSheet(
                        onAddEffect = { fx ->
                            if (fx != null) {
                                viewModel.addTimelineEffect(fx)
                            }
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.FILTERS_LIBRARY -> {
                    FiltersSheet(
                        activeFilter = currentClip?.filter ?: com.example.model.VideoFilter.NONE,
                        onSelectFilter = { filter ->
                            viewModel.setClipFilter(filter)
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.ADJUSTMENTS -> {
                    AdjustmentsSheet(
                        clip = currentClip,
                        onUpdateAdjustments = { b, c, s, v ->
                            viewModel.updateClipAdjustments(b, c, s, v)
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.CANVAS_RATIO -> {
                    CanvasRatioSheet(
                        activeRatio = project.aspectRatio,
                        currentPaperStyle = project.paperStyle,
                        onSelectRatio = { viewModel.setAspectRatio(it) },
                        onSelectPaperStyle = { viewModel.setPaperStyle(it) },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.STICKERS_PICKER -> {
                    StickersSheet(
                        onAddSticker = { emoji ->
                            viewModel.addSticker(emoji)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.TRANSITIONS_PICKER -> {
                    TransitionsPickerSheet(
                        clipId = activeTransitionClipId ?: "",
                        onSelectTransition = { trans ->
                            viewModel.setClipTransition(trans)
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }

                ActiveToolSheet.KEYFRAME -> {
                    KeyframeSheet(
                        clip = currentClip,
                        onAddKeyframe = { prop, val1, ease ->
                            if (selectedClipId != null) {
                                viewModel.addKeyframe(selectedClipId!!, prop, val1, ease)
                            }
                        },
                        onRemoveKeyframe = { kfId ->
                            if (selectedClipId != null) {
                                viewModel.removeKeyframe(selectedClipId!!, kfId)
                            }
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.BLEND_MODE -> {
                    BlendModeSheet(
                        selectedMode = currentClip?.blendMode ?: com.example.model.BlendMode.NORMAL,
                        onSelectBlendMode = { mode ->
                            if (selectedClipId != null) {
                                viewModel.setClipBlendMode(selectedClipId!!, mode)
                            }
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.MASK -> {
                    MaskSheet(
                        maskData = currentClip?.mask ?: com.example.model.MaskData(),
                        onUpdateMask = { mask ->
                            if (selectedClipId != null) {
                                viewModel.setClipMask(selectedClipId!!, mask)
                            }
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.CHROMA_KEY -> {
                    ChromaKeySheet(
                        chromaData = currentClip?.chromaKey ?: com.example.model.ChromaKeyData(),
                        isEyedropperActive = isEyedropperActive,
                        onToggleEyedropper = { viewModel.setEyedropperActive(!isEyedropperActive) },
                        onUpdateChroma = { data ->
                            if (selectedClipId != null) {
                                viewModel.setClipChromaKey(selectedClipId!!, data)
                            }
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.VOLUME_BOOST -> {
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
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.CAPTIONS -> {
                    CaptionsSheet(
                        onAutoCaptions = { viewModel.addAutoCaptions() },
                        onImportCaptions = { onLaunchCaptionImport() },
                        onExportSrt = {
                            val content = viewModel.exportSubtitles(toVtt = false)
                            if (content == null) {
                                Toast.makeText(context, "No captions to export yet", Toast.LENGTH_LONG).show()
                            } else {
                                shareSubtitleExport(context, project.id, content)
                            }
                        },
                        onExportVtt = {
                            val content = viewModel.exportSubtitles(toVtt = true)
                            if (content == null) {
                                Toast.makeText(context, "No captions to export yet", Toast.LENGTH_LONG).show()
                            } else {
                                shareSubtitleExport(context, project.id, content)
                            }
                        },
                        onAddText = {
                            viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.ANIMATIONS -> {
                    AnimationSheet(
                        current = currentClip?.animationIn ?: com.example.model.ClipAnimation.NONE,
                        onSelect = { viewModel.setClipAnimation(it) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.CROP -> {
                    CropSheet(
                        sourceWidth = currentClip?.sourceWidth ?: 0,
                        sourceHeight = currentClip?.sourceHeight ?: 0,
                        currentScale = currentClip?.scale ?: 1.0f,
                        currentPosX = currentClip?.posX ?: 0.5f,
                        currentPosY = currentClip?.posY ?: 0.5f,
                        onApply = { scale, posX, posY ->
                            viewModel.setSelectedClipCrop(scale, posX, posY)
                        },
                        onReset = { viewModel.resetSelectedClipCrop() },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.OPACITY -> {
                    OpacitySheet(
                        currentOpacity = currentClip?.opacity ?: 1.0f,
                        onOpacityChange = { viewModel.setClipOpacity(it) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.AI_TOOLS -> {
                    AiToolsSheet(
                        onSelect = { name ->
                            Toast.makeText(
                                context,
                                "$name needs an online AI model — unavailable in this offline build",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.TIMELINE_TOOLS -> {
                    TimelineToolsSheet(
                        isMultiSelect = isMultiSelect,
                        magneticSnap = magneticSnap,
                        zoom = timelineZoom,
                        markerCount = project?.markers?.size ?: 0,
                        hasSelectedClip = selectedClipId != null,
                        onBeatSync = { viewModel.autoCutToBeat() },
                        onAddMarker = { viewModel.addMarker() },
                        onPreviousMarker = { viewModel.jumpToMarker(-1) },
                        onNextMarker = { viewModel.jumpToMarker(1) },
                        onToggleMultiSelect = {
                            val next = !isMultiSelect
                            onMultiSelectChange(next)
                            if (!next) viewModel.clearSelection()
                        },
                        onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
                        onZoomChanged = { viewModel.setTimelineZoom(it) },
                        onToggleKeyframe = { viewModel.toggleKeyframeAtPlayhead() },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.BACKGROUND -> {
                    BackgroundSheet(
                        currentPaperStyle = project.paperStyle,
                        onSelectBackground = { viewModel.setPaperStyle(it) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.COVER_EDITOR -> {
                    CoverEditorSheet(
                        project = project,
                        onSaveCover = { _, _ ->
                            Toast.makeText(context, "Cover saved successfully", Toast.LENGTH_SHORT).show()
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.EXPORT_MODAL -> {}
                ActiveToolSheet.NONE -> {}
            }
        }
}
