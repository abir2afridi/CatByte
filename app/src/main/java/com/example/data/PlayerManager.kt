package com.example.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
 * Wraps an [ExoPlayer] instance that plays the project clips sequentially.
 * Exposes playback position and playing state as [StateFlow]s so the Compose
 * UI and [com.example.viewmodel.EditorViewModel] can observe them.
 */
class PlayerManager(context: Context) {

    companion object {
        private const val TAG = "PlayerManager"
    }

    private val _player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
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
                AppLog.error("PlayerManager", "Playback failed", error)
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

    /**
     * Every clip of the timeline with the position it occupies. Clips without a
     * media uri (the drawn placeholder clips) are part of the timeline but are
     * not loaded into the player.
     */
    private data class Slot(
        val clip: MediaClip,
        val startMs: Long,
        val endMs: Long,
        val mediaIndex: Int
    )

    private var slots: List<Slot> = emptyList()
    private var mediaIndexToSlot: Map<Int, Slot> = emptyMap()

    val exoPlayer: ExoPlayer get() = _player

    /** Loads all playable clips of the project into the player as a playlist. */
    fun loadProject(project: VideoProject) {
        AppLog.info(PlayerManager.TAG, "loadProject() called: projectId=${project.id}, clips=${project.clips.size}, totalDuration=${project.totalDurationMs}ms")
        _totalDurationMs = project.totalDurationMs

        val newSlots = mutableListOf<Slot>()
        val mediaItems = mutableListOf<MediaItem>()
        var cursor = 0L
        project.clips.forEach { clip ->
            val end = cursor + clip.durationMs
            val uri = clip.uriString
            if (!uri.isNullOrBlank()) {
                val mediaIndex = mediaItems.size
                AppLog.debug(TAG, "Loading clip: id=${clip.id}, uri=$uri, timelinePos=${cursor}-${end}ms, mediaIndex=$mediaIndex")
                mediaItems += MediaItem.fromUri(Uri.parse(uri))
                newSlots += Slot(clip, cursor, end, mediaIndex)
            } else {
                AppLog.debug(TAG, "Skipping clip without URI: id=${clip.id}, title=${clip.title}")
                newSlots += Slot(clip, cursor, end, -1)
            }
            cursor = end
        }

        slots = newSlots
        mediaIndexToSlot = newSlots.filter { it.mediaIndex >= 0 }.associateBy { it.mediaIndex }
        AppLog.info(TAG, "Slots prepared: totalSlots=${slots.size}, playableItems=${mediaItems.size}")

        if (mediaItems.isEmpty()) {
            AppLog.warn(PlayerManager.TAG, "No playable media items in project")
            _player.clearMediaItems()
            _currentPositionMs.value = 0L
            _isPlayingInternal.value = false
            return
        }

        AppLog.info(PlayerManager.TAG, "Setting ${mediaItems.size} media items on ExoPlayer")
        _player.setMediaItems(mediaItems)
        _player.prepare()
        _currentPositionMs.value = 0L
        AppLog.info(PlayerManager.TAG, "Player prepared successfully")
    }

    fun play() {
        AppLog.info(PlayerManager.TAG, "play() called, mediaItemCount=${_player.mediaItemCount}, currentPos=${_currentPositionMs.value}ms")
        if (_player.mediaItemCount == 0) {
            AppLog.warn(PlayerManager.TAG, "play() called but no media items loaded")
            return
        }
        _player.playWhenReady = true
        _player.play()
        AppLog.debug(PlayerManager.TAG, "play() executed, playWhenReady=${_player.playWhenReady}")
    }

    fun pause() {
        AppLog.info(PlayerManager.TAG, "pause() called, currentPos=${_currentPositionMs.value}ms")
        _player.pause()
    }

    /** Seeks to a position on the *project* timeline. */
    fun seekTo(positionMs: Long) {
        AppLog.info(PlayerManager.TAG, "seekTo() called: requestedPos=${positionMs}ms, totalDuration=${_totalDurationMs}ms")
        val clamped = positionMs.coerceIn(0L, _totalDurationMs)
        val slot = slots.firstOrNull { clamped >= it.startMs && clamped < it.endMs }
            ?: slots.lastOrNull { it.startMs <= clamped }

        if (slot != null && slot.mediaIndex >= 0) {
            val seekPos = (clamped - slot.startMs).coerceAtLeast(0L)
            AppLog.info(PlayerManager.TAG, "Seeking to mediaIndex=${slot.mediaIndex}, clipStart=${slot.startMs}ms, seekPos=${seekPos}ms")
            _player.seekTo(slot.mediaIndex, seekPos)
        } else {
            AppLog.warn(PlayerManager.TAG, "No valid slot for position=${clamped}ms, trying next playable")
            // The playhead sits on a placeholder clip: jump to the next playable one.
            val next = slots.firstOrNull { it.mediaIndex >= 0 && it.startMs > clamped }
            if (next != null) {
                AppLog.info(PlayerManager.TAG, "Seeking to next playable mediaIndex=${next.mediaIndex}")
                _player.seekTo(next.mediaIndex, 0L)
            }
        }
        _currentPositionMs.value = clamped
    }

    /** Must be called periodically (e.g. from a coroutine) to sync the position. */
    fun syncPosition() {
        if (_player.mediaItemCount == 0) {
            AppLog.debug(PlayerManager.TAG, "syncPosition() called but no media items")
            return
        }
        val index = _player.currentMediaItemIndex.coerceAtLeast(0)
        val slot = mediaIndexToSlot[index]
        val offset = slot?.startMs ?: 0L
        val newPos = (offset + _player.currentPosition).coerceIn(0L, _totalDurationMs)
        if (kotlin.math.abs(newPos - _currentPositionMs.value) > 500) { // Log only significant changes
            AppLog.debug(PlayerManager.TAG, "syncPosition: mediaIndex=$index, slotStart=${offset}ms, playerPos=${_player.currentPosition}ms, newPos=${newPos}ms")
        }
        _currentPositionMs.value = newPos
    }

    /** Sets the volume of the whole preview (0f - 1f). */
    fun setVolume(volume: Float) {
        _player.volume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        runCatching { _player.release() }
    }
}
