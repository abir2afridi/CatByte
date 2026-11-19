package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The mixer's stereo balance maths. */
class StereoPanTest {

    @Test
    fun `the centre keeps both sides at full level`() {
        val gains = StereoPan.gains(0f)

        assertEquals(1f, gains.left, 0.0001f)
        assertEquals(1f, gains.right, 0.0001f)
    }

    @Test
    fun `panning right fades the left side out`() {
        val gains = StereoPan.gains(0.5f)

        assertEquals(0.5f, gains.left, 0.0001f)
        assertEquals(1f, gains.right, 0.0001f)
    }

    @Test
    fun `panning left fades the right side out`() {
        val gains = StereoPan.gains(-0.25f)

        assertEquals(1f, gains.left, 0.0001f)
        assertEquals(0.75f, gains.right, 0.0001f)
    }

    @Test
    fun `the balance never raises the level`() {
        listOf(-1f, -0.5f, 0f, 0.5f, 1f).forEach { pan ->
            val gains = StereoPan.gains(pan)
            assertTrue("left gain too high for $pan", gains.left <= 1f)
            assertTrue("right gain too high for $pan", gains.right <= 1f)
        }
    }

    @Test
    fun `out of range and broken values are clamped`() {
        assertEquals(1f, StereoPan.clamp(4f), 0.0001f)
        assertEquals(-1f, StereoPan.clamp(-9f), 0.0001f)
        assertEquals(0f, StereoPan.clamp(Float.NaN), 0.0001f)
        // A broken value falls back to the centre, so both sides stay audible.
        assertEquals(1f, StereoPan.gains(Float.NaN).left, 0.0001f)
        assertEquals(1f, StereoPan.gains(Float.NaN).right, 0.0001f)
    }

    @Test
    fun `the label shows the position`() {
        assertEquals("C", StereoPan.label(0f))
        assertEquals("L 40", StereoPan.label(-0.4f))
        assertEquals("R 25", StereoPan.label(0.25f))
        assertEquals("L 100", StereoPan.label(-2f))
    }
}
