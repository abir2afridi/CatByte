package com.example.engine

import com.example.model.ClipAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipAnimationMathTest {

    @Test
    fun `none is the identity transform`() {
        val transform = ClipAnimationMath.transformAt(ClipAnimation.NONE, 0L)
        assertEquals(1f, transform.scale, 0.0001f)
        assertEquals(0f, transform.rotationDeg, 0.0001f)
        assertEquals(0f, transform.translateX, 0.0001f)
        assertEquals(0f, transform.translateY, 0.0001f)
        assertFalse(ClipAnimationMath.hasMotion(ClipAnimation.NONE))
    }

    @Test
    fun `zoom settles from one and a quarter back to unity`() {
        val start = ClipAnimationMath.transformAt(ClipAnimation.ZOOM_IN, 0L)
        val end = ClipAnimationMath.transformAt(ClipAnimation.ZOOM_IN, ClipAnimationMath.DURATION_MS)
        assertEquals(1.25f, start.scale, 0.0001f)
        assertEquals(1f, end.scale, 0.0001f)
        assertTrue(ClipAnimationMath.hasMotion(ClipAnimation.ZOOM_IN))
    }

    @Test
    fun `swipe enters from off canvas and lands centred`() {
        val start = ClipAnimationMath.transformAt(ClipAnimation.SWIPE_RIGHT, 0L)
        val end = ClipAnimationMath.transformAt(ClipAnimation.SWIPE_RIGHT, ClipAnimationMath.DURATION_MS)
        assertEquals(-1.6f, start.translateX, 0.0001f)
        assertEquals(0f, end.translateX, 0.0001f)
    }

    @Test
    fun `spin completes a full turn`() {
        val start = ClipAnimationMath.transformAt(ClipAnimation.SPIN, 0L)
        val mid = ClipAnimationMath.transformAt(ClipAnimation.SPIN, ClipAnimationMath.DURATION_MS / 2L)
        val end = ClipAnimationMath.transformAt(ClipAnimation.SPIN, ClipAnimationMath.DURATION_MS)
        assertEquals(360f, start.rotationDeg, 0.0001f)
        assertEquals(180f, mid.rotationDeg, 0.0001f)
        assertEquals(0f, end.rotationDeg, 0.0001f)
    }

    @Test
    fun `bounce ends at the original scale`() {
        val start = ClipAnimationMath.transformAt(ClipAnimation.BOUNCE, 0L)
        val end = ClipAnimationMath.transformAt(ClipAnimation.BOUNCE, ClipAnimationMath.DURATION_MS)
        assertEquals(1.18f, start.scale, 0.0001f)
        assertEquals(1f, end.scale, 0.0001f)
    }

    @Test
    fun `times beyond the duration clamp to the final transform`() {
        val overshoot = ClipAnimationMath.transformAt(ClipAnimation.ZOOM_IN, 5_000L)
        assertEquals(1f, overshoot.scale, 0.0001f)
    }

    @Test
    fun `fade in does not fake motion with a matrix`() {
        val transform = ClipAnimationMath.transformAt(ClipAnimation.FADE_IN, 100L)
        assertEquals(1f, transform.scale, 0.0001f)
        assertEquals(0f, transform.rotationDeg, 0.0001f)
        assertEquals(0f, transform.translateX, 0.0001f)
        assertFalse(ClipAnimationMath.hasMotion(ClipAnimation.FADE_IN))
    }

    @Test
    fun `zero duration yields the identity transform`() {
        val transform = ClipAnimationMath.transformAt(ClipAnimation.SPIN, 100L, durationMs = 0L)
        assertEquals(1f, transform.scale, 0.0001f)
        assertEquals(0f, transform.rotationDeg, 0.0001f)
    }
}
