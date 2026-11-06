package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * Multi-track timeline coordinator.
 *
 * Gesture responsibilities are deliberately separated so they cannot steal each
 * other's events:
 *
 * * the viewport pans with [horizontalScroll] (free timeline drag),
 * * two-finger pinch zooms the timeline (Initial-pass handler that outranks
 *   the pan, so a single finger still pans),
 * * the ruler tap/drag scrubs (inside [TimelineRuler]),
 * * the playhead needle drags (rendered outside the scroll container),
 * * clips long-press to reorder without breaking normal panning.
 *
 * Playback follow-scroll only runs while the user is not touching the timeline.
 * Any pan or scrub pauses playback first, so the timeline can never "freeze"
 * under a moving video and a cut always lands where the user puts the needle.
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
    onSetAllAudioMuted: (Boolean) -> Unit = {},
    beats: List<Long> = emptyList(),
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val totalMs = project.totalDurationMs.coerceAtLeast(TimelineGeometry.MIN_TIMELINE_MS)
    val msToDp = TimelineGeometry.dpPerMs(zoom)
    val timelineWidthDp = TimelineGeometry.contentWidthDp(totalMs, zoom)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var viewportWidthPx by remember { mutableStateOf(0) }
    var isScrubbing by remember { mutableStateOf(false) }

    val isUserDragging by scrollState.interactionSource.collectIsDraggedAsState()
    val latestInteraction by rememberUpdatedState(onUserInteraction)
    val latestOnZoom by rememberUpdatedState(onZoomChange)
    val latestZoom by rememberUpdatedState(zoom)

    val fps = project.fps.coerceIn(1, 120)
    /** dp per frame: shared by snapping and the follow-scroll maths. */
    val pxPerFrameDp = (msToDp * (1_000f / fps)).coerceAtLeast(0.0001f)
    val pxPerFramePx = pxPerFrameDp * density.density
    val contentWidthPx = timelineWidthDp * density.density

    /**
     * Magnets come from [SnapEngine] so the ruler, clip edges, text, audio and
     * effect boundaries share one priority order. The playhead itself is not a
     * magnet while scrubbing, otherwise the needle would stick to its own
     * previous position.
     */
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
        // Detected beats become the lowest-priority magnets (clip edges win).
        val beatFrames = beats.map { it * fps / 1_000L }
        SnapEngine.collect(
            playhead = -1L,
            clipEdges = edges,
            markers = emptyList(),
            beats = beatFrames
        ).filter { it.frame >= 0L }
    }
    val snapThresholdFrames = remember(pxPerFrameDp) {
        SnapEngine.thresholdFrames(TimelineGeometry.SNAP_THRESHOLD_DP, pxPerFrameDp)
    }

    // A pan under a playing video stops playback instead of fighting it.
    LaunchedEffect(isUserDragging) {
        if (isUserDragging) latestInteraction()
    }

    // Follow the playhead only while the user is not touching the timeline.
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

    // Keep the same frame under the needle when zoom or viewport size changes.
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
            .background(colors.timelineTrack)
            .border(1.2.dp, colors.borderSubtle)
            .testTag("multi_track_timeline")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.width(54.dp).fillMaxHeight()) {
                val allAudioMuted = project.audioTracks.isNotEmpty() &&
                    project.audioTracks.all { it.isMuted }
                val headerRows = buildList {
                    add(
                        TimelineHeaderRow(
                            title = "Time",
                            heightDp = 31.dp,
                            icon = Icons.Default.Schedule
                        )
                    )
                    add(
                        TimelineHeaderRow(
                            title = "Main",
                            heightDp = 67.dp,
                            isMuted = project.isMainTrackMuted,
                            isLocked = project.isMainTrackLocked,
                            isSolo = project.isMainTrackSolo,
                            canMute = true,
                            canLock = true,
                            canSolo = true,
                            onMute = {
                                onTrackMute(TimelineTrackIds.MAIN, !project.isMainTrackMuted)
                            },
                            onLock = {
                                onTrackLock(TimelineTrackIds.MAIN, !project.isMainTrackLocked)
                            },
                            onSolo = {
                                onTrackSolo(TimelineTrackIds.MAIN, !project.isMainTrackSolo)
                            }
                        )
                    )
                    project.overlayTracks.forEachIndexed { index, track ->
                        add(
                            TimelineHeaderRow(
                                title = "OV ${index + 1}",
                                heightDp = 51.dp,
                                isMuted = track.isMuted,
                                isLocked = track.isLocked,
                                isSolo = track.isSolo,
                                canMute = true,
                                canLock = true,
                                canSolo = true,
                                icon = Icons.Default.VideoLibrary,
                                onMute = { onTrackMute(track.id, !track.isMuted) },
                                onLock = { onTrackLock(track.id, !track.isLocked) },
                                onSolo = { onTrackSolo(track.id, !track.isSolo) }
                            )
                        )
                    }
                    if (project.audioTracks.isNotEmpty()) {
                        add(
                            TimelineHeaderRow(
                                title = "Audio",
                                heightDp = 39.dp,
                                isMuted = allAudioMuted,
                                canMute = true,
                                icon = Icons.Default.Headset,
                                onMute = { onSetAllAudioMuted(!allAudioMuted) }
                            )
                        )
                    }
                    if (project.textOverlays.isNotEmpty()) {
                        add(
                            TimelineHeaderRow(
                                title = "Text",
                                heightDp = 37.dp,
                                icon = Icons.Default.TextFields
                            )
                        )
                    }
                    if (project.effects.isNotEmpty()) {
                        add(
                            TimelineHeaderRow(
                                title = "FX",
                                heightDp = 30.dp,
                                icon = Icons.Default.AutoAwesome
                            )
                        )
                    }
                }
                TimelineTrackHeaders(rows = headerRows)
            }

            // Viewport: the content scrolls, the playhead stays in screen space.
            // No clipToBounds here: the scroll container already clips its own
            // content on the scroll axis, and clipping this box would cut off
            // the playhead's floating timestamp above the ruler.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Two-finger pinch zoom. Runs on the Initial pass so the
                    // scroll container never sees a partially-consumed pinch;
                    // a single finger is left alone and pans normally.
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(timelineWidthDp.dp)
                            .fillMaxHeight()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TimelineRuler(
                                totalMs = totalMs,
                                zoom = zoom,
                                fps = fps,
                                magneticSnap = magneticSnap,
                                snapCandidates = snapCandidates,
                                snapThresholdFrames = snapThresholdFrames,
                                scrollState = scrollState,
                                beats = beats,
                                onSeek = onSeek,
                                onScrubStart = beginScrub,
                                onScrubEnd = endScrub
                            )

                            Spacer(modifier = Modifier.height(3.dp))

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
                                isLocked = project.isMainTrackLocked
                            )

                            Spacer(modifier = Modifier.height(5.dp))

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
                                            val targetIndex =
                                                (trackIndex + trackShift).coerceIn(
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
                                    onTrimClip = { _, edge, deltaMs -> onTrimClip(edge, deltaMs) },
                                    fps = fps,
                                    isLocked = track.isLocked,
                                    trackIndex = trackIndex,
                                    trackCount = project.overlayTracks.size
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                            }

                            if (project.audioTracks.isNotEmpty()) {
                                TimelineAudioTrack(
                                    tracks = project.audioTracks,
                                    msToDp = msToDp
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                            }

                            if (project.textOverlays.isNotEmpty()) {
                                TimelineTextTrack(
                                    texts = project.textOverlays,
                                    selectedTextId = selectedTextId,
                                    msToDp = msToDp,
                                    onSelectText = onSelectText
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                            }

                            if (project.effects.isNotEmpty()) {
                                TimelineEffectsTrack(
                                    effects = project.effects,
                                    msToDp = msToDp
                                )
                            }
                        }
                    }
                }

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

                // Snap guide: shows the magnet the ruler/needle snapped to while
                // the user is scrubbing. It is drawn in screen space, like the
                // needle, so it stays visible while the content scrolls.
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
                        color = colors.accentYellow,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 1.6f * density.density
                    )
                }
            }
        }
    }
}
