package com.example.data

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the self-healing preview player policy: an errored/idle player must be
 * rebuilt, and only on foreground return — a healthy player is never touched.
 */
class PlayerRecoveryPolicyTest {

    @Test
    fun `an errored player is broken even when its state looks ready`() {
        assertTrue(PlayerRecoveryPolicy.isBroken(lastError = "boom", playbackState = Player.STATE_READY))
    }

    @Test
    fun `an idle player is broken even without a stored error`() {
        assertTrue(PlayerRecoveryPolicy.isBroken(lastError = null, playbackState = Player.STATE_IDLE))
    }

    @Test
    fun `a healthy ready player is not broken`() {
        assertFalse(PlayerRecoveryPolicy.isBroken(lastError = null, playbackState = Player.STATE_READY))
        assertFalse(PlayerRecoveryPolicy.isBroken(lastError = null, playbackState = Player.STATE_ENDED))
    }

    @Test
    fun `rebuild happens on foreground return of a broken player`() {
        assertTrue(
            PlayerRecoveryPolicy.shouldRebuildOnHostResume(
                backgrounded = true,
                lastError = "Error processing video frames",
                playbackState = Player.STATE_IDLE
            )
        )
    }

    @Test
    fun `a healthy player is never rebuilt on foreground return`() {
        assertFalse(
            PlayerRecoveryPolicy.shouldRebuildOnHostResume(
                backgrounded = true,
                lastError = null,
                playbackState = Player.STATE_READY
            )
        )
    }

    @Test
    fun `a broken player is not rebuilt while the app never left the foreground`() {
        assertFalse(
            PlayerRecoveryPolicy.shouldRebuildOnHostResume(
                backgrounded = false,
                lastError = "boom",
                playbackState = Player.STATE_IDLE
            )
        )
    }

    @Test
    fun `a stalled playhead is recovered after the startup grace`() {
        // Repro: play -> pause -> scrub -> press play. The player can freeze
        // while still reporting isPlaying=true (androidx/media#3452), so the
        // decision must be driven by the non-advancing playhead alone.
        assertFalse(
            PlayerRecoveryPolicy.shouldRecoverStall(
                startupGraceOver = false,
                stalledTicks = PlayerRecoveryPolicy.STALL_TICKS
            )
        )
        assertFalse(
            PlayerRecoveryPolicy.shouldRecoverStall(
                startupGraceOver = true,
                stalledTicks = PlayerRecoveryPolicy.STALL_TICKS - 1
            )
        )
        assertTrue(
            PlayerRecoveryPolicy.shouldRecoverStall(
                startupGraceOver = true,
                stalledTicks = PlayerRecoveryPolicy.STALL_TICKS
            )
        )
    }

    @Test
    fun `stall recovery does not depend on the isPlaying flag`() {
        // The function has no isPlaying parameter on purpose: CompositionPlayer
        // keeps reporting isPlaying=true while its frames are frozen, so a
        // flag-based retry would never fire. The playhead position is the only
        // input besides the grace window.
        assertTrue(
            PlayerRecoveryPolicy.shouldRecoverStall(
                startupGraceOver = true,
                stalledTicks = PlayerRecoveryPolicy.STALL_TICKS + 3
            )
        )
    }

    @Test
    fun `play is deferred while the player is buffering or idle`() {
        // Device-log proven: a play() issued while CompositionPlayer is still
        // BUFFERING from a scrub seek freezes the playhead forever, while a
        // play() at STATE_READY always played smoothly. Only READY/ENDED may
        // start playback immediately.
        assertTrue(PlayerRecoveryPolicy.shouldDeferPlay(Player.STATE_BUFFERING))
        assertTrue(PlayerRecoveryPolicy.shouldDeferPlay(Player.STATE_IDLE))
        assertFalse(PlayerRecoveryPolicy.shouldDeferPlay(Player.STATE_READY))
        assertFalse(PlayerRecoveryPolicy.shouldDeferPlay(Player.STATE_ENDED))
    }

    @Test
    fun `an ended state at the timeline end stops playback`() {
        assertTrue(PlayerRecoveryPolicy.hasReachedEnd(67_052L, 67_052L, Player.STATE_ENDED))
        assertFalse(PlayerRecoveryPolicy.hasReachedEnd(1_800L, 67_052L, Player.STATE_ENDED))
        assertFalse(PlayerRecoveryPolicy.hasReachedEnd(67_052L, 67_052L, Player.STATE_READY))
    }

    @Test
    fun `pressing play does not rebuild a ready player with a stale error`() {
        // The error string survives a successful recovery (the ExoPlayer
        // fallback keeps it until a composition loads again). Rebuilding on
        // every press re-created the media items each time and made playback
        // restart with a visible lag instead of resuming.
        assertFalse(
            PlayerRecoveryPolicy.shouldRebuildBeforePlay(
                lastError = "Error processing video frames",
                playbackState = Player.STATE_READY
            )
        )
        assertFalse(
            PlayerRecoveryPolicy.shouldRebuildBeforePlay(
                lastError = "Error processing video frames",
                playbackState = Player.STATE_ENDED
            )
        )
    }

    @Test
    fun `pressing play rebuilds a player that cannot answer play`() {
        // STATE_IDLE ignores play() silently (Media3 needs prepare() there).
        assertTrue(
            PlayerRecoveryPolicy.shouldRebuildBeforePlay(
                lastError = null,
                playbackState = Player.STATE_IDLE
            )
        )
        assertTrue(
            PlayerRecoveryPolicy.shouldRebuildBeforePlay(
                lastError = "Preview playback failed",
                playbackState = Player.STATE_IDLE
            )
        )
        // An errored player that is stuck in a non-usable state also needs a
        // fresh build.
        assertTrue(
            PlayerRecoveryPolicy.shouldRebuildBeforePlay(
                lastError = "Preview playback failed",
                playbackState = Player.STATE_BUFFERING
            )
        )
    }

    @Test
    fun `a buffering player is never treated as a freeze`() {
        // At a clip cut the incoming item is still loading: the playhead
        // legitimately stops and CompositionPlayer resumes by itself when
        // every item is READY again. Nudging during that window is what made
        // the playhead crawl and get stuck.
        assertFalse(PlayerRecoveryPolicy.countsAsStallCandidate(Player.STATE_BUFFERING))
        assertTrue(PlayerRecoveryPolicy.countsAsStallCandidate(Player.STATE_READY))
        assertTrue(PlayerRecoveryPolicy.countsAsStallCandidate(Player.STATE_IDLE))
        assertTrue(PlayerRecoveryPolicy.countsAsStallCandidate(Player.STATE_ENDED))
    }
}
