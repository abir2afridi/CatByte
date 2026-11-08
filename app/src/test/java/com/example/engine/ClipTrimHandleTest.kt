package com.example.engine

import com.example.model.BezierPoint
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Inline trim handle maths: frame-quantised, source-accurate, speed-aware. */
class ClipTrimHandleTest {

    /**
     * A clip whose selected source window is shorter than the original, so both
     * trim handles have room to move.
     */
    private fun clip(
        durationMs: Long,
        originalMs: Long = 5_000L,
        selectedMs: Long = 3_000L,
        trimStartMs: Long = 0L,
        speed: Float = 1f,
        curve: SpeedCurve = SpeedCurve()
    ) = MediaClip(
        id = "clip",
        title = "Clip",
        durationMs = durationMs,
        originalDurationMs = originalMs,
        trimStartMs = trimStartMs,
        trimEndMs = selectedMs,
        speed = speed,
        speedCurve = curve
    )

    @Test
    fun `end handle grows the source trim and timeline duration`() {
        val result = ClipTrimHandle.trim(
            clip(durationMs = 3_000L),
            ClipTrimHandle.Edge.END,
            outputDeltaMs = 1_000L,
            fps = 30
        )
        assertEquals(0L, result?.trimStartMs)
        // 1000 ms quantises to 30 frames at 30 fps (30 * 33 ms).
        assertEquals(3_990L, result?.trimEndMs)
        assertEquals(3_990L, result?.durationMs)
    }

    @Test
    fun `start handle moves the trim start in and shortens the clip`() {
        val result = ClipTrimHandle.trim(
            clip(durationMs = 3_000L),
            ClipTrimHandle.Edge.START,
            outputDeltaMs = 500L,
            fps = 30
        )
        assertEquals(495L, result?.trimStartMs)
        assertEquals(3_000L, result?.trimEndMs)
        assertEquals(2_505L, result?.durationMs)
    }

    @Test
    fun `delta is quantised to whole frames`() {
        val result = ClipTrimHandle.trim(
            clip(durationMs = 3_000L),
            ClipTrimHandle.Edge.END,
            outputDeltaMs = 320L,
            fps = 30
        )
        // 320 ms rounds up to 10 frames (10 * 33 ms).
        assertEquals(3_330L, result?.trimEndMs)
        assertEquals(3_330L, result?.durationMs)
    }

    @Test
    fun `speed doubles the source distance for the same timeline drag`() {
        val result = ClipTrimHandle.trim(
            clip(durationMs = 1_500L, speed = 2f),
            ClipTrimHandle.Edge.END,
            outputDeltaMs = 1_000L,
            fps = 30
        )
        assertEquals(4_980L, result?.trimEndMs)
        assertEquals(2_490L, result?.durationMs)
    }

    @Test
    fun `trimming past the source stops at the minimum duration`() {
        val result = ClipTrimHandle.trim(
            clip(durationMs = 3_000L),
            ClipTrimHandle.Edge.START,
            outputDeltaMs = 20_000L,
            fps = 30
        )
        assertEquals(2_700L, result?.trimStartMs)
        assertEquals(300L, result?.durationMs)
    }

    @Test
    fun `outward trim after a split restores the hidden source footage`() {
        val source = MediaClip(
            id = "source",
            title = "Source",
            durationMs = 10_000L,
            originalDurationMs = 10_000L
        )
        val firstHalf = ClipSplit.split(source, 4_000L) { "second-half" }!!.first

        // Drag the end handle left: only the out-point moves, source stays.
        val shortened = ClipTrimHandle.trim(
            firstHalf,
            ClipTrimHandle.Edge.END,
            outputDeltaMs = -1_000L,
            fps = 30
        )!!
        assertEquals(3_010L, shortened.trimEndMs)
        assertEquals(3_010L, shortened.durationMs)

        // Drag it back right: the hidden footage returns.
        val restored = ClipTrimHandle.trim(
            firstHalf.copy(
                trimEndMs = shortened.trimEndMs,
                durationMs = shortened.durationMs
            ),
            ClipTrimHandle.Edge.END,
            outputDeltaMs = 1_000L,
            fps = 30
        )!!
        assertEquals(4_000L, restored.trimEndMs)
        assertEquals(4_000L, restored.durationMs)
    }

    @Test
    fun `zero delta is a no-op`() {
        assertNull(ClipTrimHandle.trim(clip(durationMs = 3_000L), ClipTrimHandle.Edge.END, 0L, 30))
    }

    @Test
    fun `velocity curves refuse inline handles`() {
        val curved = clip(
            durationMs = 3_000L,
            curve = SpeedCurve(
                preset = SpeedCurvePreset.CUSTOM,
                points = listOf(
                    BezierPoint(0f, 0.8f),
                    BezierPoint(1f, 1.4f)
                )
            )
        )
        assertNull(ClipTrimHandle.trim(curved, ClipTrimHandle.Edge.END, 1_000L, 30))
    }
}
