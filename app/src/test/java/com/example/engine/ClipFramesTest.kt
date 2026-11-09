package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Filmstrip sampling: real, different, cache-friendly frames per tile. */
class ClipFramesTest {

    @Test
    fun `tiles sample the centre of their own time span`() {
        // 10s window, 4 tiles -> centres at 1.25s, 3.75s, 6.25s, 8.75s.
        val offsets = (0 until 4).map { ClipFrames.sampleOffsetMs(10_000L, it, 4) }
        assertEquals(listOf(1_250L, 3_750L, 6_250L, 8_750L), offsets)
    }

    @Test
    fun `every tile gets a distinct increasing frame`() {
        val offsets = (0 until 6).map { ClipFrames.sampleOffsetMs(60_000L, it, 6) }
        assertEquals(offsets, offsets.sorted())
        assertEquals(offsets.size, offsets.toSet().size)
    }

    @Test
    fun `samples snap to the 500ms grid for cache reuse across zooms`() {
        // 60s window, 5 tiles: raw centres 6s, 18s, 30s, 42s, 54s.
        val offsets = (0 until 5).map { ClipFrames.sampleOffsetMs(60_000L, it, 5) }
        offsets.forEach { offset ->
            val remainder = offset % ClipFrames.SAMPLE_GRID_MS
            // Snapped samples land on grid/2 (250ms); tiny windows keep raw.
            assertTrue("offset $offset not on the cache grid", remainder == 250L || remainder == 0L)
        }
    }

    @Test
    fun `samples never reach past the end of the source window`() {
        for (count in 1..9) {
            for (index in 0 until count) {
                val offset = ClipFrames.sampleOffsetMs(1_234L, index, count)
                assertTrue(offset in 0 until 1_234L)
            }
        }
    }

    @Test
    fun `tiny windows keep the raw centre instead of snapping past the end`() {
        // 300ms window: snapping would overshoot, so the raw centre is used.
        assertEquals(75L, ClipFrames.sampleOffsetMs(300L, 0, 2))
        assertEquals(225L, ClipFrames.sampleOffsetMs(300L, 1, 2))
        assertEquals(0L, ClipFrames.sampleOffsetMs(0L, 0, 3))
        assertEquals(0L, ClipFrames.sampleOffsetMs(5_000L, 0, 0))
    }
}
