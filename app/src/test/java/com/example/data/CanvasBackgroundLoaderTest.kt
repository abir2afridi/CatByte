package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Background decode sampling keeps large photos out of the heap. */
class CanvasBackgroundLoaderTest {

    @Test
    fun `small images are not downsampled`() {
        assertEquals(1, CanvasBackgroundLoader.sampleSizeFor(640, 480))
        assertEquals(1, CanvasBackgroundLoader.sampleSizeFor(1440, 1440))
    }

    @Test
    fun `large images are sampled down to the max edge`() {
        // 4000 px / 1440 = 2.77 -> sample 3 -> 1333 px.
        assertEquals(3, CanvasBackgroundLoader.sampleSizeFor(4000, 3000))
        assertEquals(2, CanvasBackgroundLoader.sampleSizeFor(2000, 2000))
    }

    @Test
    fun `sample size is never zero`() {
        assertEquals(1, CanvasBackgroundLoader.sampleSizeFor(0, 0))
    }
}
