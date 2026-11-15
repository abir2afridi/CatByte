package com.example.viewmodel.features

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.MediaMetadata
import com.example.data.MediaVideoInfo
import com.example.engine.AspectMath
import com.example.model.MediaClip
import com.example.model.VideoFilter
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/** Importing media into the timeline (single clip and multi-URI batches). */
internal class ImportFeature(private val vm: EditorViewModel) {

    /** The running batch import, so a new import cancels the previous one. */
    private var importJob: Job? = null

    /** Cancels an in-flight batch import (called when the editor is cleared). */
    fun cancel() {
        importJob?.cancel()
        importJob = null
    }

    /** Adds a clip by looking up its real duration and waveform from the file. */
    suspend fun addClipFromUri(uriString: String, title: String) {
        val ctx = vm.context ?: return
        val uri = Uri.parse(uriString)
        AppLog.info("EditorViewModel", "addClipFromUri() started: uri=$uriString, title=$title")
        vm._isImporting.value = true
        try {
            AppLog.debug("EditorViewModel", "Fetching duration for uri=$uriString")
            val durationMs = MediaMetadata.durationMs(ctx, uri)
            val info = runCatching { MediaMetadata.videoInfo(ctx, uri) }.getOrNull()
            AppLog.debug("EditorViewModel", "Fetching waveform for uri=$uriString")
            val waveform = MediaMetadata.waveform(ctx, uri, 48) ?: emptyList()
            val realTitle = title.takeIf { it.isNotBlank() }
                ?: MediaMetadata.displayName(ctx, uri)
            // Place clip after existing clips on the timeline. A fresh project
            // (or one holding only a legacy placeholder) starts at zero and
            // discards the placeholder so the imported clip is visible.
            val currentProject = vm._currentProject.value
            val isFreshProject = currentProject == null ||
                currentProject.clips.all { it.isPlaceholderClip }
            val lastEndMs = if (isFreshProject) {
                0L
            } else {
                currentProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            }
            AppLog.debug("EditorViewModel", "Creating clip: title=$realTitle, startTimelineMs=$lastEndMs, durationMs=$durationMs, waveformSize=${waveform.size}")
            val clip = MediaClip(
                id = UUID.randomUUID().toString(),
                title = realTitle,
                uriString = uriString,
                startTimelineMs = lastEndMs,
                durationMs = if (durationMs > 0) durationMs else 4000L,
                originalDurationMs = if (durationMs > 0) durationMs else 4000L,
                waveform = waveform.ifEmpty { emptyList() },
                sourceWidth = info?.width ?: 0,
                sourceHeight = info?.height ?: 0,
                filter = VideoFilter.NONE
            )
            vm._isImporting.value = false
            vm.updateProject { proj ->
                val fresh = isFreshProject && proj.clips.all { it.isPlaceholderClip }
                val ratio = if (fresh) {
                    info?.let { AspectMath.fromVideo(it.width, it.height, it.rotation) }
                } else {
                    null
                }
                proj.copy(
                    aspectRatio = ratio ?: proj.aspectRatio,
                    clips = (if (fresh) emptyList() else proj.clips) + clip
                )
            }
            vm._selectedClipId.value = clip.id
            vm._currentPositionMs.value = clip.startTimelineMs
            vm.playerManager?.seekTo(clip.startTimelineMs)
            AppLog.info("EditorViewModel", "Successfully imported clip: $realTitle (${durationMs}ms), clipId=${clip.id}")
            // Reload the player with the updated project
        } catch (e: Exception) {
            vm._isImporting.value = false
            AppLog.error("EditorViewModel", "Failed to import clip from $uriString", e)
            // Still create a clip with fallback values so the user can at least see it
            val currentProject = vm._currentProject.value
            val lastEndMs = currentProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val fallbackClip = MediaClip(
                id = UUID.randomUUID().toString(),
                title = title.takeIf { it.isNotBlank() } ?: "Imported Clip",
                uriString = uriString,
                startTimelineMs = lastEndMs,
                durationMs = 4000L,
                originalDurationMs = 4000L,
                filter = VideoFilter.NONE
            )
            vm.updateProject { proj ->
                proj.copy(clips = proj.clips + fallbackClip)
            }
            vm._selectedClipId.value = fallbackClip.id
        }
    }

