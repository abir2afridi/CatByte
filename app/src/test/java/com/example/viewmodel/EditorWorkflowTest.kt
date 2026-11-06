package com.example.viewmodel

import com.example.engine.ClipTrimHandle
import com.example.model.AspectRatioType
import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import com.example.model.TimelineTrackIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Editor state machine without Android media plumbing: repository is in
 * memory (no Context) and playback/export managers are left null.
 */
class EditorWorkflowTest {

    private fun newViewModel(): EditorViewModel {
        val repo = ProjectRepository()
        return EditorViewModel(repository = repo)
    }

    private fun vmWithOneClip(): EditorViewModel {
        val viewModel = newViewModel()
        val clip = MediaClip(id = "clip_1", title = "First", durationMs = 3000L, originalDurationMs = 3000L)
        val repo = ProjectRepository()
        val project = repo.createNewProject("Seeded", initialClips = listOf(clip))
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `empty repository starts without a current project`() {
        val viewModel = newViewModel()
        assertNull(viewModel.currentProject.value)
    }

    @Test
    fun `createNewProjectWithParams sets title ratio and paper style`() {
        val viewModel = newViewModel()
        viewModel.createNewProjectWithParams("Blueprint Test", AspectRatioType.RATIO_16_9, "Standard")

        val current = viewModel.currentProject.value
        assertNotNull(current)
        assertEquals("Blueprint Test", current?.title)
        assertEquals(AspectRatioType.RATIO_16_9, current?.aspectRatio)
        assertEquals("Standard", current?.paperStyle)
        assertTrue(viewModel.projects.value.any { it.id == current?.id })
    }

    @Test
    fun `setAspectRatio is undoable`() {
        val viewModel = vmWithOneClip()
        viewModel.setAspectRatio(AspectRatioType.RATIO_16_9)
        assertEquals(AspectRatioType.RATIO_16_9, viewModel.currentProject.value?.aspectRatio)

        viewModel.undo()
        assertEquals(AspectRatioType.RATIO_9_16, viewModel.currentProject.value?.aspectRatio)

        viewModel.redo()
        assertEquals(AspectRatioType.RATIO_16_9, viewModel.currentProject.value?.aspectRatio)
    }

    @Test
    fun `undo and redo flags follow the stacks`() {
        val viewModel = vmWithOneClip()
        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.setAspectRatio(AspectRatioType.RATIO_1_1)
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.undo()
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)

        viewModel.redo()
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
    }

    @Test
    fun `undo with empty history is a no-op`() {
        val viewModel = vmWithOneClip()
        val before = viewModel.currentProject.value
        viewModel.undo()
        assertEquals(before, viewModel.currentProject.value)
        viewModel.redo()
        assertEquals(before, viewModel.currentProject.value)
    }

    @Test
    fun `split at playhead divides the selected clip`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(1200L)
        viewModel.splitClipAtPlayhead()

