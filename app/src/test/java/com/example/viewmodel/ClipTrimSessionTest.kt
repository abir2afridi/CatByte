package com.example.viewmodel

import com.example.engine.ClipTrimHandle
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the inline trim drag rules in [ClipTrimSession] (own file so future
 * edits elsewhere cannot dilute them):
 *  - trim changes only the source in/out points;
 *  - the main track is magnetic: a duration change ripples the clips after it;
 *  - overlay clips are absolute: a start trim keeps the end pinned;
 *  - application is idempotent (cumulative delta against the drag-start
 *    snapshot), and locked tracks refuse the edit.
 */
class ClipTrimSessionTest {

    private fun twoClipProject(): VideoProject = VideoProject(
        id = "p1",
        title = "Session",
        clips = listOf(
            MediaClip(
                id = "a", title = "A",
                durationMs = 3_000L, originalDurationMs = 10_000L,
                trimStartMs = 0L, trimEndMs = 3_000L
            ),
            MediaClip(
                id = "b", title = "B",
                startTimelineMs = 3_000L,
                durationMs = 2_000L, originalDurationMs = 2_000L
            )
        )
    )

    @Test
    fun `end trim inward shortens and ripples the following clip left`() {
        val project = twoClipProject()
        val session = ClipTrimSession(
            clipId = "a",
            edge = ClipTrimHandle.Edge.END,
            baseClip = project.clips.first(),
            baseProject = project
        )

        val updated = session.applyTo(project, -1_000L, fps = 30)!!

        assertEquals(2_010L, updated.clips[0].durationMs)
        assertEquals(2_010L, updated.clips[1].startTimelineMs)
    }

    @Test
    fun `end trim outward restores the hidden source and ripples right`() {
        val project = twoClipProject()
        val session = ClipTrimSession(
            clipId = "a",
            edge = ClipTrimHandle.Edge.END,
            baseClip = project.clips.first(),
            baseProject = project
        )

        val restored = session.applyTo(project, 1_000L, fps = 30)!!

        assertEquals(3_990L, restored.clips[0].durationMs)
        assertEquals(3_990L, restored.clips[0].trimEndMs)
        assertEquals(3_990L, restored.clips[1].startTimelineMs)
    }

    @Test
    fun `repeated application of the same cumulative delta is idempotent`() {
        val project = twoClipProject()
        val session = ClipTrimSession(
            clipId = "a",
            edge = ClipTrimHandle.Edge.END,
            baseClip = project.clips.first(),
            baseProject = project
        )

        val once = session.applyTo(project, -1_000L, fps = 30)!!
        // Same cumulative delta against the already-applied state: no change,
        // so the session reports null and the host skips the state write.
        val twice = session.applyTo(once, -1_000L, fps = 30) ?: once

        assertEquals(once, twice)
    }

    @Test
    fun `overlay start trim keeps the end pinned`() {
        val overlay = MediaClip(
            id = "ov", title = "Pip",
            startTimelineMs = 1_000L,
            durationMs = 3_000L, originalDurationMs = 5_000L,
            trimStartMs = 1_000L, trimEndMs = 4_000L
        )
        val project = VideoProject(
            id = "p2",
            title = "Overlay",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 6_000L, originalDurationMs = 6_000L)
            ),
            overlayTracks = listOf(VideoTrack(id = "t", name = "T", isMainTrack = false, clips = listOf(overlay)))
        )
        val session = ClipTrimSession(
            clipId = "ov",
            edge = ClipTrimHandle.Edge.START,
            baseClip = overlay,
            baseProject = project
        )

        // Drag the start edge left (outward): -1s quantises to -990ms of
        // source at 30fps. The clip grows and its start moves left so the END
        // edge stays exactly where it was.
        val updated = session.applyTo(project, -1_000L, fps = 30)!!

        val pip = updated.overlayTracks.first().clips.first()
        assertEquals(3_990L, pip.durationMs)
        assertEquals(10L, pip.startTimelineMs)
        assertEquals(4_000L, pip.startTimelineMs + pip.durationMs)
        // The main track is untouched.
        assertEquals(6_000L, updated.clips.first().durationMs)
    }

    @Test
    fun `locked track refuses the trim`() {
        val project = twoClipProject().copy(isMainTrackLocked = true)
        val session = ClipTrimSession(
            clipId = "a",
            edge = ClipTrimHandle.Edge.END,
            baseClip = project.clips.first(),
            baseProject = project
        )

        assertNull(session.applyTo(project, -1_000L, fps = 30))
    }

    @Test
    fun `sub frame delta is a no-op`() {
        val project = twoClipProject()
        val session = ClipTrimSession(
            clipId = "a",
            edge = ClipTrimHandle.Edge.END,
            baseClip = project.clips.first(),
            baseProject = project
        )

        assertNull(session.applyTo(project, 5L, fps = 30))
    }

    @Test
    fun `outward end trim never passes the real source end`() {
        val project = twoClipProject()
        val session = ClipTrimSession(
            clipId = "a",
            edge = ClipTrimHandle.Edge.END,
            baseClip = project.clips.first(),
            baseProject = project
        )

        val maxed = session.applyTo(project, 60_000L, fps = 30)

        assertNotNull(maxed)
        assertEquals(10_000L, maxed!!.clips[0].trimEndMs)
        assertEquals(10_000L, maxed.clips[0].durationMs)
        assertEquals(10_000L, maxed.clips[1].startTimelineMs)
    }
}
