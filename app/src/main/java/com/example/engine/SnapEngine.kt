package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * What a snapped edge is allowed to grab. Order matters: when two candidates
 * are equally close, the earlier enum wins (playhead beats a clip edge).
 */
enum class SnapKind { PLAYHEAD, MARKER, BEAT, CLIP_EDGE, GRID }

data class SnapCandidate(
    val frame: Long,
    val kind: SnapKind,
    val label: String? = null
)

data class SnapResult(
    val frame: Long,
    val candidate: SnapCandidate?,
    val distanceFrames: Long
) {
    val snapped: Boolean get() = candidate != null
}

/**
 * CapCut-style magnetic snapping.
 *
 * All distances are expressed in frames so the result is identical at every
 * zoom level, and the pixel threshold is converted once by the caller.
 */
object SnapEngine {

    /** Distance at which a frame is attracted, in frames (1e-9 if disabled). */
    fun thresholdFrames(thresholdPx: Float, pxPerFrame: Float): Long {
        if (pxPerFrame <= 0f) return 0L
        return max(1L, (thresholdPx / pxPerFrame).roundToLong())
    }

    fun snap(
        targetFrame: Long,
        candidates: List<SnapCandidate>,
        thresholdFrames: Long,
        enabled: Boolean = true
    ): SnapResult {
        if (!enabled || thresholdFrames <= 0L) return SnapResult(targetFrame, null, 0L)
        var best: SnapCandidate? = null
        var bestDistance = Long.MAX_VALUE
        for (candidate in candidates) {
            val distance = abs(candidate.frame - targetFrame)
            if (distance > thresholdFrames) continue
            if (distance < bestDistance ||
                (distance == bestDistance && best != null && candidate.kind.ordinal < best.kind.ordinal)
            ) {
                best = candidate
                bestDistance = distance
            }
        }
        return if (best == null) {
            SnapResult(targetFrame, null, 0L)
        } else {
            SnapResult(best.frame, best, bestDistance)
        }
    }

    /**
     * Every magnet in the current timeline. Duplicates at the same frame are
     * collapsed, keeping the highest priority kind.
     */
    fun collect(
        playhead: Long,
        clipEdges: List<Long> = emptyList(),
        markers: List<Long> = emptyList(),
        beats: List<Long> = emptyList()
    ): List<SnapCandidate> {
        val all = buildList {
            add(SnapCandidate(playhead, SnapKind.PLAYHEAD))
            markers.forEach { add(SnapCandidate(it, SnapKind.MARKER)) }
            beats.forEach { add(SnapCandidate(it, SnapKind.BEAT)) }
            clipEdges.forEach { add(SnapCandidate(it, SnapKind.CLIP_EDGE)) }
        }
        return all.groupBy { it.frame }
            .map { (frame, group) -> group.minByOrNull { it.kind.ordinal } ?: SnapCandidate(frame, SnapKind.GRID) }
            .sortedBy { it.frame }
    }

    /**
     * Magnetic main-track placement: the requested start is pushed to the end of
     * whatever clip it would overlap, so the main track can never grow a gap or
     * an overlap.
     */
    fun magneticStart(
        desiredStartMs: Long,
        durationMs: Long,
        occupied: List<LongRange>
    ): Long {
        if (occupied.isEmpty()) return desiredStartMs.coerceAtLeast(0L)
        val sorted = occupied.sortedBy { it.first }
        var start = desiredStartMs.coerceAtLeast(0L)
        var moved = true
        var guard = 0
        while (moved && guard < sorted.size + 1) {
            moved = false
            guard++
            val end = start + durationMs
            val collision = sorted.firstOrNull { it.first < end && start < it.last + 1 }
            if (collision != null) {
                start = collision.last + 1
                moved = true
            }
        }
        return start
    }

    /** Frame ranges for the clips of one track, used by [magneticStart]. */
    fun occupiedRanges(spans: List<Pair<Long, Long>>): List<LongRange> =
        spans.map { (start, duration) -> start until (start + duration) }
}
