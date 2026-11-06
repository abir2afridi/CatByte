package com.example.engine

import kotlin.math.abs

/**
 * Magnetic timeline edit maths.
 *
 * The Compose timeline owns gestures and pixels; this object owns the single
 * decision "where does the clip actually land". Keeping it here means the same
 * rules run in unit tests today and in the GL compositor's layer layout later.
 *
 * All values are milliseconds on the project timeline (frame snapping stays in
 * [SnapEngine] so the two never disagree about fps).
 */
object TimelineEdits {

    const val MIN_DURATION_MS = 100L

    data class ClipSpan(val id: String, val startMs: Long, val durationMs: Long) {
        init {
            require(startMs >= 0L) { "startMs must be >= 0 (was $startMs)" }
            require(durationMs > 0L) { "durationMs must be > 0 (was $durationMs)" }
        }

        val endMs: Long get() = startMs + durationMs

        fun overlaps(other: ClipSpan): Boolean =
            id != other.id && startMs < other.endMs && other.startMs < endMs
    }

    enum class SnapKind { NONE, PLAYHEAD, MARKER, BEAT, CLIP_START, CLIP_END }

    data class Target(val atMs: Long, val kind: SnapKind)

    data class Placement(val startMs: Long, val snappedAtMs: Long?, val kind: SnapKind) {
        val snapped: Boolean get() = kind != SnapKind.NONE
    }

    /**
     * Resolves a dragged clip to its final start time.
     *
     * Both edges are candidates: the clip start can land on a target, or its end
     * can, so a clip can be pulled until it *ends* on the playhead. The nearest
     * edge inside [thresholdMs] wins; otherwise the raw (clamped) start is used.
     */
    fun resolvePlacement(
        desiredStartMs: Long,
        durationMs: Long,
        limitMs: Long,
        targets: List<Target>,
        thresholdMs: Long,
    ): Placement {
        require(durationMs > 0L) { "durationMs must be > 0 (was $durationMs)" }
        val maxStart = (limitMs - durationMs).coerceAtLeast(0L)
        val clamped = desiredStartMs.coerceIn(0L, maxStart)

        var best: Target? = null
        var bestDistance = Long.MAX_VALUE
        for (target in targets) {
            val startEdge = target.atMs.coerceIn(0L, maxStart)
            val endEdge = (target.atMs - durationMs).coerceIn(0L, maxStart)
            val dStart = abs(clamped - startEdge)
            if (dStart <= thresholdMs && dStart < bestDistance) {
                bestDistance = dStart
                best = target
            }
            val dEnd = abs(clamped - endEdge)
            if (dEnd <= thresholdMs && dEnd < bestDistance) {
                bestDistance = dEnd
                best = target
            }
        }
        val hit = best ?: return Placement(clamped, null, SnapKind.NONE)
        val startEdge = hit.atMs.coerceIn(0L, maxStart)
        val endEdge = (hit.atMs - durationMs).coerceIn(0L, maxStart)
        val start = if (abs(clamped - startEdge) <= abs(clamped - endEdge)) startEdge else endEdge
        return Placement(start, hit.atMs, hit.kind)
    }

    /**
     * Drops [movingId] into the main track and closes every gap, CapCut style:
     * the other clips keep their order and are re-laid contiguously around the
     * new insertion point.
     */
    fun magneticPlace(
        spans: List<ClipSpan>,
        movingId: String,
        desiredStartMs: Long,
        thresholdMs: Long = 0L,
        targets: List<Target> = emptyList(),
    ): List<ClipSpan> {
        val moving = spans.firstOrNull { it.id == movingId } ?: return spans
        val others = spans.filter { it.id != movingId }.sortedBy { it.startMs }
        val total = others.sumOf { it.durationMs } + moving.durationMs
        val placement = resolvePlacement(desiredStartMs, moving.durationMs, total, targets, thresholdMs)

        var cursor = 0L
        var index = others.size
        for ((i, span) in others.withIndex()) {
            if (cursor + span.durationMs > placement.startMs) {
                index = i
                break
            }
            cursor += span.durationMs
        }

        val ordered = ArrayList<ClipSpan>(others.size + 1)
        ordered.addAll(others.take(index))
        ordered.add(moving)
        ordered.addAll(others.drop(index))

        var at = 0L
        val out = ArrayList<ClipSpan>(ordered.size)
        for (span in ordered) {
            out.add(ClipSpan(span.id, at, span.durationMs))
            at += span.durationMs
        }
        return out
    }

