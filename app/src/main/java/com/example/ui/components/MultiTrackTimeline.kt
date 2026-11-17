package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.engine.CenterPlayhead
import com.example.engine.ClipTrimHandle
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import com.example.model.TimelineEffect
import com.example.model.TimelineTrackIds
import com.example.model.VideoProject

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
    /**
     * Live playhead. Passed as a State on purpose: reading it directly in this
     * composable's body would re-compose every track clip ~60x/s while playing
     * (the "timeline moves with a lag" complaint). It is only needed by the two
     * scroll-sync effects below, so the State is read inside them.
     */
    currentPositionMs: State<Long>,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedTextId: String?,
    selectedAudioId: String? = null,
    zoom: Float,
    magneticSnap: Boolean = true,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onSelectAudio: (String) -> Unit = {},
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
    /** Tapping empty timeline space clears the clip selection. */
    onClearSelection: () -> Unit = {},
    onAddAudioClick: () -> Unit = {},
    onAddTextClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    /** Tapping a placed effect opens the duration/intensity editor. */
    onEditEffect: (TimelineEffect) -> Unit = {},
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

    val snapCandidates = remember(project, fps, beats) {
        timelineSnapCandidates(project, fps, beats)
    }
    val snapThresholdFrames = remember(pxPerFrameDp) {
        SnapEngine.thresholdFrames(TimelineGeometry.SNAP_THRESHOLD_DP, pxPerFrameDp)
    }

    TimelinePositionScrollSync(
        currentPositionMs = currentPositionMs,
        scrollState = scrollState,
        pxPerFramePx = pxPerFramePx,
        viewportWidthPx = viewportWidthPx,
        timelineWidthPx = timelineWidthPx,
        fps = fps,
        programmaticScroll = { programmaticScroll = it }
    )

    TimelineScrollSeekSync(
        currentPositionMs = currentPositionMs,
        scrollState = scrollState,
        pxPerFramePx = pxPerFramePx,
        viewportWidthPx = viewportWidthPx,
        timelineWidthPx = timelineWidthPx,
        fps = fps,
        totalMs = totalMs,
        isProgrammaticScroll = { programmaticScroll },
        isPlaying = isPlaying,
        onSeek = onSeek
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0F))
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
                audioTracks = project.audioTracks,
                onToggleAudioMute = onToggleAudioMute,
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
                    .timelineViewportGestures(
                        totalMs = totalMs,
                        fps = fps,
                        zoom = zoom,
                        viewportWidthPx = viewportWidthPx,
                        needleInsetPx = needleInsetPx,
                        msToDp = msToDp,
                        scrollState = scrollState,
                        magneticSnap = magneticSnap,
                        snapCandidates = snapCandidates,
                        snapThresholdFrames = snapThresholdFrames,
                        onUserInteraction = onUserInteraction,
                        onZoomChange = onZoomChange,
                        onSeek = onSeek,
                        onClearSelection = onClearSelection
                    )
                    .onSizeChanged { viewportWidthPx = it.width }
            ) {
                TimelineRulerStrip(
                    totalMs = totalMs,
                    zoom = zoom,
                    beats = beats,
                    markers = project.markers,
                    scrollState = scrollState,
                    leadingPadDp = leadingPadDp,
                    trailingPadDp = trailingPadDp,
                    timelineWidthDp = timelineWidthDp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                )

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
                                TimelineTracksContent(
                                    project = project,
                                    onEditEffect = onEditEffect,
                                    selectedClipId = selectedClipId,
                                    selectedTextId = selectedTextId,
                                    selectedAudioId = selectedAudioId,
                                    msToDp = msToDp,
                                    fps = fps,
                                    isMultiSelect = isMultiSelect,
                                    selectedClipIds = selectedClipIds,
                                    onSelectClip = onSelectClip,
                                    onTransitionClick = onTransitionClick,
                                    onAddMediaClick = onAddMediaClick,
                                    onMoveClip = onMoveClip,
                                    onMoveOverlayClip = onMoveOverlayClip,
                                    onMoveOverlayClipToTrack = onMoveOverlayClipToTrack,
                                    onTrimStart = onTrimStart,
                                    onTrimPreview = onTrimPreview,
                                    onTrimEnd = onTrimEnd,
                                    onMoveAudioClip = onMoveAudioClip,
                                    onSelectAudio = onSelectAudio,
                                    onToggleAudioMute = onToggleAudioMute,
                                    onToggleClipSelection = onToggleClipSelection,
                                    onSelectText = onSelectText,
                                    onAddAudioClick = onAddAudioClick,
                                    onAddTextClick = onAddTextClick
                                )
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
