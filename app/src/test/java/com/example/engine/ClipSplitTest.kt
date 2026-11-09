package com.example.engine

import com.example.model.BezierPoint
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipSplitTest {

    private var ids = 0
    private val idFactory: () -> String = { "split-${++ids}" }

    @Test
    fun `constant speed clip splits at exact timeline offset`() {
        val clip = MediaClip(
            id = "a",
            title = "clip",
            durationMs = 3_000L,
            originalDurationMs = 3_000L
        )
        val result = ClipSplit.split(clip, 1_200L, idFactory)

        assertNotNull(result)
        result!!
        assertEquals(1_200L, result.first.durationMs)
        assertEquals(1_800L, result.second.durationMs)
        assertEquals(0L, result.first.trimStartMs)
        assertEquals(1_200L, result.first.trimEndMs)
        assertEquals(1_200L, result.second.trimStartMs)
        assertEquals(3_000L, result.second.trimEndMs)
        assertEquals(3_000L, result.first.durationMs + result.second.durationMs)
    }

    @Test
    fun `sped up clip splits at the matching source frame`() {
        val clip = MediaClip(
            id = "fast",
            title = "fast",
            durationMs = 2_000L,
            originalDurationMs = 4_000L,
            trimStartMs = 1_000L,
            trimEndMs = 4_000L,
            speed = 2f
        )
        // 1000ms of output at 2x consumes 2000ms of source.
        val result = ClipSplit.split(clip, 1_000L, idFactory)!!
        assertEquals(1_000L, result.first.durationMs)
        assertEquals(1_000L, result.first.trimStartMs)
        assertEquals(3_000L, result.first.trimEndMs)
        assertEquals(3_000L, result.second.trimStartMs)
        assertEquals(4_000L, result.second.trimEndMs)
    }

    @Test
    fun `speed curve split consumes the right amount of source`() {
        val clip = MediaClip(
            id = "curve",
            title = "curve",
            durationMs = 4_000L,
            originalDurationMs = 4_000L,
            speedCurve = SpeedCurve(
                preset = SpeedCurvePreset.CUSTOM,
                points = listOf(
                    BezierPoint(0f, 2f),
                    BezierPoint(1f, 2f)
                )
            )
        )
        val offset = com.example.data.ClipTiming.sourceOffsetForOutputMs(clip, 1_000L)
        assertTrue("expected ~2000ms of source, got $offset", offset in 1_900L..2_100L)
    }

    @Test
    fun `split near either edge is refused`() {
        val clip = MediaClip(id = "a", title = "clip", durationMs = 1_000L)
        assertNull(ClipSplit.split(clip, 50L, idFactory))
        assertNull(ClipSplit.split(clip, 950L, idFactory))
        assertNull(ClipSplit.split(clip, 0L, idFactory))
        assertNull(ClipSplit.split(clip, 1_000L, idFactory))
    }

    @Test
    fun `keyframes are partitioned and the second half is rebased`() {
        val clip = MediaClip(
            id = "a",
            title = "clip",
            durationMs = 2_000L,
            keyframes = listOf(
                Keyframe(timestampMs = 500L, property = KeyframeProperty.SCALE, value = 1.2f),
                Keyframe(timestampMs = 1_500L, property = KeyframeProperty.OPACITY, value = 0.5f)
            )
        )
        val result = ClipSplit.split(clip, 1_000L, idFactory)!!
        assertEquals(listOf(500L), result.first.keyframes.map { it.timestampMs })
        assertEquals(listOf(500L), result.second.keyframes.map { it.timestampMs })
        assertEquals(KeyframeProperty.OPACITY, result.second.keyframes.single().property)
    }

    @Test
    fun `transition stays on the outgoing half only`() {
        val clip = MediaClip(
            id = "a",
            title = "clip",
            durationMs = 2_000L,
            transition = TransitionType.DISSOLVE
        )
        val result = ClipSplit.split(clip, 1_000L, idFactory)!!
        assertEquals(TransitionType.NONE, result.first.transition)
        assertEquals(TransitionType.DISSOLVE, result.second.transition)
    }
}

