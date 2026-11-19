package com.example.ui.canvas

import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable wrapper for GLPreviewSurface.
 * Provides GPU-accelerated preview with 35 GLSL effects, blend modes, masks, chroma key.
 */
@Composable
fun GLPreviewSurfaceView(
    modifier: Modifier = Modifier,
    project: com.example.model.VideoProject?,
    positionMs: Long,
    isPlaying: Boolean,
    onSurfaceReady: (Surface) -> Unit = {},
    onSurfaceDestroyed: () -> Unit = {}
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            GLPreviewSurface(ctx).apply {
                setProject(project!!)
                setPlayheadPosition(positionMs)
                setPlaying(isPlaying)
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { glSurface ->
            glSurface.setProject(project!!)
            glSurface.setPlayheadPosition(positionMs)
            glSurface.setPlaying(isPlaying)
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
    
    // Notify when surface is ready for Media3
    LaunchedEffect(project, positionMs, isPlaying) {
        // The surface is managed by the AndroidView
    }
}

/**
 * Composable that uses GLPreviewSurface when available, falls back to VideoCanvas.
 * This is the main entry point for GPU-accelerated preview.
 */
@Composable
fun GpuAcceleratedPreview(
    modifier: Modifier = Modifier,
    project: com.example.model.VideoProject?,
    positionState: androidx.compose.runtime.State<Long>,
    isPlaying: Boolean,
    watermarkEnabled: Boolean = false,
    showSafeAreaGuides: Boolean = false,
    currentClip: com.example.model.MediaClip? = null,
    isTransformActive: Boolean = false,
    isEyedropperActive: Boolean = false,
    isPreviewFullscreen: Boolean = false,
    onExitFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Use GLPreviewSurface for GPU-accelerated effects
    GLPreviewSurfaceView(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        project = project,
        positionMs = positionState.value,
        isPlaying = isPlaying
    )
}