package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.CompositionPlayer
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the preview player.
 *
 * The preferred path is a [CompositionPlayer] fed by exactly the same
 * [com.example.data.ProjectCompositionFactory] composition that the exporter
 * uses. That means multi-track overlays, keyframed PIP motion, text/stickers,
 * colour grading, speed curves and transitions are all visible in the live
 * preview and cannot drift from the final MP4.
 *
 * If the device cannot create or run a composition (for example a GPU/driver
 * limitation), the manager falls back to a plain [ExoPlayer] playlist and
 * exposes [usesCompositionPreview] = false so the UI can use its Compose
 * fallback renderer.
 */
@UnstableApi
@androidx.annotation.OptIn(ExperimentalApi::class)
class PlayerManager(context: Context) {

    companion object {
        private const val TAG = "PlayerManager"
    }

    private val appContext: Context = context.applicationContext

    private var compositionPlayer: CompositionPlayer? = null
    private var fallbackPlayer: ExoPlayer? = null
    private var compositionFailed = false
    private var released = false

    private val _activePlayer = MutableStateFlow<Player?>(null)
    val activePlayer: StateFlow<Player?> = _activePlayer.asStateFlow()

    private val _usesCompositionPreview = MutableStateFlow(false)
    val usesCompositionPreview: StateFlow<Boolean> = _usesCompositionPreview.asStateFlow()

    private val _isPlayingInternal = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlayingInternal.asStateFlow()

    /**
     * The user's last explicit intent. Media3's `pause()` is asynchronous, so
     * `player.isPlaying` can still be true for a tick afterwards; without this
     * flag `syncPosition()` used to resurrect a "playing" state right after a
     * pause, which made the next play()/loadProject() decision inconsistent.
     */
    private var playRequested = false

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _previewWarnings = MutableStateFlow<List<String>>(emptyList())
    val previewWarnings: StateFlow<List<String>> = _previewWarnings.asStateFlow()

    private var totalDurationMs: Long = 0L
    private var currentProject: VideoProject? = null
    private var lastCompositionProject: VideoProject? = null
    private var watermarkEnabled: Boolean = false

    /**
     * Timeline slots for the fallback playlist. The composition path does not
     * need them because Media3 maps the full timeline itself.
     */
    private data class Slot(
        val clip: MediaClip,
        val startMs: Long,
        val endMs: Long,
        val mediaIndex: Int
    )

