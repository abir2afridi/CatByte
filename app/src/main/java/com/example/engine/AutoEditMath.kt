package com.example.engine

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToLong

/**
 * Offline "auto edit" maths: scene cuts from luma histograms, a beat grid from an
 * energy envelope, and subject-box smoothing for auto-reframe/tracking.
 *
 * These are the parts of the AI features that do not need a model. The detectors
 * (MediaPipe/ONNX/Whisper) feed them; everything here runs on the JVM in tests.
 */
object SceneMath {

    fun luma(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }

    /** Normalised luma histogram; callers compare whole frames with [distance]. */
    fun histogram(pixels: IntArray, bins: Int = 16): FloatArray {
        require(bins > 0) { "bins must be > 0" }
        val hist = FloatArray(bins)
        if (pixels.isEmpty()) return hist
        for (pixel in pixels) {
            val bin = (luma(pixel) * bins).toInt().coerceIn(0, bins - 1)
            hist[bin] += 1f
        }
        val total = pixels.size.toFloat()
        for (i in hist.indices) hist[i] /= total
        return hist
    }

    /** Half L1 distance in 0..1; 0 is identical, 1 is disjoint. */
    fun distance(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        if (size == 0) return 0f
        var sum = 0f
        for (i in 0 until size) sum += abs(a[i] - b[i])
        return (sum / 2f).coerceIn(0f, 1f)
    }

    /**
     * Indices of frames that start a new shot. [minGapFrames] suppresses the
     * burst of cuts a fast pan produces.
     */
    fun detectCuts(
        frames: List<IntArray>,
        threshold: Float = 0.35f,
        minGapFrames: Int = 6,
        bins: Int = 16,
    ): List<Int> {
        if (frames.size < 2) return emptyList()
        val histograms = frames.map { histogram(it, bins) }
        val cuts = ArrayList<Int>()
        var lastCut = Int.MIN_VALUE / 2
        for (i in 1 until histograms.size) {
            val d = distance(histograms[i - 1], histograms[i])
            if (d >= threshold && i - lastCut >= minGapFrames) {
                cuts.add(i)
                lastCut = i
            }
        }
        return cuts
    }
}

object BeatGrid {

    data class Onset(val atMs: Long, val strength: Float)

    data class Grid(val bpm: Double, val beatsMs: List<Long>) {
        val beatIntervalMs: Double get() = 60000.0 / bpm
    }

    /** Peaks in a normalised energy envelope (one value per [hopMs] window). */
    fun onsets(
        energy: FloatArray,
        hopMs: Long,
        sensitivity: Float = 1.5f,
        minGapMs: Long = 120L,
    ): List<Onset> {
        if (energy.isEmpty() || hopMs <= 0L) return emptyList()
        val mean = energy.average().toFloat()
        val threshold = mean * sensitivity
        if (threshold <= 0f) return emptyList()
        val out = ArrayList<Onset>()
        var lastMs = Long.MIN_VALUE
        for (i in energy.indices) {
            val value = energy[i]
            if (value < threshold) continue
            val left = if (i > 0) energy[i - 1] else 0f
            val right = if (i < energy.size - 1) energy[i + 1] else 0f
            if (value < left || value < right) continue
            val at = i * hopMs
            if (lastMs != Long.MIN_VALUE && at - lastMs < minGapMs) continue
            out.add(Onset(at, value))
            lastMs = at
        }
        return out
    }

    /**
     * Estimates tempo from the median inter-onset interval and lays a beat grid
     * across the whole timeline. Octave errors are folded into [minBpm]..[maxBpm].
     */
    fun grid(
        onsets: List<Onset>,
        durationMs: Long,
        minBpm: Double = 60.0,
        maxBpm: Double = 200.0,
    ): Grid? {
        if (onsets.size < 4 || durationMs <= 0L) return null
        val intervals = onsets.zipWithNext { a, b -> (b.atMs - a.atMs).toDouble() }
            .filter { it > 0.0 }
            .sorted()
        if (intervals.isEmpty()) return null
        var bpm = 60_000.0 / intervals[intervals.size / 2]
        while (bpm > maxBpm && bpm / 2.0 >= minBpm) bpm /= 2.0
        while (bpm < minBpm && bpm * 2.0 <= maxBpm) bpm *= 2.0
        if (bpm < minBpm || bpm > maxBpm) return null

        val interval = 60_000.0 / bpm
        val beats = ArrayList<Long>()
        var t = onsets.first().atMs.toDouble()
        while (t <= durationMs + 0.5) {
            beats.add(t.roundToLong())
            t += interval
        }
        return Grid(bpm, beats)
    }

    fun nearestBeat(grid: Grid, atMs: Long): Long? =
        grid.beatsMs.minByOrNull { abs(it - atMs) }
}

object SubjectTrack {

    data class Box(val cx: Float, val cy: Float, val w: Float, val h: Float)

    /** Clamps a normalised box fully inside the frame. */
    fun clamp(box: Box): Box {
        val w = box.w.coerceIn(0.02f, 1f)
        val h = box.h.coerceIn(0.02f, 1f)
        return Box(
            cx = box.cx.coerceIn(w / 2f, 1f - w / 2f),
            cy = box.cy.coerceIn(h / 2f, 1f - h / 2f),
            w = w,
            h = h,
        )
    }

    /**
     * Exponential smoothing with a deadband, which is what stops a tracked
     * subject from jittering the crop on every frame.
     */
    fun smooth(
        previous: Box?,
        next: Box,
        smoothing: Float = 0.25f,
        deadband: Float = 0.01f,
    ): Box {
        val p = previous ?: return clamp(next)
        val k = smoothing.coerceIn(0f, 1f)
        fun follow(from: Float, to: Float): Float =
            if (abs(to - from) < deadband) from else from + (to - from) * k
        return clamp(
            Box(
                cx = follow(p.cx, next.cx),
                cy = follow(p.cy, next.cy),
                w = follow(p.w, next.w),
                h = follow(p.h, next.h),
            )
        )
    }

    /** A crop window of [aspect] around a subject, never smaller than [minWidth]. */
    fun cropWindow(box: Box, aspect: Float, minWidth: Float = 0.2f): Box {
        require(aspect > 0f) { "aspect must be > 0" }
        var width = maxOf(minWidth, box.w * 1.2f)
        var height = width / aspect
        if (height > 1f) {
            height = 1f
            width = aspect
        }
        if (width > 1f) {
            width = 1f
            height = 1f / aspect
        }
        return clamp(Box(box.cx, box.cy, width, height))
    }

    fun distance(a: Box, b: Box): Float = hypot(a.cx - b.cx, a.cy - b.cy)
}
