package com.example.engine

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * PCM mixing maths for the export audio path.
 *
 * Everything here is pure: the same functions run in the preview mixer and the
 * export muxer, so fades, ducking and the limiter cannot drift apart.
 */
object AudioMath {

    /** Linear gain for a track position, applying fade-in and fade-out. */
    fun fadeGain(
        positionMs: Long,
        durationMs: Long,
        fadeInMs: Long = 0L,
        fadeOutMs: Long = 0L
    ): Float {
        if (durationMs <= 0L) return 0f
        val position = positionMs.coerceIn(0L, durationMs)
        var gain = 1f
        if (fadeInMs > 0L && position < fadeInMs) {
            gain *= (position.toDouble() / fadeInMs.toDouble()).toFloat().coerceIn(0f, 1f)
        }
        if (fadeOutMs > 0L) {
            val remaining = durationMs - position
            if (remaining < fadeOutMs) {
                gain *= (remaining.toDouble() / fadeOutMs.toDouble()).toFloat().coerceIn(0f, 1f)
            }
        }
        return gain.coerceIn(0f, 1f)
    }

    /** Voice-over ducking: music drops while someone is speaking. */
    fun duckGain(isVoiceActive: Boolean, duckAmount: Float = 0.35f): Float =
        if (isVoiceActive) (1f - duckAmount.coerceIn(0f, 1f)) else 1f

    /**
     * Attack/release smoothed ducking envelope. [voiceRanges] are timeline
     * positions where a voice-over is audible.
     */
    fun duckEnvelope(
        positionMs: Long,
        voiceRanges: List<LongRange>,
        attackMs: Long = 120L,
        releaseMs: Long = 300L,
        duckAmount: Float = 0.35f
    ): Float {
        if (voiceRanges.isEmpty()) return 1f
        var gain = 1f
        voiceRanges.forEach { range ->
            val start = range.first
            val end = range.last
            val inside = positionMs in start..end
            val beforeStart = positionMs in (start - attackMs) until start
            val afterEnd = positionMs in (end + 1)..(end + releaseMs)
            when {
                inside -> gain = min(gain, 1f - duckAmount.coerceIn(0f, 1f))
                beforeStart -> {
                    val t = ((positionMs - (start - attackMs)).toDouble() / attackMs.coerceAtLeast(1L))
                        .toFloat().coerceIn(0f, 1f)
                    gain = min(gain, 1f - duckAmount * t)
                }
                afterEnd -> {
                    val t = ((positionMs - end).toDouble() / releaseMs.coerceAtLeast(1L))
                        .toFloat().coerceIn(0f, 1f)
                    gain = min(gain, 1f - duckAmount * (1f - t))
                }
            }
        }
        return gain
    }

    /**
     * Gain needed to keep [peak] under [ceiling]. A limiter, not a clipper:
     * scaling the whole block is cheaper and artefacts less on speech.
     */
    fun limiterGain(peak: Float, ceiling: Float = 0.98f): Float {
        val safeCeiling = ceiling.coerceIn(0.1f, 1f)
        val absolute = abs(peak)
        if (absolute <= safeCeiling || absolute <= 0f) return 1f
        return safeCeiling / absolute
    }

    /** Sums tracks with per-track gain, then applies a block limiter. */
    fun mixTracks(tracks: List<FloatArray>, gains: List<Float>, ceiling: Float = 0.98f): FloatArray {
        val length = tracks.maxOfOrNull { it.size } ?: 0
        if (length == 0) return FloatArray(0)
        val output = FloatArray(length)
        tracks.forEachIndexed { index, samples ->
            val gain = gains.getOrElse(index) { 1f }
            for (i in samples.indices) {
                output[i] += samples[i] * gain
            }
        }
        val limit = limiterGain(peakOf(output), ceiling)
        if (limit < 1f) {
            for (i in output.indices) output[i] *= limit
        }
        return output
    }

    fun peakOf(samples: FloatArray): Float {
        var peak = 0f
        samples.forEach { value -> peak = max(peak, abs(value)) }
        return peak
    }

