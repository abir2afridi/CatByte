package com.example.data

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.CompositionPlayer
import com.example.data.player.beginScrubMode
import com.example.data.player.buildCompositionPlayer
import com.example.data.player.cancelScrubMode
import com.example.data.player.clearPreviewErrorState
import com.example.data.player.endScrubKeepPlayingMode
import com.example.data.player.endScrubMode
import com.example.data.player.handleHostPause
import com.example.data.player.handleHostResume
import com.example.data.player.handlePlayerError
import com.example.data.player.loadProjectInternal
import com.example.data.player.pausePlayer
import com.example.data.player.playPlayer
import com.example.data.player.previewIsReadyState
import com.example.data.player.rebuildPreviewAtPosition
import com.example.data.player.releasePlayers
import com.example.data.player.seekPlayerTo
import com.example.data.player.setCompareOriginalState
import com.example.data.player.setPlayerVolume
import com.example.data.player.setWatermarkState
import com.example.data.player.syncPlayerPosition
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the preview player.
 *
 * The preferred path is a [CompositionPlayer] fed by exactly the same
 * [ProjectCompositionFactory] composition that the exporter uses. That means
 * multi-track overlays, keyframed PIP motion, text/stickers, colour grading,
 * speed curves and transitions are all visible in the live preview and cannot
 * drift from the final MP4.
 *
 * If the device cannot create or run a composition (for example a GPU/driver
 * limitation), the manager falls back to a plain [ExoPlayer] playlist and
 * exposes [usesCompositionPreview] = false so the UI can use its Compose
 * fallback renderer.
 *
 * This file owns the state; the behaviour lives in its own files under
 * `data/player/`:
 *  - `PlayerCompositionLoading.kt`: player construction and project loading.
 *  - `PlayerPlaybackControls.kt`: play, pause, seek, volume, position sync.
 *  - `PlayerScrubMode.kt`: the muted-but-rendering scrub gesture.
 *  - `PlayerHostLifecycle.kt`: recovery after the app was backgrounded.
 */
@UnstableApi
@androidx.annotation.OptIn(ExperimentalApi::class)
class PlayerManager(context: Context) {

    companion object {
        internal const val TAG = "PlayerManager"
    }

    internal val appContext: Context = context.applicationContext

    internal var compositionPlayer: CompositionPlayer? = null
    internal var fallbackPlayer: ExoPlayer? = null
    internal var compositionFailed = false
    internal var released = false

    internal val _activePlayer = MutableStateFlow<Player?>(null)
    val activePlayer: StateFlow<Player?> = _activePlayer.asStateFlow()

    internal val _usesCompositionPreview = MutableStateFlow(false)
    val usesCompositionPreview: StateFlow<Boolean> = _usesCompositionPreview.asStateFlow()

    internal val _isPlayingInternal = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlayingInternal.asStateFlow()

    /**
     * The user's last explicit intent. Media3's `pause()` is asynchronous, so
     * `player.isPlaying` can still be true for a tick afterwards; without this
     * flag `syncPosition()` used to resurrect a "playing" state right after a
     * pause, which made the next play()/loadProject() decision inconsistent.
     */
    internal var playRequested = false

    internal val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    internal val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    internal val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    internal val _previewWarnings = MutableStateFlow<List<String>>(emptyList())
    val previewWarnings: StateFlow<List<String>> = _previewWarnings.asStateFlow()

    internal var totalDurationMs: Long = 0L
    internal var currentProject: VideoProject? = null

    /**
     * The project as the user made it. [currentProject] is the preview
     * variant, which can differ while the "original" comparison is on.
     */
    internal var sourceProject: VideoProject? = null
    internal var lastCompositionProject: VideoProject? = null
    internal var watermarkEnabled: Boolean = false

    /**
     * Preview-only "compare with the original": the selected clip is shown
     * without its colour adjustments. The project itself keeps the grade, so
     * the export is unaffected.
     */
    internal var compareOriginal: Boolean = false
    internal var compareClipId: String? = null

    /** Last built composition, reused while the project is unchanged. */
    internal var cachedBuild: ProjectCompositionFactory.Result? = null
    internal var cachedBuildProject: VideoProject? = null
    internal var cachedBuildWatermark: Boolean = false

