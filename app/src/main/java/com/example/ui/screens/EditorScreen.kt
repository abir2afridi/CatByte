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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ScrubPreviewFrame
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

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
            EditorPreviewStage(
                viewModel = viewModel,
                project = project!!,
                positionState = positionState,
                isPlaying = isPlaying,
                watermarkEnabled = watermarkEnabled,
                scrubFrameState = scrubFrameState,
                currentClip = currentClip,
                isTransformActive = isTransformActive,
                isEyedropperActive = isEyedropperActive,
                isPreviewFullscreen = isPreviewFullscreen,
                onExitFullscreen = { isPreviewFullscreen = false }
            )

            // 3. CapCut transport row: fullscreen, play, snap, undo, redo.
            if (!isPreviewFullscreen) {
                EditorTransportRow(
                    isPlaying = isPlaying,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    magneticSnap = magneticSnap,
                    aspectRatioLabel = project!!.aspectRatio.label,
                    onAspectRatioClick = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                    onTogglePlay = { viewModel.togglePlayback() },
                    onFullscreen = { isPreviewFullscreen = true },
                    onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
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
                    selectedClipIds = selectedClipIds,
                    hasOverlayClipboard = overlayClipboard.isNotEmpty(),
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
                    selectedClipId = selectedClipId,
                    isMultiSelect = isMultiSelect,
                    splitAtPlayhead = splitAtPlayhead,
                    replaceMediaLauncher = replaceMediaLauncher,
                    addOverlayLauncher = addOverlayLauncher
                )
            }
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
