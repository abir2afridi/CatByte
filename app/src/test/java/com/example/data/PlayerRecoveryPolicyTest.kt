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
}
