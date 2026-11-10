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
 *  - [shouldRebuildBeforePlay]: pressing Play rebuilds only a player that
 *    cannot answer play() — a stored error alone must not, otherwise every
 *    press restarts the whole preview and playback looks laggy;
 *  - the composition path's failure latch is allowed to be retried on such a
 *    forced rebuild; if the composition still fails, the fallback path simply
 *    continues;
 *  - [shouldRecoverStall]: a playhead that stops advancing is recovered with a
 *    one-frame nudge seek, never by re-issuing play() alone — CompositionPlayer
 *    can report isPlaying=true while its frames are frozen (androidx/media#3452);
 *  - [countsAsStallCandidate]: a buffering player is never a freeze — at a
 *    clip cut the incoming item legitimately stops the playhead until it is
 *    loaded, and CompositionPlayer resumes by itself.
 */
internal object PlayerRecoveryPolicy {

    /**
     * Playhead refresh interval.
     *
     * The playhead and the timeline auto-scroll follow the player at ~60 Hz.
     * With the old 50 ms poll the timeline moved in visible 20 Hz steps, which
     * read as "laggy" even when the playback itself was perfectly smooth.
     */
    const val POSITION_TICK_MS = 16L

    /**
     * The recovery watchdog runs every Nth position tick, so all of its
     * budgets keep their old ~50 ms cadence (3 * 16 ms = 48 ms) while the
     * playhead itself updates at display rate.
     */
    const val WATCHDOG_STRIDE = 3

    /** Ignore transient idle/ended states while a freshly issued play() settles. */
    const val STARTUP_GRACE_TICKS = 4

    /** Non-advancing ticks before the player is nudged out of a freeze. */
    const val STALL_TICKS = 6

    /**
     * Position ticks to wait for STATE_READY after a scrub seek before
     * rebuilding the player from scratch (180 * 16 ms = ~2.9 s).
     */
    const val WAIT_READY_POSITION_TICKS = 180

    /**
     * Watchdog ticks a player may stay buffering before it is rebuilt at the
     * playhead (60 * 48 ms = ~2.9 s).
     */
    const val WAIT_BUFFERING_WATCHDOG_TICKS = 60

    /** True when the player can no longer play and must be rebuilt. */
    fun isBroken(lastError: String?, playbackState: Int): Boolean =
        lastError != null || playbackState == Player.STATE_IDLE

    /**
     * True when pressing Play has to rebuild the player instead of resuming it.
     *
     * A stored error string alone must NOT trigger a rebuild: the error stays
     * recorded after the player recovered (the ExoPlayer fallback path keeps
     * it set until a composition loads again), and rebuilding on every press
     * re-created the media items each time — the preview then restarted with
     * a visible lag instead of resuming.
     *
     * A rebuild is required only when the player cannot answer play() at all
     * (STATE_IDLE — Media3 silently ignores play() there) or when it carries an
     * error *and* sits in no usable state.
     */
    fun shouldRebuildBeforePlay(lastError: String?, playbackState: Int): Boolean {
        if (playbackState == Player.STATE_IDLE) return true
        val usable = playbackState == Player.STATE_READY ||
            playbackState == Player.STATE_ENDED
        return lastError != null && !usable
    }

    /**
     * `play()` issued while the player is still BUFFERING from a scrub seek
     * freezes CompositionPlayer: the device log shows every BUFFERING-state
     * play() frozen (the playhead only ever moved when the recovery nudged it)
     * and every READY-state play() smooth. The play() call must therefore be
     * deferred until the player reports STATE_READY.
     */
    fun shouldDeferPlay(playbackState: Int): Boolean =
        playbackState != Player.STATE_READY && playbackState != Player.STATE_ENDED

    /**
     * Media3 can silently drop `play()` while a seek is still settling (repro:
     * play -> pause -> scrub the timeline -> press play), and CompositionPlayer
     * can keep `isPlaying=true` while its frames stop after starting on an
     * outgoing clip's last frame (androidx/media#3452). Both look identical from
     * the outside: the playhead stops advancing. Position is therefore the only
     * truthful signal — stall detection must NOT depend on `isPlaying`.
     *
     * Recovery itself is a seek to a different position plus play(); re-issuing
     * play() alone cannot fix the last-frame freeze.
     */
    fun shouldRecoverStall(
        startupGraceOver: Boolean,
        stalledTicks: Int
    ): Boolean = startupGraceOver && stalledTicks >= STALL_TICKS

    /**
     * True when a non-advancing playhead may be treated as a freeze.
     *
     * While the player is BUFFERING the playhead legitimately stops: at a clip
     * cut the incoming item is still loading, and CompositionPlayer pauses its
     * internal players and restores them by itself once every item is READY
     * again. Counting that as a stall made the watchdog nudge-seek during
     * every transition — the "playhead crawls and gets stuck in the middle"
     * symptom. Only a READY (or otherwise live) player that stops advancing
     * is a real freeze.
     */
    fun countsAsStallCandidate(playbackState: Int): Boolean =
        playbackState != Player.STATE_BUFFERING

    /** Only a real end-of-timeline stops playback; an ENDED state mid-seek must not. */
    fun hasReachedEnd(positionMs: Long, durationMs: Long, playbackState: Int): Boolean =
        playbackState == Player.STATE_ENDED && positionMs >= durationMs - 1L

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
