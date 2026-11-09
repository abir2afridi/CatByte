package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasCropTest {

    @Test
    fun `rotation normalisation wraps into plus minus 180`() {
        assertEquals(-170f, CanvasMath.normaliseRotation(190f), 0.001f)
        assertEquals(170f, CanvasMath.normaliseRotation(-190f), 0.001f)
        assertEquals(0f, CanvasMath.normaliseRotation(360f), 0.001f)
        assertEquals(45f, CanvasMath.normaliseRotation(405f), 0.001f)
    }

    @Test
    fun `drag moves the layer centre`() {
        val moved = CanvasMath.drag(LayerTransform(), 0.1f, -0.2f)
        assertEquals(0.6f, moved.centerX, 0.001f)
        assertEquals(0.3f, moved.centerY, 0.001f)
    }

    @Test
    fun `pinch keeps the focal point anchored`() {
        val start = LayerTransform(centerX = 0.5f, centerY = 0.5f, scale = 1f)
        val result = CanvasMath.pinch(start, focalX = 0f, focalY = 0.5f, scaleFactor = 2f, rotationDeltaDeg = 0f)
        assertEquals(2f, result.scale, 0.001f)
        assertEquals(1f, result.centerX, 0.001f)
        assertEquals(0.5f, result.centerY, 0.001f)
    }

    @Test
    fun `pinch clamps the scale range`() {
        val start = LayerTransform(scale = 5f)
        val result = CanvasMath.pinch(start, 0.5f, 0.5f, scaleFactor = 4f, rotationDeltaDeg = 0f)
        assertEquals(CanvasMath.MAX_SCALE, result.scale, 0.001f)
    }

    @Test
    fun `rotation moves the centre around the focal point`() {
        val start = LayerTransform(centerX = 1f, centerY = 0.5f)
        val result = CanvasMath.pinch(start, focalX = 0.5f, focalY = 0.5f, scaleFactor = 1f, rotationDeltaDeg = 90f)
        assertEquals(0.5f, result.centerX, 0.001f)
        assertEquals(1f, result.centerY, 0.001f)
        assertEquals(90f, result.rotationDeg, 0.001f)
    }

    @Test
    fun `clamp keeps part of the layer visible`() {
        val far = CanvasMath.clampInside(LayerTransform(centerX = 5f, centerY = 5f, scale = 1f), minVisible = 0.1f)
        assertEquals(1.4f, far.centerX, 0.001f)
        assertEquals(1.4f, far.centerY, 0.001f)
    }

    @Test
    fun `centre snapping only fires inside the threshold`() {
        assertEquals(0.5f, CanvasMath.snapToCenter(LayerTransform(centerX = 0.51f), 0.02f).centerX, 0.001f)
        assertEquals(0.60f, CanvasMath.snapToCenter(LayerTransform(centerX = 0.60f), 0.02f).centerX, 0.001f)
    }

    @Test
    fun `fit and fill scales differ for mismatched aspects`() {
        assertEquals(0.5625f, CanvasMath.aspectFitScale(1920, 1080, 1080, 1920), 0.001f)
        assertEquals(1.7778f, CanvasMath.aspectFillScale(1920, 1080, 1080, 1920), 0.01f)
    }

    @Test
    fun `content check reports the layer position`() {
        assertTrue(CanvasMath.hasContentNear(LayerTransform(), 0.5f, 0.5f))
        assertFalse(CanvasMath.hasContentNear(LayerTransform(), 0f, 0f))
    }

    @Test
    fun `crop by aspect fills the short axis`() {
        val portrait = CropMath.byAspect(9f / 16f)
        assertEquals(0.5625f, portrait.width, 0.001f)
        assertEquals(1f, portrait.height, 0.001f)

        val landscape = CropMath.byAspect(16f / 9f)
        assertEquals(1f, landscape.width, 0.001f)
        assertEquals(0.5625f, landscape.height, 0.001f)
    }

    @Test
    fun `crop coverage scales the rectangle`() {
        val half = CropMath.byAspect(1f, coverage = 0.5f)
        assertEquals(0.5f, half.width, 0.001f)
        assertEquals(0.5f, half.height, 0.001f)
        assertEquals(0.25f, half.left, 0.001f)
    }

    @Test
    fun `crop clamp pulls the rect inside the frame`() {
        val clamped = CropMath.clamp(CropMath.Crop(left = -0.2f, top = 0.8f, right = 0.5f, bottom = 1.4f))
        assertEquals(0f, clamped.left, 0.001f)
        assertEquals(0.7f, clamped.width, 0.001f)
        assertEquals(0.4f, clamped.top, 0.001f)
        assertEquals(1f, clamped.bottom, 0.001f)
    }

    @Test
    fun `rotating the source swaps the crop axes`() {
        val rotated = CropMath.rotated(CropMath.Crop(0.2f, 0.3f, 0.6f, 0.5f), 90f)
        assertEquals(0.2f, rotated.width, 0.001f)
        assertEquals(0.4f, rotated.height, 0.001f)
        assertEquals(0.4f, rotated.centerX, 0.001f)
        assertEquals(0.4f, rotated.centerY, 0.001f)

        val unchanged = CropMath.rotated(CropMath.Crop(0.2f, 0.3f, 0.6f, 0.5f), 180f)
        assertEquals(0.4f, unchanged.width, 0.001f)
    }

    @Test
    fun `pixel rect is even aligned for the encoder`() {
        val pixels = CropMath.toPixels(
            CropMath.Crop(0.1f, 0.1f, 0.9f, 0.9f),
            frameWidth = 1920,
            frameHeight = 1080,
        )
        assertEquals(192, pixels[0])
        assertEquals(108, pixels[1])
        assertEquals(1536, pixels[2])
        assertEquals(864, pixels[3])
        assertEquals(0, pixels[2] % 2)
        assertEquals(0, pixels[3] % 2)
    }
}