    private var fallbackSlots: List<Slot> = emptyList()

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = state
            if (state == Player.STATE_ENDED) {
                _currentPositionMs.value = totalDurationMs
                _isPlayingInternal.value = false
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // A late "true" from an asynchronous pause must not flip the UI
            // back into the playing state.
            _isPlayingInternal.value = playRequested && isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlayerError(error)
        }
    }

    init {
        buildCompositionPlayer()
    }

    // ------------------------------------------------------------------
    // Player construction
    // ------------------------------------------------------------------

    private fun buildCompositionPlayer() {
        if (released) return
        compositionPlayer = try {
            CompositionPlayer.Builder(appContext)
                .setLooper(Looper.getMainLooper())
                .setVideoGraphFactory(MultipleInputVideoGraph.Factory())
                .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
                .build()
                .also { it.addListener(playerListener) }
        } catch (error: Throwable) {
            compositionFailed = true
            AppLog.warn(TAG, "CompositionPlayer unavailable; using ExoPlayer fallback", error)
            null
        }
    }

    private fun buildFallbackPlayer(): ExoPlayer? {
        if (released) return null
        return fallbackPlayer ?: try {
            ExoPlayer.Builder(appContext)
                .build()
                .apply {
                    setAudioAttributes(audioAttributes, true)
                    addListener(playerListener)
                }
                .also { fallbackPlayer = it }
        } catch (error: Throwable) {
            AppLog.error(TAG, "Could not create the fallback player", error)
            _lastError.value = "Preview player could not be created: ${error.message}"
            null
        }
    }

    // ------------------------------------------------------------------
    // Project loading
    // ------------------------------------------------------------------

    /**
     * Loads [project] into the best available preview path. When the project is
     * unchanged this is a no-op (apart from restoring the play state), which is
     * what makes debounced preview refresh cheap.
     *
     * [forceRebuild] skips that fast path and retries a latched-off composition
     * path: a player that errored (e.g. "Error processing video frames" when the
     * host surface died in the background) sits in STATE_IDLE forever, and the
     * process-scoped singleton would keep it after the app reopens — which is
     * why nothing played until the user cleared the app from recents.
     */
    fun loadProject(project: VideoProject, startPositionMs: Long? = null, forceRebuild: Boolean = false) {
        if (released) return
        if (forceRebuild) {
            _lastError.value = null
            compositionFailed = false
            // A forced rebuild must create a BRAND-NEW CompositionPlayer.
            // Re-calling setComposition() on a player that already ran a
            // composition — and seekTo() on it — leaves Media3 1.11.1 parked
            // in STATE_BUFFERING forever: the device log shows rebuilds at
            // the playhead that never reported READY even after seconds,
            // while every first-ever load reached READY immediately.
            // Releasing the surface here is fine: the editor attaches a fresh
            // one via key(player) as soon as _activePlayer changes.
            runCatching { compositionPlayer?.release() }
            compositionPlayer = null
            buildCompositionPlayer()
        }
        val previous = currentProject
        val start = (startPositionMs ?: _currentPositionMs.value)
            .coerceIn(0L, project.totalDurationMs)
        val wasPlaying = _isPlayingInternal.value ||
            (_activePlayer.value?.playWhenReady == true)

        currentProject = project
        totalDurationMs = project.totalDurationMs
        _currentPositionMs.value = start

        val hasMedia = project.allVideoClips.any { !it.uriString.isNullOrBlank() } ||
            project.audioTracks.any { !it.uriString.isNullOrBlank() }
        if (!hasMedia) {
            AppLog.info(TAG, "Project has no playable media; keeping the Compose fallback preview")
            _activePlayer.value = null
            _usesCompositionPreview.value = false
            _isPlayingInternal.value = false
            fallbackPlayer?.pause()
            return
        }

        // Same project: keep the prepared composition (editing calls this method
        // after every mutation, so rebuilding identical input would stutter) —
        // unless the rebuild was forced because the player is known-broken.
        if (!forceRebuild &&
            previous == project &&
            lastCompositionProject == project &&
            _activePlayer.value != null
        ) {
            _activePlayer.value?.let { player ->
                if (startPositionMs != null) seekPlayer(player, start)
            }
            // The prepared player is unchanged, so its play state must be left
            // alone. This method is also called from startPlayback() and from
            // debounced edit refreshes: pausing here could cancel a play() that
            // was still settling after a timeline scrub. The caller owns the
            // play state (startPlayback calls play() right after this returns).
            return
        }

        if (!compositionFailed) {
            val result = runCatching {
                ProjectCompositionFactory.buildPreview(project, watermarkEnabled)
            }
            val built = result.getOrNull()
            val player = compositionPlayer
            if (built != null && player != null) {
                try {
                    player.setComposition(built.composition, start)
                    player.prepare()
                    player.playWhenReady = wasPlaying
                    _activePlayer.value = player
                    _usesCompositionPreview.value = true
                    lastCompositionProject = project
                    _previewWarnings.value = built.warnings
                    _playbackState.value = player.playbackState
                    _isPlayingInternal.value = player.isPlaying
                    _lastError.value = null
                    built.warnings.forEach { AppLog.warn(TAG, it) }
                    AppLog.info(
                        TAG,
                        "Composition preview loaded: ${built.width}x${built.height}, " +
                            "${built.durationMs}ms, ${built.warnings.size} warning(s)"
                    )
                    return
                } catch (error: Throwable) {
                    AppLog.error(TAG, "CompositionPlayer rejected the composition; falling back", error)
                    compositionFailed = true
                    runCatching { player.release() }
                    compositionPlayer = null
                }
            } else if (result.exceptionOrNull() != null) {
                AppLog.error(
                    TAG,
                    "Could not build the preview composition; falling back",
                    result.exceptionOrNull()
                )
            }
        }

        loadFallback(project, start, wasPlaying)
    }

    private fun loadFallback(project: VideoProject, startMs: Long, wasPlaying: Boolean) {
        val player = buildFallbackPlayer()
        if (player == null) {
            _activePlayer.value = null
            _usesCompositionPreview.value = false
            _isPlayingInternal.value = false
            return
        }

        val mediaItems = mutableListOf<MediaItem>()
        val slots = mutableListOf<Slot>()
        var cursor = 0L
        project.clips.forEach { clip ->
            val end = cursor + clip.durationMs
            val uri = clip.uriString
            if (!uri.isNullOrBlank()) {
                val mediaIndex = mediaItems.size
                val clipping = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                    .apply {
                        if (clip.trimEndMs > clip.trimStartMs) {
                            setEndPositionMs(clip.trimEndMs)
                        }
                    }
                    .build()
                mediaItems += MediaItem.Builder()
                    .setUri(Uri.parse(uri))
                    .setClippingConfiguration(clipping)
                    .build()
                slots += Slot(clip, cursor, end, mediaIndex)
            } else {
                slots += Slot(clip, cursor, end, -1)
            }
            cursor = end
        }

        fallbackSlots = slots
        _activePlayer.value = player
        _usesCompositionPreview.value = false
        _previewWarnings.value = emptyList()

        if (mediaItems.isEmpty()) {
            player.clearMediaItems()
            _currentPositionMs.value = 0L
            _isPlayingInternal.value = false
            return
        }

        player.setMediaItems(mediaItems)
        player.prepare()
        player.playWhenReady = wasPlaying
        _currentPositionMs.value = startMs
        seekFallback(startMs)
        AppLog.warn(TAG, "Preview running on the ExoPlayer fallback path")
    }

    private fun handlePlayerError(error: PlaybackException) {
        AppLog.error(TAG, "Preview playback failed", error)
        val wasPlaying = _isPlayingInternal.value
        _lastError.value = error.message ?: "Preview playback failed"
        _isPlayingInternal.value = false
        if (_usesCompositionPreview.value) {
            // A driver/GL failure must not leave a frozen preview. Rebuild the
            // same project as a plain playlist.
            AppLog.warn(TAG, "Switching to ExoPlayer fallback after composition error")
            compositionFailed = true
            runCatching { compositionPlayer?.release() }
            compositionPlayer = null
            _usesCompositionPreview.value = false
            currentProject?.let { project ->
                loadFallback(
                    project = project,
                    startMs = _currentPositionMs.value,
                    wasPlaying = wasPlaying
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Playback controls
    // ------------------------------------------------------------------

    /**
     * Starts playback.
     *
     * Returns true when the play request was actually handed to the player and
     * false when it was deferred (the player was still BUFFERING) or dropped
     * (no player). The caller must retry on false — otherwise a deferred play
     * would be lost and the preview would stay silent.
     */
    fun play(): Boolean {
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
                loadProject(project, _currentPositionMs.value, forceRebuild = true)
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
            val safeStart = safeStartPositionMs()
            if (safeStart != _currentPositionMs.value) {
                AppLog.info(
                    TAG,
                    "play(): moving start off a clip's last frame " +
                        "${_currentPositionMs.value}ms -> ${safeStart}ms"
                )
                seekTo(safeStart)
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

    fun pause() {
        playRequested = false
        _activePlayer.value?.pause()
        // Never let an async player callback resurrect "playing" after a pause.
        _isPlayingInternal.value = false
    }

    /** Seeks to a position on the *project* timeline. */
    fun seekTo(positionMs: Long) {
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

    /** Safe playback start: never on the last frame of an outgoing clip. */
    private fun safeStartPositionMs(positionMs: Long = _currentPositionMs.value): Long {
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
     * CompositionPlayer 1.11.1 cannot seek reliably: after a scrub seek it
     * parks in STATE_BUFFERING forever (device log: even minutes of waiting
     * never reported READY, and a full setComposition() rebuild on the same
     * instance never did either), and the old one-frame nudge only faked
     * progress because [seekTo] writes the target into the position flow
     * synchronously without any frame being rendered. A brand-new player
     * instance prepared at the playhead is the only path proven to reach
     * READY — every first-ever load in the log did.
     */
    fun rebuildPreviewAt(positionMs: Long) {
        val project = currentProject ?: return
        val at = safeStartPositionMs(positionMs.coerceIn(0L, totalDurationMs))
        AppLog.warn(TAG, "Rebuilding the preview player at ${at}ms")
        // Pause while the old instance is still alive; loadProject's forced
        // rebuild then releases it and prepares a fresh one at [at].
        pause()
        loadProject(project, at, forceRebuild = true)
    }

    private fun seekPlayer(player: Player, positionMs: Long) {
        if (_usesCompositionPreview.value) {
            player.seekTo(positionMs.coerceIn(0L, totalDurationMs))
        } else {
            seekFallback(positionMs)
        }
    }

    private fun seekFallback(positionMs: Long) {
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
    fun syncPosition() {
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

    fun setVolume(volume: Float) {
        _activePlayer.value?.volume = volume.coerceIn(0f, 1f)
    }

    /** Rebuilds the preview because the watermark is burned into the graph. */
    fun setWatermarkEnabled(enabled: Boolean) {
        if (watermarkEnabled == enabled) return
        watermarkEnabled = enabled
        currentProject?.let { project ->
            lastCompositionProject = null
            loadProject(project, _currentPositionMs.value)
        }
    }

    fun release() {
        released = true
        runCatching { compositionPlayer?.release() }
        compositionPlayer = null
        runCatching { fallbackPlayer?.release() }
        fallbackPlayer = null
        _activePlayer.value = null
        _isPlayingInternal.value = false
    }

    // ------------------------------------------------------------------
    // Host lifecycle recovery
    // ------------------------------------------------------------------

    /**
     * This manager is a process-scoped singleton: it outlives the activity.
     * When the app is only backgrounded (not killed), a player whose surface
     * died can end up errored/idle, and on reopen nothing would play — not
     * even freshly imported media — until the user cleared the app from
     * recents. The editor UI calls these two hooks around foreground events;
     * on resume we rebuild a broken player at the same playhead.
     */
    private var hostBackgrounded = false

    fun onHostPause() {
        hostBackgrounded = true
    }

    fun onHostResume() {
        if (!hostBackgrounded) return
        hostBackgrounded = false
        val project = currentProject ?: return
        if (PlayerRecoveryPolicy.shouldRebuildOnHostResume(
                backgrounded = true,
                lastError = _lastError.value,
                playbackState = _playbackState.value
            )
        ) {
            AppLog.warn(TAG, "Preview player is idle after the host returned; rebuilding it")
            loadProject(project, _currentPositionMs.value, forceRebuild = true)
        }
    }
}
