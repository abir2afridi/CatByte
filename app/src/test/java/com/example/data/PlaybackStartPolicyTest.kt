package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the playback start policy that works around Media3
 * CompositionPlayer 1.11.1 defects:
 *
 *  - starting on the last frame of an outgoing clip freezes playback for good
 *    (androidx/media#3452), so a paused start must be moved onto the clip
 *    boundary;
 *  - a scrub seek parks the player in STATE_BUFFERING forever, so recovery is
 *    a brand-new player instance (PlayerManager.rebuildPreviewAt), never a
 *    nudge seek — nudges only wrote the position without rendering a frame.
 *
 * These tests exist so a future edit to the timeline or PlayerManager cannot
 * silently reintroduce the "play does nothing after scrubbing" bug.
 */
class PlaybackStartPolicyTest {

    @Test
    fun `frame duration clamps an invalid fps`() {
        assertEquals(33L, PlaybackStartPolicy.frameDurationMs(30))
        assertEquals(16L, PlaybackStartPolicy.frameDurationMs(60))
        assertEquals(1_000L, PlaybackStartPolicy.frameDurationMs(0))
        assertEquals(8L, PlaybackStartPolicy.frameDurationMs(500))
    }

    @Test
    fun `clip boundaries are cumulative main-track durations`() {
        assertEquals(
            listOf(2_400L, 4_800L, 6_000L),
            PlaybackStartPolicy.mainClipBoundariesMs(listOf(2_400L, 2_400L, 1_200L))
        )
        assertEquals(
            emptyList<Long>(),
            PlaybackStartPolicy.mainClipBoundariesMs(emptyList())
        )
    }

    @Test
    fun `a position away from a cut starts unchanged`() {
        assertEquals(
            1_000L,
            PlaybackStartPolicy.safeStartMs(
                positionMs = 1_000L,
                durationMs = 4_800L,
                fps = 30,
                boundariesMs = listOf(2_400L, 4_800L)
            )
        )
    }

    @Test
    fun `starting on the last frame of a clip moves to the cut boundary`() {
        // 2366 ms is the last frame of a 2400 ms clip: starting there freezes
        // CompositionPlayer (androidx/media#3452), so playback must start on
        // 2400 instead.
        assertEquals(
            2_400L,
            PlaybackStartPolicy.safeStartMs(
                positionMs = 2_366L,
                durationMs = 4_800L,
                fps = 30,
                boundariesMs = listOf(2_400L, 4_800L)
            )
        )
    }

    @Test
    fun `the transition window is 150 ms wide`() {
        // #3452 freezes ~130-150 ms AFTER the start, i.e. ~100 ms past the
        // cut, so two frames (66 ms) was too narrow: a start 70 ms before the
        // cut still froze after the transition. The whole 150 ms before a cut
        // must therefore snap onto the boundary.
        val boundaries = listOf(2_400L, 4_800L)
        // 2400 - 2250 = 150 ms -> exactly at the edge of the window.
        assertEquals(
            2_400L,
            PlaybackStartPolicy.safeStartMs(2_250L, 4_800L, 30, boundaries)
        )
        // 2400 - 2249 = 151 ms -> outside the window, untouched.
        assertEquals(
            2_249L,
            PlaybackStartPolicy.safeStartMs(2_249L, 4_800L, 30, boundaries)
        )
        // A low fps can never shrink the window below 150 ms even though two
        // 100 fps frames are only 20 ms.
        assertEquals(
            2_400L,
            PlaybackStartPolicy.safeStartMs(2_300L, 4_800L, 100, boundaries)
        )
    }

    @Test
    fun `starting exactly on a boundary is already safe`() {
        assertEquals(
            2_400L,
            PlaybackStartPolicy.safeStartMs(2_400L, 4_800L, 30, listOf(2_400L, 4_800L))
        )
    }

    @Test
    fun `starting on the final frame of the timeline restarts from the top`() {
        assertEquals(
            0L,
            PlaybackStartPolicy.safeStartMs(2_390L, 2_400L, 30, listOf(2_400L))
        )
    }

    @Test
    fun `a position outside the timeline is clamped first`() {
        assertEquals(
            0L,
            PlaybackStartPolicy.safeStartMs(-50L, 4_800L, 30, listOf(2_400L, 4_800L))
        )
        // Clamps to 4800, which is the final frame -> restart from the top.
        assertEquals(
            0L,
            PlaybackStartPolicy.safeStartMs(9_999L, 4_800L, 30, listOf(2_400L, 4_800L))
        )
    }

    @Test
    fun `an empty timeline always starts at zero`() {
        assertEquals(
            0L,
            PlaybackStartPolicy.safeStartMs(500L, 0L, 30, emptyList())
        )
    }
}
