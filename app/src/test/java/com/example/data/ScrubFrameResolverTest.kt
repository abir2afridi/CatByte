package com.example.data

import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the timeline -> source-frame mapping used by the live scrub preview.
 *
 * The preview cannot use Media3's CompositionPlayer for scrubbing (a seek blacks
 * its surface out until it settles), so frames are read from the source media
 * instead. If this mapping drifts, the preview shows the wrong frame while the
 * user scrolls, which is the exact bug these tests exist to prevent.
 */
class ScrubFrameResolverTest {

    @Test
    fun `main track resolves by cumulative clip durations`() {
        val project = project(
            clip(id = "a", durationMs = 1_000L),
            clip(id = "b", durationMs = 2_000L)
        )

        assertEquals("a" to 0L, resolve(project, 0L))
        assertEquals("a" to 999L, resolve(project, 999L))
        assertEquals("b" to 0L, resolve(project, 1_000L))
        assertEquals("b" to 1_999L, resolve(project, 2_999L))
    }

    @Test
    fun `trim and speed map output time to the matching source frame`() {
        val spedUp = clip(
            id = "fast",
            durationMs = 1_000L,
            originalDurationMs = 5_000L,
            trimStartMs = 500L,
            trimEndMs = 4_500L,
            speed = 2f
        )
        val project = project(spedUp)

        // 250 ms of 2x output = 500 ms of source, starting at the trim point.
        assertEquals("fast" to 1_000L, resolve(project, 250L))
        // 500 ms of output = 1_000 ms of source.
        assertEquals("fast" to 1_500L, resolve(project, 500L))
    }

    @Test
    fun `at the exact end the last frame is held`() {
        val project = project(clip(id = "only", durationMs = 3_000L, originalDurationMs = 3_000L))

        // seekTo() clamps to totalDurationMs, so this is the only "past the
        // content" position the editor can actually request.
        assertEquals("only" to 2_999L, resolve(project, 3_000L))
    }

    @Test
    fun `placeholder clips have no source frame`() {
        val project = project(
            clip(id = "placeholder", durationMs = 1_000L, uri = null)
        )

        assertNull(ScrubFrameResolver.resolve(project, 500L))
    }

    @Test
    fun `active overlay is used when the playhead is past the main track`() {
        val overlay = clip(
            id = "pip",
            durationMs = 2_000L,
            originalDurationMs = 2_000L,
            uri = "content://pip"
        ).copy(startTimelineMs = 1_000L)
        val project = VideoProject(
            id = "project",
            title = "Project",
            clips = listOf(clip(id = "main", durationMs = 1_000L)),
            overlayTracks = listOf(VideoTrack(id = "track", name = "PIP", clips = listOf(overlay)))
        )

        assertEquals("pip" to 500L, resolve(project, 1_500L))
    }

    @Test
    fun `the top-most active overlay wins`() {
        val lower = clip(
            id = "lower",
            durationMs = 2_000L,
            uri = "content://lower"
        ).copy(startTimelineMs = 0L)
        val upper = clip(
            id = "upper",
            durationMs = 2_000L,
            uri = "content://upper"
        ).copy(startTimelineMs = 0L)
        val project = VideoProject(
            id = "project",
            title = "Project",
            clips = listOf(clip(id = "main", durationMs = 1_000L, uri = null)),
            overlayTracks = listOf(
                VideoTrack(id = "track-1", name = "Lower", clips = listOf(lower)),
                VideoTrack(id = "track-2", name = "Upper", clips = listOf(upper))
            )
        )

        assertEquals("upper" to 400L, resolve(project, 400L))
    }

    private fun resolve(project: VideoProject, timelineMs: Long): Pair<String, Long>? =
        ScrubFrameResolver.resolve(project, timelineMs)?.let { it.clipId to it.sourceTimeMs }

    private fun project(vararg clips: MediaClip): VideoProject =
        VideoProject(id = "project", title = "Project", clips = clips.toList())

    private fun clip(
        id: String,
        durationMs: Long,
        originalDurationMs: Long = durationMs,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L,
        speed: Float = 1f,
        uri: String? = "content://$id"
    ): MediaClip = MediaClip(
        id = id,
        title = id,
        uriString = uri,
        durationMs = durationMs,
        originalDurationMs = originalDurationMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        speed = speed
    )
}
