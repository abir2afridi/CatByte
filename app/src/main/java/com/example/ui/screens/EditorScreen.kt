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
import com.example.ui.components.TransitionsPickerSheet
import com.example.ui.canvas.VideoCanvas
import com.example.ui.theme.PencilTheme
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.data.MediaDurationFallback
import com.example.data.ScrubPreviewFrame

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
    // The playhead changes ~60x/s while playing. Collecting it at the screen
    // level used to recompose the WHOLE editor (canvas + timeline + toolbars)
    // on every tick, which made the timeline scroll feel laggy. The position
    // is handed down as a State instead, so only the few components that
    // really need the playhead recompose.
    val positionState = remember(viewModel) { mutableStateOf(viewModel.currentPositionMs.value) }
    LaunchedEffect(viewModel) {
        viewModel.currentPositionMs.collect { positionState.value = it }
    }
    // Live scrub frames arrive a few times per second while the timeline moves.
    // Handing them down as a State (like the playhead) keeps the recomposition
    // inside VideoCanvas instead of re-running the whole editor screen.
    val scrubFrameState = remember(viewModel) { mutableStateOf<ScrubPreviewFrame?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.scrubPreviewFrame.collect { scrubFrameState.value = it }
    }
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
    val exportSettings by viewModel.exportSettings.collectAsState()

    var activeTransitionClipId by remember { mutableStateOf<String?>(null) }
    var showTtsDialog by remember { mutableStateOf(false) }
    var ttsInputText by remember { mutableStateOf("") }
    var isMultiSelect by remember { mutableStateOf(false) }
    var showFeatureSearch by remember { mutableStateOf(false) }
    var isPreviewFullscreen by remember { mutableStateOf(false) }
    var showExportDropdown by remember { mutableStateOf(false) }
    var showExportOptionsBottomSheet by remember { mutableStateOf(false) }
    var showQuitExportDialog by remember { mutableStateOf(false) }

    // Intercept back button if any sheet or export is open
    BackHandler {
        if (showQuitExportDialog) {
            showQuitExportDialog = false
        } else if (exportState.isExporting) {
            showQuitExportDialog = true
        } else if (isPreviewFullscreen) {
            isPreviewFullscreen = false
        } else if (showExportOptionsBottomSheet) {
            showExportOptionsBottomSheet = false
        } else if (showExportDropdown) {
            // The AI UHD dropdown is the top-most overlay: Back closes it
            // before anything else.
            showExportDropdown = false
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
                        viewModel.seekTo((positionState.value - FRAME_STEP_MS).coerceAtLeast(0L))
                    Shortcuts.Command.NEXT_FRAME -> viewModel.seekTo(positionState.value + FRAME_STEP_MS)
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
                exportOpen = showExportDropdown,
                exportTab = exportSettings.tab,
                onBack = onBackToHome,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleExportDropdown = {
                    val opening = !showExportDropdown
                    showExportDropdown = opening
                    if (opening && activeSheet != ActiveToolSheet.NONE) {
                        // The export dropdown is a top overlay; close any open
                        // bottom tool sheet so the two never overlap.
                        viewModel.setActiveSheet(ActiveToolSheet.NONE)
                    }
                },
                onExportTabSelected = { tab ->
                    viewModel.updateExportSettings(exportSettings.copy(tab = tab))
                    showExportDropdown = true
                    if (activeSheet != ActiveToolSheet.NONE) {
                        viewModel.setActiveSheet(ActiveToolSheet.NONE)
                    }
                },
                // ONE click: export with the settings pre-selected in the
                // AI UHD dropdown - no extra dialog.
                onStartExport = {
                    showExportDropdown = false
                    showExportOptionsBottomSheet = true
                },
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

        // PlayerManager is process-scoped and outlives the activity: when the
        // app is backgrounded its surface dies and the player can end up
        // errored/idle, which used to make nothing play after reopening the
        // app (until "Clear all" killed the process). Rebuild it on resume.
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(playerManager, lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> playerManager?.onHostPause()
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> playerManager?.onHostResume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                    currentPositionMs = positionState,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.togglePlayback() },
                    watermarkEnabled = watermarkEnabled,
                    scrubFrame = scrubFrameState,
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
                aspectRatioLabel = project?.aspectRatio?.label ?: "16:9",
                onAspectRatioClick = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
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

        // 4. Multi-Track Timeline (integrated CapCut left gutter and timecode)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp, max = 220.dp)
        ) {
            MultiTrackTimeline(
                project = project!!,
                currentPositionMs = positionState,
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
                onClearSelection = { viewModel.clearSelection() },
                onAddAudioClick = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
                onAddTextClick = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
                onCoverClick = { viewModel.setActiveSheet(ActiveToolSheet.COVER_EDITOR) },
                modifier = Modifier.fillMaxSize()
            )
        }

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

        // Floating overlay tool sheets, one feature per sheet file.
        EditorToolSheetHost(
            viewModel = viewModel,
            project = project!!,
            activeSheet = activeSheet,
            currentClip = currentClip,
            currentText = currentText,
            captionExportCount = captionExportCount,
            selectedClipId = selectedClipId,
            isEyedropperActive = isEyedropperActive,
            isMultiSelect = isMultiSelect,
            onMultiSelectChange = { isMultiSelect = it },
            activeTransitionClipId = activeTransitionClipId,
            magneticSnap = magneticSnap,
            timelineZoom = timelineZoom,
            splitAtPlayhead = splitAtPlayhead,
            isRecordingVoice = isRecordingVoice,
            voiceRecordingDurationMs = voiceRecordingDurationMs,
            onShowTts = { showTtsDialog = true },
            onLaunchCaptionImport = { importCaptionsLauncher.launch(arrayOf("*/*")) }
        )

        // Export surfaces: dropdown, options sheet, progress/result and quit guard.
        EditorExportOverlays(
            viewModel = viewModel,
            project = project,
            activeSheet = activeSheet,
            exportSettings = exportSettings,
            exportState = exportState,
            watermarkEnabled = watermarkEnabled,
            isPreviewFullscreen = isPreviewFullscreen,
            showExportDropdown = showExportDropdown,
            showExportOptionsBottomSheet = showExportOptionsBottomSheet,
            onDismissExportOptions = { showExportOptionsBottomSheet = false },
            showQuitExportDialog = showQuitExportDialog,
            onDismissQuitExport = { showQuitExportDialog = false }
        )

    if (showFeatureSearch) {
        EditorFeatureSearch(
            viewModel = viewModel,
            selectedClipId = selectedClipId,
            splitAtPlayhead = splitAtPlayhead,
            onAddMedia = {
                addMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onReplaceMedia = {
                replaceMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onPickTransition = { clipId ->
                activeTransitionClipId = clipId
                viewModel.setActiveSheet(ActiveToolSheet.TRANSITIONS_PICKER)
            },
            onDismiss = { showFeatureSearch = false }
        )
    }

    if (showTtsDialog) {
        EditorTtsDialog(
            text = ttsInputText,
            onTextChange = { ttsInputText = it },
            onDismiss = { showTtsDialog = false },
            onGenerate = {
                if (ttsInputText.isNotBlank()) {
                    viewModel.startTtsVoiceover(ttsInputText)
                    ttsInputText = ""
                    showTtsDialog = false
                }
            }
        )
    }
}
}

// ==========================================
// Top Bar
// ==========================================