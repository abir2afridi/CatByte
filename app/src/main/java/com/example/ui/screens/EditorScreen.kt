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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.example.model.MediaClip
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.components.AudioSheet
import com.example.ui.components.BlendModeSheet
import com.example.ui.components.CanvasRatioSheet
import com.example.ui.components.PlaybackControlsStrip
import com.example.ui.components.SelectionControlsBar
import com.example.ui.components.EditClipSheet
import com.example.ui.components.EffectsSheet
import com.example.ui.components.ExportDialog
import com.example.ui.components.FiltersSheet
import com.example.ui.components.KeyframeSheet
import com.example.ui.components.MaskSheet
import com.example.ui.components.ChromaKeySheet
import com.example.ui.components.VolumeSheet
import com.example.ui.components.MultiTrackTimeline
import com.example.ui.components.SpeedSheet
import com.example.ui.components.StickersSheet
import com.example.ui.components.TextEditorSheet
import com.example.ui.components.TransitionsPickerSheet
import com.example.ui.components.VideoCanvas
import com.example.ui.theme.PencilTheme
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.data.MediaDurationFallback

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val selectedClipIds by viewModel.selectedClipIds.collectAsState()
    val overlayClipboard by viewModel.overlayClipboard.collectAsState()
    val selectedTextId by viewModel.selectedTextId.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val timelineZoom by viewModel.timelineZoom.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceRecordingDurationMs by viewModel.voiceRecordingDurationMs.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val magneticSnap by viewModel.magneticSnap.collectAsState()
    val watermarkEnabled by viewModel.watermarkEnabled.collectAsState()
    val snapBeats by viewModel.snapBeats.collectAsState()

    var activeTransitionClipId by remember { mutableStateOf<String?>(null) }
    var showTtsDialog by remember { mutableStateOf(false) }
    var ttsInputText by remember { mutableStateOf("") }
    var isMultiSelect by remember { mutableStateOf(false) }

    // Intercept back button if any sheet is open
    BackHandler {
        if (activeSheet != ActiveToolSheet.NONE) {
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        } else if (showTtsDialog) {
            showTtsDialog = false
        } else {
            onBackToHome()
        }
    }

    // Media Picker for adding clips to project
    val addMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.launchImportUris(uris)
    }

    // Subtitle import picker (SRT / VTT)
    val importCaptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val raw = readCaptionFile(context, uri)
        if (raw == null) {
            Toast.makeText(context, "Could not read the file (max 2 MB)", Toast.LENGTH_LONG).show()
        } else {
            val result = viewModel.importSubtitles(raw)
            if (result.imported == 0) {
                Toast.makeText(
                    context,
                    result.issues.firstOrNull() ?: "No valid timecodes found",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val note = when {
                    result.issues.isEmpty() -> ""
                    result.issues.size == 1 -> " â€” ${result.issues.first()}"
                    else -> " â€” ${result.issues.size} notes (overlapping cues?)"
                }
                Toast.makeText(
                    context,
                    "Imported ${result.imported} caption${if (result.imported == 1) "" else "s"}$note",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Media Picker for adding Overlay PIP clips
    val addOverlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addOverlayClipFromUri(uri.toString(), "PIP Clip")
        }
    }

    val currentClip = project?.allVideoClips?.find { it.id == selectedClipId }
    val isEyedropperActive by viewModel.isEyedropperActive.collectAsState()
    val isTransformActive = activeSheet == ActiveToolSheet.EDIT_CLIP || activeSheet == ActiveToolSheet.KEYFRAME
    val currentText = project?.textOverlays?.find { it.id == selectedTextId }
    val captionExportCount = project?.textOverlays?.count { it.text.isNotBlank() } ?: 0

    // One split path for the toolbar, keyboard shortcut and clip sheet, with
    // feedback instead of a silent no-op when the playhead is on a clip edge.
    val splitAtPlayhead: () -> Unit = {
        if (!viewModel.splitClipAtPlayhead()) {
            Toast.makeText(
                context,
                "Move the playhead inside a clip to split it",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_active_project), color = colors.textPrimary)
        }
        return
    }

    // Tablets, Chromebooks and DeX: the editor takes focus on open so the
    // Shortcuts table works without tapping the canvas first.
    val shortcutFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { shortcutFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .focusRequester(shortcutFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val command = Shortcuts.resolve(
                    key = event.key.toEditorKey(),
                    ctrl = event.isCtrlPressed,
                    shift = event.isShiftPressed,
                    alt = event.isAltPressed,
                ) ?: return@onPreviewKeyEvent false
                when (command) {
                    Shortcuts.Command.PLAY_PAUSE -> viewModel.togglePlayback()
                    Shortcuts.Command.SPLIT -> splitAtPlayhead()
                    Shortcuts.Command.DELETE -> viewModel.deleteSelectedClip()
                    Shortcuts.Command.UNDO -> viewModel.undo()
                    Shortcuts.Command.REDO -> viewModel.redo()
                    Shortcuts.Command.ZOOM_IN -> viewModel.setTimelineZoom(timelineZoom + 0.25f)
                    Shortcuts.Command.ZOOM_OUT -> viewModel.setTimelineZoom(timelineZoom - 0.25f)
                    Shortcuts.Command.PREV_FRAME ->
                        viewModel.seekTo((currentPositionMs - FRAME_STEP_MS).coerceAtLeast(0L))
                    Shortcuts.Command.NEXT_FRAME -> viewModel.seekTo(currentPositionMs + FRAME_STEP_MS)
                    // No mute toggle exists in the editor yet, so let the key pass.
                    Shortcuts.Command.TOGGLE_MUTE -> return@onPreviewKeyEvent false
                }
                true
            }
    ) {
        // 1. Top Bar
        EditorTopBar(
            project = project!!,
            canUndo = canUndo,
            canRedo = canRedo,
            onBack = onBackToHome,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onOpenFormat = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
            onExport = { viewModel.setActiveSheet(ActiveToolSheet.EXPORT_MODAL) },
            onOpenSettings = onOpenSettings
        )

        // 2. Video Preview Canvas
        val playerManager = androidx.compose.runtime.remember(context) {
            (context.applicationContext as? com.example.CatByteApplication)
                ?.container?.playerManager
        }
        val previewPlayer = playerManager
            ?.activePlayer
            ?.collectAsState()
            ?.value
        val compositionPreview = playerManager
            ?.usesCompositionPreview
            ?.collectAsState()
            ?.value == true
        val previewError = playerManager
            ?.lastError
            ?.collectAsState()
            ?.value
        val previewWarnings = playerManager
            ?.previewWarnings
            ?.collectAsState()
            ?.value
            .orEmpty()

        // Background export needs the notification permission on Android 13+.
        // A denial must not block the export (the foreground service runs
        // either way; only the progress notification stays hidden).
        var pendingBackgroundExport by remember { mutableStateOf<Pair<String, Int>?>(null) }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ ->
            pendingBackgroundExport?.let { (res, fps) ->
                viewModel.startBackgroundExportWithContext(context, res, fps)
            }
            pendingBackgroundExport = null
        }
        val startBackgroundExport: (resolution: String, fps: Int) -> Unit = { res, fps ->
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingBackgroundExport = res to fps
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.startBackgroundExportWithContext(context, res, fps)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.components.LocalVideoPlayer provides previewPlayer
            ) {
                VideoCanvas(
                    project = project!!,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.togglePlayback() },
                    watermarkEnabled = watermarkEnabled,
                    player = previewPlayer,
                    compositionPreview = compositionPreview,
                    previewError = previewError,
                    previewWarnings = previewWarnings,
                    selectedClip = currentClip,
                    isTransformInteractive = isTransformActive,
                    onTransformClip = { zoom, rot, panX, panY ->
                        viewModel.updateSelectedClipTransform(zoom, rot, panX, panY)
                    },
                    isEyedropperActive = isEyedropperActive,
                    onSampleColorAt = { nx, ny ->
                        viewModel.sampleChromaColorAt(context, nx, ny)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. Playback Controls & Quick Split Strip
        PlaybackControlsStrip(
            isPlaying = isPlaying,
            zoom = timelineZoom,
            magneticSnap = magneticSnap,
            onTogglePlay = { viewModel.togglePlayback() },
            onStepBackward = { viewModel.stepBackward() },
            onStepForward = { viewModel.stepForward() },
            onQuickSplit = splitAtPlayhead,
            onBeatSync = { viewModel.autoCutToBeat() },
            onAddMarker = { viewModel.addMarker() },
            onPreviousMarker = { viewModel.jumpToMarker(-1) },
            onNextMarker = { viewModel.jumpToMarker(1) },
            markerCount = project?.markers?.size ?: 0,
            onToggleMultiSelect = {
                isMultiSelect = !isMultiSelect
                if (!isMultiSelect) viewModel.clearSelection()
            },
            isMultiSelect = isMultiSelect,
            onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
            onZoomChanged = { viewModel.setTimelineZoom(it) },
            selectedClip = currentClip,
            currentPositionMs = currentPositionMs,
            onToggleKeyframe = { viewModel.toggleKeyframeAtPlayhead() }
        )

        // Group actions for multi-selection.
        val selectedOverlayClips = remember(project, selectedClipIds) {
            project?.overlayTracks?.flatMap { it.clips }
                ?.filter { it.id in selectedClipIds }
                ?: emptyList()
        }
        if (selectedClipIds.size > 1) {
            SelectionControlsBar(
                selectedCount = selectedClipIds.size,
                isMultiSelect = isMultiSelect,
                canMoveOverlays = selectedOverlayClips.isNotEmpty(),
                canPaste = overlayClipboard.isNotEmpty(),
                onMoveLeft = { viewModel.moveSelectedOverlays(-100L) },
                onMoveRight = { viewModel.moveSelectedOverlays(100L) },
                onDelete = { viewModel.deleteSelectedClips() },
                onCopy = { viewModel.copySelectedOverlays() },
                onPaste = { viewModel.pasteOverlays() },
                onClear = {
                    viewModel.clearSelection()
                    isMultiSelect = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Adaptive heights: small phones must not overlap the bottom bar.
        val timelineHeight = if (LocalConfiguration.current.screenHeightDp < 640) {
            148.dp
        } else {
            175.dp
        }

        // 4. Multi-Track Timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(timelineHeight)
        ) {
            MultiTrackTimeline(
                project = project!!,
                currentPositionMs = currentPositionMs,
                isPlaying = isPlaying,
                selectedClipId = selectedClipId,
                selectedTextId = selectedTextId,
                zoom = timelineZoom,
                magneticSnap = magneticSnap,
                onSeek = { viewModel.seekTo(it) },
                onSelectClip = {
                    viewModel.selectClip(it)
                    viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP)
                },
                onSelectText = {
                    viewModel.selectText(it)
                    viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
                },
                onAddMediaClick = {
                    addMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onTransitionClick = { clipId ->
                    activeTransitionClipId = clipId
                    viewModel.setActiveSheet(ActiveToolSheet.TRANSITIONS_PICKER)
                },
                onMoveClip = { from, to -> viewModel.moveClip(from, to) },
                onMoveOverlayClip = { clipId, startMs -> viewModel.moveOverlayClip(clipId, startMs) },
                onMoveOverlayClipToTrack = { clipId, trackId, startMs ->
                    viewModel.moveClipToTrack(clipId, trackId, startMs)
                },
                onTrimClip = { edge, deltaMs ->
                    viewModel.trimSelectedClipEdge(edge, deltaMs, project?.fps ?: 30)
                },
                onTrackMute = { trackId, muted -> viewModel.setTrackMuted(trackId, muted) },
                onTrackLock = { trackId, locked -> viewModel.setTrackLocked(trackId, locked) },
                onTrackSolo = { trackId, solo -> viewModel.setTrackSolo(trackId, solo) },
                onMoveOverlayTrack = { from, to -> viewModel.moveOverlayTrack(from, to) },
                onToggleAudioMute = { audioId, muted -> viewModel.setAudioClipMuted(audioId, muted) },
                onZoomChange = { viewModel.setTimelineZoom(it) },
                onToggleClipSelection = { viewModel.toggleClipSelection(it) },
                isMultiSelect = isMultiSelect,
                selectedClipIds = selectedClipIds,
                beats = snapBeats,
                onUserInteraction = { viewModel.pausePlayback() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 5. Active Tool Sheet / Bottom Bar
        if (activeSheet != ActiveToolSheet.NONE) {
            when (activeSheet) {
                ActiveToolSheet.TTS_VOICEOVER -> {
                    // Handled by the AudioSheet when TTS is triggered
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
                        onSpeedSelected = { viewModel.setClipSpeed(it) },
                        onSelectSpeedCurve = { viewModel.setClipSpeedCurve(it) },
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
                            showTtsDialog = true
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
                        onImportCaptions = { importCaptionsLauncher.launch(arrayOf("*/*")) },
                        onExportCaptions = {
                            val content = viewModel.exportSubtitles(toVtt = false)
                            if (content == null) {
                                Toast.makeText(context, "No text overlays to export yet", Toast.LENGTH_LONG).show()
                            } else {
                                shareSubtitleExport(context, project!!.id, content)
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
                            viewModel.addTimelineEffect(fx)
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
                        activeRatio = project!!.aspectRatio,
                        currentPaperStyle = project!!.paperStyle,
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
                ActiveToolSheet.EXPORT_MODAL -> {
                    ExportDialog(
                        exportState = exportState,
                        defaultResolution = project?.resolution ?: "1080P",
                        defaultFps = project?.fps ?: 30,
                        watermarkEnabled = watermarkEnabled,
                        currentAspectRatio = project?.aspectRatio ?: com.example.model.AspectRatioType.RATIO_16_9,
                        projectLengthMs = project?.totalDurationMs ?: 60_000L,
                        onSelectAspectRatio = { viewModel.setAspectRatio(it) },
                        onToggleWatermark = { viewModel.toggleWatermark(it) },
                        onStartExport = { res, fps, _ -> viewModel.startExport(res, fps) },
                        onCancelExport = { viewModel.cancelExport() },
                        onDismiss = { viewModel.dismissExport() },
                        onShareExport = { platform -> viewModel.shareExportMessage(platform) },
                        onStartBackgroundExport = { res, fps, _ -> startBackgroundExport(res, fps) }
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
                        onVolumeChange = { vol ->
                            viewModel.setClipVolume(vol)
                        },
                        onClose = {
                            viewModel.setEyedropperActive(false)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        }
                    )
                }
                ActiveToolSheet.NONE -> {}
            }
        } else {
            // Bottom Action Toolbar
            CapCutBottomActionBar(
                onSplit = splitAtPlayhead,
                onAudio = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
                onText = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
                onOverlay = {
                    addOverlayLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                onFilters = { viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY) },
                onFormat = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) }
            )
        }
    }

    if (showTtsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTtsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface)
                        .border(1.5.dp, colors.border, RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("AI Voiceover", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Enter the text you want to convert to speech:", color = colors.textSecondary, fontSize = 14.sp)

                        OutlinedTextField(
                            value = ttsInputText,
                            onValueChange = { ttsInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { showTtsDialog = false },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Cancel", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                            }
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (ttsInputText.isNotBlank()) {
                                        viewModel.startTtsVoiceover(ttsInputText)
                                        ttsInputText = ""
                                        showTtsDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentCyan),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text("Generate", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Top Bar
// ==========================================
