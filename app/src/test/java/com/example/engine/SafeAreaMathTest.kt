package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Geometry checks for the preview's safe-area / rule-of-thirds guides. */
class SafeAreaMathTest {

    @Test
    fun `action safe box is centred and keeps 90 percent`() {
        val rect = SafeAreaMath.guideRect(SafeAreaMath.ACTION_SAFE, 1000f, 500f)
        assertEquals(50f, rect.left, 0.001f)
        assertEquals(950f, rect.right, 0.001f)
        assertEquals(25f, rect.top, 0.001f)
        assertEquals(475f, rect.bottom, 0.001f)
        assertEquals(0.9f, rect.width / 1000f, 0.0001f)
        assertEquals(0.9f, rect.height / 500f, 0.0001f)
    }

    @Test
    fun `title safe box is smaller than action safe`() {
        val action = SafeAreaMath.guideRect(SafeAreaMath.ACTION_SAFE, 720f, 1280f)
        val title = SafeAreaMath.guideRect(SafeAreaMath.TITLE_SAFE, 720f, 1280f)
        assertTrue(title.width < action.width)
        assertTrue(title.height < action.height)
        assertTrue(title.left > action.left)
        assertTrue(title.top > action.top)
    }

    @Test
    fun `guide boxes stay inside the frame`() {
        for (scale in listOf(-1f, 0f, 0.5f, 1f, 2f)) {
            val rect = SafeAreaMath.guideRect(scale, 100f, 40f)
            assertTrue(rect.left >= 0f)
            assertTrue(rect.top >= 0f)
            assertTrue(rect.right <= 100f)
            assertTrue(rect.bottom <= 40f)
            assertTrue(rect.width >= 0f)
            assertTrue(rect.height >= 0f)
        }
    }

    @Test
    fun `full scale fills the whole frame`() {
        val rect = SafeAreaMath.guideRect(1f, 1080f, 1920f)
        assertEquals(0f, rect.left, 0.001f)
        assertEquals(0f, rect.top, 0.001f)
        assertEquals(1080f, rect.right, 0.001f)
        assertEquals(1920f, rect.bottom, 0.001f)
    }

    @Test
    fun `thirds split the frame into equal bands`() {
        assertEquals(listOf(1f / 3f, 2f / 3f), SafeAreaMath.THIRDS)
    }
}
