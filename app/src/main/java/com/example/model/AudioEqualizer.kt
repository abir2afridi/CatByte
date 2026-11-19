package com.example.model

import kotlin.math.abs

/**
 * Three-band equaliser for an independent audio track.
 *
 * The bands are real biquad sections that run offline on the device:
 * a low shelf around 200 Hz, a peaking band at 1 kHz and a high shelf around
 * 4 kHz. Gains are in dB and clamped to ±[MAX_GAIN_DB]; a flat EQ is a no-op,
 * so a track that never touched the mixer pays for no processor at all.
 *
 * The value is stored on [AudioClip.equalizer], so the shared composition
 * factory feeds the exact same filters to the preview and to the exported
 * file.
 */
data class AudioEqualizer(
    val lowGainDb: Float = 0f,
    val midGainDb: Float = 0f,
    val highGainDb: Float = 0f
) {
    /** True when every band is at 0 dB, i.e. the signal needs no filtering. */
    val isFlat: Boolean
        get() = lowGainDb == 0f && midGainDb == 0f && highGainDb == 0f

    /** Returns this EQ with finite, in-range gains; near-zero snaps to flat. */
    fun sanitized(): AudioEqualizer = AudioEqualizer(
        lowGainDb = clampGain(lowGainDb),
        midGainDb = clampGain(midGainDb),
        highGainDb = clampGain(highGainDb)
    )

    companion object {
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f

        /** Corner of the low shelf. */
        const val LOW_SHELF_HZ = 200f

        /** Centre of the mid peaking band. */
        const val MID_PEAK_HZ = 1_000f
        const val MID_Q = 0.9f

        /** Corner of the high shelf. */
        const val HIGH_SHELF_HZ = 4_000f

        /** Clamps one band gain; NaN/infinity fall back to flat. */
        fun clampGain(db: Float): Float {
            if (!db.isFinite()) return 0f
            val clamped = db.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            // A hundredth of a dB is inaudible; snapping keeps the model
            // canonical, so a flat EQ stays byte-identical across saves.
            return if (abs(clamped) < 0.05f) 0f else clamped
        }

        /** Canonicalises an EQ value from persisted or UI input. */
        fun clamp(equalizer: AudioEqualizer): AudioEqualizer = equalizer.sanitized()
    }
}
