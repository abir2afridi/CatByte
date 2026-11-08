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
            _isPlayingInternal.value = isPlaying
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
            if (compositionFailed) {
                // Retry the composition path once per forced rebuild; if it
                // errors again the fallback path continues as before.
                compositionFailed = false
                if (compositionPlayer == null) buildCompositionPlayer()
            }
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
            if (wasPlaying) play() else pause()
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

    fun play() {
        if (_activePlayer.value == null) {
            AppLog.warn(TAG, "play() called without an active preview player")
            return
        }
        // A player that errored sits in STATE_IDLE and silently ignores play()
        // (it needs prepare(), not play()). Rebuild it from the project so
        // pressing play always recovers instead of doing nothing.
        if (_playbackState.value == Player.STATE_IDLE) {
            currentProject?.let { project ->
                loadProject(project, _currentPositionMs.value, forceRebuild = true)
            }
        }
        val player = _activePlayer.value ?: return
        if (player.playbackState == Player.STATE_ENDED) {
            seekPlayer(player, 0L)
            _currentPositionMs.value = 0L
        }
        player.play()
    }

    fun pause() {
        _activePlayer.value?.pause()
        _isPlayingInternal.value = false
    }

    /** Seeks to a position on the *project* timeline. */
    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, totalDurationMs.coerceAtLeast(0L))
        _currentPositionMs.value = clamped
        val player = _activePlayer.value ?: return
        if (_usesCompositionPreview.value) {
            player.seekTo(clamped)
        } else {
            seekFallback(clamped)
        }
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
        _isPlayingInternal.value = player.isPlaying
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
        val broken = _lastError.value != null || _playbackState.value == Player.STATE_IDLE
        if (broken) {
            AppLog.warn(TAG, "Preview player is idle after the host returned; rebuilding it")
            loadProject(project, _currentPositionMs.value, forceRebuild = true)
        }
    }
}
