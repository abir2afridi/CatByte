package com.example.ui.canvas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ScrubPreviewFrame
import com.example.engine.backgroundArgbFor
import com.example.model.MediaClip
import com.example.model.TextAnimationType
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The editor's video preview frame.
 *
 * This file owns the frame itself (sizing, the active clip, the pulse
 * animation and the gesture wiring); every layer lives in its own file under
 * `ui/canvas/`:
 *  - `CanvasOverlayLayers.kt`: text, sticker and watermark overlays.
 *  - `CanvasTransformHandles.kt`: the selection bounding box and handles.
 *  - `CanvasEyedropperReticle.kt`: the chroma-key colour picker reticle.
 *  - `CanvasPlaybackHud.kt`: the play button, the filter pill and the
 *    render-fidelity notice.
 *  - `CanvasEmptyState.kt`: the "no media" placeholder.
 *  - `CanvasGestures.kt`: the pointer handling of the frame.
 */
@Composable
fun VideoCanvas(
    project: VideoProject,
    currentPositionMs: State<Long>,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    watermarkEnabled: Boolean = false,
    /** Draws the action/title-safe boxes and rule-of-thirds grid over the video. */
    showSafeAreaGuides: Boolean = false,
    /**
     * Frame decoded straight from the source media while the timeline is being
     * scrubbed, together with the position it belongs to. Drawn over the player
     * surface, whose seeks would otherwise black the preview out for the whole
     * drag.
     */
    scrubFrame: State<ScrubPreviewFrame?>? = null,
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
    // Source frame extracted while the timeline is scrubbed (null otherwise).
    val scrub = scrubFrame?.value
    val scrubBitmap = scrub?.bitmap
    // The playhead changes ~60x/s while playing; this composable is the only
    // one that genuinely needs it (active clip, overlay animations, fallback
    // canvas). While a scrub frame is up, the frame's own position is the
    // source of truth: the `?:` short-circuits the playhead read, so a fling
    // only recomposes the canvas when a new bitmap actually arrives instead of
    // on every scroll tick.
    val positionMs = scrub?.positionMs ?: currentPositionMs.value
    // Wrapping the bitmap is cheap but not free; keep it stable across the
    // frequent playhead recompositions during a drag.
    val scrubImage = remember(scrubBitmap) { scrubBitmap?.asImageBitmap() }

    // Active clip resolution
    var accumulatedMs = 0L
    var activeClip: MediaClip? = null
    var clipLocalTimeMs = 0L

    for (clip in project.clips) {
        val clipEnd = accumulatedMs + clip.durationMs
        if (positionMs in accumulatedMs..clipEnd) {
            activeClip = clip
            clipLocalTimeMs = positionMs - accumulatedMs
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
        positionMs >= it.startTimelineMs && positionMs <= (it.startTimelineMs + it.durationMs)
    }
    val activeStickers = project.stickers.filter {
        positionMs >= it.startTimelineMs && positionMs <= (it.startTimelineMs + it.durationMs)
    }
    val activeEffects = project.effects.filter {
        positionMs >= it.startTimelineMs && positionMs <= (it.startTimelineMs + it.durationMs)
    }

    // Pulse animation - only runs while playing AND only when something actually
    // renders the pulse (sample clips, the no-player preview, NEON_PULSE text).
    // It must live inside LaunchedEffect's own scope: the previous version
    // launched into rememberCoroutineScope() and looped on a captured Boolean,
    // so every play/pause cycle leaked an immortal 60 fps coroutine and the
    // editor got laggier with each one (repro: scrub, press play, watch the
    // timeline stutter more and more).
    val canvasSizeState = remember { mutableStateOf(IntSize(1, 1)) }
    val reticlePos = remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val pulseAlphaState: MutableState<Float> = remember { mutableStateOf(0.85f) }
    val needsPulse = player == null ||
        (activeClip != null && activeClip.uriString == null && activeClip.drawableResId == null) ||
        project.textOverlays.any { it.animation == TextAnimationType.NEON_PULSE }
    LaunchedEffect(isPlaying, needsPulse) {
        if (!isPlaying || !needsPulse) {
            pulseAlphaState.value = 0.85f
            return@LaunchedEffect
        }
        var growing = true
        val duration = 600L
        while (isActive) {
            val target = if (growing) 1.0f else 0.85f
            growing = !growing
            // Simple linear interpolation over 600ms
            val start = pulseAlphaState.value
            val startTime = System.currentTimeMillis()
            while (isActive && System.currentTimeMillis() - startTime < duration) {
                val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
                pulseAlphaState.value = start + (target - start) * progress
                delay(16) // ~60fps
            }
            if (!isActive) return@LaunchedEffect
            pulseAlphaState.value = target
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0F))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fit the largest exact-ratio canvas inside the available space. Using
        // fillMaxSize() here would force min = max constraints and make
        // aspectRatio() a no-op, stretching/overflowing the editor layout.
        val boundedWidth = maxWidth.value.isFinite() && maxWidth > 0.dp
        val boundedHeight = maxHeight.value.isFinite() && maxHeight > 0.dp
        val fittedWidth: Dp
        val fittedHeight: Dp
        if (targetAspect > 0f) {
            when {
                boundedWidth && boundedHeight -> {
                    val widthAtMaxHeight = maxHeight * targetAspect
                    if (widthAtMaxHeight <= maxWidth) {
                        fittedWidth = widthAtMaxHeight
                        fittedHeight = maxHeight
                    } else {
                        fittedWidth = maxWidth
                        fittedHeight = maxWidth / targetAspect
                    }
                }
                boundedWidth -> {
                    fittedWidth = maxWidth
                    fittedHeight = maxWidth / targetAspect
                }
                boundedHeight -> {
                    fittedHeight = maxHeight
                    fittedWidth = maxHeight * targetAspect
                }
                else -> {
                    fittedWidth = 0.dp
                    fittedHeight = 0.dp
                }
            }
        } else {
            fittedWidth = if (boundedWidth) maxWidth else 0.dp
            fittedHeight = if (boundedHeight) maxHeight else 0.dp
        }

        // Outer Canvas Frame maintaining exact project aspect ratio (CapCut clean studio style)
        Box(
            modifier = Modifier
                .width(fittedWidth)
                .height(fittedHeight)
                .clip(RoundedCornerShape(8.dp))
                // The project background is what shows in the bars around a
                // video whose aspect does not match the canvas.
                .background(Color(backgroundArgbFor(project.paperStyle)))
                .onSizeChanged { canvasSizeState.value = it }
                .canvasGestures(
                    isEyedropperActive = isEyedropperActive,
                    isTransformInteractive = isTransformInteractive,
                    selectedClipId = selectedClip?.id,
                    canvasSize = canvasSizeState,
                    onReticleMoved = { reticlePos.value = it },
                    onSampleColorAt = onSampleColorAt,
                    onTransformClip = onTransformClip,
                    onTogglePlay = onTogglePlay
                )
                .testTag("video_canvas_preview"),
            contentAlignment = Alignment.Center
        ) {

            // Project background image (fallback preview only; the composition
            // preview paints it into the canvas bars itself).
            if (project.backgroundImageUri.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(project.backgroundImageUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            val isSampleClip = activeClip != null && activeClip.uriString == null && activeClip.drawableResId == null
            if (isSampleClip) {
                RenderClipVisual(
                    clip = activeClip!!,
                    activeEffects = activeEffects,
                    pulseAlpha = pulseAlphaState.value,
                    currentClipLocalMs = clipLocalTimeMs
                )
            } else if (player != null) {
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
            } else if (activeClip != null) {
                // No player at all: show the generated preview frame only.
                RenderClipVisual(
                    clip = activeClip,
                    activeEffects = activeEffects,
                    pulseAlpha = pulseAlphaState.value,
                    currentClipLocalMs = clipLocalTimeMs
                )

                // Transition effect
                if (activeClip.transition != com.example.model.TransitionType.NONE && clipLocalTimeMs < activeClip.transitionDurationMs) {
                    RenderTransitionEffect(
                        transition = activeClip.transition,
                        progress = clipLocalTimeMs.toFloat() / activeClip.transitionDurationMs
                    )
                }
            } else {
                // Empty state inside canvas
                CanvasEmptyState()
            }

            // Timeline scrub: draw the source frame extracted for the playhead
            // over whatever is behind it. Independent of the player on purpose:
            // the frame comes straight from the source file, so it must still
            // appear when the preview player itself has failed (which is exactly
            // when the error pill used to cover the screen).
            //
            // CROP, not Fit: the extracted bitmap is the RAW source frame and
            // can have a different aspect ratio than the project canvas. Fit
            // letterboxed it with black bars, which made the preview look
            // zoomed out / aspect-changed while the timeline was scrolling.
            // Cropping fills the exact-ratio canvas frame just like the normal
            // video surface (RenderClipVisual also uses ContentScale.Crop).
            if (scrubImage != null) {
                Image(
                    bitmap = scrubImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Text / sticker / watermark overlays (fallback renderer only -
            // composition burns them in; during a scrub the extracted frame is
            // raw, so draw them on top).
            if (!compositionPreview || scrubBitmap != null) {
                CanvasTextOverlayLayers(
                    activeTexts = activeTexts,
                    positionMs = positionMs,
                    pulseAlpha = pulseAlphaState.value
                )
                CanvasStickerOverlayLayer(activeStickers)
                if (watermarkEnabled) {
                    CanvasWatermarkOverlay()
                }
            } // end fallback-only 2D layers

            // Composition guides (preview only, never exported).
            if (showSafeAreaGuides) {
                CanvasSafeAreaGuides()
            }

            // CapCut-style Selection Bounding Box & Transform Handles
            if (isTransformInteractive && selectedClip != null && !isEyedropperActive) {
                CanvasTransformHandles(
                    selectedClip = selectedClip,
                    canvasSize = canvasSizeState.value
                )
            }

            // CapCut-style Eyedropper Magnifier Reticle
            if (isEyedropperActive) {
                CanvasEyedropperReticle(
                    reticlePos = reticlePos.value,
                    canvasSize = canvasSizeState.value,
                    pickedColorHex = selectedClip?.chromaKey?.keyColorHex ?: 0xFF00FF00L
                )
            }

            if (!isPlaying && !isTransformInteractive && !isEyedropperActive) {
                CanvasPlayHud()
            }

            // Top Status Overlay (Active Filter only).
            // Canvas ratio is shown in the transport row below the preview, never
            // on top of the video frame itself.
            CanvasTopStatusRow(activeClip)

            // Render-fidelity notice.
            CanvasPreviewNotice(
                previewError = previewError,
                previewWarnings = previewWarnings,
                hasScrubFrame = scrubBitmap != null
            )

            // The playhead timecode lives in the timeline's left gutter (see
            // EditorTimecodeText). Nothing time-related is painted over the video.
        }
    }
}
