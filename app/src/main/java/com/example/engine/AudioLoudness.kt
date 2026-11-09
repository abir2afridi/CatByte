package com.example.engine

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Loudness normalisation for the per-clip Volume panel.
 *
 * CapCut's "Loudness adjustment" switch raises or lowers a clip so quiet
 * recordings sit near a target level. We derive that gain from the clip's
 * decoded waveform instead of guessing: the waveform is a compact RMS/peak
 * envelope, so the same maths works in preview and export (the gain is written
 * to [com.example.model.MediaClip.volume], which both paths already read).
 *
 * The maths is intentionally conservative: at most +12 dB of boost and never
 * below a quarter of the original level, so a silent or malformed waveform can
 * never produce a runaway gain.
 */
object AudioLoudness {

    /** Target RMS for speech/music mixed at normal listening level. */
    const val DEFAULT_TARGET_RMS = 0.20f

    /** Maximum automatic boost, matching the CapCut panel's 0-1000% range. */
    const val MAX_BOOST_DB = 12f

    const val MIN_GAIN = 0.25f

    /** RMS of a waveform envelope. Returns 0 when the waveform is empty. */
    fun rms(waveform: List<Float>): Float {
        val usable = waveform.filter { it.isFinite() && it > 0f }
        if (usable.isEmpty()) return 0f
        val meanSquare = usable.sumOf { (it * it).toDouble() } / usable.size
        return sqrt(meanSquare).toFloat()
    }

    /**
     * Gain that moves [waveform] to [targetRms]. Returns 1 (no change) when the
     * clip has no analysable waveform yet, so the UI can show "no data" instead
     * of applying a bogus boost.
     */
    fun gainFor(
        waveform: List<Float>,
        targetRms: Float = DEFAULT_TARGET_RMS
    ): Float {
        val level = rms(waveform)
        if (level < 1e-4f) return 1f
        val maxGain = 10f.pow(MAX_BOOST_DB / 20f)
        return (targetRms / level).coerceIn(MIN_GAIN, maxGain)
    }

    /** True when the clip has enough waveform data to normalise. */
    fun canNormalise(waveform: List<Float>): Boolean = rms(waveform) >= 1e-4f
}