    /**
     * Timeline slots for the fallback playlist. The composition path does not
     * need them because Media3 maps the full timeline itself.
     */
    internal data class Slot(
        val clip: MediaClip,
        val startMs: Long,
        val endMs: Long,
        val mediaIndex: Int
    )

    internal var fallbackSlots: List<Slot> = emptyList()

    internal val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    internal val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = state
            if (state == Player.STATE_READY && _lastError.value != null) {
                // A player that reached READY is proof the preview works again.
                // Keeping the old string is what left the red "Preview error"
                // pill on screen long after a successful recovery (the fallback
                // path never cleared it).
                _lastError.value = null
            }
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
            this@PlayerManager.handlePlayerError(error)
        }
    }

    /** True while the host is in the background (recovery hook). */
    internal var hostBackgrounded = false

    /** True while a scrub gesture keeps the player rendering (muted). */
    internal var scrubbing = false
    internal var scrubRestoreVolume = 1f

    init {
        buildCompositionPlayer()
    }

    // ------------------------------------------------------------------
    // Public API (the behaviour lives in data/player/)
    // ------------------------------------------------------------------

    /**
     * Loads [project] into the best available preview path. When the project is
     * unchanged this is a no-op (apart from restoring the play state), which is
     * what makes debounced preview refresh cheap.
     */
    fun loadProject(project: VideoProject, startPositionMs: Long? = null, forceRebuild: Boolean = false) {
        loadProjectInternal(project, startPositionMs, forceRebuild)
    }

    /**
     * Starts playback.
     *
     * Returns true when the play request was actually handed to the player and
     * false when it was deferred (the player was still BUFFERING) or dropped
     * (no player). The caller must retry on false.
     */
    fun play(): Boolean = playPlayer()

    fun pause() {
        pausePlayer()
    }

    /** Seeks to a position on the *project* timeline. */
    fun seekTo(positionMs: Long) {
        seekPlayerTo(positionMs)
    }

    /**
     * Enters scrub mode: the player keeps rendering (muted) while the user drags
     * the timeline.
     */
    fun beginScrub() {
        beginScrubMode()
    }

    /** Stops the scrub rendering. The last frame stays on the surface. */
    fun endScrub() {
        endScrubMode()
    }

    /** Leaves a scrub playing (the user pressed Play while still dragging). */
    fun endScrubKeepPlaying() {
        endScrubKeepPlayingMode()
    }

    /** Abandons a scrub without landing a frame. */
    fun cancelScrub() {
        cancelScrubMode()
    }

    /** Drops a stale error once the preview is demonstrably working again. */
    fun clearPreviewError() {
        clearPreviewErrorState()
    }

    /** True while the preview can actually show a frame. */
    fun previewIsReady(): Boolean = previewIsReadyState()

    /**
     * Tears the preview player down and rebuilds a BRAND-NEW one prepared at
     * [positionMs].
     */
    fun rebuildPreviewAt(positionMs: Long) {
        rebuildPreviewAtPosition(positionMs)
    }

    /**
     * Periodically called by the ViewModel while playing. The composition path
     * reads Media3's real playhead, so the Compose timeline stays in sync.
     */
    fun syncPosition() {
        syncPlayerPosition()
    }

    fun setVolume(volume: Float) {
        setPlayerVolume(volume)
    }

    /** Rebuilds the preview because the watermark is burned into the graph. */
    fun setWatermarkEnabled(enabled: Boolean) {
        setWatermarkState(enabled)
    }

    /**
     * Shows the selected clip without its colour adjustments (preview only).
     *
     * The comparison needs a rebuild: the colour grade is part of the
     * composition, so the preview is rebuilt from the untouched project.
     */
    fun setCompareOriginal(enabled: Boolean, clipId: String?, rebuild: Boolean = true) {
        setCompareOriginalState(enabled, clipId, rebuild)
    }

    fun release() {
        releasePlayers()
    }

    /** Remembers that the host went to the background. */
    fun onHostPause() {
        handleHostPause()
    }

    /** Rebuilds a broken player when the host comes back. */
    fun onHostResume() {
        handleHostResume()
    }
}
