package com.example.viewmodel.features

import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.PlayerManager
import com.example.data.PlayerRecoveryPolicy
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Playback engine: play/pause, the playhead polling loop and the self-healing
 * recovery of a frozen preview.
 *
 * The state lives in this class (one instance per editor) so two editors can
 * never share a playback job.
 */
internal class PlaybackFeature(private val vm: EditorViewModel) {

    private var playbackJob: Job? = null

    fun togglePlayback() {
        if (vm._isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        val project = vm._currentProject.value ?: return
        val totalMs = vm.totalDurationMsOf(project)
        var restartFromEnd = false
        if (vm._currentPositionMs.value >= totalMs) {
            vm._currentPositionMs.value = 0L
            restartFromEnd = true
        }
        vm._isPlaying.value = true
        AppLog.info("EditorViewModel", "startPlayback() at ${vm._currentPositionMs.value}ms")

        val manager = vm.playerManager
        if (manager != null) {
            // The timeline may have been scrubbed without the player following
            // it: that is the whole point of the extracted-frame overlay, the
            // player is left alone until the gesture ends. Seek it to the
            // VISIBLE playhead before playing. syncPosition() must not run here
            // in that case — it would overwrite the playhead with the player's
            // stale position.
            val pendingScrub = vm.scrubPreview.isSettlePending() || restartFromEnd
            vm.scrubPreview.cancelSettle()

            // loadProject() is a no-op when the same project is already prepared.
            manager.loadProject(project)

            if (pendingScrub) {
                // CompositionPlayer 1.11.1 wedges on a paused seek of more than
                // ~100 ms (every larger scrub seek stayed BUFFERING forever).
                // The proven path is a FRESH player prepared at the visible
                // playhead. The extracted frame covers the preview while it
                // prepares, so there is no black flash — and, unlike the old
                // beginScrub()+seek path, no failed seek to raise a preview
                // error. The playhead follows the safe-start snap.
                if (manager.usesCompositionPreview.value) {
                    manager.rebuildPreviewAt(vm._currentPositionMs.value)
                    vm._currentPositionMs.value = manager.currentPositionMs.value
                } else {
                    // The ExoPlayer fallback seeks cheaply and never blacks out.
                    manager.beginScrub()
                    manager.seekTo(vm._currentPositionMs.value)
                    manager.endScrubKeepPlaying()
                }
            } else {
                manager.endScrubKeepPlaying()
                // Only re-sync for a REAL divergence (e.g. a project switch reset
                // the UI playhead): a seek to the SAME position while paused
                // flushes CompositionPlayer's audio graph and the following
                // play() starts silent (androidx/media#3482).
                manager.syncPosition()
                val drift = kotlin.math.abs(
                    manager.currentPositionMs.value - vm._currentPositionMs.value
                )
                if (drift > PLAYHEAD_RESYNC_TOLERANCE_MS) {
                    manager.beginScrub()
                    manager.seekTo(vm._currentPositionMs.value)
                    manager.endScrubKeepPlaying()
                }
            }
            // Keep the extracted frame up until the real surface shows the frame
            // playback starts from; otherwise the hand-over flashes black.
            vm.scrubPreview.scheduleFrameHandoff(manager)
            // play() itself defers while the player is BUFFERING; the polling
            // loop below issues it as soon as the player reports READY.
            manager.play()
            startPlaybackPolling(totalMs, manager)
        } else {
            playbackJob?.cancel()
            playbackJob = vm.viewModelScope.launch {
                val stepMs = 50L
                while (isActive && vm._isPlaying.value) {
                    delay(stepMs)
                    val newPos = vm._currentPositionMs.value + stepMs
                    if (newPos >= totalMs) {
                        vm._currentPositionMs.value = totalMs
                        vm._isPlaying.value = false
                        break
                    } else {
                        vm._currentPositionMs.value = newPos
                    }
                }
            }
        }
    }

    /**
     * Drives the playhead from the real player and self-heals a frozen preview.
     *
     * Two failure modes live here:
     *  1. `play()` landing while the player is still BUFFERING from a scrub
     *     seek freezes CompositionPlayer — the device log shows every
     *     BUFFERING-state play() frozen and every READY-state play() smooth.
     *     startPlayback() therefore hands over to phase 1 below, which issues
     *     the real `play()` only once the player reports STATE_READY.
     *  2. CompositionPlayer can freeze with a stuck playhead even after a
     *     READY start (androidx/media#3452). Nudge seeks never rendered a
     *     frame, so the recovery is a brand-new player rebuilt at the playhead
     *     ([PlayerManager.rebuildPreviewAt]) — the only path proven to reach
     *     READY again.
     */
    private fun startPlaybackPolling(totalMs: Long, manager: PlayerManager) {
        playbackJob?.cancel()
        playbackJob = vm.viewModelScope.launch {
            var tick = 0
            var readyTicks = 0
            var watchdogTicks = 0
            var playIssued = false
            var rebuilds = 0
            var recoveries = 0
            var lastRecoveryPosition = 0L
            var stalledTicks = 0
            var bufferingTicks = 0
            var lastPosition = vm._currentPositionMs.value
            while (isActive && vm._isPlaying.value) {
                // The playhead follows the player at display rate (~60 Hz).
                // With the old 50 ms poll the timeline moved in visible 20 Hz
                // steps, which read as "laggy" even when the playback itself
                // was perfectly smooth. The recovery watchdog keeps its old
                // ~50 ms cadence (it runs every WATCHDOG_STRIDE position
                // ticks), so all of its budgets mean exactly what they meant
                // before.
                delay(PlayerRecoveryPolicy.POSITION_TICK_MS)
                manager.syncPosition()
                val position = manager.currentPositionMs.value
                vm._currentPositionMs.value = position
                val state = manager.playbackState.value
                val error = manager.lastError.value

                if (PlayerRecoveryPolicy.hasReachedEnd(position, totalMs, state)) {
                    vm._isPlaying.value = false
                    vm.scrubPreview.clearFrame()
                    break
                }

                // ---- Phase 1: reach STATE_READY at the playhead, THEN play ----
                if (!playIssued) {
                    readyTicks++
                    when {
                        state == androidx.media3.common.Player.STATE_READY -> {
                            // A deferred play (the LIVE state was still
                            // BUFFERING even though the cached state says
                            // READY) must not be marked as issued — otherwise
                            // the preview would stay silent forever.
                            if (manager.play()) {
                                playIssued = true
                                tick = 0
                                readyTicks = 0
                                stalledTicks = 0
                                bufferingTicks = 0
                                lastPosition = manager.currentPositionMs.value
                                vm._currentPositionMs.value = lastPosition
                            }
                        }

                        state == androidx.media3.common.Player.STATE_IDLE && error != null -> {
                            // Broken player: rebuild right away instead of
                            // waiting out the whole READY timeout.
                            rebuilds++
                            readyTicks = 0
                            AppLog.warn(
                                "EditorViewModel",
                                "Preview player broken; rebuilding at ${vm._currentPositionMs.value}ms"
                            )
                            manager.rebuildPreviewAt(vm._currentPositionMs.value)
                        }

                        readyTicks > PlayerRecoveryPolicy.WAIT_READY_POSITION_TICKS -> {
                            if (rebuilds < MAX_READY_REBUILDS) {
                                rebuilds++
                                readyTicks = 0
                                AppLog.warn(
                                    "EditorViewModel",
                                    "Preview never became READY after scrub; rebuilding at ${vm._currentPositionMs.value}ms"
                                )
                                manager.rebuildPreviewAt(vm._currentPositionMs.value)
                            } else {
                                AppLog.error(
                                    "EditorViewModel",
                                    "Preview player never became READY; stopping playback"
                                )
                                vm._isPlaying.value = false
                                vm.scrubPreview.clearFrame()
                                break
                            }
                        }
                    }
                    continue
                }

                // ---- Phase 2: playing — the watchdog runs at the slow cadence ----
                watchdogTicks++
                if (watchdogTicks < PlayerRecoveryPolicy.WATCHDOG_STRIDE) {
                    // Fast path: only the playhead moves. The freeze watchdog
                    // below needs its own cadence, otherwise a 300 ms stall
                    // would be detected after 100 ms.
                    lastPosition = position
                    continue
                }
                watchdogTicks = 0
                tick++
                val graceOver = tick > PlayerRecoveryPolicy.STARTUP_GRACE_TICKS
                if (graceOver &&
                    state == androidx.media3.common.Player.STATE_IDLE &&
                    error != null
                ) {
                    vm._isPlaying.value = false
                    vm.scrubPreview.clearFrame()
                    break
                }

                // A player that keeps buffering will never resume on its own:
                // rebuild it at the playhead instead of waiting forever.
                if (state == androidx.media3.common.Player.STATE_BUFFERING) {
                    bufferingTicks++
                    if (bufferingTicks >= PlayerRecoveryPolicy.WAIT_BUFFERING_WATCHDOG_TICKS &&
                        rebuilds < MAX_READY_REBUILDS
                    ) {
                        rebuilds++
                        bufferingTicks = 0
                        AppLog.warn(
                            "EditorViewModel",
                            "Preview stayed buffering for " +
                                "${PlayerRecoveryPolicy.WAIT_BUFFERING_WATCHDOG_TICKS} ticks; " +
                                "rebuilding at ${position}ms"
                        )
                        manager.rebuildPreviewAt(position)
                        playIssued = false
                        tick = 0
                        stalledTicks = 0
                        lastPosition = manager.currentPositionMs.value
                        continue
                    }
                } else {
                    bufferingTicks = 0
                }

                // `isPlaying` is deliberately NOT part of this test:
                // CompositionPlayer reports isPlaying=true while frames are
                // frozen (#3452). Only a non-advancing playhead counts.
                //
                // A BUFFERING player is not a freeze: at a clip cut the incoming
                // item is still loading, the playhead legitimately stops, and
                // CompositionPlayer resumes by itself once every item is READY
                // again. Counting that as a stall made the watchdog nudge-seek
                // at every transition — the playhead crawled and got stuck.
                val stallCandidate = PlayerRecoveryPolicy.countsAsStallCandidate(state)
                if (graceOver && stallCandidate && position <= lastPosition + 1L) {
                    stalledTicks++
                } else {
                    stalledTicks = 0
                }
                if (PlayerRecoveryPolicy.shouldRecoverStall(
                        startupGraceOver = graceOver,
                        stalledTicks = stalledTicks
                    )
                ) {
                    stalledTicks = 0
                    // A freeze the playhead escaped from long ago starts a new
                    // recovery budget.
                    if (position - lastRecoveryPosition > RECOVERY_RESET_MS) recoveries = 0
                    lastRecoveryPosition = position
                    recoveries++
                    when {
                        rebuilds < MAX_READY_REBUILDS -> {
                            rebuilds++
                            AppLog.warn(
                                "EditorViewModel",
                                "Playback froze at ${position}ms; rebuilding the preview player"
                            )
                            manager.rebuildPreviewAt(position)
                            playIssued = false
                        }

                        else -> {
                            AppLog.error(
                                "EditorViewModel",
                                "Preview keeps freezing at ${position}ms; stopping playback"
                            )
                            vm._isPlaying.value = false
                            vm.scrubPreview.clearFrame()
                            break
                        }
                    }
                    // Recovery seeks are asynchronous: restart the grace window
                    // so stale positions from the seek cannot trigger another
                    // recovery immediately. PlayerManager applies the target to
                    // its own position flow synchronously.
                    tick = 0
                    lastPosition = manager.currentPositionMs.value
                    vm._currentPositionMs.value = lastPosition
                    continue
                }
                lastPosition = position
            }
        }
    }

    fun pausePlayback() {
        val wasPlaying = vm._isPlaying.value
        if (wasPlaying) {
            AppLog.info("EditorViewModel", "pausePlayback() at ${vm._currentPositionMs.value}ms")
        }
        vm._isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        vm.scrubPreview.cancelFrameHandoff()
        val manager = vm.playerManager ?: return
        manager.pause()
        // Only adopt the player's position when it was actually the source of
        // truth (the user paused playback). Pressing the timeline while already
        // paused must NOT snap the playhead back to the player's pre-scrub
        // position: the extracted frame on screen is the visible playhead.
        if (!wasPlaying) {
            vm.scrubPreview.beginGesture()
            return
        }
        manager.syncPosition()
        vm._currentPositionMs.value = manager.currentPositionMs.value
        vm.scrubPreview.clearFrame()
    }

    fun seekTo(positionMs: Long) {
        val project = vm._currentProject.value ?: return
        // Cached: this runs on every scroll event.
        val clamped = positionMs.coerceIn(0L, vm.totalDurationMsOf(project))
        vm._currentPositionMs.value = clamped
        vm.scrubPreview.setTarget(clamped)

        val manager = vm.playerManager
        if (vm._isPlaying.value) {
            // Playback already keeps the render loop alive, so the composition
            // can follow the seek directly. The extracted-frame overlay exists
            // only for a PAUSED player, whose seek blacks the surface out.
            manager?.seekTo(clamped)
            return
        }

        vm.scrubPreview.markSettlePending()
        vm.scrubPreview.requestFrame(project, clamped)
        vm.scrubPreview.scheduleSettle()
    }

    fun stepForward() {
        seekTo(vm._currentPositionMs.value + 1000L)
    }

    fun stepBackward() {
        seekTo(vm._currentPositionMs.value - 1000L)
    }

    /** Stops the playback job (the editor is gone for good). */
    fun cancel() {
        playbackJob?.cancel()
        playbackJob = null
    }

    private companion object {
        /** Bounded self-healing attempts for a frozen preview. */
        const val MAX_READY_REBUILDS = 2
        const val RECOVERY_RESET_MS = 1_000L

        /**
         * Playback only re-syncs the player to the UI playhead when the two
         * really diverged (a project switch). A scrub legitimately leaves the
         * player a few frames ahead of the playhead, and seeking back there is
         * what parked the preview in BUFFERING.
         */
        const val PLAYHEAD_RESYNC_TOLERANCE_MS = 750L
    }
}
