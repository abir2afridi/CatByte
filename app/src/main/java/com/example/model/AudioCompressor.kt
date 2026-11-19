package com.example.model

/**
 * Dynamics compressor for an independent audio track.
 *
 * The compressor is real on-device DSP: an envelope follower feeds a
 * soft-knee ratio compressor and the resulting gain reduction is smoothed
 * with an attack/release pair. The value is stored on
 * [AudioClip.compressor], so the shared composition factory feeds the exact
 * same kernel to the preview and to the exported file.
 *
 * A disabled compressor is a no-op, so a track that never touched the
 * compressor costs no processor at all.
 */
data class AudioCompressor(
    /** False = the track is not compressed at all. */
    val enabled: Boolean = false,
    /** Level above which the signal gets compressed, in dB. */
    val thresholdDb: Float = DEFAULT_THRESHOLD_DB,
    /** Input/output ratio above the threshold. 1 = no compression. */
    val ratio: Float = DEFAULT_RATIO,
    /** Extra gain after compression, in dB. */
    val makeupGainDb: Float = 0f,
    /** How fast the compressor reacts to a loud peak, in ms. */
    val attackMs: Float = DEFAULT_ATTACK_MS,
    /** How fast the compressor lets the level recover, in ms. */
    val releaseMs: Float = DEFAULT_RELEASE_MS
) {
    /** True when the track must not pay for a compressor processor. */
    val isNoOp: Boolean
        get() = !enabled

    /** Returns this compressor with finite, in-range parameters. */
    fun sanitized(): AudioCompressor = AudioCompressor(
        enabled = enabled,
        thresholdDb = clampDb(thresholdDb, MIN_THRESHOLD_DB, MAX_THRESHOLD_DB, DEFAULT_THRESHOLD_DB),
        ratio = clampRatio(ratio),
        makeupGainDb = clampDb(makeupGainDb, MIN_MAKEUP_GAIN_DB, MAX_MAKEUP_GAIN_DB, 0f),
        attackMs = clampMs(attackMs, MIN_ATTACK_MS, MAX_ATTACK_MS, DEFAULT_ATTACK_MS),
        releaseMs = clampMs(releaseMs, MIN_RELEASE_MS, MAX_RELEASE_MS, DEFAULT_RELEASE_MS)
    )

    companion object {
        const val MIN_THRESHOLD_DB = -60f
        const val MAX_THRESHOLD_DB = 0f
        const val DEFAULT_THRESHOLD_DB = -18f

        const val MIN_RATIO = 1f
        const val MAX_RATIO = 20f
        const val DEFAULT_RATIO = 3f

        const val MIN_MAKEUP_GAIN_DB = 0f
        const val MAX_MAKEUP_GAIN_DB = 18f

        const val MIN_ATTACK_MS = 0.5f
        const val MAX_ATTACK_MS = 100f
        const val DEFAULT_ATTACK_MS = 10f

        const val MIN_RELEASE_MS = 20f
        const val MAX_RELEASE_MS = 1000f
        const val DEFAULT_RELEASE_MS = 150f

        /** Width of the soft knee around the threshold, in dB. */
        const val KNEE_DB = 6f

        /** Clamps one decibel value; NaN/infinity fall back to [fallback]. */
        fun clampDb(value: Float, min: Float, max: Float, fallback: Float): Float {
            if (!value.isFinite()) return fallback.coerceIn(min, max)
            return value.coerceIn(min, max)
        }

        /** Clamps the ratio; a ratio of 1 means "no compression". */
        fun clampRatio(value: Float): Float {
            if (!value.isFinite()) return DEFAULT_RATIO
            return value.coerceIn(MIN_RATIO, MAX_RATIO)
        }

        /** Clamps a time constant in milliseconds. */
        fun clampMs(value: Float, min: Float, max: Float, fallback: Float): Float {
            if (!value.isFinite()) return fallback.coerceIn(min, max)
            return value.coerceIn(min, max)
        }

        /** Canonicalises a compressor value from persisted or UI input. */
        fun clamp(compressor: AudioCompressor): AudioCompressor = compressor.sanitized()

        /**
         * The gain reduction the compressor applies to a level, in dB.
         *
         * Below the knee the signal passes untouched, inside the knee the
         * curve bends smoothly, and above the knee the slope is `1 / ratio`.
         * The result is never negative, so the compressor can only ever
         * attenuate - the level is multiplied by `10^(-reduction/20)`.
         */
        fun gainReductionDb(
            levelDb: Float,
            thresholdDb: Float = DEFAULT_THRESHOLD_DB,
            ratio: Float = DEFAULT_RATIO,
            kneeDb: Float = KNEE_DB
        ): Float {
            val safeRatio = clampRatio(ratio)
            val slope = 1f - 1f / safeRatio
            if (slope <= 0f) return 0f
            val over = levelDb - thresholdDb
            val knee = kneeDb.coerceAtLeast(0f)
            if (knee <= 0f) {
                return if (over > 0f) over * slope else 0f
            }
            return when {
                over <= -knee / 2f -> 0f
                over >= knee / 2f -> over * slope
                else -> {
                    val t = over + knee / 2f
                    t * t * slope / (2f * knee)
                }
            }
        }
    }
}