    /** Imports multiple URIs sequentially, then calls [onComplete] when all are done. */
    fun importMultipleUris(
        uris: List<String>,
        titles: List<String>,
        onComplete: () -> Unit
    ) {
        importJob?.cancel()
        importJob = vm.viewModelScope.launch {
            AppLog.info("EditorViewModel", "importMultipleUris() started: ${uris.size} URIs to import")
            vm._isImporting.value = true
            val built = mutableListOf<MediaClip>()
            val startingProject = vm._currentProject.value
            // A fresh project (new, or one that only contains a legacy demo
            // placeholder) starts at zero and adopts the first imported clip's
            // ratio/title. Placeholder-only timelines are dropped entirely so
            // the imported footage is what the user sees.
            val isFreshProject = startingProject == null ||
                startingProject.clips.all { it.isPlaceholderClip }
            var cursor = if (isFreshProject) {
                0L
            } else {
                startingProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            }
            var firstInfo: MediaVideoInfo? = null

            uris.forEachIndexed { index, uriString ->
                val ctx = vm.context
                AppLog.info("EditorViewModel", "Importing URI ${index + 1}/${uris.size}: $uriString")
                val info = if (ctx != null) {
                    runCatching { MediaMetadata.videoInfo(ctx, Uri.parse(uriString)) }.getOrNull()
                        ?: MediaVideoInfo(0)
                } else {
                    MediaVideoInfo(0)
                }
                if (firstInfo == null && info.width > 0 && info.height > 0) {
                    firstInfo = info
                }
                val fallbackTitle = titles.getOrNull(index)?.takeIf { it.isNotBlank() }
                val fileTitle = info.displayName.substringBeforeLast('.').takeIf { it.isNotBlank() }
                val realTitle = (fallbackTitle?.takeIf { !it.startsWith("Clip ") })
                    ?: fileTitle
                    ?: "Clip ${index + 1}"
                val durationMs = if (info.durationMs > 0L) info.durationMs else 4_000L
                built += MediaClip(
                    id = UUID.randomUUID().toString(),
                    title = realTitle,
                    uriString = uriString,
                    startTimelineMs = cursor,
                    durationMs = durationMs,
                    originalDurationMs = durationMs,
                    sourceWidth = info.width,
                    sourceHeight = info.height,
                    // Waveform decode is expensive; video clips use the default
                    // waveform until the user opens the Audio sheet.
                    waveform = emptyList(),
                    filter = VideoFilter.NONE
                )
                cursor += durationMs
            }

            vm.updateProject { proj ->
                val fresh = isFreshProject && proj.clips.all { it.isPlaceholderClip }
                val existingClips = if (fresh) emptyList() else proj.clips
                val renamed = if (fresh && built.isNotEmpty() &&
                    (proj.title.isBlank() || proj.title == "New Project" ||
                        proj.title.startsWith("New Project") || proj.title.startsWith("Untitled"))
                ) {
                    built.first().title
                } else {
                    proj.title
                }
                val ratio = if (fresh) {
                    firstInfo?.let { AspectMath.fromVideo(it.width, it.height, it.rotation) }
                } else {
                    null
                }
                proj.copy(
                    title = renamed,
                    aspectRatio = ratio ?: proj.aspectRatio,
                    clips = existingClips + built
                )
            }
            // Show the imported footage immediately instead of leaving the
            // playhead parked on the (now removed) placeholder at 0.
            val firstImportedStart = built.firstOrNull()?.startTimelineMs ?: 0L
            vm._currentPositionMs.value = firstImportedStart
            vm.playerManager?.seekTo(firstImportedStart)
            vm._selectedClipId.value = built.lastOrNull()?.id
            vm._isImporting.value = false
            AppLog.info("EditorViewModel", "importMultipleUris() completed ${built.size} clip(s)")
            onComplete()
        }
    }

    /** Launch import of multiple URIs from Composable (non-suspend context). */
    fun launchImportUris(uris: List<Uri>) {
        val uriStrings = uris.map { it.toString() }
        AppLog.info("EditorViewModel", "launchImportUris() called: ${uris.size} URIs")
        // Empty titles: the real file display names come from MediaMetadata.
        importMultipleUris(uriStrings, emptyList()) { }
    }
}
