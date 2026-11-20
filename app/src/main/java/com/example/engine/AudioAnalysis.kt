package com.example.engine

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Offline audio analysis used by the AI Lab.
 *
 * Everything here is pure maths over a decoded PCM buffer, so it is fully
 * unit-testable on the JVM: a loudness envelope, silence detection and a tempo
 * estimate built on top of [BeatGrid].
 */
object AudioAnalysis {

    /** One analysed slice of the audio. */
    data class Window(
        val startMs: Long,
        /** Root-mean-square loudness of the window, 0..1. */
        val rms: Float,
        /** Highest absolute sample in the window, 0..1. */
        val peak: Float
    )

    /** A stretch of audio that is quieter than the threshold. */
    data class Silence(val startMs: Long, val endMs: Long) {
        val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
    }

    /** The full result of [analyse]. */
    data class Analysis(
        val durationMs: Long,
        val windows: List<Window>,
        val silences: List<Silence>,
        /** Beat positions in milliseconds. */
        val beatsMs: List<Long>,
        /** Estimated tempo, or null when the audio has no usable pulse. */
        val bpm: Double?,
        val overallPeak: Float,
        val overallRms: Float
    ) {
        val beatCount: Int get() = beatsMs.size
    }

    /** Default envelope hop: 10 ms is fine enough for beat and silence work. */
    const val DEFAULT_HOP_MS = 10L

    /** Default silence threshold in dBFS relative to the loudest peak. */
    const val DEFAULT_SILENCE_THRESHOLD_DB = -45f

    /** Silences shorter than this are ignored (clicks and breath, not real gaps). */
    const val DEFAULT_MIN_SILENCE_MS = 350L

    /**
     * Splits [samples] into windows of [hopMs] and measures loudness in each.
     *
     * The window length is derived from the sample rate, so the returned list is
     * always in milliseconds and independent of the device's sample rate.
     */
    fun envelope(
        samples: FloatArray,
        sampleRate: Int,
        hopMs: Long = DEFAULT_HOP_MS
    ): List<Window> {
        require(sampleRate > 0) { "sampleRate must be positive" }
        val hop = hopMs.coerceAtLeast(1L)
        val windowSamples = (sampleRate.toLong() * hop / 1_000L).coerceAtLeast(1L).toInt()
        val windows = ArrayList<Window>()
        var index = 0
        var startMs = 0L
        while (index < samples.size) {
            val end = minOf(samples.size, index + windowSamples)
            var sum = 0.0
            var peak = 0f
            for (i in index until end) {
                val value = samples[i]
                sum += value * value
                val magnitude = abs(value)
                if (magnitude > peak) peak = magnitude
            }
            val count = end - index
            val rms = if (count > 0) sqrt(sum / count).toFloat() else 0f
            windows += Window(startMs, rms, peak)
            index = end
            startMs += hop
        }
        return windows
    }

    /**
     * Finds the stretches where the audio drops below [thresholdDb] relative to
     * the loudest peak of the whole file.
     */
    fun silences(
        windows: List<Window>,
        thresholdDb: Float = DEFAULT_SILENCE_THRESHOLD_DB,
        minSilenceMs: Long = DEFAULT_MIN_SILENCE_MS
    ): List<Silence> {
        if (windows.isEmpty()) return emptyList()
        val peak = windows.maxOf { it.peak }
        if (peak <= 0f) {
            // A completely silent file is one long silence.
            val last = windows.last()
            return listOf(Silence(0L, last.startMs + DEFAULT_HOP_MS))
        }
        val threshold = peak * amplitudeFromDb(thresholdDb)
        val out = ArrayList<Silence>()
        var start: Long? = null
        for (window in windows) {
            val quiet = window.rms <= threshold
            if (quiet && start == null) {
                start = window.startMs
            } else if (!quiet && start != null) {
                val from = start!!
                val to = window.startMs
                if (to - from >= minSilenceMs) out += Silence(from, to)
                start = null
            }
        }
        // A file that ends in silence still has a trailing gap.
        val pending = start
        if (pending != null) {
            val last = windows.last()
            val to = last.startMs + DEFAULT_HOP_MS
            if (to - pending >= minSilenceMs) out += Silence(pending, to)
        }
        return out
    }

    /**
     * Full analysis of a decoded buffer: loudness, silences and a tempo grid.
     *
     * [sensitivity] controls how strict the onset detector is: higher values
     * need a louder jump to count as a beat.
     */
    fun analyse(
        samples: FloatArray,
        sampleRate: Int,
        hopMs: Long = DEFAULT_HOP_MS,
        sensitivity: Float = 1.5f,
        silenceThresholdDb: Float = DEFAULT_SILENCE_THRESHOLD_DB,
        minSilenceMs: Long = DEFAULT_MIN_SILENCE_MS
    ): Analysis {
        val windows = envelope(samples, sampleRate, hopMs)
        val durationMs = if (sampleRate > 0 && samples.isNotEmpty()) {
            samples.size * 1_000L / sampleRate
        } else {
            0L
        }
        val quiet = silences(windows, silenceThresholdDb, minSilenceMs)
        val energy = onsetEnergy(windows)
        val onsets = BeatGrid.onsets(energy, hopMs, sensitivity, minGapMs = 120L)
        val grid = BeatGrid.grid(onsets, durationMs)
        val overallPeak = windows.maxOfOrNull { it.peak } ?: 0f
        val overallRms = if (windows.isEmpty()) {
            0f
        } else {
            windows.sumOf { it.rms.toDouble() }.toFloat() / windows.size
        }
        return Analysis(
            durationMs = durationMs,
            windows = windows,
            silences = quiet,
            beatsMs = grid?.beatsMs ?: emptyList(),
            bpm = grid?.bpm,
            overallPeak = overallPeak,
            overallRms = overallRms
        )
    }

    /**
     * Positive differences of the loudness envelope: a jump in loudness marks
     * an onset, which is what [BeatGrid.onsets] looks for.
     */
    fun onsetEnergy(windows: List<Window>): FloatArray {
        if (windows.isEmpty()) return FloatArray(0)
        val energy = FloatArray(windows.size)
        for (i in 1 until windows.size) {
            energy[i] = max(0f, windows[i].rms - windows[i - 1].rms)
        }
        return energy
    }

    /** Converts a dBFS value (-1..0) into a linear amplitude factor. */
    fun amplitudeFromDb(db: Float): Float {
        if (db <= -120f) return 0f
        return Math.pow(10.0, db.toDouble() / 20.0).toFloat()
    }

    /** Converts a linear amplitude into dBFS, clamped at -120 dB. */
    fun dbFromAmplitude(amplitude: Float): Float {
        if (amplitude <= 0f) return -120f
        return (20.0 * log10(amplitude.toDouble())).toFloat().coerceAtLeast(-120f)
    }
}
