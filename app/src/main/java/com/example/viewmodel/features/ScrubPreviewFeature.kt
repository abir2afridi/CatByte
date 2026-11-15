package com.example.viewmodel.features

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.data.PlayerManager
import com.example.data.ScrubFrameAccuracy
import com.example.data.ScrubFrameLoader
import com.example.data.ScrubFramePolicy
import com.example.data.ScrubFrameResolver
import com.example.data.ScrubPreviewFrame
import com.example.model.VideoProject
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Live scrub preview (source-frame extraction).
 *
 * CompositionPlayer 1.11.1 stops its render loop for the whole duration of
 * every seek, so seeking with the timeline kept the preview black until the
 * drag stopped. The scrub preview therefore does NOT touch the player at all:
 * frames are decoded straight from the source media and drawn over the preview.
 * The player is only rebuilt/seeked when the user presses Play, where a short
 * prepare is hidden behind the still frame.
 *
 * The state lives in this class (one instance per editor) so two editors can
 * never share a decode lane.
 */
internal class ScrubPreviewFeature(private val vm: EditorViewModel) {

    /**
     * Two independent decoder lanes, both decoding frame-exact pictures.
     * Per-frame precision was explicitly requested for the split workflow: a
     * keyframe preview freezes inside a GOP and only jumps at GOP boundaries,
     * so the user cannot tell which frame a split will land on. The lanes
     * differ in purpose, not accuracy:
     *  - the drag lane follows the moving playhead;
     *  - the settle lane decodes the frame the gesture stopped on with its own
     *    decoder, so a long in-flight drag decode can never delay it.
     */
    private val scrubFrameExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "catbyte-scrub-frame").apply { isDaemon = true }
    }
    private val scrubFrameDispatcher = scrubFrameExecutor.asCoroutineDispatcher()
    private val scrubFrameLoader = ScrubFrameLoader()

    private val scrubExactExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "catbyte-scrub-exact").apply { isDaemon = true }
    }
    private val scrubExactDispatcher = scrubExactExecutor.asCoroutineDispatcher()
    /** Only ever touches the clip under the final playhead. */
    private val scrubExactLoader = ScrubFrameLoader(maxCachedRetrievers = 1)

    /**
     * One conflated slot per lane: while the finger moves only the newest
     * playhead is worth decoding. A fling overwrites the pending request instead
     * of allocating a coroutine per scroll event, which is what made the
     * timeline itself stutter.
     */
    private val scrubFrameRequests = Channel<ScrubFrameRequest>(Channel.CONFLATED)
    private val scrubExactRequests = Channel<ScrubFrameRequest>(Channel.CONFLATED)
    private var scrubFrameWorker: Job? = null
    private var scrubExactWorker: Job? = null
    /** Incremented when the overlay is cleared so stale decodes are dropped. */
    private var scrubFrameGeneration = 0
    private var scrubFrameHandoffJob: Job? = null
    /** Watches a preview reloaded by an edit made while a scrub still is up. */
    private var scrubEditHandoffJob: Job? = null
    private var lastScrubRequestedMs = Long.MIN_VALUE
    /**
     * Position of the frame currently on screen ([ScrubFramePolicy.NOTHING_PUBLISHED]
     * until the first publish of a gesture). Feeds the staleness rule that
     * keeps late decode results from dragging the picture backwards.
     */
    private var lastPublishedScrubMs = ScrubFramePolicy.NOTHING_PUBLISHED
    private val _scrubPreviewFrame = MutableStateFlow<ScrubPreviewFrame?>(null)

    /** Source frame drawn over the preview while the timeline is scrubbed. */
    val scrubPreviewFrame: StateFlow<ScrubPreviewFrame?> = _scrubPreviewFrame.asStateFlow()

    /**
     * One frame decode request. [final] selects the settle lane (the frame the
     * gesture stopped on); both lanes decode frame-exact pictures.
     */
    private data class ScrubFrameRequest(
        val project: VideoProject,
        val positionMs: Long,
        val final: Boolean,
        val generation: Int
    )

    /** Debounced "gesture over" job that decodes the frame-exact still. */
    private var scrubSettleJob: Job? = null
    /** Latest playhead the scrub preview should show. */
    private var scrubTargetMs = 0L
    /**
     * True while the player has not landed on the scrub playhead yet. Playback
     * must seek the player to the UI playhead instead of trusting its position.
     */
    private var scrubSettlePending = false

    /** True while a scrub still covers the preview (or is about to). */
    fun isScrubVisible(): Boolean = _scrubPreviewFrame.value != null || scrubSettlePending

    /** True while the player still has to catch up with the scrubbed playhead. */
    fun isSettlePending(): Boolean = scrubSettlePending || scrubSettleJob?.isActive == true

    /** True while the settle watcher is running. */
    fun isSettleActive(): Boolean = scrubSettleJob?.isActive == true

    /** Marks the player as "behind" the visible playhead. */
    fun markSettlePending() {
        scrubSettlePending = true
    }

    /** Drops the pending settle so playback can take the player over. */
    fun cancelSettle() {
        scrubSettlePending = false
        scrubSettleJob?.cancel()
        scrubSettleJob = null
    }

    /** Latest playhead the scrub preview should show. */
    fun setTarget(positionMs: Long) {
        scrubTargetMs = positionMs
    }

    /**
     * Hands the preview from the extracted scrub frame back to the real player
     * surface.
     *
     * The player can report READY a frame or two before the surface has drawn
     * the new frame, so the overlay is held for [SCRUB_FRAME_HANDOFF_MS]. The
     * timeout guarantees a failed player can never leave a still frame covering
     * a live preview forever.
     */
    fun scheduleFrameHandoff(manager: PlayerManager) {
        if (_scrubPreviewFrame.value == null) return
        scrubFrameHandoffJob?.cancel()
        scrubFrameHandoffJob = vm.viewModelScope.launch {
            val startPositionMs = manager.activePlayer.value?.currentPosition ?: 0L
            var waited = 0L
            while (waited < SCRUB_FRAME_HANDOFF_TIMEOUT_MS && vm._isPlaying.value) {
                val player = manager.activePlayer.value
                val ready = player?.playbackState == Player.STATE_READY
                val advanced = player != null &&
                    kotlin.math.abs(player.currentPosition - startPositionMs) >=
                    SCRUB_HANDOFF_MIN_ADVANCE_MS
                if (ready && advanced) break
                delay(SCRUB_POLL_MS)
                waited += SCRUB_POLL_MS
            }
            delay(SCRUB_FRAME_HANDOFF_MS)
            if (vm._isPlaying.value) clearFrame()
        }
    }

    /** Cancels the hand-over watcher (playback stopped). */
    fun cancelFrameHandoff() {
        scrubFrameHandoffJob?.cancel()
        scrubFrameHandoffJob = null
    }

    /**
     * Waits for the composition reloaded by an edit to reach READY at the
     * visible playhead, then removes the scrub still.
     *
     * Deliberately conservative: it never pulls the still out from under a
     * live drag, and on timeout it keeps the still (a correct picture) rather
     * than exposing a player that may still be buffering. Pressing Play then
     * takes the proven rebuild path.
     */
    fun awaitEditHandoff(manager: PlayerManager, positionMs: Long) {
        scrubEditHandoffJob?.cancel()
        val reloadedPlayer = manager.activePlayer.value ?: return
        scrubEditHandoffJob = vm.viewModelScope.launch {
            var waited = 0L
            while (waited < SCRUB_FRAME_HANDOFF_TIMEOUT_MS) {
                if (vm._isPlaying.value) return@launch
                val ready = manager.activePlayer.value === reloadedPlayer &&
                    reloadedPlayer.playbackState == Player.STATE_READY
                val quiet = scrubSettleJob?.isActive != true &&
                    vm._currentPositionMs.value == positionMs
                if (ready && quiet) {
                    // Give the surface time to draw the refreshed composition
                    // before the still disappears.
                    delay(SCRUB_FRAME_HANDOFF_MS)
                    if (!vm._isPlaying.value &&
                        scrubSettleJob?.isActive != true &&
                        vm._currentPositionMs.value == positionMs
                    ) {
                        scrubSettlePending = false
                        clearFrame()
                    }
                    return@launch
                }
                delay(SCRUB_POLL_MS)
                waited += SCRUB_POLL_MS
            }
            com.example.data.AppLog.warn(
                "EditorViewModel",
                "Refreshed preview not ready at ${positionMs}ms; keeping the scrub still"
            )
        }
    }

    /**
     * Starts a new scrub gesture without removing the frame already on screen.
     * Decodes queued for the previous playhead are dropped so they cannot flash
     * back while the finger is already somewhere else.
     */
    fun beginGesture() {
        scrubFrameGeneration++
        while (scrubFrameRequests.tryReceive().isSuccess) {
            // Drain the conflated slot.
        }
        while (scrubExactRequests.tryReceive().isSuccess) {
            // A pending settle still belongs to the previous gesture.
        }
        lastScrubRequestedMs = Long.MIN_VALUE
        lastPublishedScrubMs = ScrubFramePolicy.NOTHING_PUBLISHED
        // The preview is about to be driven by decoded source frames; a stale
        // player error from an earlier recovery must not sit over the scrub.
        vm.playerManager?.clearPreviewError()
    }

    /**
     * Decodes the source frame under [positionMs] and draws it over the preview.
     *
     * This is what makes the timeline scroll show real frames in realtime: the
     * player is not seeked at all during the drag (every CompositionPlayer seek
     * stops rendering until it settles, which is the reported black screen).
     * Every frame is decoded frame-exact so the user can read WHICH frame a
     * split will land on while scrolling.
     */
    fun requestFrame(project: VideoProject, positionMs: Long, final: Boolean = false) {
        val appContext = vm.context?.applicationContext ?: return
        // Small moves reuse the frame already on screen: at 30 fps one source
        // frame is ~33 ms, so anything closer than that is the same picture.
        // The settle request bypasses this so the final playhead is always
        // decoded.
        if (!final &&
            lastScrubRequestedMs != Long.MIN_VALUE &&
            kotlin.math.abs(positionMs - lastScrubRequestedMs) < SCRUB_FRAME_MIN_DELTA_MS
        ) {
            return
        }
        lastScrubRequestedMs = positionMs
        ensureScrubWorkers(appContext)
        val request = ScrubFrameRequest(
            project = project,
            positionMs = positionMs,
            final = final,
            generation = scrubFrameGeneration
        )
        if (final) {
            scrubExactRequests.trySend(request)
        } else {
            scrubFrameRequests.trySend(request)
        }
    }

    /**
     * One worker per lane decodes whatever the newest request is; the conflated
     * channels drop intermediate playheads instead of queueing a decode per
     * scroll event. That is what keeps both the preview and the timeline smooth.
     */
    private fun ensureScrubWorkers(appContext: Context) {
        if (scrubFrameWorker?.isActive != true) {
            scrubFrameWorker = vm.viewModelScope.launch {
                for (request in scrubFrameRequests) {
                    val bitmap = withContext(scrubFrameDispatcher) {
                        decodeScrubFrame(appContext, request, scrubFrameLoader)
                    }
                    publishScrubFrame(request, bitmap, requireCurrentTarget = false)
                }
            }
        }
        if (scrubExactWorker?.isActive != true) {
            scrubExactWorker = vm.viewModelScope.launch {
                for (request in scrubExactRequests) {
                    val bitmap = withContext(scrubExactDispatcher) {
                        decodeScrubFrame(appContext, request, scrubExactLoader)
                    }
                    // The settle frame is only useful if the playhead is still
                    // there; a resumed drag already has its own lane.
                    publishScrubFrame(request, bitmap, requireCurrentTarget = true)
                }
            }
        }
    }

    private fun decodeScrubFrame(
        appContext: Context,
        request: ScrubFrameRequest,
        loader: ScrubFrameLoader
    ): Bitmap? {
        val source = ScrubFrameResolver.resolve(request.project, request.positionMs)
            ?: return null
        // Both lanes decode frame-exact: a keyframe preview freezes inside a
        // GOP, which is exactly what made "which frame do I split at" unreadable.
        // [ScrubFrameAccuracy.KEYFRAME] stays available in the loader for a
        // future adaptive mode on sources exact decoding cannot keep up with.
        return loader.load(appContext, source, ScrubFrameAccuracy.EXACT)
    }

    private fun publishScrubFrame(
        request: ScrubFrameRequest,
        bitmap: Bitmap?,
        requireCurrentTarget: Boolean
    ) {
        if (bitmap == null) return
        if (vm._isPlaying.value) return
        if (request.generation != scrubFrameGeneration) return
        if (requireCurrentTarget && request.positionMs != scrubTargetMs) return
        // A late decode result must not drag the picture backwards past the
        // frame already on screen (ScrubFramePolicy). A failed decode keeps the
        // previous frame instead of flashing black.
        if (!ScrubFramePolicy.shouldPublish(
                requestedMs = request.positionMs,
                targetMs = scrubTargetMs,
                lastPublishedMs = lastPublishedScrubMs
            )
        ) {
            return
        }
        lastPublishedScrubMs = request.positionMs
        _scrubPreviewFrame.value = ScrubPreviewFrame(
            bitmap = bitmap,
            positionMs = request.positionMs
        )
    }

    /**
     * Waits for the timeline to go quiet, then decodes the frame-exact still.
     *
     * One watcher per gesture: it re-arms itself when the user moves while the
     * decode is pending. The player is deliberately NOT seeked here — a seek on
     * the paused CompositionPlayer is what blacked the preview out and, when it
     * never settled, produced the preview error pill. The player is rebuilt at
     * the visible playhead only when the user presses Play.
     */
    fun scheduleSettle() {
        if (scrubSettleJob?.isActive == true) return
        if (vm._isPlaying.value) return
        scrubSettleJob = vm.viewModelScope.launch {
            while (true) {
                val target = scrubTargetMs
                delay(SCRUB_SETTLE_MS)
                if (vm._isPlaying.value) return@launch
                if (scrubTargetMs != target) continue
                vm._currentProject.value?.let { requestFrame(it, target, final = true) }
                return@launch
            }
        }
    }

    /** Removes the overlay frame and drops every queued decode. */
    fun clearFrame() {
        // Drop queued decodes: their frames belong to the gesture that just
        // ended and must not flash back over the live player.
        scrubFrameGeneration++
        while (scrubFrameRequests.tryReceive().isSuccess) {
            // Drain the conflated slot.
        }
        while (scrubExactRequests.tryReceive().isSuccess) {
            // Drain the conflated slot.
        }
        _scrubPreviewFrame.value = null
        lastScrubRequestedMs = Long.MIN_VALUE
        lastPublishedScrubMs = ScrubFramePolicy.NOTHING_PUBLISHED
    }

    /** Cancels every scrub job and removes the overlay (project switch, destroy). */
    fun cancel() {
        scrubSettlePending = false
        scrubSettleJob?.cancel()
        scrubSettleJob = null
        scrubFrameHandoffJob?.cancel()
        scrubFrameHandoffJob = null
        scrubEditHandoffJob?.cancel()
        scrubEditHandoffJob = null
        vm.playerManager?.cancelScrub()
        clearFrame()
    }

    /** Releases the decoder threads (the editor is gone for good). */
    fun shutdown() {
        scrubFrameWorker?.cancel()
        scrubFrameWorker = null
        scrubExactWorker?.cancel()
        scrubExactWorker = null
        scrubFrameRequests.close()
        scrubExactRequests.close()
        // Release the decoders on their own threads AFTER any in-flight decode;
        // shutdown() drains the queue and then stops the thread.
        scrubFrameExecutor.execute { scrubFrameLoader.clear() }
        scrubFrameExecutor.shutdown()
        scrubExactExecutor.execute { scrubExactLoader.clear() }
        scrubExactExecutor.shutdown()
    }

    private companion object {
        /**
         * How long the timeline must stay quiet before the settle lane decodes
         * the stopped-at frame with its dedicated decoder. The player is not
         * touched at all: the decoded frame simply sharpens the overlay.
         */
        const val SCRUB_SETTLE_MS = 180L

        /**
         * Small playhead moves reuse the frame already on screen. Without this,
         * a slow drag decoded a new frame every few milliseconds of movement,
         * which wasted the decoder and competed with the UI thread.
         */
        const val SCRUB_FRAME_MIN_DELTA_MS = 32L

        /** Poll interval for the playback hand-over watcher. */
        const val SCRUB_POLL_MS = 24L

        /**
         * The extracted frame stays up briefly after the player reports READY so
         * the surface has time to draw the first playback frame before the
         * hand-over.
         */
        const val SCRUB_FRAME_HANDOFF_MS = 120L

        /**
         * The hand-over waits until playback has actually advanced by this much,
         * i.e. frames are really being rendered. A READY state alone can still
         * belong to the old position while a seek is in flight.
         */
        const val SCRUB_HANDOFF_MIN_ADVANCE_MS = 32L

        /** Never leave the extracted frame covering the preview forever. */
        const val SCRUB_FRAME_HANDOFF_TIMEOUT_MS = 1_500L
    }
}
