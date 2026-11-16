package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Relinking must clamp to the ends of the replacement file. */
class RelinkMathTest {

    @Test
    fun `same length source keeps the clip unchanged`() {
        val timing = RelinkMath.relinkedTiming(
            durationMs = 5_000L,
            speed = 1f,
            trimStartMs = 2_000L,
            newSourceMs = 30_000L
        )
        assertEquals(2_000L, timing.trimStartMs)
        assertEquals(5_000L, timing.durationMs)
    }

    @Test
    fun `shorter source shortens the clip`() {
        val timing = RelinkMath.relinkedTiming(
            durationMs = 8_000L,
            speed = 1f,
            trimStartMs = 1_000L,
            newSourceMs = 4_000L
        )
        assertEquals(1_000L, timing.trimStartMs)
        assertEquals(3_000L, timing.durationMs)
    }

    @Test
    fun `trim start is clamped inside the new source`() {
        val timing = RelinkMath.relinkedTiming(
            durationMs = 4_000L,
            speed = 1f,
            trimStartMs = 20_000L,
            newSourceMs = 6_000L
        )
        assertEquals(5_999L, timing.trimStartMs)
        assertEquals(1L, timing.durationMs)
    }

    @Test
    fun `speed is respected when clamping`() {
        // 4 s of timeline at 2x uses 8 s of source; only 5 s is available.
        val timing = RelinkMath.relinkedTiming(
            durationMs = 4_000L,
            speed = 2f,
            trimStartMs = 0L,
            newSourceMs = 5_000L
        )
        assertEquals(2_500L, timing.durationMs)
    }

    @Test
    fun `invalid speed falls back to normal speed`() {
        val timing = RelinkMath.relinkedTiming(
            durationMs = 1_000L,
            speed = 0f,
            trimStartMs = 0L,
            newSourceMs = 2_000L
        )
        assertEquals(1_000L, timing.durationMs)
    }

    @Test
    fun `zero length source still yields a drawable clip`() {
        val timing = RelinkMath.relinkedTiming(
            durationMs = 3_000L,
            speed = 1f,
            trimStartMs = 500L,
            newSourceMs = 0L
        )
        assertEquals(0L, timing.trimStartMs)
        assertEquals(1L, timing.durationMs)
    }
}
