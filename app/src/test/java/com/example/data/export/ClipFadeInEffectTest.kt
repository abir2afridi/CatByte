package com.example.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fade-in entry animation must really darken the clip: Media3 samples
 * the colour matrix once per frame, so the progress curve has to follow the
 * frame's own presentation time.
 */
class ClipFadeInEffectTest {

    private fun redScale(effect: ClipFadeInEffect, timeUs: Long): Float =
        effect.getMatrix(timeUs, false)[0]

    @Test
    fun `the clip starts black`() {
        val effect = ClipFadeInEffect(durationMs = 1_000L)
        assertEquals(0f, redScale(effect, 0L), 0.0001f)
    }

    @Test
    fun `the clip is at full brightness after the animation`() {
        val effect = ClipFadeInEffect(durationMs = 1_000L)
        assertEquals(1f, redScale(effect, 1_000_000L), 0.0001f)
    }

    @Test
    fun `the fade is halfway at half the duration`() {
        val effect = ClipFadeInEffect(durationMs = 1_000L)
        assertEquals(0.5f, redScale(effect, 500_000L), 0.0001f)
    }

    @Test
    fun `the green and blue channels fade with the red one`() {
        val effect = ClipFadeInEffect(durationMs = 1_000L)
        val matrix = effect.getMatrix(250_000L, false)
        assertEquals(matrix[0], matrix[5], 0.0001f)
        assertEquals(matrix[0], matrix[10], 0.0001f)
    }

    @Test
    fun `the matrix only scales the colour channels`() {
        val effect = ClipFadeInEffect(durationMs = 1_000L)
        val matrix = effect.getMatrix(0L, false)
        // The alpha row and the translation column stay neutral, so the
        // shader keeps the picture's alpha untouched.
        assertEquals(1f, matrix[15], 0.0001f)
        assertEquals(0f, matrix[3], 0.0001f)
        assertEquals(0f, matrix[7], 0.0001f)
        assertEquals(0f, matrix[11], 0.0001f)
        assertEquals(0f, matrix[12], 0.0001f)
        assertEquals(0f, matrix[13], 0.0001f)
        assertEquals(0f, matrix[14], 0.0001f)
    }

    @Test
    fun `a longer animation fades more slowly`() {
        val fast = ClipFadeInEffect(durationMs = 500L)
        val slow = ClipFadeInEffect(durationMs = 2_000L)
        assertTrue("the slower fade must still be darker", redScale(slow, 500_000L) < redScale(fast, 500_000L))
    }

    @Test
    fun `an animation without a duration does nothing`() {
        val effect = ClipFadeInEffect(durationMs = 0L)
        assertTrue(effect.isNoOp(1080, 1920))
        assertFalse(ClipFadeInEffect(durationMs = 600L).isNoOp(1080, 1920))
    }
}
