package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiImageSizeTest {

    @Test
    fun portraitCanvasRequestsPortraitImage() {
        assertEquals(AiImageSize.PORTRAIT, AiImageSize.forAspect(9f / 16f))
        assertEquals(AiImageSize.PORTRAIT, AiImageSize.forAspect(4f / 5f))
    }

    @Test
    fun landscapeCanvasRequestsLandscapeImage() {
        assertEquals(AiImageSize.LANDSCAPE, AiImageSize.forAspect(16f / 9f))
        assertEquals(AiImageSize.LANDSCAPE, AiImageSize.forAspect(1.5f))
    }

    @Test
    fun squareCanvasRequestsSquareImage() {
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(1f))
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(0.95f))
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(1.05f))
    }

    @Test
    fun invalidRatioFallsBackToSquare() {
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(0f))
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(-2f))
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(Float.NaN))
        assertEquals(AiImageSize.SQUARE, AiImageSize.forAspect(Float.POSITIVE_INFINITY))
    }
}