    /**
     * Deletes a clip. With [ripple] the gap closes (main-track default); without
     * it the hole stays so overlay/audio clips keep their absolute timing.
     */
    fun delete(spans: List<ClipSpan>, id: String, ripple: Boolean = true): List<ClipSpan> {
        if (spans.none { it.id == id }) return spans
        val remaining = spans.filter { it.id != id }.sortedBy { it.startMs }
        if (!ripple) return remaining
        var at = 0L
        val out = ArrayList<ClipSpan>(remaining.size)
        for (span in remaining) {
            out.add(ClipSpan(span.id, at, span.durationMs))
            at += span.durationMs
        }
        return out
    }

    /**
     * Deletes many clips at once. With [ripple] the gap closes (main-track
     * default); without it the holes stay so overlay/audio clips keep their
     * absolute timing.
     */
    fun deleteAll(spans: List<ClipSpan>, ids: Set<String>, ripple: Boolean = true): List<ClipSpan> {
        if (ids.isEmpty()) return spans
        val remaining = spans.filterNot { it.id in ids }.sortedBy { it.startMs }
        if (!ripple) return remaining
        var at = 0L
        val out = ArrayList<ClipSpan>(remaining.size)
        for (span in remaining) {
            out.add(ClipSpan(span.id, at, span.durationMs))
            at += span.durationMs
        }
        return out
    }

    /**
     * Moves a multi-selection by [deltaMs] without breaking its relative
     * offsets. The shift is clamped so the left-most member never goes negative.
     */
    fun shiftGroup(spans: List<ClipSpan>, ids: Set<String>, deltaMs: Long): List<ClipSpan> {
        val members = spans.filter { it.id in ids }
        if (members.isEmpty()) return spans
        val lowest = members.minOf { it.startMs }
        val applied = deltaMs.coerceAtLeast(-lowest)
        if (applied == 0L) return spans
        return spans.map { span ->
            if (span.id in ids) span.copy(startMs = span.startMs + applied) else span
        }
    }

    /**
     * Trims one clip inside its free space. Neighbours it does not touch are
     * ignored, so trimming a clip in the middle of a magnetic track is bounded by
     * the clips directly before/after it.
     */
    fun trim(
        spans: List<ClipSpan>,
        id: String,
        newStartMs: Long,
        newEndMs: Long,
        minDurationMs: Long = MIN_DURATION_MS,
    ): List<ClipSpan> {
        val clip = spans.firstOrNull { it.id == id } ?: return spans
        val others = spans.filter { it.id != id }
        val leftBound = others.filter { it.endMs <= clip.startMs }.maxOfOrNull { it.endMs } ?: 0L
        val rightBound = others.filter { it.startMs >= clip.endMs }.minOfOrNull { it.startMs }
        val latestStart = (clip.endMs - minDurationMs).coerceAtLeast(leftBound)

        var start = newStartMs.coerceIn(leftBound, latestStart)
        var end = newEndMs.coerceAtLeast(start + minDurationMs)
        if (rightBound != null) end = end.coerceAtMost(rightBound)
        if (end - start < minDurationMs) end = start + minDurationMs

        return spans.map { if (it.id == id) ClipSpan(it.id, start, end - start) else it }
    }

    fun totalDuration(spans: List<ClipSpan>): Long = spans.maxOfOrNull { it.endMs } ?: 0L

    fun firstOverlap(spans: List<ClipSpan>): Pair<ClipSpan, ClipSpan>? {
        val sorted = spans.sortedBy { it.startMs }
        for (i in 0 until sorted.size - 1) {
            if (sorted[i].overlaps(sorted[i + 1])) return sorted[i] to sorted[i + 1]
        }
        return null
    }
}
