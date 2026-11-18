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
import androidx.compose.foundation.layout.BoxScope
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

@Composable
internal fun BoxScope.EditorExportOverlays(
    viewModel: EditorViewModel,
    project: VideoProject?,
    activeSheet: ActiveToolSheet,
    exportSettings: ExportSettings,
    exportState: ExportUiState,
    watermarkEnabled: Boolean,
    isPreviewFullscreen: Boolean,
    showExportDropdown: Boolean,
    showExportOptionsBottomSheet: Boolean,
    onDismissExportOptions: () -> Unit,
    showQuitExportDialog: Boolean,
    onDismissQuitExport: () -> Unit
) {
        if (!isPreviewFullscreen) {
            AnimatedVisibility(
                visible = showExportDropdown,
                enter = expandVertically(
                    animationSpec = tween(280),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(280)),
                exit = shrinkVertically(
                    animationSpec = tween(220),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(220)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp)
                    .fillMaxWidth()
                    .zIndex(100f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .background(Color(0xFF141416))
                        .verticalScroll(rememberScrollState())
                ) {
                    ExportDropdownPanel(
                        settings = exportSettings,
                        watermarkEnabled = watermarkEnabled,
                        currentAspectRatio = project?.aspectRatio
                            ?: com.example.model.AspectRatioType.RATIO_16_9,
                        projectLengthMs = project?.totalDurationMs ?: 60_000L,
                        onUpdateSettings = { viewModel.updateExportSettings(it) },
                        onToggleWatermark = { viewModel.toggleWatermark(it) }
                    )
                }
            }
        }

        // Export Options Bottom Sheet Drawer (Screenshot 1: TikTok, Instagram, Save to Device cards)
        if (showExportOptionsBottomSheet) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(150f)
            ) {
                ExportOptionsBottomSheet(
                    watermarkEnabled = watermarkEnabled,
                    onToggleWatermark = { viewModel.toggleWatermark(it) },
                    boostLoudness = exportSettings.boostLoudness,
                    onToggleBoost = { enabled ->
                        viewModel.updateExportSettings(exportSettings.copy(boostLoudness = enabled))
                    },
                    onSaveToDevice = {
                        onDismissExportOptions()
                        viewModel.startSelectedExport()
                    },
                    onShareTikTok = {
                        onDismissExportOptions()
                        viewModel.startSelectedExport()
                    },
                    onShareInstagram = {
                        onDismissExportOptions()
                        viewModel.startSelectedExport()
                    },
                    onSaveAudioOnly = {
                        onDismissExportOptions()
                        viewModel.startAudioOnlyExport()
                    },
                    onDismiss = { onDismissExportOptions() }
                )
            }
        }

        if (activeSheet == ActiveToolSheet.EXPORT_MODAL ||
            exportState.isExporting ||
            exportState.isCompleted ||
            exportState.error != null
        ) {
            ExportDialog(
                exportState = exportState,
                settings = exportSettings,
                onUpdateSettings = { viewModel.updateExportSettings(it) },
                watermarkEnabled = watermarkEnabled,
                currentAspectRatio = project?.aspectRatio ?: com.example.model.AspectRatioType.RATIO_16_9,
                projectLengthMs = project?.totalDurationMs ?: 60_000L,
                onToggleWatermark = { viewModel.toggleWatermark(it) },
                onStartSelectedExport = { viewModel.startSelectedExport() },
                onCancelExport = { viewModel.cancelExport() },
                onDismiss = {
                    if (exportState.isExporting) {
                        // Keep the render alive; only close the sheet.
                        viewModel.setActiveSheet(ActiveToolSheet.NONE)
                    } else {
                        viewModel.dismissExport()
                    }
                },
                onShareExport = { platform -> viewModel.shareExportMessage(platform) }
            )
        }

        // Quit Exporting Confirmation Alert Dialog
        if (showQuitExportDialog) {
            QuitExportingDialog(
                onContinue = { onDismissQuitExport() },
                onQuit = {
                    onDismissQuitExport()
                    viewModel.cancelExport()
                }
            )
        }
}
