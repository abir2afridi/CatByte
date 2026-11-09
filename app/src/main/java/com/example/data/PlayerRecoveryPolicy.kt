package com.example.data

import androidx.media3.common.Player

/**
 * Self-healing policy for the process-scoped preview player, as pure decisions.
 *
 * Own file on purpose — this locks the fix for the "nothing plays after
 * reopening the app until Clear all" bug class. PlayerManager is a singleton
 * that outlives the activity; when its surface dies in the background the
 * player can end up errored/idle, and without these rules it stayed broken
 * forever because:
 *  - Media3 silently ignores `play()` on a STATE_IDLE player (it needs
 *    prepare, not play), and
 *  - loadProject's same-project fast path kept the dead instance.
 *
 * Rules:
 *  - [isBroken]: an errored or idle player must be REBUILT from the project
 *    before it can play again;
 *  - [shouldRebuildOnHostResume]: the rebuild happens when the app returns to
 *    the foreground (and only then — a healthy resumed player must not be
 *    touched);
 *  - the composition path's failure latch is allowed to be retried on such a
 *    forced rebuild; if the composition still fails, the fallback path simply
 *    continues.
 */
internal object PlayerRecoveryPolicy {

    /** True when the player can no longer play and must be rebuilt. */
    fun isBroken(lastError: String?, playbackState: Int): Boolean =
        lastError != null || playbackState == Player.STATE_IDLE

    /**
     * True when the app just returned to the foreground AND the player is
     * broken — the only moment a rebuild is warranted.
     */
    fun shouldRebuildOnHostResume(
        backgrounded: Boolean,
        lastError: String?,
        playbackState: Int
    ): Boolean = backgrounded && isBroken(lastError, playbackState)
}
