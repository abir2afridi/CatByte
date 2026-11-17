package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ScrubPreviewFrame
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.ui.canvas.VideoCanvas
import com.example.ui.components.LocalVideoPlayer
import com.example.viewmodel.EditorViewModel

/**
 * The editor's preview stage: the player surface, the composition preview and
 * the fullscreen exit button.
 */
@Composable
internal fun ColumnScope.EditorPreviewStage(
    viewModel: EditorViewModel,
    project: VideoProject,
    positionState: State<Long>,
    isPlaying: Boolean,
    watermarkEnabled: Boolean,
    showSafeAreaGuides: Boolean = false,
    scrubFrameState: State<ScrubPreviewFrame?>,
    currentClip: MediaClip?,
    isTransformActive: Boolean,
    isEyedropperActive: Boolean,
    isPreviewFullscreen: Boolean,
    onExitFullscreen: () -> Unit
) {
    val context = LocalContext.current

    // PlayerManager is process-scoped and outlives the activity: when the
    // app is backgrounded its surface dies and the player can end up
    // errored/idle, which used to make nothing play after reopening the
    // app (until "Clear all" killed the process). Rebuild it on resume.
    val playerManager = remember(context) {
        (context.applicationContext as? com.example.CatByteApplication)
            ?.container?.playerManager
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(playerManager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> playerManager?.onHostPause()
                Lifecycle.Event.ON_RESUME -> playerManager?.onHostResume()
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
        CompositionLocalProvider(LocalVideoPlayer provides previewPlayer) {
            VideoCanvas(
                project = project,
                currentPositionMs = positionState,
                isPlaying = isPlaying,
                onTogglePlay = { viewModel.togglePlayback() },
                watermarkEnabled = watermarkEnabled,
                showSafeAreaGuides = showSafeAreaGuides,
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
                onClick = onExitFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("editor_exit_fullscreen")
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bottom Floating Fullscreen Player Controls
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF26E5D7),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    val curSec = positionState.value / 1000
                    val totSec = project.totalDurationMs / 1000
                    androidx.compose.material3.Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totSec / 60, totSec % 60),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
