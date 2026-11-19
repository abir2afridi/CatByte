package com.example.data.player

import androidx.media3.common.Player
import com.example.data.AppLog
import com.example.data.PlayerManager
import com.example.data.PlayerManager.Companion.TAG
import com.example.data.PlaybackStartPolicy

/**
 * Timeline scrubbing of the preview player.
 *
 * CompositionPlayer only settles a seek while its frame processor is
 * processing frames. With rendering stopped, every seek parks the player in
 * STATE_BUFFERING and the preview stays black until a full player rebuild.
 * Keeping the render loop alive during the gesture is what makes the preview
 * follow the playhead in real time instead of jumping once at the end.
 */

/**
 * Enters scrub mode: the player keeps rendering (muted) while the user drags
 * the timeline.
 *
 * CompositionPlayer only settles a seek while its frame processor is
 * processing frames — `waitingForFrameAfterSeek` is cleared from
 * `onFrameProcessed()`. With rendering stopped, every seek parks the player
 * in STATE_BUFFERING and the preview stays black until a full player
 * rebuild; that is the "1-2 s black screen while scrolling" report. Keeping
 * the render loop alive during the gesture is also what makes the preview
 * follow the playhead in real time instead of jumping once at the end.
 */
internal fun PlayerManager.beginScrubMode() {
    if (scrubbing) return
    val player = _activePlayer.value ?: return
    scrubbing = true
    scrubRestoreVolume = player.volume
    player.volume = 0f
    player.playWhenReady = true
    _isPlayingInternal.value = true
    AppLog.info(TAG, "Scrub preview started (rendering, muted)")
}

/**
 * Stops the scrub rendering. The last frame stays on the surface, so the
 * preview keeps showing the frame the user dragged to.
 */
internal fun PlayerManager.endScrubMode() {
    if (!scrubbing) return
    scrubbing = false
    val player = _activePlayer.value ?: return
    player.playWhenReady = false
    player.volume = scrubRestoreVolume
    // The user was scrubbing, not playing: drop any stale play intent so an
    // async isPlaying callback cannot resurrect playback after the freeze.
    playRequested = false
    _isPlayingInternal.value = false
    AppLog.info(TAG, "Scrub preview ended at ${_currentPositionMs.value}ms")
}

/** Leaves a scrub playing (the user pressed Play while still dragging). */
internal fun PlayerManager.endScrubKeepPlayingMode() {
    if (!scrubbing) return
    scrubbing = false
    val player = _activePlayer.value ?: return
    player.volume = scrubRestoreVolume
    _isPlayingInternal.value = player.isPlaying
}

/**
 * Abandons a scrub without landing a frame (project switch, editor destroy).
 * Restores the volume and drops the render-only play intent so a muted
 * player cannot leak into the next session.
 */
internal fun PlayerManager.cancelScrubMode() {
    if (!scrubbing) return
    scrubbing = false
    val player = _activePlayer.value ?: return
    player.volume = scrubRestoreVolume
    player.playWhenReady = false
    playRequested = false
    _isPlayingInternal.value = false
}

/**
 * Drops a stale error once the preview is demonstrably working again.
 * Kept separate from the project load so the scrub gesture can clear a pill
 * left over from an earlier recovery without rebuilding the player.
 */
internal fun PlayerManager.clearPreviewErrorState() {
    _lastError.value = null
}

/**
 * True while the preview can actually show a frame. Used to detect a seek
 * that never settled (the only failure the heavy rebuild path exists for).
 */
internal fun PlayerManager.previewIsReadyState(): Boolean =
    _activePlayer.value?.playbackState == Player.STATE_READY

/** Safe playback start: never on the last frame of an outgoing clip. */
internal fun PlayerManager.safeStartPositionMs(positionMs: Long = _currentPositionMs.value): Long {
    val project = currentProject ?: return positionMs
    return PlaybackStartPolicy.safeStartMs(
        positionMs = positionMs,
        durationMs = totalDurationMs,
        fps = project.fps,
        boundariesMs = PlaybackStartPolicy.mainClipBoundariesMs(
            project.clips.map { it.durationMs }
        )
    )
}

/**
 * Tears the preview player down and rebuilds a BRAND-NEW one prepared at
 * [positionMs].
 *
 * The LAST RESORT, used only when a live scrub seek cannot settle (the
 * preview stays black): on that path releasing the surface is unavoidable
 * and the user sees a black flash while the fresh player prepares.
 *
 * The historical reason this was the default is that CompositionPlayer
 * 1.11.1 parks a seek in STATE_BUFFERING while rendering is stopped. That is
 * now handled by the scrub mode (the render loop stays alive during a drag),
 * so scrubbing no longer rebuilds and the preview stays live.
 */
internal fun PlayerManager.rebuildPreviewAtPosition(positionMs: Long) {
    val project = sourceProject ?: return
    val at = safeStartPositionMs(positionMs.coerceIn(0L, totalDurationMs))
    AppLog.warn(TAG, "Rebuilding the preview player at ${at}ms")
    // Leave scrub mode first (it would otherwise mute the fresh player).
    scrubbing = false
    // Pause while the old instance is still alive; the forced rebuild then
    // releases it and prepares a fresh one at [at].
    pausePlayer()
    loadProjectInternal(project, at, forceRebuild = true)
}
