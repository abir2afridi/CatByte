package com.example.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps an [ExoPlayer] instance that plays the project's clips sequentially.
 * Exposes playback position and playing state as [StateFlow]s so the Compose
 * UI and [com.example.viewmodel.EditorViewModel] can observe them.
 */
class PlayerManager(context: Context) {

    private val _player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _isPlayingInternal.value = state == Player.STATE_READY && playWhenReady
                if (state == Player.STATE_ENDED) {
                    _currentPositionMs.value = _totalDurationMs
                    _isPlayingInternal.value = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingInternal.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                _lastError.value = error.message ?: "Playback error"
            }
        })
    }

    private val _isPlayingInternal = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlayingInternal.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var _totalDurationMs: Long = 0L
    private var clipStartOffsets: List<Long> = emptyList()
    private var clips: List<MediaClip> = emptyList()

    val exoPlayer: ExoPlayer get() = _player

    /** Loads all clips of the project into the player as a playlist. */
    fun loadProject(project: VideoProject) {
        clips = project.clips
        _totalDurationMs = project.totalDurationMs
        clipStartOffsets = buildList {
            var acc = 0L
            for (clip in clips) {
                add(acc)
                acc += clip.durationMs
            }
        }
        val mediaItems = clips.mapNotNull { clip ->
            clip.uriString?.let { Uri.parse(it) }?.let { MediaItem.fromUri(it) }
        }
        _player.setMediaItems(mediaItems)
        _player.prepare()
        _currentPositionMs.value = 0L
    }

    fun play() {
        _player.playWhenReady = true
        _player.play()
    }

    fun pause() {
        _player.pause()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _totalDurationMs)
        // Find which clip contains this position and seek within it.
        val index = clipStartOffsets.indexOfLast { it <= clamped }
        if (index >= 0 && index < clips.size) {
            val localMs = clamped - clipStartOffsets[index]
            _player.seekTo(index, localMs)
        }
        _currentPositionMs.value = clamped
    }

    /** Must be called periodically (e.g. from a coroutine) to sync position. */
    fun syncPosition() {
        val index = _player.currentMediaItemIndex.coerceAtLeast(0)
        val offset = clipStartOffsets.getOrNull(index) ?: 0L
        _currentPositionMs.value = offset + _player.currentPosition
    }

    fun release() {
        _player.release()
    }
}
