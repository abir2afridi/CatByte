package com.example.data.player

import android.net.Uri
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.CompositionPlayer
import com.example.data.AppLog
import com.example.data.CanvasBackgroundLoader
import com.example.data.PlayerManager
import com.example.data.PlayerManager.Companion.TAG
import com.example.data.ProjectCompositionFactory
import com.example.model.VideoProject

/**
 * Preview player construction and project loading.
 *
 * The preferred path is a [CompositionPlayer] fed by exactly the same
 * composition that the exporter uses, so the preview cannot drift from the
 * final MP4. If the device cannot run a composition, the manager falls back to
 * a plain [ExoPlayer] playlist.
 */

/** Builds the CompositionPlayer (or latches the composition path off). */
@androidx.annotation.OptIn(ExperimentalApi::class)
internal fun PlayerManager.buildCompositionPlayer() {
    if (released) return
    compositionPlayer = try {
        CompositionPlayer.Builder(appContext)
            .setLooper(Looper.getMainLooper())
            .setVideoGraphFactory(MultipleInputVideoGraph.Factory())
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            // The meter taps the real sink output, so the on-canvas level is
            // measured audio rather than a decorative animation.
            .setAudioSink(buildMeteredAudioSink(appContext))
            .build()
            .also { it.addListener(playerListener) }
    } catch (error: Throwable) {
        compositionFailed = true
        AppLog.warn(TAG, "CompositionPlayer unavailable; using ExoPlayer fallback", error)
        null
    }
}

/** Creates (once) the ExoPlayer used when the composition path is unavailable. */
internal fun PlayerManager.buildFallbackPlayer(): ExoPlayer? {
    if (released) return null
    return fallbackPlayer ?: try {
        ExoPlayer.Builder(appContext, meteredRenderersFactory(appContext))
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
internal fun PlayerManager.loadProjectInternal(
    project: VideoProject,
    startPositionMs: Long? = null,
    forceRebuild: Boolean = false
) {
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
            // The built Composition is immutable, so it is reused while the
            // project (and watermark flag) are unchanged. Scrub rebuilds and
            // debounced edit refreshes then skip the ~150 ms factory pass
            // and only re-prepare the player at the new start position.
            val cached = cachedBuild
            if (cached != null &&
                cachedBuildProject == project &&
                cachedBuildWatermark == watermarkEnabled
            ) {
                cached
            } else {
                ProjectCompositionFactory.buildPreview(
                    project,
                    watermarkEnabled,
                    backgroundImage = CanvasBackgroundLoader.load(appContext, project.backgroundImageUri)
                ).also {
                    cachedBuild = it
                    cachedBuildProject = project
                    cachedBuildWatermark = watermarkEnabled
                }
            }
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

/** Loads the project as a plain ExoPlayer playlist (composition fallback). */
internal fun PlayerManager.loadFallback(project: VideoProject, startMs: Long, wasPlaying: Boolean) {
    val player = buildFallbackPlayer()
    if (player == null) {
        _activePlayer.value = null
        _usesCompositionPreview.value = false
        _isPlayingInternal.value = false
        return
    }

    val mediaItems = mutableListOf<MediaItem>()
    val slots = mutableListOf<PlayerManager.Slot>()
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
            slots += PlayerManager.Slot(clip, cursor, end, mediaIndex)
        } else {
            slots += PlayerManager.Slot(clip, cursor, end, -1)
        }
        cursor = end
    }

    fallbackSlots = slots
    _activePlayer.value = player
    _usesCompositionPreview.value = false
    val hasIndependentAudio = project.audioTracks.any { !it.uriString.isNullOrBlank() }
    _previewWarnings.value = if (hasIndependentAudio) {
        listOf(
            "Independent audio is unavailable in the device fallback preview; " +
                "the exported file still uses the full mixer."
        )
    } else {
        emptyList()
    }
    if (mediaItems.isNotEmpty()) {
        // The composition path failed, but the fallback preview is a
        // working substitute: a red "Preview error" pill over it would be
        // stale noise. If the fallback itself fails, onPlayerError sets the
        // error again.
        _lastError.value = null
    }

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

/** Turns a player error into a fallback playlist (or a visible error pill). */
internal fun PlayerManager.handlePlayerError(error: PlaybackException) {
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
