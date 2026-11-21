package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.example.engine.CenterPlayhead
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

/** Throttle for scrub-while-playing seeks (the auto-scroll must breathe). */
private const val LIVE_SEEK_INTERVAL_MS = 250L

/**
 * Position -> scroll: keep the current frame under the fixed needle. Skipped
 * while the user is dragging/flinging; when the finger lifts the same flow
 * re-emits with inProgress = false and settles the needle.
 */
@Composable
internal fun TimelinePositionScrollSync(
    currentPositionMs: State<Long>,
    scrollState: ScrollState,
    pxPerFramePx: Float,
    viewportWidthPx: Int,
    timelineWidthPx: Float,
    fps: Int,
    programmaticScroll: (Boolean) -> Unit
) {
    LaunchedEffect(pxPerFramePx, viewportWidthPx, timelineWidthPx, fps, currentPositionMs) {
        if (viewportWidthPx <= 0 || pxPerFramePx <= 0f) return@LaunchedEffect
        snapshotFlow { currentPositionMs.value to scrollState.isScrollInProgress }
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
                programmaticScroll(true)
                try {
                    scrollState.scrollTo(target.roundToInt())
                } finally {
                    programmaticScroll(false)
                }
            }
    }
}

/**
 * Scroll -> position: the frame under the needle while the user scrolls. This
 * is the whole scrub gesture now that the playhead cannot move.
 */
@Composable
internal fun TimelineScrollSeekSync(
    currentPositionMs: State<Long>,
    scrollState: ScrollState,
    pxPerFramePx: Float,
    viewportWidthPx: Int,
    timelineWidthPx: Float,
    fps: Int,
    totalMs: Long,
    isProgrammaticScroll: () -> Boolean,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit
) {
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestOnSeek by rememberUpdatedState(onSeek)
    LaunchedEffect(pxPerFramePx, viewportWidthPx, timelineWidthPx, fps, totalMs, currentPositionMs) {
        if (viewportWidthPx <= 0 || pxPerFramePx <= 0f) return@LaunchedEffect
        var lastLiveSeekMs = 0L
        var lastLiveSeekAt = 0L
        snapshotFlow { scrollState.value to scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (offset, scrollInProgress) ->
                if (!scrollInProgress || isProgrammaticScroll()) return@collect
                val expected = CenterPlayhead.offsetForFrame(
                    playheadFrame = currentPositionMs.value * fps / 1_000L,
                    pxPerFrame = pxPerFramePx,
                    timelineWidthPx = timelineWidthPx,
                    viewportWidthPx = viewportWidthPx.toFloat()
                )
                if (abs(offset - expected) < 1.5f) return@collect
                val frame = CenterPlayhead.frameAtPlayhead(offset.toFloat(), pxPerFramePx)
                val ms = (frame * 1_000L / fps).coerceIn(0L, totalMs)
                if (abs(ms - currentPositionMs.value) < 2L) return@collect
                if (latestIsPlaying) {
                    // LIVE SCRUB WHILE PLAYING: the position -> scroll sync
                    // above also runs during playback, so an unthrottled seek
                    // would queue dozens of expensive CompositionPlayer seeks
                    // per second. A 4 Hz seek keeps the preview chasing the
                    // finger without starving the encoder.
                    val now = System.currentTimeMillis()
                    if (now - lastLiveSeekAt < LIVE_SEEK_INTERVAL_MS &&
                        abs(ms - lastLiveSeekMs) < (ms * 20L).coerceAtLeast(100L)
                    ) {
                        return@collect
                    }
                    lastLiveSeekAt = now
                    lastLiveSeekMs = ms
                    latestOnSeek(ms)
                    return@collect
                }
                latestOnSeek(ms)
            }
    }
}
