package com.example.ui.components

import android.view.SurfaceView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.R
import com.example.data.KeyframeInterpolator
import com.example.model.AspectRatioType
import com.example.model.BlendMode
import com.example.model.EffectType
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.TextAnimationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.model.TextFontStyle
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.ui.theme.DraftingCropCorners
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.WashiTapeCorner
import com.example.ui.theme.sketchPencilBorder

/** Provides the shared ExoPlayer to the video canvas for real playback. */
val LocalVideoPlayer = compositionLocalOf<androidx.media3.common.Player?> { null }

@Composable
fun VideoCanvas(
    project: VideoProject,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    watermarkEnabled: Boolean = false,
    /**
     * Composition preview player. When [compositionPreview] is true this single
     * surface already contains the composited video, PIP layers, text, stickers,
     * effects and watermark, so the Compose fallback layers are skipped.
     */
    player: Player? = null,
    compositionPreview: Boolean = false,
    previewError: String? = null,
    previewWarnings: List<String> = emptyList(),
    selectedClip: MediaClip? = null,
    isTransformInteractive: Boolean = false,
    onTransformClip: ((scaleMultiplier: Float, rotationDelta: Float, panDeltaX: Float, panDeltaY: Float) -> Unit)? = null,
    isEyedropperActive: Boolean = false,
    onSampleColorAt: ((normX: Float, normY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val ratio = project.aspectRatio
    val targetAspect = ratio.ratioWidth / ratio.ratioHeight

    // Active clip resolution
    var accumulatedMs = 0L
    var activeClip: MediaClip? = null
    var clipLocalTimeMs = 0L

    for (clip in project.clips) {
        val clipEnd = accumulatedMs + clip.durationMs
        if (currentPositionMs in accumulatedMs..clipEnd) {
            activeClip = clip
            clipLocalTimeMs = currentPositionMs - accumulatedMs
            break
        }
        accumulatedMs = clipEnd
    }
    if (activeClip == null && project.clips.isNotEmpty()) {
        activeClip = project.clips.lastOrNull()
        clipLocalTimeMs = activeClip?.durationMs ?: 0L
    }

    // Active overlays
    val activeTexts = project.textOverlays.filter {
        currentPositionMs >= it.startTimelineMs && currentPositionMs <= (it.startTimelineMs + it.durationMs)
    }
    val activeStickers = project.stickers.filter {
        currentPositionMs >= it.startTimelineMs && currentPositionMs <= (it.startTimelineMs + it.durationMs)
    }
    val activeEffects = project.effects.filter {
        currentPositionMs >= it.startTimelineMs && currentPositionMs <= (it.startTimelineMs + it.durationMs)
    }

    // Pulse animation - only runs when playing (coroutine-based to avoid divide-by-zero crash)
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var reticlePos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val pulseAlphaState: androidx.compose.runtime.MutableState<Float> = remember { mutableStateOf(0.85f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            scope.launch {
                var growing = true
                while (isPlaying) {
                    val target = if (growing) 1.0f else 0.85f
                    growing = !growing
                    // Simple linear interpolation over 600ms
                    val start = pulseAlphaState.value
                    val duration = 600L
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < duration) {
                        if (!isPlaying) break
                        val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
                        pulseAlphaState.value = start + (target - start) * progress
                        delay(16) // ~60fps
                    }
                    if (!isPlaying) break
                    pulseAlphaState.value = target
                }
            }
        } else {
            pulseAlphaState.value = 0.85f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Outer Canvas Frame with sketched/clean border
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(targetAspect)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 14.dp, isPencilStyle = colors.isPencilSketch)
                .onSizeChanged { canvasSize = it }
                .pointerInput(isEyedropperActive) {
                    if (isEyedropperActive) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val nx = (offset.x / canvasSize.width.toFloat()).coerceIn(0f, 1f)
                                val ny = (offset.y / canvasSize.height.toFloat()).coerceIn(0f, 1f)
                                reticlePos = Offset(nx, ny)
                                onSampleColorAt?.invoke(nx, ny)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val nx = (change.position.x / canvasSize.width.toFloat()).coerceIn(0f, 1f)
                                val ny = (change.position.y / canvasSize.height.toFloat()).coerceIn(0f, 1f)
                                reticlePos = Offset(nx, ny)
                                onSampleColorAt?.invoke(nx, ny)
                            }
                        )
                    }
                }
                .pointerInput(isEyedropperActive, isTransformInteractive, selectedClip?.id) {
                    if (isEyedropperActive) {
                        detectTapGestures { offset ->
                            val nx = (offset.x / canvasSize.width.toFloat()).coerceIn(0f, 1f)
                            val ny = (offset.y / canvasSize.height.toFloat()).coerceIn(0f, 1f)
                            reticlePos = Offset(nx, ny)
                            onSampleColorAt?.invoke(nx, ny)
                        }
                    } else if (isTransformInteractive && selectedClip != null) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            val panDeltaX = pan.x / canvasSize.width.toFloat()
                            val panDeltaY = pan.y / canvasSize.height.toFloat()
                            onTransformClip?.invoke(zoom, rotation, panDeltaX, panDeltaY)
                        }
                    } else {
                        detectTapGestures { onTogglePlay() }
                    }
                }
                .testTag("video_canvas_preview")
                .width(720.dp),
            contentAlignment = Alignment.Center
        ) {
            // Drafting Crop Marks in 4 corners
            DraftingCropCorners(
                modifier = Modifier
                    .matchParentSize()
                    .padding(6.dp),
                strokeColor = colors.accentCyan.copy(alpha = 0.7f),
                lineLength = 14.dp,
                strokeWidth = 1.6.dp
            )

            if (colors.isPencilSketch) {
                // Drafting Washi Tape mounting strips on top corners for Pencil theme
                WashiTapeCorner(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-2).dp),
                    rotationDegrees = -15f,
                    widthDp = 44.dp,
                    heightDp = 14.dp,
                    tapeLabel = "PIN"
                )
                WashiTapeCorner(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp),
                    rotationDegrees = 15f,
                    widthDp = 44.dp,
                    heightDp = 14.dp,
                    tapeLabel = "DRAFT"
                )
            }
            if (player != null) {
                // One surface: composition preview when available, otherwise the
                // ExoPlayer fallback playlist. The fallback must still render a
                // real video surface, or playback advances the audio/timeline
                // while the picture stays frozen on a generated still.
                key(player) {
                    PlayerPreviewSurface(
                        player = player,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // The fallback player renders plain video, so draw title/text
                // overlays ourselves (composition burns them in).
                if (!compositionPreview) {
                    // Drafting overlay for transform handles (existing code below)
                }
            } else if (activeClip != null) {
                // No player at all: show the generated preview frame only.
                RenderClipVisual(
                    clip = activeClip,
                    activeEffects = activeEffects,
                    pulseAlpha = pulseAlphaState.value,
                    currentClipLocalMs = clipLocalTimeMs
                )

                // Transition effect
                if (activeClip.transition != TransitionType.NONE && clipLocalTimeMs < activeClip.transitionDurationMs) {
                    RenderTransitionEffect(
                        transition = activeClip.transition,
                        progress = clipLocalTimeMs.toFloat() / activeClip.transitionDurationMs
                    )
                }
            } else {
                // Empty state inside canvas
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_media_clips),
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tap_plus_add_footage),
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }
            }

            // Text Overlays (fallback renderer only - composition burns them in)
            if (!compositionPreview) {
            activeTexts.forEach { textOverlay ->
                val displayText = if (textOverlay.animation == TextAnimationType.TYPEWRITER) {
                    val progress = ((currentPositionMs - textOverlay.startTimelineMs).toFloat() / textOverlay.durationMs).coerceIn(0f, 1f)
                    val charCount = (textOverlay.text.length * progress * 2).toInt().coerceIn(1, textOverlay.text.length)
                    textOverlay.text.take(charCount)
                } else {
                    textOverlay.text
                }

                val textScale = if (textOverlay.animation == TextAnimationType.POP_UP) {
                    val p = ((currentPositionMs - textOverlay.startTimelineMs).toFloat() / 300f).coerceIn(0f, 1f)
                    0.6f + (0.4f * p)
                } else if (textOverlay.animation == TextAnimationType.NEON_PULSE) {
                    pulseAlphaState.value
                } else {
                    1.0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = when {
                        textOverlay.posY < 0.4f -> Alignment.TopCenter
                        textOverlay.posY > 0.6f -> Alignment.BottomCenter
                        else -> Alignment.Center
                    }
                ) {
                    Text(
                        text = displayText,
                        color = Color(textOverlay.color),
                        fontSize = when (textOverlay.fontStyle) {
                            TextFontStyle.CAPCUT_BOLD -> 23.sp
                            TextFontStyle.NEON_GLOW -> 21.sp
                            TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> 20.sp
                            TextFontStyle.SUBTITLE -> 16.sp
                            else -> 17.sp
                        },
                        fontWeight = when (textOverlay.fontStyle) {
                            TextFontStyle.CAPCUT_BOLD -> FontWeight.Black
                            TextFontStyle.CINEMATIC_SERIF -> FontWeight.SemiBold
                            else -> FontWeight.Bold
                        },
                        fontFamily = when (textOverlay.fontStyle) {
                            TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> FontFamily.Serif
                            else -> FontFamily.SansSerif
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .scale(textScale * textOverlay.scale)
                            .background(
                                color = if (textOverlay.backgroundColor != null) Color(textOverlay.backgroundColor) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Sticker Overlays
            activeStickers.forEach { sticker ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sticker.symbol,
                        fontSize = (44 * sticker.scale).sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Watermark overlay
            if (watermarkEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✏️ CatByte",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            } // end fallback-only 2D layers

            // Centered Play/Pause HUD when paused - Sketched Paper & Graphite Button
            // CapCut-style Selection Bounding Box & Transform Handles
            if (isTransformInteractive && selectedClip != null && !isEyedropperActive) {
                val clipScale = selectedClip.scale
                val clipRotation = selectedClip.rotation
                val clipPosX = selectedClip.posX
                val clipPosY = selectedClip.posY

                Box(modifier = Modifier.fillMaxSize()) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val boxWidthDp = with(density) { (canvasSize.width * 0.80f * clipScale).toDp() }
                    val boxHeightDp = with(density) { (canvasSize.height * 0.80f * clipScale).toDp() }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                val cx = clipPosX * canvasSize.width
                                val cy = clipPosY * canvasSize.height
                                val halfW = (canvasSize.width * 0.40f * clipScale)
                                val halfH = (canvasSize.height * 0.40f * clipScale)
                                IntOffset((cx - halfW).toInt(), (cy - halfH).toInt())
                            }
                            .size(boxWidthDp.coerceAtLeast(40.dp), boxHeightDp.coerceAtLeast(40.dp))
                            .rotate(clipRotation)
                            .border(1.5.dp, colors.accentCyan, RoundedCornerShape(4.dp))
                    ) {
                        listOf(
                            Alignment.TopStart to IntOffset(-6, -6),
                            Alignment.TopEnd to IntOffset(6, -6),
                            Alignment.BottomStart to IntOffset(-6, 6),
                            Alignment.BottomEnd to IntOffset(6, 6)
                        ).forEach { (align, offs) ->
                            Box(
                                modifier = Modifier
                                    .align(align)
                                    .offset(offs.x.dp, offs.y.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, colors.accentCyan, CircleShape)
                            )
                        }

                        // Top Rotation Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-20).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(colors.accentYellow)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⟳", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        // Badge showing scale and rotation
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(0.8.dp, colors.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(clipScale * 100).toInt()}% • ${clipRotation.toInt()}°",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // CapCut-style Eyedropper Magnifier Reticle
            if (isEyedropperActive) {
                val pickedColorHex = selectedClip?.chromaKey?.keyColorHex ?: 0xFF00FF00L
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                val rx = (reticlePos.x * canvasSize.width) - 26.dp.toPx()
                                val ry = (reticlePos.y * canvasSize.height) - 26.dp.toPx()
                                IntOffset(rx.toInt(), ry.toInt())
                            }
                            .size(52.dp)
                    ) {
                        // Outer ring with sampled color
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(3.dp, Color(pickedColorHex), CircleShape)
                        )
                        // Inner circle border
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        // Center dot with sampled color
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color(pickedColorHex))
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        // Crosshairs
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(52.dp)
                                .align(Alignment.Center)
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(1.5.dp)
                                .align(Alignment.Center)
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }

                    // Bottom instruction pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, colors.accentCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 Tap or drag reticle to pick chroma key color",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isPlaying && !isTransformInteractive && !isEyedropperActive) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.90f))
                        .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 29.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .size(34.dp)
                            .offset(x = 2.dp)
                    )
                }
            }

            // Top Status Overlay (Quality & Active Filter)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aspect Ratio indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.aspectRatio.label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Filter Pill
                if (activeClip != null && activeClip.filter != VideoFilter.NONE) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accentCyan)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeClip.filter.displayName,
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Render-fidelity notice. Warnings are never hidden: if a mask or
            // blend mode is approximated, the user sees it before exporting.
            val previewNotice = previewError?.let { "Preview error: $it" }
                ?: previewWarnings.firstOrNull()
            if (previewNotice != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 36.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (previewError != null) {
                                colors.accentRed.copy(alpha = 0.92f)
                            } else {
                                colors.accentYellow.copy(alpha = 0.92f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (previewWarnings.size > 1) {
                            "$previewNotice (+${previewWarnings.size - 1} more)"
                        } else {
                            previewNotice
                        },
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }

            // Bottom Timecode Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                val currentSec = currentPositionMs / 1000f
                val totalSec = project.totalDurationMs / 1000f
                Text(
                    text = String.format("%02d:%04.1f / %02d:%04.1f", (currentSec / 60).toInt(), currentSec % 60, (totalSec / 60).toInt(), totalSec % 60),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * A SurfaceView is required by CompositionPlayer (it rejects TextureView).
 * The player instance is part of the key in the caller, so switching between
 * the composition and fallback players always re-attaches a fresh surface.
 */
@Composable
private fun PlayerPreviewSurface(
    player: Player,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).also { surfaceView ->
                runCatching { player.setVideoSurfaceView(surfaceView) }
            }
        },
        update = { surfaceView ->
            runCatching { player.setVideoSurfaceView(surfaceView) }
        },
        modifier = modifier
    )
    DisposableEffect(player) {
        onDispose {
            runCatching { player.clearVideoSurface() }
        }
    }
}

@Composable
private fun RenderClipVisual(
    clip: MediaClip,
    activeEffects: List<com.example.model.TimelineEffect>,
    pulseAlpha: Float,
    currentClipLocalMs: Long = 0L
) {
    val keyScale = KeyframeInterpolator.getScale(clip, currentClipLocalMs)
    val keyRotation = KeyframeInterpolator.getRotation(clip, currentClipLocalMs)
    val keyOpacity = KeyframeInterpolator.getOpacity(clip, currentClipLocalMs)
    val colorFilter = when (clip.filter) {
        VideoFilter.SKETCH_GRAPHITE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                0.33f, 0.33f, 0.33f, 0f, 15f,
                0.33f, 0.33f, 0.33f, 0f, 12f,
                0.33f, 0.33f, 0.33f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.BLUEPRINT -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                0.1f, 0.2f, 0.4f, 0f, -30f,
                0.2f, 0.5f, 0.8f, 0f, 10f,
                0.4f, 0.7f, 1.4f, 0f, 50f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.SEPIA_NOTEBOOK -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 25f,
                0.349f, 0.686f, 0.168f, 0f, 15f,
                0.272f, 0.534f, 0.131f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.CHARCOAL_SMUDGE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.3f, 1.3f, 1.3f, 0f, -50f,
                1.3f, 1.3f, 1.3f, 0f, -50f,
                1.3f, 1.3f, 1.3f, 0f, -50f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.HALFTONE_MANGA -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.4f, 0f, 0f, 0f, -20f,
                0f, 1.4f, 0f, 0f, -20f,
                0f, 0f, 1.4f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.CYBERPUNK -> ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(1.4f)
        })
        VideoFilter.VINTAGE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.2f, 0.1f, 0.1f, 0f, 15f,
                0.1f, 1.0f, 0.1f, 0f, 10f,
                0.1f, 0.1f, 0.8f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.GOLDEN_HOUR -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.3f, 0.2f, 0f, 0f, 20f,
                0.1f, 1.1f, 0f, 0f, 10f,
                0f, 0f, 0.7f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.NOIR -> ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(0f)
        })
        VideoFilter.TEAL_ORANGE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.25f, 0f, 0f, 0f, 10f,
                0f, 1.15f, 0.1f, 0f, 0f,
                0f, 0.1f, 1.35f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.MOODY -> ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(0.6f)
        })
        VideoFilter.GLITCH_ART -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.5f, 0f, 0f, 0f, 30f,
                0f, 0.8f, 0f, 0f, -10f,
                0f, 0f, 1.5f, 0f, 25f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.NONE -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(keyScale)
            .rotate(keyRotation)
            .scale(if (clip.isMirrored) -1f else 1f, 1f)
    ) {
        if (clip.uriString != null) {
            // Real video frame via ExoPlayer when available, else thumbnail via Coil
            var showVideo by remember(clip.uriString) { mutableStateOf(true) }
            val player = LocalVideoPlayer.current
            if (showVideo && player != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(clip.uriString)
                        .crossfade(true)
                        .build(),
                    contentDescription = clip.title,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                    onError = { showVideo = false }
                )
            }
        } else if (clip.drawableResId != null) {
            Image(
                painter = painterResource(id = clip.drawableResId),
                contentDescription = clip.title,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val startColor = Color(clip.gradientColors.firstOrNull() ?: 0xFF1E2640)
            val endColor = Color(clip.gradientColors.lastOrNull() ?: 0xFF0D1B2A)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(startColor, endColor)
                        )
                    )
            )
        }

        // Active visual effects
        activeEffects.forEach { effect ->
            when (effect.effectType) {
                EffectType.RGB_SPLIT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color.Cyan.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                }
                EffectType.SHAKE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((pulseAlpha * 6).toInt(), 0) }
                    )
                }
                EffectType.VHS_GRAIN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.07f))
                    )
                }
                EffectType.NEON_HALO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(5.dp, Color(0xFF38BDF8).copy(alpha = pulseAlpha * 0.7f))
                    )
                }
                EffectType.LIGHTNING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = if (pulseAlpha > 0.95f) 0.35f else 0f))
                    )
                }
                EffectType.ZOOM_BLUR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.04f)
                    )
                }
            }
        }

        // Render Mask preview overlay
        when (clip.mask.type) {
            MaskType.RECTANGLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .border(2.dp, Color.Cyan, RoundedCornerShape(8.dp))
                )
            }
            MaskType.CIRCLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Cyan, CircleShape)
                )
            }
            MaskType.SPLIT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )
            }
            MaskType.MIRROR, MaskType.FILMSTRIP -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Magenta.copy(alpha = 0.7f))
                )
            }
            MaskType.NONE -> {}
        }

        if (clip.blendMode != BlendMode.NORMAL) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
