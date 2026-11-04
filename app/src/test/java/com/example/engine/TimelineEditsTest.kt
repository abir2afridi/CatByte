package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEditsTest {

    private fun span(id: String, start: Long, dur: Long) = TimelineEdits.ClipSpan(id, start, dur)

    @Test
    fun `placement snaps the clip start to the playhead`() {
        val result = TimelineEdits.resolvePlacement(
            desiredStartMs = 980,
            durationMs = 500,
            limitMs = 5_000,
            targets = listOf(TimelineEdits.Target(1_000, TimelineEdits.SnapKind.PLAYHEAD)),
            thresholdMs = 50,
        )
        assertEquals(1_000, result.startMs)
        assertEquals(TimelineEdits.SnapKind.PLAYHEAD, result.kind)
        assertEquals(1_000L, result.snappedAtMs)
        assertTrue(result.snapped)
    }

    @Test
    fun `placement can snap the clip end onto a target`() {
        val result = TimelineEdits.resolvePlacement(
            desiredStartMs = 498,
            durationMs = 500,
            limitMs = 5_000,
            targets = listOf(TimelineEdits.Target(1_000, TimelineEdits.SnapKind.MARKER)),
            thresholdMs = 50,
        )
        assertEquals(500, result.startMs)
        assertEquals(1_000L, result.snappedAtMs)
    }

    @Test
    fun `placement ignores targets outside the threshold`() {
        val result = TimelineEdits.resolvePlacement(
            desiredStartMs = 900,
            durationMs = 500,
            limitMs = 5_000,
            targets = listOf(TimelineEdits.Target(1_000, TimelineEdits.SnapKind.BEAT)),
            thresholdMs = 10,
        )
        assertEquals(900, result.startMs)
        assertNull(result.snappedAtMs)
        assertEquals(TimelineEdits.SnapKind.NONE, result.kind)
    }

    @Test
    fun `placement never pushes the clip past the timeline end`() {
        val result = TimelineEdits.resolvePlacement(
            desiredStartMs = 10_000,
            durationMs = 1_000,
            limitMs = 3_000,
            targets = emptyList(),
            thresholdMs = 0,
        )
        assertEquals(2_000, result.startMs)
    }

    @Test
    fun `magnetic place moves a clip to the front and closes gaps`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_000, 1_000), span("c", 2_000, 1_000))
        val result = TimelineEdits.magneticPlace(spans, "c", desiredStartMs = 0)
        assertEquals(listOf("c", "a", "b"), result.map { it.id })
        assertEquals(0, result[0].startMs)
        assertEquals(1_000, result[1].startMs)
        assertEquals(2_000, result[2].startMs)
    }

    @Test
    fun `magnetic place keeps the order when dropped at the end`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_000, 1_000), span("c", 2_000, 1_000))
        val result = TimelineEdits.magneticPlace(spans, "a", desiredStartMs = 2_500)
        assertEquals(listOf("b", "c", "a"), result.map { it.id })
        assertEquals(0, result.first().startMs)
        assertEquals(2_000, result.last().startMs)
    }

    @Test
    fun `ripple delete closes the gap`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_000, 1_000), span("c", 2_000, 1_000))
        val result = TimelineEdits.delete(spans, "b")
        assertEquals(listOf("a", "c"), result.map { it.id })
        assertEquals(1_000, result[1].startMs)
    }

    @Test
    fun `lift delete keeps absolute timing`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_500, 1_000))
        val result = TimelineEdits.delete(spans, "a", ripple = false)
        assertEquals(1, result.size)
        assertEquals(1_500, result[0].startMs)
    }

    @Test
    fun `group shift keeps relative offsets and clamps at zero`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 2_000, 1_000), span("c", 5_000, 500))
        val moved = TimelineEdits.shiftGroup(spans, setOf("a", "b"), 500)
        assertEquals(500, moved[0].startMs)
        assertEquals(2_500, moved[1].startMs)
        assertEquals(5_000, moved[2].startMs)

        val clamped = TimelineEdits.shiftGroup(spans, setOf("a", "b"), -9_000)
        assertEquals(0, clamped[0].startMs)
        assertEquals(2_000, clamped[1].startMs)
    }

    @Test
    fun `trim is bounded by the next clip`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_000, 1_000))
        val result = TimelineEdits.trim(spans, "a", newStartMs = 0, newEndMs = 2_500)
        assertEquals(1_000, result[0].endMs)
    }

    @Test
    fun `trim respects the minimum duration`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_000, 1_000))
        val result = TimelineEdits.trim(spans, "a", newStartMs = 2_000, newEndMs = 2_100, minDurationMs = 100)
        assertEquals(900, result[0].startMs)
        assertEquals(100, result[0].durationMs)
    }

    @Test
    fun `total duration uses the last end`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 1_500, 500))
        assertEquals(2_000, TimelineEdits.totalDuration(spans))
    }

    @Test
    fun `overlap detection finds the first clash`() {
        val spans = listOf(span("a", 0, 1_000), span("b", 500, 1_000), span("c", 3_000, 500))
        val clash = TimelineEdits.firstOverlap(spans)
        assertNotNull(clash)
        assertEquals("a", clash!!.first.id)
        assertEquals("b", clash.second.id)
        assertFalse(TimelineEdits.ClipSpan("x", 0, 100).overlaps(TimelineEdits.ClipSpan("x", 10, 100)))
    }

    @Test
    fun `negative start is rejected`() {
        assertTrue(runCatching { TimelineEdits.ClipSpan("x", -1, 100) }.isFailure)
        assertTrue(runCatching { TimelineEdits.ClipSpan("x", 0, 0) }.isFailure)
    }
}
