package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrubFramePolicyTest {

    @Test
    fun `publishes when nothing is on screen yet`() {
        assertTrue(
            ScrubFramePolicy.shouldPublish(
                requestedMs = 5_000,
                targetMs = 5_200,
                lastPublishedMs = ScrubFramePolicy.NOTHING_PUBLISHED
            )
        )
    }

    @Test
    fun `publishes a frame closer to the current playhead`() {
        assertTrue(
            ScrubFramePolicy.shouldPublish(
                requestedMs = 4_900,
                targetMs = 5_000,
                lastPublishedMs = 4_000
            )
        )
    }

    @Test
    fun `drops a stale frame that would drag the picture backwards`() {
        // The settle lane already published 5_000; a drag decode for 4_200
        // arriving later must not replace it.
        assertFalse(
            ScrubFramePolicy.shouldPublish(
                requestedMs = 4_200,
                targetMs = 5_000,
                lastPublishedMs = 5_000
            )
        )
    }

    @Test
    fun `allows replacing the frame at the same position`() {
        assertTrue(
            ScrubFramePolicy.shouldPublish(
                requestedMs = 5_000,
                targetMs = 5_000,
                lastPublishedMs = 5_000
            )
        )
    }

    @Test
    fun `tracks a backwards drag towards its own target`() {
        // Target moved back to 2_000; a decode for 2_500 is closer to it than
        // the 4_000 frame still on screen.
        assertTrue(
            ScrubFramePolicy.shouldPublish(
                requestedMs = 2_500,
                targetMs = 2_000,
                lastPublishedMs = 4_000
            )
        )
        // ... while an even older 1_000 decode arriving late is farther away
        // than the just-published 2_500 and must be dropped.
        assertFalse(
            ScrubFramePolicy.shouldPublish(
                requestedMs = 1_000,
                targetMs = 2_000,
                lastPublishedMs = 2_500
            )
        )
    }
}
