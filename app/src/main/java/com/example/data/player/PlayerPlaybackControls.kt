package com.example.data.player

import androidx.media3.common.Player
import com.example.data.AppLog
import com.example.data.PlayerManager
import com.example.data.PlayerManager.Companion.TAG
import com.example.data.PlayerRecoveryPolicy
import com.example.data.PlaybackStartPolicy

/**
 * Playback controls of the preview player: play, pause, seek, volume and the
 * position sync back into the editor timeline.
 */

/**
 * Starts playback.
 *
 * Returns true when the play request was actually handed to the player and
 * false when it was deferred (the player was still BUFFERING) or dropped
 * (no player). The caller must retry on false — otherwise a deferred play
 * would be lost and the preview would stay silent.
 */
internal fun PlayerManager.playPlayer(): Boolean {
    playRequested = true
    val before = _activePlayer.value
    if (before == null) {
        AppLog.warn(TAG, "play() called without an active preview player")
        return false
    }
    // A player that errored sits in STATE_IDLE and silently ignores play()
    // (it needs prepare(), not play()). Rebuild it from the project so
    // pressing play always recovers instead of doing nothing. Use the LIVE
    // state: the cached flow can still say READY while a seek is settling.
    // A stored error alone must NOT rebuild: the error string survives a
    // successful recovery (the ExoPlayer fallback keeps it), and rebuilding
    // on every press made playback restart from scratch — the laggy,
    // stuttering start.
    if (PlayerRecoveryPolicy.shouldRebuildBeforePlay(_lastError.value, before.playbackState)) {
        currentProject?.let { project ->
            loadProjectInternal(project, _currentPositionMs.value, forceRebuild = true)
        }
    }
    val player = _activePlayer.value ?: return false
    if (player.playbackState == Player.STATE_ENDED) {
        // Restart at the playhead instead of always at 0: a scrub near the
        // end must resume where the user left it.
        val restartAt = _currentPositionMs.value.coerceIn(
            0L,
            (totalDurationMs - 1L).coerceAtLeast(0L)
        )
        seekPlayer(player, restartAt)
        _currentPositionMs.value = restartAt
    }

    // Media3 CompositionPlayer 1.11.1 freezes for good when playback
    // starts on the last frame of an outgoing sequence item
    // (androidx/media#3452). The fixed-centre timeline makes that easy to
    // hit: scrubbing while paused can park the playhead on a clip's final
    // frame, and the next Play then freezes. Start on the clip boundary
    // instead — the tested-safe position. Only when paused: a redundant
    // play() during playback must never jump the playhead.
    if (_usesCompositionPreview.value && !_isPlayingInternal.value) {
        // Correcting the start position is a SEEK, and a seek on a PAUSED
        // CompositionPlayer parks it in BUFFERING forever: its frame
        // processor only clears `waitingForFrameAfterSeek` from
        // `onFrameProcessed()`, which needs the render loop to be running.
        // The user pressed Play, so render first and seek while playing.
        val startPlayer = _activePlayer.value
        if (startPlayer != null && !startPlayer.playWhenReady) {
            startPlayer.playWhenReady = true
        }
        val safeStart = safeStartPositionMs()
        if (safeStart != _currentPositionMs.value) {
            AppLog.info(
                TAG,
                "play(): moving start off a clip's last frame " +
                    "${_currentPositionMs.value}ms -> ${safeStart}ms"
            )
            seekPlayerTo(safeStart)
        }
    }

    val readyPlayer = _activePlayer.value ?: return false
    // play() must not land while the player is still BUFFERING from a
    // scrub seek: the device log shows every BUFFERING-state play() frozen
    // (the playhead only ever moved when the recovery nudged it) while
    // every READY-state play() played smoothly. The LIVE state decides
    // here — the cached flow can still say READY while a seek settles.
    // The caller retries once the player reports READY.
    if (PlayerRecoveryPolicy.shouldDeferPlay(readyPlayer.playbackState)) {
        AppLog.info(
            TAG,
            "play(): deferred until the player is ready (state=${readyPlayer.playbackState})"
        )
        return false
    }
    readyPlayer.play()
    _playbackState.value = readyPlayer.playbackState
    AppLog.info(
        TAG,
        "play(): playWhenReady=${readyPlayer.playWhenReady}, " +
            "state=${readyPlayer.playbackState}, pos=${_currentPositionMs.value}ms"
    )
    return true
}

