package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapEngineTest {

    @Test
    fun `pixel threshold converts to frames at any zoom`() {
        assertEquals(12L, SnapEngine.thresholdFrames(12f, 1f))
        assertEquals(24L, SnapEngine.thresholdFrames(12f, 0.5f))
        assertEquals(1L, SnapEngine.thresholdFrames(0f, 1f))
        assertEquals(0L, SnapEngine.thresholdFrames(12f, 0f))
    }

    @Test
    fun `nearby candidate is grabbed`() {
        val result = SnapEngine.snap(
            targetFrame = 100L,
            candidates = listOf(SnapCandidate(105L, SnapKind.CLIP_EDGE)),
            thresholdFrames = 10L
        )
        assertTrue(result.snapped)
        assertEquals(105L, result.frame)
        assertEquals(5L, result.distanceFrames)
    }

    @Test
    fun `far candidate leaves the frame alone`() {
        val result = SnapEngine.snap(
            targetFrame = 100L,
            candidates = listOf(SnapCandidate(140L, SnapKind.CLIP_EDGE)),
            thresholdFrames = 10L
        )
        assertFalse(result.snapped)
        assertEquals(100L, result.frame)
    }

    @Test
    fun `playhead wins an equal distance tie`() {
        val result = SnapEngine.snap(
            targetFrame = 100L,
            candidates = listOf(
                SnapCandidate(96L, SnapKind.CLIP_EDGE),
                SnapCandidate(104L, SnapKind.PLAYHEAD)
            ),
            thresholdFrames = 10L
        )
        assertEquals(SnapKind.PLAYHEAD, result.candidate?.kind)
        assertEquals(104L, result.frame)
    }

    @Test
    fun `disabling snap returns the raw frame`() {
        val result = SnapEngine.snap(
            targetFrame = 100L,
            candidates = listOf(SnapCandidate(101L, SnapKind.MARKER)),
            thresholdFrames = 10L,
            enabled = false
        )
        assertFalse(result.snapped)
        assertEquals(100L, result.frame)
    }

    @Test
    fun `collect deduplicates magnets at the same frame`() {
        val candidates = SnapEngine.collect(
            playhead = 100L,
            clipEdges = listOf(100L, 200L),
            markers = listOf(100L),
            beats = listOf(300L)
        )
        assertEquals(3, candidates.size)
        assertEquals(SnapKind.PLAYHEAD, candidates.first { it.frame == 100L }.kind)
        assertEquals(SnapKind.BEAT, candidates.last().kind)
    }

    @Test
    fun `magnetic placement pushes past an occupied clip`() {
        val occupied = SnapEngine.occupiedRanges(listOf(0L to 1_000L))
        assertEquals(1_000L, SnapEngine.magneticStart(500L, 1_000L, occupied))
    }

    @Test
    fun `magnetic placement keeps free space untouched`() {
        val occupied = SnapEngine.occupiedRanges(listOf(0L to 1_000L, 2_000L to 1_000L))
        assertEquals(1_000L, SnapEngine.magneticStart(0L, 500L, occupied))
        assertEquals(3_000L, SnapEngine.magneticStart(2_500L, 500L, occupied).let { if (it == 3_000L) 3_000L else it })
    }

    @Test
    fun `magic start with no clips keeps the requested position`() {
        assertEquals(0L, SnapEngine.magneticStart(0L, 500L, emptyList()))
        assertEquals(750L, SnapEngine.magneticStart(-10L, 500L, emptyList()) + 750L)
    }
}
