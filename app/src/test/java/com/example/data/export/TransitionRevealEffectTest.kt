package com.example.data.export

import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The transition reveal shader: the progress curve decides how much of the
 * clip is visible at a given frame, so the preview and the export agree
 * and the curve can be pinned in a unit test.
 */
@UnstableApi
class TransitionRevealEffectTest {

    private val clipUs = 10_000_000L
    private val transitionUs = 1_000_000L

    @Test
    fun `the clip starts hidden`() {
        assertEquals(
            0f,
            TransitionRevealEffect.progressAt(0L, clipUs, transitionUs),
            0.0001f
        )
    }

    @Test
    fun `the clip is half revealed in the middle of the transition`() {
        assertEquals(
            0.5f,
            TransitionRevealEffect.progressAt(500_000L, clipUs, transitionUs),
            0.0001f
        )
    }

    @Test
    fun `the clip is fully visible after the transition`() {
        assertEquals(
            1f,
            TransitionRevealEffect.progressAt(1_500_000L, clipUs, transitionUs),
            0.0001f
        )
        assertEquals(
            1f,
            TransitionRevealEffect.progressAt(5_000_000L, clipUs, transitionUs),
            0.0001f
        )
    }

    @Test
    fun `the clip disappears again at the very end`() {
        // The outgoing clip fades out over the transition window too.
        assertEquals(
            0.5f,
            TransitionRevealEffect.progressAt(9_500_000L, clipUs, transitionUs),
            0.0001f
        )
        assertEquals(
            0f,
            TransitionRevealEffect.progressAt(10_000_000L, clipUs, transitionUs),
            0.0001f
        )
    }

    @Test
    fun `a transition without a length leaves the clip alone`() {
        assertEquals(1f, TransitionRevealEffect.progressAt(0L, clipUs, 0L), 0.0001f)
    }

    @Test
    fun `the progress never leaves the zero to one range`() {
        var time = -2_000_000L
        while (time <= 12_000_000L) {
            val progress = TransitionRevealEffect.progressAt(time, clipUs, transitionUs)
            assertTrue("progress out of range at $time: $progress", progress in 0f..1f)
            time += 250_000L
        }
    }
}
