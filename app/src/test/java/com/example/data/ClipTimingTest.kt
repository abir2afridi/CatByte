package com.example.data

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.example.model.BezierPoint
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

/**
 * Timing is the contract between the editor timeline, the live composition
 * preview and the exported MP4. These tests pin down trim/speed retiming and
 * the normalised [ClipSpeedProvider] so preview/export drift cannot regress.
 */
@UnstableApi
class ClipTimingTest {

    private fun clip(
        durationMs: Long = 2000L,
        originalDurationMs: Long = 5000L,
        trimStartMs: Long = 500L,
        trimEndMs: Long = 4500L,
        speed: Float = 1f,
        speedCurve: SpeedCurve = SpeedCurve()
    ) = MediaClip(
        id = "clip",
        title = "Clip",
        durationMs = durationMs,
        originalDurationMs = originalDurationMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        speed = speed,
        speedCurve = speedCurve
    )

    @Test
    fun `source length honours trim handles`() {
        assertEquals(4000L, ClipTiming.sourceLengthMs(clip()))
        assertEquals(5000L, ClipTiming.sourceLengthMs(clip(trimStartMs = 0L, trimEndMs = 0L)))
        assertEquals(1L, ClipTiming.sourceLengthMs(clip(trimStartMs = 5000L, trimEndMs = 5000L)))
    }

    @Test
    fun `constant speed retimes the timeline length`() {
        assertEquals(2000L, ClipTiming.presentationDurationMs(clip(speed = 2f)))
        assertEquals(8000L, ClipTiming.presentationDurationMs(clip(speed = 0.5f)))
        // Extremely fast speeds still keep a usable timeline clip.
        assertEquals(ClipTiming.MIN_CLIP_MS, ClipTiming.presentationDurationMs(clip(speed = 32f)))
    }

    @Test
    fun `velocity curve output uses the same integration as the provider`() {
        val curve = SpeedCurve(
            preset = SpeedCurvePreset.CUSTOM,
            points = listOf(BezierPoint(0f, 0.5f), BezierPoint(1f, 2f))
        )
        val sourceUs = 4_000_000L
        val expected = ClipTiming.curveOutputUs(sourceUs, curve)
        val provider = ClipSpeedProvider(
            clip(durationMs = (expected / 1000L).coerceAtLeast(ClipTiming.MIN_CLIP_MS), speedCurve = curve),
            sourceUs
        )

        assertEquals(expected.toDouble(), integratedOutputUs(provider, sourceUs), 2_000.0)
        assertTrue(kotlin.math.abs(expected - sourceUs) > 100_000L)
    }

    @Test
    fun `provider is normalised to the stored timeline duration`() {
        val curve = SpeedCurve(
            preset = SpeedCurvePreset.CUSTOM,
            points = listOf(BezierPoint(0f, 0.4f), BezierPoint(1f, 0.4f))
        )
        val sourceUs = 5_000_000L
        val storedDurationMs = 2_500L
        val provider = ClipSpeedProvider(
            clip(durationMs = storedDurationMs, speedCurve = curve),
            sourceUs
        )

        val output = integratedOutputUs(provider, sourceUs)
        assertEquals(storedDurationMs * 1000.0, output, storedDurationMs * 1000.0 * 0.02)
    }

    @Test
    fun `speed change timestamps are source-relative and end correctly`() {
        val curve = SpeedCurve(
            preset = SpeedCurvePreset.CUSTOM,
            points = listOf(BezierPoint(0f, 1f), BezierPoint(1f, 4f))
        )
        val provider = ClipSpeedProvider(clip(speedCurve = curve), 4_000_000L)

        assertEquals(40_000L, provider.getNextSpeedChangeTimeUs(0L))
        assertEquals(1_000_000L, provider.getNextSpeedChangeTimeUs(999_999L))
        assertEquals(C.TIME_UNSET, provider.getNextSpeedChangeTimeUs(3_999_999L))
    }

    @Test
    fun `standard curve is treated as constant speed`() {
        assertFalse(ClipTiming.hasSpeedCurve(SpeedCurve()))
        assertFalse(
            ClipTiming.hasSpeedCurve(
                SpeedCurve(
                    preset = SpeedCurvePreset.MONTAGE,
                    points = listOf(BezierPoint(0f, 1f), BezierPoint(1f, 1f))
                )
            )
        )
    }

    @Test
    fun `trim length helper clamps invalid ranges`() {
        assertEquals(500L, DurationMath.trimLengthMs(1000L, 500L, 500L))
        assertEquals(1000L, DurationMath.trimLengthMs(1000L, -20L, 0L))
        assertEquals(1L, DurationMath.trimLengthMs(0L, 0L, 0L))
    }

    private fun integratedOutputUs(
        provider: androidx.media3.common.audio.SpeedProvider,
        sourceUs: Long
    ): Double {
        var timeUs = 0L
        var outputUs = 0.0
        while (timeUs < sourceUs) {
            val next = min(timeUs + 40_000L, sourceUs)
            val speed = provider.getSpeed(timeUs).coerceAtLeast(0.01f)
            outputUs += (next - timeUs) / speed.toDouble()
            timeUs = next
        }
        return outputUs
    }
}