private fun RenderTransitionEffect(
    transition: TransitionType,
    progress: Float
) {
    val alpha = (1f - (progress * 2 - 1f) * (progress * 2 - 1f)).coerceIn(0f, 1f)
    when (transition) {
        TransitionType.FADE_BLACK -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
            )
        }
        TransitionType.FLASH_WHITE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = alpha))
            )
        }
        TransitionType.WHIP_PAN -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = alpha * 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.GLITCH -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Cyan.copy(alpha = alpha * 0.5f),
                                Color.Transparent,
                                Color.Magenta.copy(alpha = alpha * 0.5f)
                            )
                        )
                    )
            )
        }
        TransitionType.PAGE_FLIP -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF2B231D).copy(alpha = alpha * 0.7f),
                                Color(0xFFFAF6EE).copy(alpha = alpha * 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.PAPER_TEAR -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFAF6EE).copy(alpha = alpha * 0.9f),
                                Color(0xFF332922).copy(alpha = alpha * 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.PENCIL_WIPE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2C241E).copy(alpha = alpha * 0.85f),
                                Color(0xFF4A3E36).copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.INK_BLEED -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF1E2838).copy(alpha = alpha * 0.85f),
                                Color(0xFF1E2838).copy(alpha = alpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.DISSOLVE, TransitionType.ZOOM_IN, TransitionType.NONE -> {}
    }
}
