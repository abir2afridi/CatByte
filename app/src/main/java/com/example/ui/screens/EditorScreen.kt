package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.MediaAvailability
import com.example.data.ScrubPreviewFrame
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.ui.components.MissingMediaBanner
import com.example.ui.components.MissingMediaSheet

/**
 * The editor screen.
 *
 * This file owns the screen state (what is selected, which sheet is open,
 * the export overlays) and the layout skeleton. The heavy parts live in their
 * own files:
 *  - `EditorPreviewStage.kt`: the player surface and the composition preview.
 *  - `EditorTimelineStage.kt`: the multi-select bar and the multi-track
 *    timeline.
 *  - `EditorBottomActionStage.kt`: the clip action bar / bottom action bar.
 *  - `EditorShortcutHandler.kt`: the keyboard shortcuts.
 *  - `EditorToolSheetHost.kt`: the bottom tool sheets.
 *  - `EditorExportOverlays.kt`: the export dropdown, options and progress.
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = com.example.ui.theme.PencilTheme.colors
    val context = LocalContext.current
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
    val clipboardCount by viewModel.clipboardCount.collectAsState()
    val selectedTextId by viewModel.selectedTextId.collectAsState()
    val selectedAudioId by viewModel.selectedAudioId.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val timelineZoom by viewModel.timelineZoom.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceRecordingDurationMs by viewModel.voiceRecordingDurationMs.collectAsState()
    val voiceRecordingLevel by viewModel.voiceRecordingLevel.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val magneticSnap by viewModel.magneticSnap.collectAsState()
    val watermarkEnabled by viewModel.watermarkEnabled.collectAsState()
    val showSafeAreaGuides by viewModel.showSafeAreaGuides.collectAsState()
    val snapBeats by viewModel.snapBeats.collectAsState()
    val exportSettings by viewModel.exportSettings.collectAsState()

    var activeTransitionClipId by remember { mutableStateOf<String?>(null) }
    var showTtsDialog by remember { mutableStateOf(false) }
    var ttsInputText by remember { mutableStateOf("") }
    var isMultiSelect by remember { mutableStateOf(false) }
    var showFeatureSearch by remember { mutableStateOf(false) }
    var showMissingMediaSheet by remember { mutableStateOf(false) }
    var isPreviewFullscreen by remember { mutableStateOf(false) }
    var showExportDropdown by remember { mutableStateOf(false) }
    var showExportOptionsBottomSheet by remember { mutableStateOf(false) }
    var showQuitExportDialog by remember { mutableStateOf(false) }
    // Which tab of the Audio & Foley studio the bottom navigation opens.
    var audioSheetTab by rememberSaveable { mutableIntStateOf(0) }

    // The studio opens on the tab of the entry point that opened it (Sounds,
    // Sound FX, Record). Every other entry point starts on the first tab.
    LaunchedEffect(activeSheet) {
        if (activeSheet != ActiveToolSheet.AUDIO_LIBRARY &&
            activeSheet != ActiveToolSheet.VOICE_RECORDER
        ) {
            audioSheetTab = 0
        }
    }

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
            androidx.compose.material3.Text(stringResource(R.string.no_active_project), color = colors.textPrimary)
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

    val scanMissing by viewModel.missingMedia.collectAsState()
    // Only keep entries that are still part of this project, so deleting a
    // broken clip removes its warning immediately.
    val projectMediaIds = remember(project) {
        project?.let { MediaAvailability.mediaRefs(it).map { ref -> ref.id }.toSet() }
            ?: emptySet()
    }
    val visibleMissing = remember(scanMissing, projectMediaIds) {
        scanMissing.filter { it.id in projectMediaIds }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .statusBarsPadding()
            .editorKeyboardShortcuts(
                focusRequester = shortcutFocus,
                viewModel = viewModel,
                timelineZoom = timelineZoom,
                positionState = positionState,
                splitAtPlayhead = splitAtPlayhead
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar
            if (!isPreviewFullscreen) {
                EditorTopBar(
                    project = project!!,
                    exportOpen = showExportDropdown,
                    exportTab = exportSettings.tab,
                    onBack = onBackToHome,
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
                    },
                    onRenameProject = { newName ->
                        viewModel.renameProject(project!!.id, newName)
                    }
                )
            }

            // 2. Missing-media warning (relink flow)
            if (!isPreviewFullscreen && visibleMissing.isNotEmpty()) {
                MissingMediaBanner(
                    missing = visibleMissing,
                    onOpen = {
                        viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        showMissingMediaSheet = true
                    }
                )
            }

            // 3. Video Preview Canvas
            EditorPreviewStage(
                viewModel = viewModel,
                project = project!!,
                positionState = positionState,
                isPlaying = isPlaying,
                watermarkEnabled = watermarkEnabled,
                showSafeAreaGuides = showSafeAreaGuides,
                scrubFrameState = scrubFrameState,
                currentClip = currentClip,
                isTransformActive = isTransformActive,
                isEyedropperActive = isEyedropperActive,
                isPreviewFullscreen = isPreviewFullscreen,
                onExitFullscreen = { isPreviewFullscreen = false }
            )

            // 3. CapCut transport row: play, snap, undo, redo, split, timecode.
            if (!isPreviewFullscreen) {
                EditorTransportRow(
                    isPlaying = isPlaying,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    magneticSnap = magneticSnap,
                    onTogglePlay = { viewModel.togglePlayback() },
                    onSplit = splitAtPlayhead,
                    onStepBackward = {
                        viewModel.seekTo((positionState.value - 33L).coerceAtLeast(0L))
                    },
                    onStepForward = {
                        viewModel.seekTo((positionState.value + 33L).coerceAtMost(project!!.totalDurationMs))
                    },
                    onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
                )

                // The fullscreen button, the aspect ratio chip and the live
                // timecode sit in their own strip below the bar: inside the
                // bar they ran into the split button on a narrow phone.
                EditorTransportInfoRow(
                    aspectRatioLabel = project!!.aspectRatio.label,
                    currentPositionMs = positionState.value,
                    totalDurationMs = project!!.totalDurationMs,
                    markers = project!!.markers,
                    onAspectRatioClick = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                    onFullscreen = { isPreviewFullscreen = true },
                    onSeek = { viewModel.seekTo(it) },
                    onJumpToMarker = { markerId -> viewModel.seekToMarker(markerId) },
                    onPreviousMarker = { viewModel.jumpToMarker(-1) },
                    onNextMarker = { viewModel.jumpToMarker(1) }
                )
            }

            if (!isPreviewFullscreen) {
                // 4. Multi-select controls + multi-track timeline
                EditorTimelineStage(
                    viewModel = viewModel,
                    project = project!!,
                    positionState = positionState,
                    isPlaying = isPlaying,
                    selectedClipId = selectedClipId,
                    selectedTextId = selectedTextId,
                    selectedAudioId = selectedAudioId,
                    selectedClipIds = selectedClipIds,
                    hasOverlayClipboard = clipboardCount > 0 || overlayClipboard.isNotEmpty(),
                    timelineZoom = timelineZoom,
                    magneticSnap = magneticSnap,
                    snapBeats = snapBeats,
                    isMultiSelect = isMultiSelect,
                    onMultiSelectChange = { isMultiSelect = it },
                    addMediaLauncher = addMediaLauncher,
                    onTransitionClick = { clipId ->
                        activeTransitionClipId = clipId
                        viewModel.setActiveSheet(ActiveToolSheet.TRANSITIONS_PICKER)
                    }
                )

                // 5. Bottom Action Bar
                EditorBottomActionStage(
                    viewModel = viewModel,
                    project = project!!,
                    selectedClipId = selectedClipId,
                    isMultiSelect = isMultiSelect,
                    splitAtPlayhead = splitAtPlayhead,
                    replaceMediaLauncher = replaceMediaLauncher,
                    addOverlayLauncher = addOverlayLauncher,
                    onOpenAudioSheet = { tab ->
                        audioSheetTab = tab
                        viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY)
                    },
                    onShowTts = { showTtsDialog = true },
                    onImportCaptions = { importCaptionsLauncher.launch(arrayOf("*/*")) }
                )
            }
        }

        // Backdrop Scrim (tap outside tool sheet to dismiss)
        if (activeSheet != ActiveToolSheet.NONE && activeSheet != ActiveToolSheet.EXPORT_MODAL && !isPreviewFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    .zIndex(110f)
            )
        }

        // Floating overlay tool sheets, one feature per sheet file.
        EditorToolSheetHost(
            viewModel = viewModel,
            project = project!!,
            activeSheet = activeSheet,
            currentClip = currentClip,
            currentText = currentText,
            captionExportCount = captionExportCount,
            selectedClipId = selectedClipId,
            selectedAudioId = selectedAudioId,
            isEyedropperActive = isEyedropperActive,
            isMultiSelect = isMultiSelect,
            onMultiSelectChange = { isMultiSelect = it },
            activeTransitionClipId = activeTransitionClipId,
            magneticSnap = magneticSnap,
            timelineZoom = timelineZoom,
            splitAtPlayhead = splitAtPlayhead,
            isRecordingVoice = isRecordingVoice,
            voiceRecordingDurationMs = voiceRecordingDurationMs,
            voiceRecordingLevel = voiceRecordingLevel,
            audioSheetTab = audioSheetTab,
            onShowTts = { showTtsDialog = true },
            onLaunchCaptionImport = { importCaptionsLauncher.launch(arrayOf("*/*")) },
            onOpenSettings = onOpenSettings
        )

        // Export surfaces: dropdown, options sheet, progress/result and quit guard.
        EditorExportOverlays(
            viewModel = viewModel,
            project = project!!,
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

        if (showMissingMediaSheet) {
            // The sheet closes itself once every file resolves.
            LaunchedEffect(visibleMissing.isEmpty()) {
                if (visibleMissing.isEmpty()) showMissingMediaSheet = false
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                MissingMediaSheet(
                    missing = visibleMissing,
                    onRelink = { clipId, uri -> viewModel.relinkClipMedia(clipId, uri) },
                    onClose = { showMissingMediaSheet = false }
                )
            }
        }

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
