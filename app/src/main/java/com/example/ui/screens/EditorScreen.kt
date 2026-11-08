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
import com.example.ui.components.AudioSheet
import com.example.ui.components.BackgroundSheet
import com.example.ui.components.BlendModeSheet
import com.example.ui.components.CanvasRatioSheet
import com.example.ui.components.CaptionsSheet
import com.example.ui.components.CropSheet
import com.example.ui.components.EditorSearchEntry
import com.example.ui.components.FeatureSearchDialog
import com.example.ui.components.OpacitySheet
import com.example.ui.components.SelectionControlsBar
import com.example.ui.components.EditClipSheet
import com.example.ui.components.EffectsSheet
import com.example.ui.components.ExportDialog
import com.example.ui.components.FiltersSheet
import com.example.ui.components.KeyframeSheet
import com.example.ui.components.MaskSheet
import com.example.ui.components.ChromaKeySheet
import com.example.ui.components.TimelineToolsSheet
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
    var showFeatureSearch by remember { mutableStateOf(false) }
    var isPreviewFullscreen by remember { mutableStateOf(false) }

    // Intercept back button if any sheet is open
    BackHandler {
        if (isPreviewFullscreen) {
            isPreviewFullscreen = false
        } else if (activeSheet != ActiveToolSheet.NONE) {
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

    // Replaces the selected clip's source file (CapCut "Replace").
    val replaceMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.replaceSelectedClipUri(uri.toString(), "")
        }
    }

    val currentClip = project?.allVideoClips?.find { it.id == selectedClipId }
    LaunchedEffect(activeSheet, selectedClipId) {
        if (activeSheet == ActiveToolSheet.VOLUME_BOOST) {
            viewModel.ensureSelectedClipWaveform()
        }
        if (activeSheet == ActiveToolSheet.CROP) {
            viewModel.ensureSelectedClipSourceSize()
        }
    }
    val isEyedropperActive by viewModel.isEyedropperActive.collectAsState()
    val isTransformActive = activeSheet == ActiveToolSheet.EDIT_CLIP || activeSheet == ActiveToolSheet.KEYFRAME
    val currentText = project?.textOverlays?.find { it.id == selectedTextId }
    val captionExportCount = project?.textOverlays?.count { it.text.isNotBlank() } ?: 0

    // One split path for the toolbar, keyboard shortcut and clip sheet, with
    // feedback instead of a silent no-op when the playhead is on a clip edge.
    val splitAtPlayhead: () -> Unit = {
        if (!viewModel.splitClipAtPlayhead()) {
            val targetClip = project?.clips?.firstOrNull()
            if (targetClip != null) {
                viewModel.seekTo(targetClip.durationMs / 2L)
                viewModel.splitClipAtPlayhead()
            } else {
                Toast.makeText(
                    context,
                    "Move the playhead inside a clip to split it",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    // Background export needs notification permission on Android 13+.
    var pendingBackgroundExport by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingBackgroundExport?.let { (res, fps, bitrate) ->
            viewModel.startBackgroundExportWithContext(context, res, fps, bitrate)
        }
        pendingBackgroundExport = null
    }
    val startBackgroundExport: (resolution: String, fps: Int, bitrateMbps: Int) -> Unit = { res, fps, bitrate ->
        viewModel.setActiveSheet(ActiveToolSheet.NONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingBackgroundExport = Triple(res, fps, bitrate)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startBackgroundExportWithContext(context, res, fps, bitrate)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .statusBarsPadding()
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
        Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top Bar
        if (!isPreviewFullscreen) {
            EditorTopBar(
                project = project!!,
                canUndo = canUndo,
                canRedo = canRedo,
                onBack = onBackToHome,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onOpenFormat = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                onExport = { viewModel.setActiveSheet(ActiveToolSheet.EXPORT_MODAL) },
                onOpenSettings = onOpenSettings,
                onSearch = { showFeatureSearch = true },
                onExportDraft = {
                    Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }

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



        Box(
            modifier = Modifier
                .weight(0.85f, fill = false)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
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

            if (isPreviewFullscreen) {
                IconButton(
                    onClick = { isPreviewFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("editor_exit_fullscreen")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. CapCut transport row: fullscreen, play, snap, undo, redo.
        if (!isPreviewFullscreen) {
            EditorTransportRow(
                isPlaying = isPlaying,
                canUndo = canUndo,
                canRedo = canRedo,
                magneticSnap = magneticSnap,
                onTogglePlay = { viewModel.togglePlayback() },
                onFullscreen = { isPreviewFullscreen = true },
                onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() }
            )
        }

        if (!isPreviewFullscreen) {
        // Group actions for multi-selection.
        val selectedOverlayClips = remember(project, selectedClipIds) {
            project?.overlayTracks?.flatMap { it.clips }
                ?.filter { it.id in selectedClipIds }
                ?: emptyList()
        }
        val selectedMainClips = remember(project, selectedClipIds) {
            project?.clips?.filter { it.id in selectedClipIds } ?: emptyList()
        }
        val canMoveMainBlock = selectedMainClips.size > 1
        if (selectedClipIds.size > 1) {
            SelectionControlsBar(
                selectedCount = selectedClipIds.size,
                isMultiSelect = isMultiSelect,
                canMoveOverlays = selectedOverlayClips.isNotEmpty(),
                canMoveMainBlock = canMoveMainBlock,
                canPaste = overlayClipboard.isNotEmpty(),
                onMoveLeft = { viewModel.moveSelectedOverlays(-100L) },
                onMoveRight = { viewModel.moveSelectedOverlays(100L) },
                onMoveBlockLeft = { viewModel.moveSelectedMainClips(-1) },
                onMoveBlockRight = { viewModel.moveSelectedMainClips(1) },
                onDelete = { viewModel.deleteSelectedClips() },
                onCopy = { viewModel.copySelectedOverlays() },
                onPaste = { viewModel.pasteOverlays() },
                onCopyAttributes = { viewModel.copySelectedClipAttributes() },
                onPasteAttributes = { viewModel.pasteClipAttributes() },
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

        // 4. Multi-Track Timeline (integrated CapCut left gutter and timecode)
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
                onTrimStart = { clipId, edge ->
                    viewModel.beginTrimClip(clipId, edge)
                },
                onTrimPreview = { _, _, deltaMs ->
                    // Live: applies against the drag-start snapshot, idempotent.
                    viewModel.updateTrimClip(deltaMs, project?.fps ?: 30)
                },
                onTrimEnd = { _, _ ->
                    viewModel.endTrimClip()
                },
                onTrackMute = { trackId, muted -> viewModel.setTrackMuted(trackId, muted) },
                onTrackLock = { trackId, locked -> viewModel.setTrackLocked(trackId, locked) },
                onTrackSolo = { trackId, solo -> viewModel.setTrackSolo(trackId, solo) },
                onMoveOverlayTrack = { from, to -> viewModel.moveOverlayTrack(from, to) },
                onMoveAudioClip = { audioId, startMs -> viewModel.moveAudioClip(audioId, startMs) },
                onToggleAudioMute = { audioId, muted -> viewModel.setAudioClipMuted(audioId, muted) },
                onZoomChange = { viewModel.setTimelineZoom(it) },
                onToggleClipSelection = { viewModel.toggleClipSelection(it) },
                isMultiSelect = isMultiSelect,
                selectedClipIds = selectedClipIds,
                beats = snapBeats,
                onUserInteraction = { viewModel.pausePlayback() },
                onAddAudioClick = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
                onAddTextClick = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
                onCoverClick = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Balanced spacer to lift timeline and transport controls slightly higher up (CapCut balance)
        Spacer(modifier = Modifier.weight(0.15f))

        // 5. Bottom Action Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121215))
                .navigationBarsPadding()
                .padding(top = 6.dp)
        ) {
            if (selectedClipId != null && !isMultiSelect) {
                ClipActionBar(
                    onBack = { viewModel.clearSelection() },
                    onSplit = splitAtPlayhead,
                    onVolume = { viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST) },
                    onAnimations = { viewModel.setActiveSheet(ActiveToolSheet.ANIMATIONS) },
                    onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                    onDelete = { viewModel.deleteSelectedClip() },
                    onSpeed = { viewModel.setActiveSheet(ActiveToolSheet.SPEED) },
                    onBeats = { viewModel.autoCutToBeat() },
                    onCrop = { viewModel.setActiveSheet(ActiveToolSheet.CROP) },
                    onDuplicate = { viewModel.duplicateSelectedClip() },
                    onReplace = {
                        replaceMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onOverlay = {
                        addOverlayLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                    onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) },
                    onAspectRatio = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                    onBackground = { viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND) },
                    onOpacity = { viewModel.setActiveSheet(ActiveToolSheet.OPACITY) },
                    onFreeze = { viewModel.freezeFrameAtPlayhead() },
                    onReverse = { viewModel.reverseSelectedClip() },
                    onExtractAudio = { viewModel.extractAudioFromSelectedClip() },
                    onMore = { viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP) },
                    onAiTools = { viewModel.setActiveSheet(ActiveToolSheet.AI_TOOLS) }
                )
            } else {
                CapCutBottomActionBar(
                    onEdit = {
                        if (selectedClipId != null) {
                            viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP)
                        } else {
                            Toast.makeText(context, "Select a clip to edit", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAudio = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
                    onText = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
                    onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                    onOverlay = {
                        addOverlayLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onCaptions = { viewModel.setActiveSheet(ActiveToolSheet.CAPTIONS) },
                    onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) },
                    onFilters = { viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY) },
                    onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                    onRatio = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                    onBackground = { viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND) },
                    onTools = { viewModel.setActiveSheet(ActiveToolSheet.TIMELINE_TOOLS) }
                )
            }
        }
        }
        } // end Column

        // Floating Overlay Tool Sheet (slides up at bottom without compressing the video canvas)
        if (activeSheet != ActiveToolSheet.NONE && activeSheet != ActiveToolSheet.EXPORT_MODAL) {
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
                        onImportCaptions = { importCaptionsLauncher.launch(arrayOf("*/*")) },
                        onExportSrt = {
                            val content = viewModel.exportSubtitles(toVtt = false)
                            if (content == null) {
                                Toast.makeText(context, "No captions to export yet", Toast.LENGTH_LONG).show()
                            } else {
                                shareSubtitleExport(context, project!!.id, content)
                            }
                        },
                        onExportVtt = {
                            val content = viewModel.exportSubtitles(toVtt = true)
                            if (content == null) {
                                Toast.makeText(context, "No captions to export yet", Toast.LENGTH_LONG).show()
                            } else {
                                shareSubtitleExport(context, project!!.id, content)
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
                            isMultiSelect = !isMultiSelect
                            if (!isMultiSelect) viewModel.clearSelection()
                        },
                        onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
                        onZoomChanged = { viewModel.setTimelineZoom(it) },
                        onToggleKeyframe = { viewModel.toggleKeyframeAtPlayhead() },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.BACKGROUND -> {
                    BackgroundSheet(
                        currentPaperStyle = project!!.paperStyle,
                        onSelectBackground = { viewModel.setPaperStyle(it) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.EXPORT_MODAL -> {}
                ActiveToolSheet.NONE -> {}
            }
        }

        if (activeSheet == ActiveToolSheet.EXPORT_MODAL) {
            ExportDialog(
                exportState = exportState,
                defaultResolution = project?.resolution ?: "1080P",
                defaultFps = project?.fps ?: 30,
                watermarkEnabled = watermarkEnabled,
                currentAspectRatio = project?.aspectRatio ?: com.example.model.AspectRatioType.RATIO_16_9,
                projectLengthMs = project?.totalDurationMs ?: 60_000L,
                onSelectAspectRatio = { viewModel.setAspectRatio(it) },
                onToggleWatermark = { viewModel.toggleWatermark(it) },
                onStartExport = { res, fps, _, bitrate ->
                    viewModel.startExport(res, fps, bitrate)
                },
                onCancelExport = { viewModel.cancelExport() },
                onDismiss = { viewModel.setActiveSheet(ActiveToolSheet.NONE) },
                onShareExport = { platform -> viewModel.shareExportMessage(platform) },
                onStartBackgroundExport = { res, fps, _, bitrate ->
                    startBackgroundExport(res, fps, bitrate)
                }
            )
        }
    }

    if (showFeatureSearch) {
        val searchEntries = listOf(
            EditorSearchEntry("Split clip", "Cut at the playhead", "cut scissors") { splitAtPlayhead() },
            EditorSearchEntry("Volume", "Gain, loudness, reduce noise", "audio sound loud") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Speed", "Retime, curves, audio pitch", "slow fast velocity") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.SPEED)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Crop", "Zoom and reframe a clip", "frame aspect") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.CROP)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Opacity", "Blend the clip over the canvas", "transparent alpha") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.OPACITY)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Animations", "Clip entry motion", "zoom swipe bounce spin") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.ANIMATIONS)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Audio studio", "Music, SFX, mic, text to speech", "sound track voice") {
                viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY)
            },
            EditorSearchEntry("Text", "Titles and subtitle editing", "caption title font") {
                viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
            },
            EditorSearchEntry("Captions", "Auto captions, import and export", "subtitle srt vtt") {
                viewModel.setActiveSheet(ActiveToolSheet.CAPTIONS)
            },
            EditorSearchEntry("Effects", "Timeline visual effects", "fx") {
                viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY)
            },
            EditorSearchEntry("Filters", "Colour looks", "grade lut") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Adjust", "Brightness, contrast, colour", "exposure saturation") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Aspect ratio", "Canvas size and paper style", "format 9:16 16:9") {
                viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO)
            },
            EditorSearchEntry("Background", "Canvas background colour", "colour backdrop") {
                viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND)
            },
            EditorSearchEntry("Stickers", "Emoji and sticker overlays", "emoji") {
                viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER)
            },
            EditorSearchEntry("Keyframes", "Animate transform and opacity", "animation") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.KEYFRAME)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Blend mode", "How the clip mixes with the track below", "multiply screen") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.BLEND_MODE)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Mask", "Shape masks and feathering", "vignette shape") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.MASK)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Chroma key", "Green-screen removal with eyedropper", "green screen") {
                if (selectedClipId != null) viewModel.setActiveSheet(ActiveToolSheet.CHROMA_KEY)
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Transition", "Join two clips with an effect", "dissolve fade") {
                val clipId = selectedClipId
                if (clipId == null) {
                    Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
                } else {
                    activeTransitionClipId = clipId
                    viewModel.setActiveSheet(ActiveToolSheet.TRANSITIONS_PICKER)
                }
            },
            EditorSearchEntry("Timeline tools", "Markers, beat sync, snap, zoom", "multi select") {
                viewModel.setActiveSheet(ActiveToolSheet.TIMELINE_TOOLS)
            },
            EditorSearchEntry("AI tools", "Cloud features and their status", "ai remix remove bg") {
                viewModel.setActiveSheet(ActiveToolSheet.AI_TOOLS)
            },
            EditorSearchEntry("Export", "Render the project", "save share mp4") {
                viewModel.setActiveSheet(ActiveToolSheet.EXPORT_MODAL)
            },
            EditorSearchEntry("Add media", "Import video or image clips", "import gallery") {
                addMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            EditorSearchEntry("Replace clip", "Swap the selected clip's source", "change media") {
                if (selectedClipId != null) {
                    replaceMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                } else {
                    Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
                }
            },
            EditorSearchEntry("Extract audio", "Copy clip audio onto an audio track", "detach sound") {
                if (selectedClipId != null) viewModel.extractAudioFromSelectedClip()
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Freeze frame", "Hold the current frame", "still pause") {
                if (selectedClipId != null) viewModel.freezeFrameAtPlayhead()
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Reverse", "Play the clip backwards", "backward") {
                if (selectedClipId != null) viewModel.reverseSelectedClip()
                else Toast.makeText(context, "Select a clip first", Toast.LENGTH_SHORT).show()
            },
            EditorSearchEntry("Beat sync", "Cut to detected beats", "music rhythm") {
                viewModel.autoCutToBeat()
            },
            EditorSearchEntry("Add marker", "Drop a labelled timeline marker", "flag") {
                viewModel.addMarker()
            }
        )
        FeatureSearchDialog(
            entries = searchEntries,
            onDismiss = { showFeatureSearch = false }
        )
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
}

// ==========================================
// Top Bar
// ==========================================
