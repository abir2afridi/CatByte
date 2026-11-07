package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.CenterPlayhead
import com.example.engine.ClipTrimHandle
import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import com.example.model.TimelineTrackIds
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Multi-track timeline coordinator matching the CapCut screenshot.
 */
@Composable
fun MultiTrackTimeline(
    project: VideoProject,
    currentPositionMs: Long,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedTextId: String?,
    zoom: Float,
    magneticSnap: Boolean = true,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onTransitionClick: (String) -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onMoveOverlayClip: (clipId: String, newStartMs: Long) -> Unit = { _, _ -> },
    onMoveOverlayClipToTrack: (clipId: String, targetTrackId: String, newStartMs: Long) -> Unit = { _, _, _ -> },
    onZoomChange: (Float) -> Unit = {},
    onTrimClip: (edge: ClipTrimHandle.Edge, deltaMs: Long) -> Unit = { _, _ -> },
    onTrackMute: (trackId: String, muted: Boolean) -> Unit = { _, _ -> },
    onTrackLock: (trackId: String, locked: Boolean) -> Unit = { _, _ -> },
    onTrackSolo: (trackId: String, solo: Boolean) -> Unit = { _, _ -> },
    onMoveOverlayTrack: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onMoveAudioClip: (audioId: String, newStartMs: Long) -> Unit = { _, _ -> },
    onToggleAudioMute: (audioId: String, muted: Boolean) -> Unit = { _, _ -> },
    isMultiSelect: Boolean = false,
    selectedClipIds: Set<String> = emptySet(),
    onToggleClipSelection: (String) -> Unit = {},
    beats: List<Long> = emptyList(),
    onUserInteraction: () -> Unit = {},
    onAddAudioClick: () -> Unit = {},
    onAddTextClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalMs = project.totalDurationMs.coerceAtLeast(TimelineGeometry.MIN_TIMELINE_MS)
    val msToDp = TimelineGeometry.dpPerMs(zoom)
    val timelineWidthDp = TimelineGeometry.contentWidthDp(totalMs, zoom)
    val scrollState = rememberScrollState()
    val trackScrollState = rememberScrollState()
    val density = LocalDensity.current

    var viewportWidthPx by remember { mutableStateOf(0) }
    var isScrubbing by remember { mutableStateOf(false) }

    val isUserDragging by scrollState.interactionSource.collectIsDraggedAsState()
    val latestInteraction by rememberUpdatedState(onUserInteraction)
    val latestOnZoom by rememberUpdatedState(onZoomChange)
    val latestZoom by rememberUpdatedState(zoom)

    val fps = project.fps.coerceIn(1, 120)
    val pxPerFrameDp = (msToDp * (1_000f / fps)).coerceAtLeast(0.0001f)
    val pxPerFramePx = pxPerFrameDp * density.density
    val contentWidthPx = timelineWidthDp * density.density

    val snapCandidates: List<SnapCandidate> = remember(project, fps, beats) {
        val edges = mutableListOf<Long>(0L)
        var acc = 0L
        project.clips.forEach { clip ->
            acc += clip.durationMs
            edges += acc * fps / 1_000L
        }
        project.overlayTracks.flatMap { it.clips }.forEach { clip ->
            edges += clip.startTimelineMs * fps / 1_000L
            edges += (clip.startTimelineMs + clip.durationMs) * fps / 1_000L
        }
        project.audioTracks.forEach { audio ->
            edges += audio.startTimelineMs * fps / 1_000L
            edges += (audio.startTimelineMs + audio.durationMs) * fps / 1_000L
        }
        project.textOverlays.forEach { text ->
            edges += text.startTimelineMs * fps / 1_000L
            edges += (text.startTimelineMs + text.durationMs) * fps / 1_000L
        }
        project.effects.forEach { effect ->
            edges += effect.startTimelineMs * fps / 1_000L
            edges += (effect.startTimelineMs + effect.durationMs) * fps / 1_000L
        }
        val beatFrames = beats.map { it * fps / 1_000L }
        val markerFrames = project.markers.map { it.atMs * fps / 1_000L }
        SnapEngine.collect(
            playhead = -1L,
            clipEdges = edges,
            markers = markerFrames,
            beats = beatFrames
        ).filter { it.frame >= 0L }
    }
    val snapThresholdFrames = remember(pxPerFrameDp) {
        SnapEngine.thresholdFrames(TimelineGeometry.SNAP_THRESHOLD_DP, pxPerFrameDp)
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) latestInteraction()
    }

    LaunchedEffect(
        currentPositionMs,
        isPlaying,
        isUserDragging,
        isScrubbing,
        pxPerFramePx,
        viewportWidthPx
    ) {
        if (!isPlaying || isUserDragging || isScrubbing || viewportWidthPx <= 0) {
            return@LaunchedEffect
        }
        val target = CenterPlayhead.offsetForFrame(
            playheadFrame = currentPositionMs * fps / 1_000L,
            pxPerFrame = pxPerFramePx,
            viewportWidthPx = viewportWidthPx.toFloat(),
            contentWidthPx = contentWidthPx
        )
        if (abs(target - scrollState.value) >= 0.5f) {
            scrollState.scrollTo(target.roundToInt())
        }
    }

    LaunchedEffect(pxPerFramePx, viewportWidthPx) {
        if (viewportWidthPx > 0) {
            val target = CenterPlayhead.offsetForFrame(
                playheadFrame = currentPositionMs * fps / 1_000L,
                pxPerFrame = pxPerFramePx,
                viewportWidthPx = viewportWidthPx.toFloat(),
                contentWidthPx = contentWidthPx
            )
            scrollState.scrollTo(target.roundToInt())
        }
    }

    val beginScrub: () -> Unit = {
        isScrubbing = true
        latestInteraction()
    }
    val endScrub: () -> Unit = { isScrubbing = false }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF101014))
            .border(0.6.dp, Color(0xFF1E2028))
            .testTag("multi_track_timeline")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // CapCut Fixed Left Gutter (Timecode, Mute clip, Cover, ♪, T)
            CapCutTimelineLeftGutter(
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalMs,
                isMainMuted = project.isMainTrackMuted,
                onToggleMainMute = {
                    onTrackMute(TimelineTrackIds.MAIN, !project.isMainTrackMuted)
                },
                onOpenCover = onCoverClick,
                overlayTrackCount = project.overlayTracks.size,
                hasAudioTrack = true,
                hasTextTrack = true,
                modifier = Modifier.verticalScroll(trackScrollState)
            )

            // Scrolling Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            var baselineSpan = 0f
                            var baselineZoom = 0f
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    latestInteraction()
                                    val span = abs(pressed[0].position.x - pressed[1].position.x)
                                    if (baselineSpan <= 0f) {
                                        baselineSpan = span.coerceAtLeast(1f)
                                        baselineZoom = latestZoom
                                    } else if (span > 0f) {
                                        val next = (baselineZoom * (span / baselineSpan))
                                            .coerceIn(0.5f, 3f)
                                        if (abs(next - latestZoom) > 0.001f) latestOnZoom(next)
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                                if (event.changes.all { !it.pressed }) break
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) {
                                    latestInteraction()
                                }
                            }
                        }
                    }
                    .onSizeChanged { viewportWidthPx = it.width }
            ) {
                // Ruler pinned to top of track scroll
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .height(28.dp)
                        .horizontalScroll(scrollState)
                ) {
                    Box(modifier = Modifier.width(timelineWidthDp.dp)) {
                        TimelineRuler(
                            totalMs = totalMs,
                            zoom = zoom,
                            fps = fps,
                            magneticSnap = magneticSnap,
                            snapCandidates = snapCandidates,
                            snapThresholdFrames = snapThresholdFrames,
                            scrollState = scrollState,
                            beats = beats,
                            markers = project.markers,
                            onSeek = onSeek,
                            onScrubStart = beginScrub,
                            onScrubEnd = endScrub
                        )
                    }
                }

                // Tracks container
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxSize()
                        .padding(top = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(trackScrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(timelineWidthDp.dp)
                                .horizontalScroll(scrollState)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. Main Video Track
                                TimelineVideoTrack(
                                    clips = project.clips,
                                    selectedClipId = selectedClipId,
                                    msToDp = msToDp,
                                    onSelectClip = onSelectClip,
                                    onTransitionClick = onTransitionClick,
                                    onAddMediaClick = onAddMediaClick,
                                    onMoveClip = onMoveClip,
                                    onTrimClip = { _, edge, deltaMs -> onTrimClip(edge, deltaMs) },
                                    fps = fps,
                                    isLocked = project.isMainTrackLocked,
                                    isMultiSelect = isMultiSelect,
                                    selectedClipIds = selectedClipIds,
                                    onToggleClipSelection = onToggleClipSelection
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                // 2. Overlay Tracks
                                project.overlayTracks.forEachIndexed { trackIndex, track ->
                                    TimelineOverlayTrack(
                                        clips = track.clips,
                                        selectedClipId = selectedClipId,
                                        msToDp = msToDp,
                                        onSelectClip = onSelectClip,
                                        onMoveClip = { clipId, startMs, trackShift ->
                                            if (trackShift == 0) {
                                                onMoveOverlayClip(clipId, startMs)
                                            } else {
                                                val targetIndex = (trackIndex + trackShift).coerceIn(
                                                    0,
                                                    project.overlayTracks.lastIndex
                                                )
                                                onMoveOverlayClipToTrack(
                                                    clipId,
                                                    project.overlayTracks[targetIndex].id,
                                                    startMs
                                                )
                                            }
                                        },
                                        onTrimClip = { _, edge, deltaMs ->
                                            onTrimClip(edge, deltaMs)
                                        },
                                        fps = fps,
                                        isLocked = track.isLocked,
                                        trackIndex = trackIndex,
                                        trackCount = project.overlayTracks.size,
                                        isMultiSelect = isMultiSelect,
                                        selectedClipIds = selectedClipIds,
                                        onToggleClipSelection = onToggleClipSelection
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                }

                                // 3. Audio Track (+ Add audio)
                                TimelineAudioTrack(
                                    tracks = project.audioTracks,
                                    msToDp = msToDp,
                                    onMoveClip = onMoveAudioClip,
                                    selectedAudioId = null,
                                    onAddAudioClick = onAddAudioClick
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                // 4. Text Track (+ Add text)
                                TimelineTextTrack(
                                    texts = project.textOverlays,
                                    selectedTextId = selectedTextId,
                                    msToDp = msToDp,
                                    onSelectText = onSelectText,
                                    onAddTextClick = onAddTextClick
                                )

                                if (project.effects.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    TimelineEffectsTrack(
                                        effects = project.effects,
                                        msToDp = msToDp
                                    )
                                }
                            }
                        }
                    }
                }

                // White Playhead Needle (Through ruler and tracks)
                TimelinePlayhead(
                    positionMs = currentPositionMs,
                    totalMs = totalMs,
                    zoom = zoom,
                    scrollState = scrollState,
                    fps = fps,
                    magneticSnap = magneticSnap,
                    snapCandidates = snapCandidates,
                    snapThresholdFrames = snapThresholdFrames,
                    onSeek = onSeek,
                    onScrubStart = beginScrub,
                    onScrubEnd = endScrub,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Snap guide line
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxSize()
                ) {
                    if (!magneticSnap || !isScrubbing) return@Canvas
                    val x = TimelineGeometry.screenXDp(
                        positionMs = currentPositionMs,
                        zoom = zoom,
                        scrollPx = scrollState.value,
                        density = density.density
                    ) * density.density
                    if (x < 0f || x > size.width) return@Canvas
                    drawLine(
                        color = Color(0xFFFFD600),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 1.6f * density.density
                    )
                }
            }
        }
    }
}
