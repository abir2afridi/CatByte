package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.TransitionType
import com.example.model.VideoProject
import com.example.engine.CenterPlayhead
import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder

private const val DP_PER_SECOND = 65f

/** Magnet radius for ruler taps, in dp (about 185 ms at the default zoom). */
private const val SNAP_THRESHOLD_DP = 12f

@Composable
fun MultiTrackTimeline(
    project: VideoProject,
    currentPositionMs: Long,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedTextId: String?,
    zoom: Float,
    onZoomChange: (Float) -> Unit = {},
    magneticSnap: Boolean = true,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onTransitionClick: (String) -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val totalMs = project.totalDurationMs.coerceAtLeast(4000L)
    val msToDp = (DP_PER_SECOND * zoom) / 1000f
    val timelineWidthDp = (totalMs * msToDp).dp + 260.dp
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var isTrackMuted by remember { mutableStateOf(false) }
    var viewportWidthDp by remember { mutableStateOf(0f) }

    val fps = project.fps.coerceIn(1, 120)
    /** dp per frame: the unit shared by snapping and the center-playhead maths. */
    val pxPerFrame = (msToDp * (1000f / fps)).coerceAtLeast(0.0001f)

    /**
     * Magnets come from [SnapEngine] so the ruler, clip edges, text, audio and
     * effect boundaries all share one priority order (the playhead beats a clip
     * edge at the same distance).
     */
    val snapCandidates: List<SnapCandidate> = remember(project, currentPositionMs, fps) {
        val edges = mutableListOf<Long>()
        var acc = 0L
        project.clips.forEach { clip ->
            acc += clip.durationMs
            edges += acc * fps / 1000L
        }
        project.overlayTracks.flatMap { it.clips }.forEach { clip ->
            edges += clip.startTimelineMs * fps / 1000L
            edges += (clip.startTimelineMs + clip.durationMs) * fps / 1000L
        }
        project.audioTracks.forEach { audio ->
            edges += audio.startTimelineMs * fps / 1000L
            edges += (audio.startTimelineMs + audio.durationMs) * fps / 1000L
        }
        project.textOverlays.forEach { text ->
            edges += text.startTimelineMs * fps / 1000L
            edges += (text.startTimelineMs + text.durationMs) * fps / 1000L
        }
        project.effects.forEach { effect ->
            edges += effect.startTimelineMs * fps / 1000L
            edges += (effect.startTimelineMs + effect.durationMs) * fps / 1000L
        }
        SnapEngine.collect(
            playhead = currentPositionMs * fps / 1000L,
            clipEdges = edges,
            markers = emptyList(),
            beats = emptyList()
        )
    }
    val snapThresholdFrames = remember(pxPerFrame) {
        SnapEngine.thresholdFrames(SNAP_THRESHOLD_DP, pxPerFrame)
    }

    // Fixed center playhead: during playback the content scrolls under a
    // stationary playhead and pins at the very start/end of the timeline.
    LaunchedEffect(currentPositionMs, isPlaying, pxPerFrame, viewportWidthDp) {
        if (isPlaying && viewportWidthDp > 0f) {
            val offset = CenterPlayhead.offsetForFrame(
                playheadFrame = currentPositionMs * fps / 1000L,
                pxPerFrame = pxPerFrame,
                viewportWidthPx = viewportWidthDp,
                contentWidthPx = timelineWidthDp.value
            )
            scrollState.scrollTo(offset.roundToInt())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.timelineTrack)
            .border(1.2.dp, colors.borderSubtle)
            .testTag("multi_track_timeline")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Track Left Controls (Mute clip audio, Cover)
            TimelineTrackHeader(
                isTrackMuted = isTrackMuted,
                onMuteClick = { isTrackMuted = !isTrackMuted }
            )

            // Scrollable Timeline Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { viewportWidthDp = with(density) { it.width.toDp().value } }
                    .horizontalScroll(scrollState)
                    .pointerInput(totalMs, pxPerFrame, magneticSnap, snapCandidates, snapThresholdFrames, fps) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val rawFrame = (offset.x / pxPerFrame).roundToLong().coerceAtLeast(0L)
                                val snapped = SnapEngine.snap(
                                    targetFrame = rawFrame,
                                    candidates = snapCandidates,
                                    thresholdFrames = snapThresholdFrames,
                                    enabled = magneticSnap
                                )
                                val targetMs = (snapped.frame * 1000L / fps).coerceIn(0L, totalMs)
                                onSeek(targetMs)
                            },
                            onDrag = { change, _ ->
                                val offset = change.position
                                val rawFrame = (offset.x / pxPerFrame).roundToLong().coerceAtLeast(0L)
                                val snapped = SnapEngine.snap(
                                    targetFrame = rawFrame,
                                    candidates = snapCandidates,
                                    thresholdFrames = snapThresholdFrames,
                                    enabled = magneticSnap
                                )
                                val targetMs = (snapped.frame * 1000L / fps).coerceIn(0L, totalMs)
                                onSeek(targetMs)
                            }
                        )
                    }
                    .pointerInput(zoom) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            val newZoom = (zoom * zoomChange).coerceIn(0.5f, 4.0f)
                            onZoomChange(newZoom)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Time Ruler with sketched notebook grid
                        TimelineTimeRuler(
                            totalMs = totalMs,
                            msToDp = msToDp
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // 2. Main Video Clips Track
                        VideoClipsTrack(
                            clips = project.clips,
                            selectedClipId = selectedClipId,
                            msToDp = msToDp,
                            onSelectClip = onSelectClip,
                            onTransitionClick = onTransitionClick,
                            onAddMediaClick = onAddMediaClick,
                            onMoveClip = onMoveClip
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // 3. PIP / Overlay Track (rendered above the main video)
                        val overlayClips = project.overlayTracks.flatMap { it.clips }
                        if (overlayClips.isNotEmpty()) {
                            OverlayClipsTrack(
                                clips = overlayClips,
                                selectedClipId = selectedClipId,
                                msToDp = msToDp,
                                onSelectClip = onSelectClip
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        // 4. Audio / Music Track
                        if (project.audioTracks.isNotEmpty()) {
                            AudioClipsTrack(
                                tracks = project.audioTracks,
                                msToDp = msToDp
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        // 4. Text / Subtitle Track
                        if (project.textOverlays.isNotEmpty()) {
                            TextClipsTrack(
                                texts = project.textOverlays,
                                selectedTextId = selectedTextId,
                                msToDp = msToDp,
                                onSelectText = onSelectText
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                        }

                        // 5. Visual Effects Track
                        if (project.effects.isNotEmpty()) {
                            EffectsClipsTrack(
                                effects = project.effects,
                                msToDp = msToDp
                            )
                        }
                    }

                    // 6. Playhead Needle (Steel White Needle in CapCut Dark mode / Wooden Pencil in Pencil mode)
                    val playheadOffsetDp = (currentPositionMs * msToDp).dp
                    Box(
                        modifier = Modifier
                            .offset(x = playheadOffsetDp - 16.dp)
                            .width(32.dp)
                            .fillMaxHeight()
                            .pointerInput(totalMs, pxPerFrame, magneticSnap, snapCandidates, snapThresholdFrames, fps) {
                                detectDragGestures(
                                    onDrag = { change, _ ->
                                        val currentX = playheadOffsetDp.toPx() + change.position.x - 16.dp.toPx()
                                        val rawFrame = (currentX / pxPerFrame).roundToLong().coerceAtLeast(0L)
                                        val snapped = SnapEngine.snap(
                                            targetFrame = rawFrame,
                                            candidates = snapCandidates,
                                            thresholdFrames = snapThresholdFrames,
                                            enabled = magneticSnap
                                        )
                                        val targetMs = (snapped.frame * 1000L / fps).coerceIn(0L, totalMs)
                                        onSeek(targetMs)
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(x = 9.dp)
                                .width(14.dp)
                                .fillMaxHeight()
                        ) {
                            // Floating timestamp badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-18).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.surfaceElevated)
                                    .border(0.8.dp, if (!colors.isPencilSketch) colors.accentCyan else colors.border, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                val sec = currentPositionMs / 1000f
                                Text(
                                    text = String.format("%.1fs", sec),
                                    color = if (!colors.isPencilSketch) colors.accentCyan else colors.textPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Vertical guide line extending through tracks
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = 10.dp)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(colors.playhead)
                            )

                            if (!colors.isPencilSketch) {
                                // CapCut Steel White Needle Top Cap
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .size(width = 10.dp, height = 14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colors.playhead)
                                        .border(1.dp, colors.accentCyan, RoundedCornerShape(3.dp))
                                )
                            } else {
                                // Top Sharpened Pencil Tip
                                SharpenedPencilTip(
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTrackHeader(
    isTrackMuted: Boolean,
    onMuteClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .width(54.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .border(width = 1.2.dp, color = colors.borderSubtle)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Video track icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Track",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Mute button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isTrackMuted) colors.surfaceVariant else colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                .clickable { onMuteClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTrackMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Mute Track",
                tint = if (isTrackMuted) colors.textTertiary else colors.accentCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        // Cover thumbnail button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cover",
                color = colors.textPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimelineTimeRuler(
    totalMs: Long,
    msToDp: Float
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(colors.surfaceElevated)
            .border(width = 0.8.dp, color = colors.borderSubtle)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height

            // 1. Bottom ruler brass edge
            drawLine(
                color = colors.border.copy(alpha = 0.4f),
                start = Offset(0f, h),
                end = Offset(size.width, h),
                strokeWidth = 1.2f
            )

            // 2. Architect drafting scale graduations (200ms mm-ticks, 500ms half-marks, 1000ms major marks)
            val subStepMs = 200L
            var timeMs = 0L
            while (timeMs <= totalMs + 4000L) {
                val x = timeMs * msToDp
                val isSecond = timeMs % 1000L == 0L
                val isHalfSecond = timeMs % 500L == 0L

                val tickHeight = when {
                    isSecond -> h * 0.55f
                    isHalfSecond -> h * 0.38f
                    else -> h * 0.22f
                }

                val strokeW = if (isSecond) 1.5f else 0.9f
                val tickColor = when {
                    isSecond -> colors.border
                    isHalfSecond -> colors.textSecondary.copy(alpha = 0.6f)
                    else -> colors.borderSubtle.copy(alpha = 0.7f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(x, 0f),
                    end = Offset(x, tickHeight),
                    strokeWidth = strokeW
                )

                timeMs += subStepMs
            }
        }

        // Stamped second markings (00:00, 00:01, ...)
        Row(modifier = Modifier.fillMaxSize()) {
            var labelMs = 0L
            while (labelMs <= totalMs + 4000L) {
                val sec = labelMs / 1000
                Box(
                    modifier = Modifier
                        .offset(x = (labelMs * msToDp).dp + 2.dp)
                        .padding(top = 13.dp)
                ) {
                    Text(
                        text = String.format("%02d:%02d", sec / 60, sec % 60),
                        color = colors.textPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                labelMs += 1000L
            }
        }
    }
}

@Composable
private fun VideoClipsTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit,
    onTransitionClick: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current
    // Raw dp widths of the clips, in the exact order the Row renders them.
    // The 26.dp transition buttons between clips are counted in the slot
    // starts so the reorder maths matches what is on screen.
    val clipWidthDps = clips.map { (it.durationMs * msToDp).coerceAtLeast(48f) }
    val transitionDp = 26f

    fun slotStart(index: Int): Float {
        var acc = 0f
        for (j in 0 until index) acc += clipWidthDps[j] + transitionDp
        return acc
    }

    /** Final insertion index for a drop after dragging [from] by [dragDp]. */
    fun targetIndex(from: Int, dragDp: Float): Int {
        val center = slotStart(from) + dragDp + clipWidthDps[from] / 2f
        var target = 0
        clips.forEachIndexed { i, _ ->
            if (i == from) return@forEachIndexed
            if (slotStart(i) + clipWidthDps[i] / 2f < center) target += 1
        }
        return target.coerceIn(0, clips.lastIndex)
    }

    // Long-press + drag reorders the main track. Scroll gestures keep working
    // because the drag only starts after a deliberate long press.
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        clips.forEachIndexed { index, clip ->
            val fromIndex = index
            val isSelected = clip.id == selectedClipId
            val isDragging = draggedId == clip.id
            val clipWidthDp = clipWidthDps[index].dp

            Box(
                modifier = Modifier
                    .width(clipWidthDp)
                    .fillMaxHeight()
                    .zIndex(if (isDragging) 20f else 0f)
                    .graphicsLayer {
                        translationX = if (isDragging) dragOffsetPx else 0f
                        shadowElevation = if (isDragging) 14f else 0f
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.clipSelected else colors.clipBg)
                    .pencilHatching(
                        color = if (isSelected) colors.accentCyan.copy(alpha = 0.18f) else colors.border.copy(alpha = 0.08f),
                        spacing = 7.dp,
                        enabled = colors.isPencilSketch
                    )
                    .sketchPencilBorder(
                        strokeWidth = if (isSelected || isDragging) 2.2.dp else 1.2.dp,
                        color = when {
                            isDragging -> colors.accentYellow
                            isSelected -> colors.accentCyan
                            else -> colors.borderSubtle
                        },
                        cornerRadius = 8.dp,
                        isPencilStyle = colors.isPencilSketch
                    )
                    .clickable { onSelectClip(clip.id) }
                    .pointerInput(clip.id, clips.size, clipWidthDps) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragOffsetPx = 0f
                                draggedId = clip.id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetPx += dragAmount.x
                            },
                            onDragEnd = {
                                val dragDp = dragOffsetPx / density.density
                                val to = targetIndex(fromIndex, dragDp)
                                if (draggedId == clip.id && to != fromIndex) {
                                    onMoveClip(fromIndex, to)
                                }
                                draggedId = null
                                dragOffsetPx = 0f
                            },
                            onDragCancel = {
                                draggedId = null
                                dragOffsetPx = 0f
                            }
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                // Waveform rendering
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 2.5.dp.toPx()
                    val spacing = 2.5.dp.toPx()
                    var currX = 0f
                    var i = 0
                    val wf = clip.waveform
                    val wfSize = wf.size
                    while (currX < size.width) {
                        val factor = if (wfSize > 0) wf[i % wfSize] else 0.5f
                        val barHeight = size.height * 0.42f * factor
                        drawLine(
                            color = if (isSelected) colors.border.copy(alpha = 0.55f) else colors.textTertiary.copy(alpha = 0.4f),
                            start = Offset(currX, size.height - barHeight),
                            end = Offset(currX, size.height),
                            strokeWidth = barWidth
                        )
                        currX += barWidth + spacing
                        i++
                    }
                }

                // Keyframe Diamond Markers on Clip (CapCut Style)
                if (clip.keyframes.isNotEmpty()) {
                    clip.keyframes.forEach { kf ->
                        val kfOffsetMs = kf.timestampMs.coerceIn(0L, clip.durationMs)
                        val kfXDp = (kfOffsetMs * msToDp).dp
                        Box(
                            modifier = Modifier
                                .offset(x = (kfXDp - 4.dp).coerceAtLeast(0.dp), y = 4.dp)
                                .size(8.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color(0xFFFF3B30), RoundedCornerShape(1.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(1.dp))
                        )
                    }
                }

                // Badges Row (Reverse, Chroma Key)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (clip.isReversed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFF2D55))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "◀ REV", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    if (clip.chromaKey.isEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF34C759))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "🟢 KEY", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Title & Info labels
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clip.title,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%.1fs", clip.durationMs / 1000f),
                            color = colors.accentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (clip.speed != 1.0f) {
                            Text(
                                text = "${clip.speed}x",
                                color = colors.accentYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Keyframe Diamond Markers Overlay (◆)
                if (clip.keyframes.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        clip.keyframes.forEach { kf ->
                            val kfRatio = if (clip.durationMs > 0) (kf.timestampMs.toFloat() / clip.durationMs).coerceIn(0f, 1f) else 0f
                            val kfOffsetDp = (clipWidthDp.value * kfRatio).dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .offset(x = kfOffsetDp - 4.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentPink)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                // Sketched tactile drag handles
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(colors.handle, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(colors.handle, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    )
                }
            }

            // Transition Icon Button between clips
            if (index < clips.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .border(1.2.dp, colors.border, RoundedCornerShape(6.dp))
                        .clickable { onTransitionClick(clip.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Transition",
                        tint = if (clip.transition != TransitionType.NONE) colors.accentCyan else colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Add Clip (+) Button at end of track
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.2.dp, colors.border, RoundedCornerShape(8.dp))
                .clickable { onAddMediaClick() }
                .testTag("add_clip_to_timeline_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Clip",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun OverlayClipsTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId
            val startOffsetDp = (clip.startTimelineMs * msToDp).dp
            val widthDp = (clip.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        colors.accentCyan.copy(alpha = if (isSelected) 0.42f else 0.22f)
                    )
                    .border(
                        width = if (isSelected) 2.2.dp else 1.2.dp,
                        color = colors.accentCyan,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Keyframe Diamonds on Overlay Clip
                if (clip.keyframes.isNotEmpty()) {
                    clip.keyframes.forEach { kf ->
                        val kfOffsetMs = kf.timestampMs.coerceIn(0L, clip.durationMs)
                        val kfXDp = (kfOffsetMs * msToDp).dp
                        Box(
                            modifier = Modifier
                                .offset(x = (kfXDp - 4.dp).coerceAtLeast(0.dp), y = 4.dp)
                                .size(8.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color(0xFFFF3B30), RoundedCornerShape(1.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(1.dp))
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Overlay clip",
                        tint = colors.accentCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = clip.title,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioClipsTrack(
    tracks: List<AudioClip>,
    msToDp: Float
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        tracks.forEach { audio ->
            val startOffsetDp = (audio.startTimelineMs * msToDp).dp
            val widthDp = (audio.durationMs * msToDp).dp.coerceAtLeast(54.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentPurple.copy(alpha = 0.25f))
                    .border(1.2.dp, colors.accentPurple, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio",
                        tint = colors.accentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = audio.title,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TextClipsTrack(
    texts: List<TextOverlay>,
    selectedTextId: String?,
    msToDp: Float,
    onSelectText: (String) -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        texts.forEach { item ->
            val isSelected = item.id == selectedTextId
            val startOffsetDp = (item.startTimelineMs * msToDp).dp
            val widthDp = (item.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentYellow.copy(alpha = if (isSelected) 0.5f else 0.25f))
                    .border(
                        width = if (isSelected) 2.dp else 1.2.dp,
                        color = colors.accentYellow,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectText(item.id) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text",
                        tint = colors.accentYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = item.text,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectsClipsTrack(
    effects: List<TimelineEffect>,
    msToDp: Float
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        effects.forEach { effect ->
            val startOffsetDp = (effect.startTimelineMs * msToDp).dp
            val widthDp = (effect.durationMs * msToDp).dp.coerceAtLeast(44.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentGreen.copy(alpha = 0.25f))
                    .border(1.2.dp, colors.accentGreen, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "FX: ${effect.name}",
                    color = colors.textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
