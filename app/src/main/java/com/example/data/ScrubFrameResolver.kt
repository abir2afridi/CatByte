package com.example.data

import com.example.model.MediaClip
import com.example.model.VideoProject

/**
 * Maps a position on the *project* timeline to a frame of the source media that
 * should be shown while the user scrubs.
 *
 * This is deliberately independent of Media3's preview player: during a drag the
 * composition player is left alone (every seek blacks its surface out for the
 * duration of the seek) and the preview draws frames extracted straight from the
 * source file instead. The resolver is pure so the mapping is unit-tested.
 */
object ScrubFrameResolver {

    /** One source frame request: which file to read and where inside it. */
    data class FrameSource(
        val clipId: String,
        val uriString: String,
        /** Position inside the source media, already accounting for trim + speed. */
        val sourceTimeMs: Long,
        /** Full source length, used to clamp the request inside the file. */
        val sourceDurationMs: Long
    )

    /**
     * Resolves [timelineMs] to the main-track clip under the playhead. When the
     * playhead is past the main track (or the main clip has no media) the
     * top-most active overlay clip is used instead, matching what the compositor
     * would show.
     */
    fun resolve(project: VideoProject, timelineMs: Long): FrameSource? {
        val positionMs = timelineMs.coerceAtLeast(0L)

        mainClipAt(project, positionMs)?.let { (clip, localMs) ->
            frameSource(clip, localMs)?.let { return it }
        }
        overlayClipAt(project, positionMs)?.let { (clip, localMs) ->
            frameSource(clip, localMs)?.let { return it }
        }
        return null
    }

    private fun mainClipAt(project: VideoProject, positionMs: Long): Pair<MediaClip, Long>? {
        if (project.clips.isEmpty()) return null
        var cursorMs = 0L
        project.clips.forEachIndexed { index, clip ->
            val clipEndMs = cursorMs + clip.durationMs
            if (positionMs < clipEndMs) {
                val localMs = (positionMs - cursorMs).coerceIn(0L, clip.durationMs)
                return clip to localMs
            }
            // Exactly at the project end (seekTo clamps there): hold the final
            // frame. Past it the main sequence is a gap, so overlays take over.
            if (index == project.clips.lastIndex && positionMs == clipEndMs) {
                return clip to clip.durationMs
            }
            cursorMs = clipEndMs
        }
        return null
    }

    private fun overlayClipAt(project: VideoProject, positionMs: Long): Pair<MediaClip, Long>? {
        // Later tracks are composited on top, so search them first.
        for (track in project.overlayTracks.asReversed()) {
            val clip = track.clips.asReversed().firstOrNull { candidate ->
                positionMs >= candidate.startTimelineMs &&
                    positionMs <= candidate.startTimelineMs + candidate.durationMs
            } ?: continue
            return clip to (positionMs - clip.startTimelineMs).coerceIn(0L, clip.durationMs)
        }
        return null
    }

    private fun frameSource(clip: MediaClip, localMs: Long): FrameSource? {
        val uri = clip.uriString?.takeIf { it.isNotBlank() } ?: return null
        val sourceDurationMs = clip.originalDurationMs.coerceAtLeast(1L)
        val sourceOffsetMs = ClipTiming.sourceOffsetForOutputMs(clip, localMs)
        val sourceTimeMs = (clip.trimStartMs.coerceAtLeast(0L) + sourceOffsetMs)
            .coerceIn(0L, sourceDurationMs - 1L)
        return FrameSource(
            clipId = clip.id,
            uriString = uri,
            sourceTimeMs = sourceTimeMs,
            sourceDurationMs = sourceDurationMs
        )
    }
}