        val clips = viewModel.currentProject.value?.clips.orEmpty()
        assertEquals(2, clips.size)
        // Part 1: 0..1200ms, Part 2: 1200..3000ms
        assertEquals(1200L, clips[0].durationMs)
        assertEquals(1800L, clips[1].durationMs)
        assertEquals(3000L, clips.sumOf { it.durationMs })
    }

    @Test
    fun `split inside the first 100ms does nothing`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(50L)
        viewModel.splitClipAtPlayhead()
        assertEquals(1, viewModel.currentProject.value?.clips?.size)
    }

    @Test
    fun `delete removes only the selected clip`() {
        val viewModel = vmWithOneClip()
        val first = viewModel.currentProject.value?.clips?.first()
        viewModel.selectClip(first?.id)
        viewModel.deleteSelectedClip()
        assertTrue(viewModel.currentProject.value?.clips.isNullOrEmpty())
    }

    @Test
    fun `filter change lands on the selected clip only`() {
        val viewModel = vmWithOneClip()
        val clipId = viewModel.currentProject.value?.clips?.firstOrNull()?.id
        viewModel.selectClip(clipId)
        viewModel.setClipFilter(VideoFilter.CYBERPUNK)
        assertEquals(VideoFilter.CYBERPUNK, viewModel.currentProject.value?.clips?.find { it.id == clipId }?.filter)
    }

    @Test
    fun `history is capped at 200 entries`() {
        val viewModel = vmWithOneClip()
        repeat(220) { index ->
            viewModel.setAspectRatio(if (index % 2 == 0) AspectRatioType.RATIO_16_9 else AspectRatioType.RATIO_9_16)
        }
        // 220 pushes, stack keeps the plan's 200 -> undo until exhausted.
        var undos = 0
        while (viewModel.canUndo.value && undos < 600) {
            viewModel.undo()
            undos++
        }
        assertEquals(200, undos)
        assertFalse(viewModel.canUndo.value)
    }

    @Test
    fun `setTimelineZoom is clamped between half and triple`() {
        val viewModel = vmWithOneClip()
        viewModel.setTimelineZoom(500f)
        assertEquals(3.0f, viewModel.timelineZoom.value, 0.0001f)
        viewModel.setTimelineZoom(0.001f)
        assertEquals(0.5f, viewModel.timelineZoom.value, 0.0001f)
    }

    @Test
    fun `magnetic snap toggles`() {
        val viewModel = vmWithOneClip()
        assertTrue(viewModel.magneticSnap.value)
        viewModel.toggleMagneticSnap()
        assertFalse(viewModel.magneticSnap.value)
        viewModel.toggleMagneticSnap()
        assertTrue(viewModel.magneticSnap.value)
    }

    @Test
    fun `selecting text clears clip selection and vice versa`() {
        val viewModel = vmWithOneClip()
        viewModel.selectClip("clip_1")
        viewModel.selectText("text_1")
        assertNull(viewModel.selectedClipId.value)
        viewModel.selectClip("clip_1")
        assertNull(viewModel.selectedTextId.value)
    }

    @Test
    fun `deleting the middle clip ripples the main track`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p1",
            title = "Ripple",
            clips = listOf(
                MediaClip(id = "a", title = "A", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "b", title = "B", durationMs = 2_000L, originalDurationMs = 2_000L),
                MediaClip(id = "c", title = "C", durationMs = 500L, originalDurationMs = 500L)
            )
        )
        viewModel.openProject(project)
        viewModel.selectClip("b")
        viewModel.deleteSelectedClip()

        val clips = viewModel.currentProject.value?.clips.orEmpty()
        assertEquals(listOf("a", "c"), clips.map { it.id })
        assertEquals(1_500L, clips.sumOf { it.durationMs })
    }

    @Test
    fun `deleting an overlay lifts it without moving the others`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p2",
            title = "Overlay delete",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 5_000L, originalDurationMs = 5_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track",
                    name = "Overlay",
                    isMainTrack = false,
                    clips = listOf(
                        MediaClip(
                            id = "ov1", title = "OV1",
                            startTimelineMs = 500L, durationMs = 1_000L,
                            originalDurationMs = 1_000L
                        ),
                        MediaClip(
                            id = "ov2", title = "OV2",
                            startTimelineMs = 2_500L, durationMs = 1_000L,
                            originalDurationMs = 1_000L
                        )
                    )
                )
            )
        )
        viewModel.openProject(project)
        viewModel.selectClip("ov1")
        viewModel.deleteSelectedClip()

        val remaining = viewModel.currentProject.value
            ?.overlayTracks?.firstOrNull()?.clips.orEmpty()
        assertEquals(listOf("ov2"), remaining.map { it.id })
        assertEquals(2_500L, remaining.single().startTimelineMs)
    }

    @Test
    fun `overlay move is clamped to the project and pushed out of overlaps`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p3",
            title = "Overlay move",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 5_000L, originalDurationMs = 5_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track",
                    name = "Overlay",
                    isMainTrack = false,
                    clips = listOf(
                        MediaClip(
                            id = "ov1", title = "OV1",
                            startTimelineMs = 0L, durationMs = 1_000L,
                            originalDurationMs = 1_000L
                        ),
                        MediaClip(
                            id = "ov2", title = "OV2",
                            startTimelineMs = 2_500L, durationMs = 1_000L,
                            originalDurationMs = 1_000L
                        )
                    )
                )
            )
        )
        viewModel.openProject(project)

        // Overlapping the second overlay pushes the clip to its end.
        viewModel.moveOverlayClip("ov1", 1_800L)
        assertEquals(
            3_500L,
            viewModel.currentProject.value!!.overlayTracks.first().clips
                .first { it.id == "ov1" }.startTimelineMs
        )

        // A negative drop clamps to the project start.
        viewModel.moveOverlayClip("ov1", -2_000L)
        assertEquals(
            0L,
            viewModel.currentProject.value!!.overlayTracks.first().clips
                .first { it.id == "ov1" }.startTimelineMs
        )

        // A drop past the end clamps to projectDuration - clipDuration.
        viewModel.moveOverlayClip("ov1", 9_000L)
        assertEquals(
            4_000L,
            viewModel.currentProject.value!!.overlayTracks.first().clips
                .first { it.id == "ov1" }.startTimelineMs
        )
    }

    @Test
    fun `track mute lock solo persist into the project`() {
        val project = VideoProject(
            id = "p4",
            title = "Track controls",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 3_000L, originalDurationMs = 3_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track_a",
                    name = "OV A",
                    isMainTrack = false,
                    clips = listOf(
                        MediaClip(
                            id = "ov_a",
                            title = "OV A",
                            startTimelineMs = 0L,
                            durationMs = 1_000L,
                            originalDurationMs = 1_000L
                        )
                    )
                ),
                VideoTrack(
                    id = "track_b",
                    name = "OV B",
                    isMainTrack = false,
                    clips = emptyList()
                )
            )
        )
        val viewModel = newViewModel()
        viewModel.openProject(project)

        viewModel.setTrackMuted(TimelineTrackIds.MAIN, true)
        viewModel.setTrackLocked("track_a", true)
        viewModel.setTrackSolo("track_b", true)

        val updated = viewModel.currentProject.value!!
        assertTrue(updated.isMainTrackMuted)
        assertTrue(updated.overlayTracks.first { it.id == "track_a" }.isLocked)
        assertTrue(updated.overlayTracks.first { it.id == "track_b" }.isSolo)
        assertFalse(updated.overlayTracks.first { it.id == "track_a" }.isSolo)
        assertFalse(updated.isMainTrackSolo)

        viewModel.setClipMuted("main", true)
        assertTrue(viewModel.currentProject.value!!.clips.first().isMuted)
    }

    @Test
    fun `locked tracks refuse moves deletes and trims`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p5",
            title = "Locked",
            clips = listOf(
                MediaClip(
                    id = "a", title = "A", durationMs = 2_000L, originalDurationMs = 2_000L,
                    trimEndMs = 0L
                ),
                MediaClip(
                    id = "b", title = "B", durationMs = 2_000L, originalDurationMs = 2_000L
                )
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track_a",
                    name = "OV",
                    isMainTrack = false,
                    isLocked = true,
                    clips = listOf(
                        MediaClip(
                            id = "ov_1",
                            title = "OV",
                            startTimelineMs = 0L,
                            durationMs = 1_000L,
                            originalDurationMs = 1_000L
                        )
                    )
                )
            ),
            isMainTrackLocked = true
        )
        viewModel.openProject(project)

        viewModel.selectClip("a")
        viewModel.moveClip(0, 1)
        assertEquals(listOf("a", "b"), viewModel.currentProject.value!!.clips.map { it.id })

        viewModel.deleteSelectedClip()
        assertEquals(2, viewModel.currentProject.value!!.clips.size)

        viewModel.trimSelectedClipEdge(ClipTrimHandle.Edge.END, 1_000L, 30)
        assertEquals(2_000L, viewModel.currentProject.value!!.clips.first().durationMs)

        viewModel.selectClip("ov_1")
        viewModel.moveOverlayClip("ov_1", 2_000L)
        assertEquals(
            0L,
            viewModel.currentProject.value!!.overlayTracks.first().clips.first().startTimelineMs
        )
    }

    @Test
    fun `overlay clip can move to another track`() {
        val project = VideoProject(
            id = "p6",
            title = "Cross track",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 4_000L, originalDurationMs = 4_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track_a",
                    name = "OV A",
                    isMainTrack = false,
                    clips = listOf(
                        MediaClip(
                            id = "ov_1",
                            title = "Pip",
                            startTimelineMs = 0L,
                            durationMs = 2_000L,
                            originalDurationMs = 2_000L
                        )
                    )
                ),
                VideoTrack(id = "track_b", name = "OV B", isMainTrack = false, clips = emptyList())
            )
        )
        val viewModel = newViewModel()
        viewModel.openProject(project)

        viewModel.moveClipToTrack("ov_1", "track_b", 1_000L)

        val updated = viewModel.currentProject.value!!.overlayTracks
        assertTrue(updated.first { it.id == "track_a" }.clips.isEmpty())
        val moved = updated.first { it.id == "track_b" }.clips.first()
        assertEquals("ov_1", moved.id)
        assertEquals(1_000L, moved.startTimelineMs)
    }

    @Test
    fun `locked main track refuses the split cut`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p8",
            title = "Locked split",
            clips = listOf(
                MediaClip(
                    id = "clip", title = "Clip",
                    durationMs = 3_000L, originalDurationMs = 3_000L
                )
            ),
            isMainTrackLocked = true
        )
        viewModel.openProject(project)
        viewModel.seekTo(1_500L)

        assertFalse(viewModel.splitClipAtPlayhead())
        assertEquals(1, viewModel.currentProject.value!!.clips.size)
    }

    @Test
    fun `inline trim handle moves the source trim and duration`() {        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p7",
            title = "Trim handle",
            clips = listOf(
                MediaClip(
                    id = "clip",
                    title = "Clip",
                    durationMs = 3_000L,
                    originalDurationMs = 5_000L,
                    trimStartMs = 0L,
                    trimEndMs = 0L
                )
            )
        )
        viewModel.openProject(project)
        viewModel.selectClip("clip")
        viewModel.trimSelectedClipEdge(ClipTrimHandle.Edge.END, 1_000L, 30)

        val clip = viewModel.currentProject.value!!.clips.first()
        assertEquals(3_990L, clip.durationMs)
        assertEquals(3_990L, clip.trimEndMs)
    }
}
