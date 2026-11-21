package com.example.data

import android.content.Context
import android.net.Uri
import com.example.engine.LoudnessMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Measures the sounding sources of a project and derives the linear gain
 * that normalises the mix to the social-platform loudness target
 * (-14 dBFS RMS, peak-capped at 95% full scale).
 *
 * Measurement decodes at most 60 seconds per source and is duration-weighted,
 * so a 3-minute video is normalised from a representative sample, not its
 * first frames. Failures degrade gracefully: any source that cannot be
 * decoded is skipped, and an all-silent project yields 1f (no change).
 */
object LoudnessProbe {

    private const val MEASURE_LIMIT_MS = 60_000L

    /**
     * Measures the sources as (uriString, trimStartMs, spanMs) triples and
     * returns the normalisation gain. Empty list or unmeasurable audio
     * returns 1f.
     */
    suspend fun measure(
        context: Context,
        soundingSources: List<Triple<String, Long, Long>>
    ): Float = withContext(Dispatchers.IO) {
        if (soundingSources.isEmpty()) return@withContext 1f
        val measurements = mutableListOf<Pair<Float, Long>>()
        var peakLinear = 0f

        for ((uriString, trimStartMs, spanMs) in soundingSources.distinct()) {
            val decoded = PcmDecoder.decode(
                context,
                Uri.parse(uriString),
                maxDurationMs = spanMs.coerceIn(1_000L, MEASURE_LIMIT_MS)
            ) ?: continue
            if (decoded.samples.isEmpty()) continue

            val dbfs = LoudnessMath.rmsDbfs(decoded.samples)
            val peak = decoded.samples.maxOf { kotlin.math.abs(it) }
            if (peak > peakLinear) peakLinear = peak
            val weightMs = spanMs.coerceAtMost(MEASURE_LIMIT_MS)
            measurements.add(dbfs to weightMs)
            AppLog.info(
                "LoudnessProbe",
                "Source ${uriString.takeLast(24)}: ${"%.1f".format(dbfs)} dBFS RMS, " +
                    "peak ${"%.2f".format(peak)} (weight ${weightMs}ms)"
            )
        }

        val combined = LoudnessMath.combineDbfs(measurements)
        val gainDb = LoudnessMath.normaliseGainDb(combined)
        var linear = LoudnessMath.dbToLinear(gainDb)

        // Peak cap: the gain must not push the loudest measured peak past
        // 95% full scale - for peak-heavy sources the ceiling wins over the
        // RMS target.
        if (peakLinear > 0f) {
            val peakCapDb = 20f * kotlin.math.log10(
                LoudnessMath.LIMITER_CEILING / peakLinear
            )
            if (peakCapDb < gainDb) {
                linear = LoudnessMath.dbToLinear(peakCapDb)
            }
        }

        val result = linear.coerceIn(0.25f, 4f)
        AppLog.info(
            "LoudnessProbe",
            "Combined ${"%.1f".format(combined)} dBFS -> gain ${"%.1f".format(gainDb)} dB " +
                "(peak-capped) -> linear ${"%.2f".format(result)}"
        )
        result
    }
}
