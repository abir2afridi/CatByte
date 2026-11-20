package com.example.ui.components

import com.example.model.EffectType
import com.example.model.TimelineEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * The effects track's lane packing: overlapping effects land in separate
 * lanes (both stay visible), non-overlapping ones share a lane. Overlap in
 * time is legal and never merged away.
 */
class EffectLanesTest {

    private fun effect(startMs: Long, durationMs: Long, id: String = UUID.randomUUID().toString()) =
        TimelineEffect(
            id = id,
            name = "fx-$startMs",
            effectType = EffectType.RGB_SPLIT,
            startTimelineMs = startMs,
            durationMs = durationMs
        )

    @Test
    fun `non-overlapping effects share one lane`() {
        val lanes = packEffectLanes(listOf(effect(0, 1_000), effect(1_000, 1_000), effect(2_000, 1_000)))
        assertEquals(1, lanes.size)
        assertEquals(3, lanes[0].size)
    }

    @Test
    fun `two overlapping effects get two lanes in start order`() {
        val a = effect(0, 2_000, id = "a")
        val b = effect(1_000, 2_000, id = "b")
        val lanes = packEffectLanes(listOf(b, a))
        assertEquals(2, lanes.size)
        assertEquals(listOf(a), lanes[0])
        assertEquals(listOf(b), lanes[1])
    }

    @Test
    fun `a freed lane is reused after the overlap ends`() {
        val a = effect(0, 1_000, id = "a")
        val b = effect(500, 1_000, id = "b")  // overlaps a -> lane 2
        val c = effect(1_600, 1_000, id = "c") // after both -> lane 1 again
        val lanes = packEffectLanes(listOf(a, b, c))
        assertEquals(2, lanes.size)
        assertEquals(listOf(a, c), lanes[0])
        assertEquals(listOf(b), lanes[1])
    }

    @Test
    fun `three-way overlap stacks three lanes`() {
        val lanes = packEffectLanes(
            listOf(effect(0, 3_000), effect(500, 3_000), effect(1_000, 3_000))
        )
        assertEquals(3, lanes.size)
        assertTrue(lanes.all { it.size == 1 })
    }

    @Test
    fun `adjacent effects pack together - end is exclusive`() {
        // b starts exactly when a ends: no overlap, same lane.
        val lanes = packEffectLanes(listOf(effect(0, 1_000), effect(1_000, 1_000)))
        assertEquals(1, lanes.size)
    }

    @Test
    fun `empty list yields no lanes`() {
        assertEquals(0, packEffectLanes(emptyList()).size)
    }

    @Test
    fun `sorting is deterministic for equal starts`() {
        val a = effect(0, 2_000, id = "aaa")
        val b = effect(0, 2_000, id = "bbb")
        val lanes = packEffectLanes(listOf(b, a))
        assertEquals(2, lanes.size)
        assertEquals("aaa", lanes[0].single().id)
        assertEquals("bbb", lanes[1].single().id)
    }
}
