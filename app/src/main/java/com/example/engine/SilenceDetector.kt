package com.example.engine

/** Inclusive millisecond range on the timeline. */
data class TimeRangeMs(val startMs: Long, val endMs: Long) {
    val lengthMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    operator fun contains(positionMs: Long): Boolean = positionMs in startMs..endMs
}

/**
 * Silence detection for "remove dead air".
 *
 * Input is the preview waveform pyramid (one peak per bucket), so it runs on the
 * timeline's existing data instead of decoding the file again.
 */
object SilenceDetector {

    fun detect(
        peaks: List<Float>,
        durationMs: Long,
        threshold: Float = 0.08f,
        minSilenceMs: Long = 600L
    ): List<TimeRangeMs> {
        if (peaks.isEmpty() || durationMs <= 0L) return emptyList()
        val stride = durationMs.toDouble() / peaks.size.toDouble()
        val ranges = mutableListOf<TimeRangeMs>()
        var runStart = -1

        peaks.forEachIndexed { index, peak ->
            val quiet = peak <= threshold
            if (quiet && runStart < 0) runStart = index
            if (!quiet && runStart >= 0) {
                appendIfLongEnough(ranges, runStart, index, stride, minSilenceMs)
                runStart = -1
            }
        }
        if (runStart >= 0) {
            appendIfLongEnough(ranges, runStart, peaks.size, stride, minSilenceMs)
        }
        return ranges
    }

    private fun appendIfLongEnough(
        output: MutableList<TimeRangeMs>,
        startIndex: Int,
        endIndex: Int,
        stride: Double,
        minSilenceMs: Long
    ) {
        val start = (startIndex * stride).toLong()
        val end = (endIndex * stride).toLong()
        if (end - start >= minSilenceMs) {
            output += TimeRangeMs(start, end)
        }
    }

    /** Ranges that survive the cut (the inverse of [silences]). */
    fun keepRanges(silences: List<TimeRangeMs>, durationMs: Long): List<TimeRangeMs> {
        if (silences.isEmpty()) return listOf(TimeRangeMs(0L, durationMs))
        val kept = mutableListOf<TimeRangeMs>()
        var cursor = 0L
        silences.sortedBy { it.startMs }.forEach { silence ->
            val start = silence.startMs.coerceIn(0L, durationMs)
            if (start > cursor) kept += TimeRangeMs(cursor, start)
            cursor = maxOf(cursor, silence.endMs.coerceIn(0L, durationMs))
        }
        if (cursor < durationMs) kept += TimeRangeMs(cursor, durationMs)
        return kept
    }

    fun removedMs(silences: List<TimeRangeMs>): Long = silences.sumOf { it.lengthMs }

    fun isMostlySilent(silences: List<TimeRangeMs>, durationMs: Long, ratio: Float = 0.6f): Boolean {
        if (durationMs <= 0L) return false
        return removedMs(silences).toDouble() / durationMs >= ratio
    }

    /** Maps a kept range back onto the original timeline for export ranges. */
    fun toExportRanges(kept: List<TimeRangeMs>, clipStartMs: Long): List<TimeRangeMs> =
        kept.map { TimeRangeMs(it.startMs + clipStartMs, it.endMs + clipStartMs) }
}