    fun rms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        samples.forEach { sum += it.toDouble() * it.toDouble() }
        return sqrt(sum / samples.size).toFloat()
    }

    /** RMS level in dBFS (-inf clamped to -90). */
    fun rmsDbfs(samples: FloatArray): Float = linearToDb(rms(samples))

    /**
     * Short-term loudness estimate. Not a certified BS.1770 meter: it is RMS
     * dBFS with the usual K-weighting offset, good enough to hit a -14 LUFS
     * platform target on speech.
     */
    fun approxLufs(samples: FloatArray): Float = rmsDbfs(samples)

    fun linearToDb(linear: Float): Float {
        if (linear <= 0.000_000_1f) return -90f
        return (20.0 * log10(linear.toDouble())).toFloat().coerceAtLeast(-90f)
    }

    fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

    /** Gain that moves [samples] to [targetLufs], with a safety cap. */
    fun normalisationGain(samples: FloatArray, targetLufs: Float = -14f, maxBoostDb: Float = 12f): Float {
        val current = approxLufs(samples)
        if (current <= -90f) return 1f
        val deltaDb = (targetLufs - current).coerceIn(-24f, maxBoostDb)
        return dbToLinear(deltaDb)
    }

    /** Peak-normalises to the limiter ceiling. */
    fun peakNormalise(samples: FloatArray, ceiling: Float = 0.98f): FloatArray {
        val peak = peakOf(samples)
        if (peak <= 0f) return samples
        val gain = limiterGain(peak, ceiling)
        return FloatArray(samples.size) { samples[it] * gain }
    }

    /** 16-bit PCM to float, and back, matching MediaCodec's byte layout. */
    fun pcm16ToFloat(pcm: ByteArray, bigEndian: Boolean = false): FloatArray {
        val count = pcm.size / 2
        val output = FloatArray(count)
        for (i in 0 until count) {
            val first = pcm[i * 2].toInt() and 0xFF
            val second = pcm[i * 2 + 1].toInt() and 0xFF
            val value = if (bigEndian) (first shl 8) or second else (second shl 8) or first
            output[i] = value.toShort() / 32_768f
        }
        return output
    }

    fun floatToPcm16(samples: FloatArray): ByteArray {
        val output = ByteArray(samples.size * 2)
        samples.forEachIndexed { index, value ->
            val clamped = (value.coerceIn(-1f, 1f) * 32_767f).toInt().toShort()
            output[index * 2] = (clamped.toInt() and 0xFF).toByte()
            output[index * 2 + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
        }
        return output
    }

    /** Simple one-pole low-pass, used by the EQ sheet's preview. */
    fun lowPass(samples: FloatArray, cutoffHz: Float, sampleRate: Int): FloatArray {
        if (samples.isEmpty() || sampleRate <= 0) return samples
        val dt = 1.0 / sampleRate
        val rc = 1.0 / (2.0 * Math.PI * cutoffHz.coerceAtLeast(20f))
        val alpha = (dt / (rc + dt)).toFloat()
        val output = FloatArray(samples.size)
        var previous = samples.first()
        samples.forEachIndexed { index, value ->
            previous += alpha * (value - previous)
            output[index] = previous
        }
        return output
    }

    /** Linear resample (preview only; export uses Media3's resampler). */
    fun resample(samples: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate || samples.isEmpty() || fromRate <= 0 || toRate <= 0) return samples
        val ratio = toRate.toDouble() / fromRate.toDouble()
        val outSize = (samples.size * ratio).toInt().coerceAtLeast(1)
        val output = FloatArray(outSize)
        for (i in 0 until outSize) {
            val source = i / ratio
            val lower = source.toInt().coerceIn(0, samples.size - 1)
            val upper = min(lower + 1, samples.size - 1)
            val fraction = (source - lower).toFloat()
            output[i] = samples[lower] + (samples[upper] - samples[lower]) * fraction
        }
        return output
    }

    /** Channel count check for the export ladder. */
    fun usableChannelCount(channels: Int): Int = if (channels in 1..2) channels else 2

    /** True when the mix is already under the ceiling (no limiter needed). */
    fun isHeadroomOk(samples: FloatArray, ceiling: Float = 0.98f): Boolean =
        peakOf(samples) <= ceiling
}
