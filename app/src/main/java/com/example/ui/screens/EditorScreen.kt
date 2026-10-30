package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VideoProject
import com.example.ui.components.AdjustmentsSheet
import com.example.ui.components.AudioSheet
import com.example.ui.components.CanvasRatioSheet
import com.example.ui.components.EditClipSheet
import com.example.ui.components.EffectsSheet
import com.example.ui.components.ExportDialog
import com.example.ui.components.FiltersSheet
import com.example.ui.components.MultiTrackTimeline
import com.example.ui.components.SpeedSheet
import com.example.ui.components.StickersSheet
import com.example.ui.components.TextEditorSheet
import com.example.ui.components.TransitionsPickerSheet
import com.example.ui.components.VideoCanvas
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    val project by viewModel.currentProject.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val selectedTextId by viewModel.selectedTextId.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val timelineZoom by viewModel.timelineZoom.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceRecordingDurationMs by viewModel.voiceRecordingDurationMs.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val magneticSnap by viewModel.magneticSnap.collectAsState()

    var activeTransitionClipId by remember { mutableStateOf<String?>(null) }

    // Intercept back button if any sheet is open
    BackHandler {
        if (activeSheet != ActiveToolSheet.NONE) {
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        } else {
            onBackToHome()
        }
    }

    // Media Picker for adding clips to project
    val addMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEachIndexed { idx, uri ->
            viewModel.addClipFromUri(uri.toString(), "Clip ${project?.clips?.size?.plus(idx + 1)}")
        }
    }

    val currentClip = project?.clips?.find { it.id == selectedClipId }
    val currentText = project?.textOverlays?.find { it.id == selectedTextId }

    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_active_project), color = colors.textPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Sketched Top Bar
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

        // 2. Sketched Video Preview Canvas
        val context = androidx.compose.ui.platform.LocalContext.current
        val videoPlayer = androidx.compose.runtime.remember(context) {
            (context.applicationContext as? com.example.CatByteApplication)
                ?.container?.playerManager?.exoPlayer
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.components.LocalVideoPlayer provides videoPlayer
            ) {
                VideoCanvas(
                    project = project!!,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.togglePlayback() },
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
            onQuickSplit = { viewModel.splitClipAtPlayhead() },
            onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
            onZoomChanged = { viewModel.setTimelineZoom(it) }
        )

        // 4. Sketched Multi-Track Timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
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
                modifier = Modifier.fillMaxSize()
            )
        }

        // 5. Active Tool Sheet / Bottom Bar
        if (activeSheet != ActiveToolSheet.NONE) {
            when (activeSheet) {
                ActiveToolSheet.EDIT_CLIP -> {
                    EditClipSheet(
                        clip = currentClip,
                        onSplit = { viewModel.splitClipAtPlayhead() },
                        onDelete = { viewModel.deleteSelectedClip() },
                        onDuplicate = { viewModel.duplicateSelectedClip() },
                        onRotate = { viewModel.rotateSelectedClip() },
                        onMirror = { viewModel.mirrorSelectedClip() },
                        onOpenSpeed = { viewModel.setActiveSheet(ActiveToolSheet.SPEED) },
                        onOpenAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.SPEED -> {
                    SpeedSheet(
                        currentSpeed = currentClip?.speed ?: 1.0f,
                        onSpeedSelected = { viewModel.setClipSpeed(it) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
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
                        onStartVoiceRecord = { viewModel.startVoiceRecording() },
                        onStopVoiceRecord = { viewModel.stopAndSaveVoiceRecording() },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
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
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.EFFECTS_LIBRARY -> {
                    EffectsSheet(
                        onAddEffect = { fx ->
                            viewModel.addTimelineEffect(fx)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.FILTERS_LIBRARY -> {
                    FiltersSheet(
                        activeFilter = currentClip?.filter ?: com.example.model.VideoFilter.NONE,
                        onSelectFilter = { filter ->
                            viewModel.setClipFilter(filter)
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.ADJUSTMENTS -> {
                    AdjustmentsSheet(
                        clip = currentClip,
                        onUpdateAdjustments = { b, c, s, v ->
                            viewModel.updateClipAdjustments(b, c, s, v)
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.CANVAS_RATIO -> {
                    CanvasRatioSheet(
                        activeRatio = project!!.aspectRatio,
                        currentPaperStyle = project!!.paperStyle,
                        onSelectRatio = { viewModel.setAspectRatio(it) },
                        onSelectPaperStyle = { viewModel.setPaperStyle(it) },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.STICKERS_PICKER -> {
                    StickersSheet(
                        onAddSticker = { emoji ->
                            viewModel.addSticker(emoji)
                            viewModel.setActiveSheet(ActiveToolSheet.NONE)
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.TRANSITIONS_PICKER -> {
                    TransitionsPickerSheet(
                        clipId = activeTransitionClipId ?: "",
                        onSelectTransition = { trans ->
                            viewModel.setClipTransition(trans)
                        },
                        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
                    )
                }
                ActiveToolSheet.EXPORT_MODAL -> {
                    ExportDialog(
                        exportState = exportState,
                        onStartExport = { res, fps -> viewModel.startExport(res, fps) },
                        onDismiss = { viewModel.dismissExport() }
                    )
                }
                ActiveToolSheet.NONE -> {}
            }
        } else {
            // Sketched Bottom Action Toolbar
            CapCutBottomActionBar(
                onSplit = { viewModel.splitClipAtPlayhead() },
                onAudio = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
                onText = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
                onOverlay = {
                    addMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                onFilters = { viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY) },
                onFormat = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) }
            )
        }
    }
}

// ==========================================
// Top Bar
// ==========================================

@Composable
private fun EditorTopBar(
    project: VideoProject,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenFormat: () -> Unit,
    onExport: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.surface)
            .border(1.2.dp, colors.borderSubtle)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            // Ratio Tag Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .border(1.2.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable { onOpenFormat() }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${project.aspectRatio.label} ✏️",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Undo / Redo
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (canUndo) colors.surfaceElevated else colors.surfaceElevated.copy(alpha = 0.45f))
                    .border(1.2.dp, if (canUndo) colors.border else colors.borderSubtle.copy(alpha = 0.45f), CircleShape)
                    .testTag("action_undo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) colors.textPrimary else colors.textTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (canRedo) colors.surfaceElevated else colors.surfaceElevated.copy(alpha = 0.45f))
                    .border(1.2.dp, if (canRedo) colors.border else colors.borderSubtle.copy(alpha = 0.45f), CircleShape)
                    .testTag("action_redo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) colors.textPrimary else colors.textTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Settings & Export Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.2.dp, colors.border, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            val exportContentColor = if (colors.isLight) colors.surface else Color.Black
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("editor_export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Export",
                    tint = exportContentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Export",
                    color = exportContentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ==========================================
// Playback Strip & Quick Split
// ==========================================

@Composable
private fun PlaybackControlsStrip(
    isPlaying: Boolean,
    zoom: Float,
    magneticSnap: Boolean,
    onTogglePlay: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onQuickSplit: () -> Unit,
    onToggleMagneticSnap: () -> Unit,
    onZoomChanged: (Float) -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(colors.surface)
            .border(1.2.dp, colors.borderSubtle)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step rewind
        IconButton(onClick = onStepBackward, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Step Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center Play / Pause Button with sketch pencil border and hatching
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .pencilHatching(color = colors.accentCyan.copy(alpha = 0.08f), spacing = 5.dp)
                .sketchPencilBorder(strokeWidth = 1.8.dp, color = colors.border, cornerRadius = 21.dp)
                .clickable { onTogglePlay() }
                .testTag("editor_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Step forward
        IconButton(onClick = onStepForward, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Step Forward",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Quick Split Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .sketchPencilBorder(strokeWidth = 1.3.dp, color = colors.border, cornerRadius = 8.dp)
                .clickable { onQuickSplit() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Quick Split",
                    tint = colors.accentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Magnetic Snap Toggle Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (magneticSnap) colors.surfaceElevated else colors.surface)
                .sketchPencilBorder(
                    strokeWidth = 1.3.dp,
                    color = if (magneticSnap) colors.border else colors.borderSubtle,
                    cornerRadius = 8.dp
                )
                .clickable { onToggleMagneticSnap() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("editor_toggle_snap")
        ) {
            Text(
                text = if (magneticSnap) "🧲 SNAP" else "🧲 OFF",
                color = if (magneticSnap) colors.textPrimary else colors.textTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Timeline Zoom (+ / -)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = { onZoomChanged(zoom - 0.25f) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = colors.textTertiary, modifier = Modifier.size(17.dp))
            }
            Text(
                text = "${String.format("%.1f", zoom)}x",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = { onZoomChanged(zoom + 0.25f) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = colors.textTertiary, modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ==========================================
// Bottom Action Bar
// ==========================================

@Composable
private fun CapCutBottomActionBar(
    onSplit: () -> Unit,
    onAudio: () -> Unit,
    onText: () -> Unit,
    onOverlay: () -> Unit,
    onEffects: () -> Unit,
    onFilters: () -> Unit,
    onFormat: () -> Unit,
    onAdjust: () -> Unit,
    onStickers: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(colors.surface)
            .border(1.2.dp, colors.borderSubtle)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomActionItem(Icons.Default.ContentCut, "Edit", colors.textPrimary, onSplit)
        BottomActionItem(Icons.Default.Audiotrack, "Audio", colors.accentPurple, onAudio)
        BottomActionItem(Icons.Default.TextFields, "Text", colors.accentYellow, onText)
        BottomActionItem(Icons.Default.Layers, "Overlay", colors.textPrimary, onOverlay)
        BottomActionItem(Icons.Default.AutoAwesome, "Effects", colors.accentGreen, onEffects)
        BottomActionItem(Icons.Default.Filter, "Filters", colors.accentCyan, onFilters)
        BottomActionItem(Icons.Default.AspectRatio, "Format", colors.textPrimary, onFormat)
        BottomActionItem(Icons.Default.Tune, "Adjust", colors.accentCyan, onAdjust)
        BottomActionItem(Icons.Default.Mood, "Stickers", colors.accentPink, onStickers)
    }
}

@Composable
private fun BottomActionItem(
    icon: ImageVector,
    label: String,
    tintColor: Color,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("bottom_action_${label.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .pencilHatching(color = tintColor.copy(alpha = 0.08f), spacing = 4.dp)
                .sketchPencilBorder(strokeWidth = 1.3.dp, color = colors.border, cornerRadius = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

