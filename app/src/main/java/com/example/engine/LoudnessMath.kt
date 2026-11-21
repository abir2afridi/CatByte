package com.example.engine

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Loudness normalisation maths for the export mix.
 *
 * An honest RMS-based approximation of loudness normalisation (not a
 * full ITU-R BS.1770 K-weighted LUFS meter): the decoded PCM of every
 * sounding source is measured, combined duration-weighted, and a gain
 * drives the whole mix towards a social-platform target of -14 dBFS RMS.
 * A soft limiter in the audio chain catches the peaks the gain pushes
 * over the ceiling.
 *
 * The label is deliberate: this is "RMS normalisation to -14 dBFS", the
 * same practical result creators reach for when they say "normalise to
 * -14 LUFS" for TikTok/YouTube loudness targets.
 */
object LoudnessMath {

    /** The social-platform loudness target, in dBFS RMS. */
    const val TARGET_DBFS = -14f

    /** Never boost or cut more than this - protects wildly quiet sources. */
    const val MAX_GAIN_DB = 12f
    const val MIN_GAIN_DB = -6f

    /** Digital silence threshold: RMS below this dBFS counts as silence. */
    const val SILENCE_DBFS = -60f

    /** Ceiling for the soft limiter, as a fraction of full scale. */
    const val LIMITER_CEILING = 0.95f

    /** Integrated RMS of PCM samples, in dBFS (-999 for digital silence). */
    fun rmsDbfs(samples: FloatArray): Float {
        if (samples.isEmpty()) return -999f
        var sum = 0.0
        for (sample in samples) {
            sum += sample.toDouble() * sample.toDouble()
        }
        val rms = sqrt(sum / samples.size).toFloat()
        if (rms <= 0f) return -999f
        return (20f * log10(rms)).coerceAtMost(0f)
    }

    /**
     * Duration-weighted combination of several measured segments. Segments
     * below the silence threshold are ignored, so a silent lead-in never
     * drags the target gain up.
     */
    fun combineDbfs(measurements: List<Pair<Float, Long>>): Float {
        // (measuredDbfs, weightMs) pairs.
        var energySum = 0.0
        var weightSum = 0L
        for ((dbfs, weightMs) in measurements) {
            if (dbfs < SILENCE_DBFS || weightMs <= 0L) continue
            val linear = 10f.pow(dbfs / 10f) // power domain
            energySum += linear * weightMs
            weightSum += weightMs
        }
        if (weightSum <= 0L) return -999f
        val avgPower = (energySum / weightSum).toFloat()
        if (avgPower <= 0f) return -999f
        return 10f * log10(avgPower)
    }

    /**
     * The gain (in dB) that brings [measuredDbfs] to [TARGET_DBFS], clamped
     * to [MIN_GAIN_DB]..[MAX_GAIN_DB]. Digital silence returns 0 - there is
     * nothing to normalise.
     */
    fun normaliseGainDb(measuredDbfs: Float): Float {
        if (measuredDbfs < SILENCE_DBFS) return 0f
        val gain = TARGET_DBFS - measuredDbfs
        return gain.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
    }

    /** dB to linear gain factor. */
    fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

    /**
     * Soft-knee limiter: samples under [ceiling] pass untouched; peaks above
     * it fold back smoothly instead of hard-clipping (which sounds harsh).
     * Applied to the mixed output only when the normalisation gain pushes
     * peaks over the ceiling.
     */
    fun softLimit(sample: Float, ceiling: Float = LIMITER_CEILING): Float {
        val magnitude = abs(sample)
        if (magnitude <= ceiling) return sample
        val overflow = magnitude - ceiling
        val room = 1f - ceiling
        val limited = ceiling + room * overflow / (overflow + room)
        return if (sample < 0f) -limited else limited
    }
}
