package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.engine.CenterPlayhead
import com.example.engine.ClipTrimHandle
import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import com.example.model.TimelineTrackIds
import com.example.model.VideoProject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Multi-track timeline coordinator matching the CapCut screenshot.
 *
 * CapCut interaction model: the white playhead is FIXED at the horizontal
 * centre of the viewport and cannot be dragged. The ruler and every track
 * scroll underneath it; the time under the needle is the current position.
 * The scroll content is padded with half a viewport on both sides so time 0
 * and the end can also sit exactly under the needle.
 *
 * Two one-way syncs keep scroll and position in agreement without fighting:
 *  - user scroll  -> derive the frame under the needle -> onSeek (scrub);
 *  - position/zoom change -> scroll so that position is under the needle.
 * Each side ignores changes that the other one caused (offset equality /
 * programmatic flag), so there is no feedback loop.
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
    onTrimStart: (clipId: String, edge: ClipTrimHandle.Edge) -> Unit = { _, _ -> },
    onTrimPreview: (clipId: String, edge: ClipTrimHandle.Edge, deltaMs: Long) -> Unit = { _, _, _ -> },
    onTrimEnd: (clipId: String, edge: ClipTrimHandle.Edge) -> Unit = { _, _ -> },
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
    var gutterWidthPx by remember { mutableStateOf(0) }
    // Set while the position-change effect scrolls the timeline itself, so the
    // scroll observer below never mistakes that for a user scrub.
    var programmaticScroll by remember { mutableStateOf(false) }

    val latestInteraction by rememberUpdatedState(onUserInteraction)
    val latestOnZoom by rememberUpdatedState(onZoomChange)
    val latestZoom by rememberUpdatedState(zoom)
    val latestPositionMs by rememberUpdatedState(currentPositionMs)
    val latestOnSeek by rememberUpdatedState(onSeek)

    val fps = project.fps.coerceIn(1, 120)
    val pxPerFrameDp = (msToDp * (1_000f / fps)).coerceAtLeast(0.0001f)
    val pxPerFramePx = pxPerFrameDp * density.density
    val timelineWidthPx = timelineWidthDp * density.density

    // The needle is pinned to the centre of the WHOLE timeline panel (so it
    // lines up with the play button above it), not the centre of the
    // viewport, which starts after the fixed left gutter. The scroll content
    // is padded by the needle inset on the left and the rest of the viewport
    // on the right; the two pads always add up to exactly one viewport, so
    // `offsetForFrame` stays `frame * pxPerFrame`.
    val needleInsetPx = CenterPlayhead.needleInsetPx(
        viewportWidthPx = viewportWidthPx.toFloat(),
        gutterWidthPx = gutterWidthPx.toFloat()
    )
    val leadingPadDp = with(density) { needleInsetPx.toDp() }
    val trailingPadDp = with(density) {
        CenterPlayhead.trailingPaddingPx(
            viewportWidthPx = viewportWidthPx.toFloat(),
            needleInsetPx = needleInsetPx
        ).toDp()
    }

    // Empty strip above the ruler reserved for the playhead head (pencil or
    // gold cap). Keeps the head off the ruler so its time labels stay visible.
    val playheadHeadHeight = 24.dp

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

    val latestSnap by rememberUpdatedState(magneticSnap)
    val latestSnapCandidates by rememberUpdatedState(snapCandidates)
    val latestSnapThreshold by rememberUpdatedState(snapThresholdFrames)

    // ------------------------------------------------------------------
    // Position -> scroll: keep the current frame under the fixed needle.
    // Skipped while the user is dragging/flinging; when the finger lifts the
    // same flow re-emits with inProgress = false and settles the needle.
    // ------------------------------------------------------------------
    LaunchedEffect(pxPerFramePx, viewportWidthPx, timelineWidthPx, fps) {
        if (viewportWidthPx <= 0 || pxPerFramePx <= 0f) return@LaunchedEffect
        snapshotFlow { latestPositionMs to scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (positionMs, scrollInProgress) ->
                if (scrollInProgress) return@collect
                val target = CenterPlayhead.offsetForFrame(
                    playheadFrame = positionMs * fps / 1_000L,
                    pxPerFrame = pxPerFramePx,
                    timelineWidthPx = timelineWidthPx,
                    viewportWidthPx = viewportWidthPx.toFloat()
                )
                if (abs(target - scrollState.value) < 1f) return@collect
                programmaticScroll = true
                try {
                    scrollState.scrollTo(target.roundToInt())
                } finally {
                    programmaticScroll = false
                }
            }
    }

    // ------------------------------------------------------------------
    // Scroll -> position: the frame under the needle while the user scrolls.
    // This is the whole scrub gesture now that the playhead cannot move.
    // ------------------------------------------------------------------
    LaunchedEffect(pxPerFramePx, viewportWidthPx, timelineWidthPx, fps, totalMs) {
        if (viewportWidthPx <= 0 || pxPerFramePx <= 0f) return@LaunchedEffect
        snapshotFlow { scrollState.value to scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (offset, scrollInProgress) ->
                if (!scrollInProgress || programmaticScroll) return@collect
                val expected = CenterPlayhead.offsetForFrame(
                    playheadFrame = latestPositionMs * fps / 1_000L,
                    pxPerFrame = pxPerFramePx,
                    timelineWidthPx = timelineWidthPx,
                    viewportWidthPx = viewportWidthPx.toFloat()
                )
                if (abs(offset - expected) < 1.5f) return@collect
                val frame = CenterPlayhead.frameAtPlayhead(offset.toFloat(), pxPerFramePx)
                val ms = (frame * 1_000L / fps).coerceIn(0L, totalMs)
                if (abs(ms - latestPositionMs) >= 2L) latestOnSeek(ms)
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF101014))
            .border(0.6.dp, Color(0xFF1E2028))
            .testTag("multi_track_timeline")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = playheadHeadHeight)
        ) {
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
                modifier = Modifier
                    .verticalScroll(trackScrollState)
                    .onSizeChanged { gutterWidthPx = it.width }
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
                    .pointerInput(totalMs, fps, zoom, viewportWidthPx, needleInsetPx) {
                        // Tap on the ruler -> seek. Deliberately non-consuming:
                        // a drag must still reach horizontalScroll so the ruler
                        // slides under the fixed needle. The ruler itself has
                        // no pointer input for exactly this reason.
                        val touchSlop = viewConfiguration.touchSlop
                        val rulerHeightPx = 28.dp.toPx()
                        val pxPerMsPx = msToDp * density.density
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.position.y > rulerHeightPx) return@awaitEachGesture
                            var moved = false
                            var multiTouch = false
                            var released = false
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1) multiTouch = true
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUpIgnoreConsumed()) {
                                    released = true
                                    break
                                }
                                if (!change.pressed) break
                                if (abs(change.position.x - down.position.x) > touchSlop ||
                                    abs(change.position.y - down.position.y) > touchSlop
                                ) {
                                    moved = true
                                }
                            }
                            if (released && !moved && !multiTouch && pxPerMsPx > 0f) {
                                val contentX = scrollState.value + down.position.x
                                val padPx = needleInsetPx
                                val rawMs = ((contentX - padPx) / pxPerMsPx)
                                    .roundToLong()
                                    .coerceAtLeast(0L)
                                latestOnSeek(
                                    snapTargetMs(
                                        rawMs = rawMs,
                                        fps = fps,
                                        totalMs = totalMs,
                                        enabled = latestSnap,
                                        candidates = latestSnapCandidates,
                                        thresholdFrames = latestSnapThreshold
                                    )
                                )
                            }
                        }
                    }
                    .onSizeChanged { viewportWidthPx = it.width }
            ) {
                // Ruler pinned to top of track scroll. The leading/trailing
                // half-viewport spacers live INSIDE the scroll content.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .height(28.dp)
                        .horizontalScroll(scrollState)
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(leadingPadDp))
                        Box(modifier = Modifier.width(timelineWidthDp.dp)) {
                            TimelineRuler(
                                totalMs = totalMs,
                                zoom = zoom,
                                beats = beats,
                                markers = project.markers
                            )
                        }
                        Spacer(modifier = Modifier.width(trailingPadDp))
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
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .horizontalScroll(scrollState)
                        ) {
                            Spacer(modifier = Modifier.width(leadingPadDp))

                            Box(
                                modifier = Modifier
                                    .width(timelineWidthDp.dp)
                                    .fillMaxHeight()
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
                                        onTrimStart = onTrimStart,
                                        onTrimPreview = onTrimPreview,
                                        onTrimEnd = onTrimEnd,
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
                                            onTrimStart = onTrimStart,
                                            onTrimPreview = onTrimPreview,
                                            onTrimEnd = onTrimEnd,
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

                            Spacer(modifier = Modifier.width(trailingPadDp))
                        }
                    }
                }
            }
        }

        // Fixed centre playhead: a pure visual overlay with no pointer input,
        // so it never steals a drag from the timeline beneath. It is a
        // sibling of the whole Row (gutter + viewport), which pins the needle
        // to the centre of the panel — exactly under the play button above.
        TimelinePlayhead(
            active = scrollState.isScrollInProgress,
            headHeight = playheadHeadHeight,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .zIndex(20f)
        )
    }
}
