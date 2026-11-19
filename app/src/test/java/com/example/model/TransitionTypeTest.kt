package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The transition model: the picker's tabs are only honest if every
 * transition really belongs to one category, and the length the slider
 * writes must match the range the renderer draws.
 */
class TransitionTypeTest {

    @Test
    fun `only the none transition has no category`() {
        assertNull(TransitionType.NONE.category)
        TransitionType.entries
            .filter { it != TransitionType.NONE }
            .forEach { transition ->
                assertTrue("$transition has no category", transition.category != null)
            }
    }

    @Test
    fun `none is the only transition that is not renderable`() {
        assertFalse(TransitionType.NONE.isRenderable)
        TransitionType.entries
            .filter { it != TransitionType.NONE }
            .forEach { transition ->
                assertTrue("$transition must be renderable", transition.isRenderable)
            }
    }

    @Test
    fun `every category offers at least one transition`() {
        TransitionCategory.entries.forEach { category ->
            val count = TransitionType.entries.count { it.category == category }
            assertTrue("category $category is empty", count > 0)
        }
    }

    @Test
    fun `the transition length is clamped into the supported range`() {
        assertEquals(TransitionType.MIN_DURATION_MS, TransitionType.sanitizeDuration(1L))
        assertEquals(TransitionType.MIN_DURATION_MS, TransitionType.sanitizeDuration(-50L))
        assertEquals(TransitionType.MAX_DURATION_MS, TransitionType.sanitizeDuration(900_000L))
        assertEquals(750L, TransitionType.sanitizeDuration(750L))
    }
}