/** Pauses playback and drops the play intent. */
internal fun PlayerManager.pausePlayer() {
    playRequested = false
    _activePlayer.value?.pause()
    // Never let an async player callback resurrect "playing" after a pause.
    _isPlayingInternal.value = false
}

/** Seeks to a position on the *project* timeline. */
internal fun PlayerManager.seekPlayerTo(positionMs: Long) {
    val clamped = positionMs.coerceIn(0L, totalDurationMs.coerceAtLeast(0L))
    val previous = _currentPositionMs.value
    _currentPositionMs.value = clamped
    val player = _activePlayer.value ?: return
    // CompositionPlayer flushes its audio graph on a same-position seek
    // while paused, and the following play() then starts silent
    // (androidx/media#3482). The playhead flow already holds the requested
    // position, so this seek carries no new information.
    if (!_isPlayingInternal.value && clamped == previous) return
    if (_usesCompositionPreview.value) {
        player.seekTo(clamped)
    } else {
        seekFallback(clamped)
    }
}

/** Seeks the active player, picking the right path for the current preview. */
internal fun PlayerManager.seekPlayer(player: Player, positionMs: Long) {
    if (_usesCompositionPreview.value) {
        player.seekTo(positionMs.coerceIn(0L, totalDurationMs))
    } else {
        seekFallback(positionMs)
    }
}

/** Seeks inside the fallback playlist, mapping the timeline to media items. */
internal fun PlayerManager.seekFallback(positionMs: Long) {
    val player = _activePlayer.value ?: return
    if (player.mediaItemCount == 0) return
    val clamped = positionMs.coerceIn(0L, totalDurationMs)
    val slot = fallbackSlots.firstOrNull { clamped >= it.startMs && clamped < it.endMs }
        ?: fallbackSlots.lastOrNull { it.startMs <= clamped && it.mediaIndex >= 0 }
    if (slot != null && slot.mediaIndex >= 0) {
        val local = (clamped - slot.startMs).coerceAtLeast(0L)
        player.seekTo(slot.mediaIndex, local)
    } else {
        val next = fallbackSlots.firstOrNull { it.mediaIndex >= 0 && it.startMs > clamped }
        if (next != null) player.seekTo(next.mediaIndex, 0L)
    }
}

/**
 * Periodically called by the ViewModel while playing. The composition path
 * reads Media3's real playhead, so the Compose timeline stays in sync.
 */
internal fun PlayerManager.syncPlayerPosition() {
    val player = _activePlayer.value ?: return
    if (player.mediaItemCount == 0 && !_usesCompositionPreview.value) return
    val newPos = if (_usesCompositionPreview.value) {
        player.currentPosition.coerceIn(0L, totalDurationMs)
    } else {
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        val slotStart = fallbackSlots.firstOrNull { it.mediaIndex == index }?.startMs ?: 0L
        (slotStart + player.currentPosition).coerceIn(0L, totalDurationMs)
    }
    _currentPositionMs.value = newPos
    _playbackState.value = player.playbackState
    // Only trust the player's isPlaying flag when the user asked to play.
    // Right after pause() ExoPlayer can still report true for one tick.
    _isPlayingInternal.value = playRequested && player.isPlaying
}

/** Sets the preview volume (0..1). */
internal fun PlayerManager.setPlayerVolume(volume: Float) {
    _activePlayer.value?.volume = volume.coerceIn(0f, 1f)
}

/** Rebuilds the preview because the watermark is burned into the graph. */
internal fun PlayerManager.setWatermarkState(enabled: Boolean) {
    if (watermarkEnabled == enabled) return
    watermarkEnabled = enabled
    currentProject?.let { project ->
        lastCompositionProject = null
        loadProjectInternal(project, _currentPositionMs.value)
    }
}

/** Tears both preview players down. */
internal fun PlayerManager.releasePlayers() {
    released = true
    runCatching { compositionPlayer?.release() }
    compositionPlayer = null
    runCatching { fallbackPlayer?.release() }
    fallbackPlayer = null
    _activePlayer.value = null
    _isPlayingInternal.value = false
}
