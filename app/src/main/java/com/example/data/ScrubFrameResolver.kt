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

    /**
     * The clip transform the preview player applies to the source video.
     *
     * Mirrored from `ProjectCompositionFactory.ClipTransformEffect` so the
     * extracted scrub frame is drawn with the same geometry as the composed
     * preview. Without it a rotated or scaled clip changed shape the moment
     * the scrub overlay appeared.
     */
    data class ClipTransform(
        val scale: Float = 1f,
        val rotationDegrees: Float = 0f,
        val mirrored: Boolean = false,
        /** Horizontal position, 0 = left edge, 1 = right edge. */
        val posX: Float = 0.5f,
        /** Vertical position, 0 = top edge, 1 = bottom edge. */
        val posY: Float = 0.5f
    ) {
        /** True when the frame can be drawn unchanged. */
        val isIdentity: Boolean
            get() = !mirrored &&
                kotlin.math.abs(scale - 1f) < 0.001f &&
                kotlin.math.abs(rotationDegrees) < 0.001f &&
                kotlin.math.abs(posX - 0.5f) < 0.001f &&
                kotlin.math.abs(posY - 0.5f) < 0.001f
    }

    /** One source frame request: which file to read and where inside it. */
    data class FrameSource(
        val clipId: String,
        val uriString: String,
        /** Position inside the source media, already accounting for trim + speed. */
        val sourceTimeMs: Long,
        /** Full source length, used to clamp the request inside the file. */
        val sourceDurationMs: Long,
        /** Transform the preview player applies to this clip. */
        val transform: ClipTransform = ClipTransform()
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
            frameSource(clip, localMs, isOverlay = false)?.let { return it }
        }
        overlayClipAt(project, positionMs)?.let { (clip, localMs) ->
            frameSource(clip, localMs, isOverlay = true)?.let { return it }
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

    private fun frameSource(
        clip: MediaClip,
        localMs: Long,
        isOverlay: Boolean
    ): FrameSource? {
        val uri = clip.uriString?.takeIf { it.isNotBlank() } ?: return null
        val sourceDurationMs = clip.originalDurationMs.coerceAtLeast(1L)
        val sourceOffsetMs = ClipTiming.sourceOffsetForOutputMs(clip, localMs)
        val sourceTimeMs = (clip.trimStartMs.coerceAtLeast(0L) + sourceOffsetMs)
            .coerceIn(0L, sourceDurationMs - 1L)
        return FrameSource(
            clipId = clip.id,
            uriString = uri,
            sourceTimeMs = sourceTimeMs,
            sourceDurationMs = sourceDurationMs,
            transform = transformOf(clip, localMs, isOverlay)
        )
    }

    /**
     * The transform the preview player applies to this clip.
     *
     * Main track: the full keyframe-aware transform (scale, rotation, mirror,
     * position) of `ProjectCompositionFactory.ClipTransformEffect`. Overlay
     * (PIP) clips only get mirrored/rotated — their position is handled by the
     * compositor, not by the clip effect.
     */
    private fun transformOf(
        clip: MediaClip,
        localMs: Long,
        isOverlay: Boolean
    ): ClipTransform {
        if (isOverlay) {
            return ClipTransform(
                scale = 1f,
                rotationDegrees = clip.rotation,
                mirrored = clip.isMirrored
            )
        }
        return ClipTransform(
            scale = KeyframeInterpolator.getScale(clip, localMs).coerceAtLeast(0.01f),
            rotationDegrees = KeyframeInterpolator.getRotation(clip, localMs),
            mirrored = clip.isMirrored,
            posX = KeyframeInterpolator.getPosX(clip, localMs).coerceIn(0f, 1f),
            posY = KeyframeInterpolator.getPosY(clip, localMs).coerceIn(0f, 1f)
        )
    }
}
