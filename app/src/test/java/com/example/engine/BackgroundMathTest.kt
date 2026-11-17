package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Geometry checks for the canvas background: the fitted ("contain") video
 * rectangle, the blur-fill sharp scales and the image cover mapping.
 */
class BackgroundMathTest {

    private fun assertNear(expected: Float, actual: Float, delta: Float = 1e-4f) {
        assertTrue("expected $expected ±$delta, got $actual", abs(expected - actual) <= delta)
    }

    // ------------------------------------------------------------------
    // fittedVideoRect
    // ------------------------------------------------------------------

    @Test
    fun `square video in landscape canvas pillarboxes`() {
        // 1:1 inside 16:9 (1.778): height fills, width = aspect ratio.
        val rect = BackgroundMath.fittedVideoRect(sourceAspect = 1f, canvasAspect = 16f / 9f)
        assertNear(1f, rect.height)
        assertNear((1f / (16f / 9f)), rect.width)
        assertNear((1f - rect.width) / 2f, rect.left)
        assertNear(0f, rect.top)
    }

    @Test
    fun `landscape video in portrait canvas letterboxes`() {
        // 16:9 inside 9:16 (0.5625): width fills, height = canvasAspect/sourceAspect.
        val rect = BackgroundMath.fittedVideoRect(sourceAspect = 16f / 9f, canvasAspect = 9f / 16f)
        assertNear(1f, rect.width)
        assertNear(((9f / 16f) / (16f / 9f)), rect.height)
        assertNear((1f - rect.height) / 2f, rect.top)
        assertNear(0f, rect.left)
    }

    @Test
    fun `matching aspects fill the whole canvas`() {
        val rect = BackgroundMath.fittedVideoRect(sourceAspect = 9f / 16f, canvasAspect = 9f / 16f)
        assertNear(0f, rect.left)
        assertNear(0f, rect.top)
        assertNear(1f, rect.right)
        assertNear(1f, rect.bottom)
    }

    @Test
    fun `rect stays inside the canvas and is centred`() {
        val rect = BackgroundMath.fittedVideoRect(sourceAspect = 4f, canvasAspect = 0.6f)
        assertTrue(rect.left >= 0f && rect.right <= 1f)
        assertTrue(rect.top >= 0f && rect.bottom <= 1f)
        assertNear(0.5f, (rect.left + rect.right) / 2f)
        assertNear(0.5f, (rect.top + rect.bottom) / 2f)
    }

    @Test
    fun `invalid aspects degrade to full canvas`() {
        for (bad in floatArrayOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val rect = BackgroundMath.fittedVideoRect(sourceAspect = bad, canvasAspect = 1.5f)
            assertEquals(0f, rect.left, 1e-6f)
            assertEquals(1f, rect.right, 1e-6f)
            val rect2 = BackgroundMath.fittedVideoRect(sourceAspect = 1.5f, canvasAspect = bad)
            assertEquals(0f, rect2.left, 1e-6f)
            assertEquals(1f, rect2.right, 1e-6f)
        }
    }

    // ------------------------------------------------------------------
    // blurSharpScales
    // ------------------------------------------------------------------

    @Test
    fun `blur sharp scales match the fitted rect sides`() {
        // 9:16 (0.5625) inside 1:1: sharp width = 0.5625, sharp height = 1.
        val (sx, sy) = BackgroundMath.blurSharpScales(9f / 16f, 1f)
        assertNear(9f / 16f, sx)
        assertNear(1f, sy)

        // 16:9 inside 9:16: sharp width = 1, sharp height = (9/16)/(16/9).
        val (sx2, sy2) = BackgroundMath.blurSharpScales(16f / 9f, 9f / 16f)
        assertNear(1f, sx2)
        assertNear(((9f / 16f) / (16f / 9f)), sy2)
    }

    @Test
    fun `blur sharp scales are 1 for invalid aspects`() {
        val (sx, sy) = BackgroundMath.blurSharpScales(0f, 1f)
        assertEquals(1f, sx, 1e-6f)
        assertEquals(1f, sy, 1e-6f)
    }

    // ------------------------------------------------------------------
    // coverMappedUv
    // ------------------------------------------------------------------

    @Test
    fun `cover mapping keeps the centre fixed`() {
        val (u, v) = BackgroundMath.coverMappedUv(0.5f, 0.5f, 1.778f, 1.333f)
        assertNear(0.5f, u)
        assertNear(0.5f, v)
    }

    @Test
    fun `wider image crops the sides`() {
        // Image aspect (2.0) > canvas aspect (1.0): u compresses, v untouched.
        val (u, v) = BackgroundMath.coverMappedUv(0f, 0.75f, 1f, 2f)
        assertNear(0.25f, u)
        assertNear(0.75f, v)
    }

    @Test
    fun `taller image crops the top and bottom`() {
        // Image aspect (0.5) < canvas aspect (1.0): the image is scaled to cover
        // the canvas width, so the canvas samples a CENTRED vertical band of the
        // image - canvas v=0 reads image v=0.25, not 0.5 (that would be no crop).
        val (u, v) = BackgroundMath.coverMappedUv(0.25f, 0f, 1f, 0.5f)
        assertNear(0.25f, u)
        assertNear(0.25f, v)
    }

    @Test
    fun `invalid aspects return identity mapping`() {
        val (u, v) = BackgroundMath.coverMappedUv(0.3f, 0.8f, 0f, 2f)
        assertNear(0.3f, u)
        assertNear(0.8f, v)
    }

    @Test
    fun `contain fit picks the smaller scale factor`() {
        // 9:16 source into a square canvas: the height limits (0.375), so the
        // video becomes 405x720 with pillarbox bars.
        assertEquals(0.375f, BackgroundMath.containFitScale(1080, 1920, 720, 720), 0.0001f)
        // 16:9 source into a square canvas: height is the limiting edge.
        assertEquals(0.5625f, BackgroundMath.containFitScale(1280, 720, 720, 720), 0.0001f)
        // Upscaling is allowed - the compositor scales up small sources.
        assertEquals(2f, BackgroundMath.containFitScale(360, 640, 720, 1280), 0.0001f)
    }

    @Test
    fun `contain fit degrades to no scaling for unusable sizes`() {
        assertEquals(1f, BackgroundMath.containFitScale(0, 100, 100, 100))
        assertEquals(1f, BackgroundMath.containFitScale(100, 100, 0, 100))
        assertEquals(1f, BackgroundMath.containFitScale(-5, 100, 100, 100))
    }
}
