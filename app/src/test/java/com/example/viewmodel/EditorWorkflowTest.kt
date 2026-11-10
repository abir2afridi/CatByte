package com.example.viewmodel

import com.example.engine.ClipTrimHandle
import com.example.model.AspectRatioType
import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.model.AudioClip
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import com.example.model.TimelineTrackIds
import com.example.model.TimelineMarker
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
    fun `opening a project starts with nothing selected`() {
        // The timeline kept showing a white selection border on the first clip
        // from the moment the editor opened.
        val viewModel = vmWithOneClip()
        assertNull(viewModel.selectedClipId.value)
        assertTrue(viewModel.selectedClipIds.value.isEmpty())
    }

    @Test
    fun `clearSelection clears the single clip selection too`() {
        val viewModel = vmWithOneClip()
        viewModel.selectClip("clip_1")
        assertNotNull(viewModel.selectedClipId.value)
        assertTrue(viewModel.selectedClipIds.value.isNotEmpty())

        viewModel.clearSelection()
        assertNull(viewModel.selectedClipId.value)
        assertTrue(viewModel.selectedClipIds.value.isEmpty())
        assertNull(viewModel.selectedTextId.value)
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
    fun `split cuts in and out points on the same source with no gap`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(1200L)
        viewModel.splitClipAtPlayhead()

        val clips = viewModel.currentProject.value?.clips.orEmpty()
        // Clip A: in = original in, out = playhead. Clip B: in = playhead.
        assertEquals(0L, clips[0].trimStartMs)
        assertEquals(1200L, clips[0].trimEndMs)
        assertEquals(1200L, clips[1].trimStartMs)
        assertEquals(3000L, clips[1].trimEndMs)
        // Contiguous on the magnetic track.
        assertEquals(0L, clips[0].startTimelineMs)
        assertEquals(1200L, clips[1].startTimelineMs)
    }

    @Test
    fun `trimming one clip ripples the magnetic main track both ways`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p9",
            title = "Ripple",
            clips = listOf(
                MediaClip(
                    id = "a", title = "A",
                    durationMs = 3_000L, originalDurationMs = 5_000L,
                    trimStartMs = 0L, trimEndMs = 3_000L
                ),
                MediaClip(
                    id = "b", title = "B",
                    startTimelineMs = 3_000L,
                    durationMs = 2_000L, originalDurationMs = 2_000L
                )
            )
        )
        viewModel.openProject(project)
        viewModel.selectClip("a")

        // Inward: clip A shortens, B shifts left to stay attached.
        viewModel.trimSelectedClipEdge(ClipTrimHandle.Edge.END, -1_000L, 30)
        var clips = viewModel.currentProject.value!!.clips
        assertEquals(2_010L, clips[0].durationMs)
        assertEquals(2_010L, clips[1].startTimelineMs)

        // Outward: the hidden footage returns, B shifts back right.
        viewModel.trimSelectedClipEdge(ClipTrimHandle.Edge.END, 1_000L, 30)
        clips = viewModel.currentProject.value!!.clips
        assertEquals(3_000L, clips[0].durationMs)
        assertEquals(3_000L, clips[0].trimEndMs)
        assertEquals(3_000L, clips[1].startTimelineMs)
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
    fun `inline trim handle moves the source trim and duration`() {
        val viewModel = newViewModel()
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

    @Test
    fun `markers add jump move and remove`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(1_000L)
        viewModel.addMarker("A")

        var marker = viewModel.currentProject.value!!.markers.single()
        assertEquals(1_000L, marker.atMs)
        assertEquals("A", marker.label)

        viewModel.seekTo(2_500L)
        viewModel.moveMarkerToPlayhead(marker.id)
        marker = viewModel.currentProject.value!!.markers.single()
        assertEquals(2_500L, marker.atMs)

        // With a single marker after the playhead, both directions land on it.
        viewModel.seekTo(0L)
        assertEquals(2_500L, viewModel.jumpToMarker(1))
        assertEquals(2_500L, viewModel.jumpToMarker(-1))

        viewModel.removeMarker(marker.id)
        assertTrue(viewModel.currentProject.value!!.markers.isEmpty())
    }

    @Test
    fun `markers jump between two markers in both directions`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(1_000L)
        viewModel.addMarker("A")
        viewModel.seekTo(2_500L)
        viewModel.addMarker("B")

        viewModel.seekTo(0L)
        assertEquals(1_000L, viewModel.jumpToMarker(1))
        assertEquals(2_500L, viewModel.jumpToMarker(1))
        assertEquals(1_000L, viewModel.jumpToMarker(-1))
    }

    @Test
    fun `group selection delete move and paste overlays`() {
        val project = VideoProject(
            id = "p10",
            title = "Group",
            clips = listOf(
                MediaClip(id = "a", title = "A", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "b", title = "B", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "c", title = "C", durationMs = 1_000L, originalDurationMs = 1_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track_a",
                    name = "OV A",
                    isMainTrack = false,
                    clips = listOf(
                        MediaClip(
                            id = "ov1", title = "OV1",
                            startTimelineMs = 0L, durationMs = 1_000L, originalDurationMs = 1_000L
                        ),
                        MediaClip(
                            id = "ov2", title = "OV2",
                            startTimelineMs = 2_000L, durationMs = 1_000L, originalDurationMs = 1_000L
                        )
                    )
                )
            )
        )
        val viewModel = newViewModel()
        viewModel.openProject(project)

        viewModel.toggleClipSelection("a")
        viewModel.toggleClipSelection("b")
        viewModel.toggleClipSelection("ov1")
        viewModel.toggleClipSelection("ov2")
        assertEquals(4, viewModel.selectedClipIds.value.size)

        // Overlay group moves keeps relative offsets, clamped to project bounds.
        viewModel.moveSelectedOverlays(1_000L)
        val overlays = viewModel.currentProject.value!!.overlayTracks.first().clips
        assertEquals(1_000L, overlays.first { it.id == "ov1" }.startTimelineMs)
        assertEquals(3_000L, overlays.first { it.id == "ov2" }.startTimelineMs)

        // Main group delete ripples; overlay group delete lifts.
        viewModel.deleteSelectedClips()
        val updated = viewModel.currentProject.value!!
        assertEquals(listOf("c"), updated.clips.map { it.id })
        assertTrue(updated.overlayTracks.first().clips.isEmpty())
    }

    @Test
    fun `overlay clipboard pastes at the playhead with relative offsets`() {        val project = VideoProject(
            id = "p11",
            title = "Clipboard",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 5_000L, originalDurationMs = 5_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track_a",
                    name = "OV",
                    isMainTrack = false,
                    clips = listOf(
                        MediaClip(
                            id = "ov1", title = "OV1",
                            startTimelineMs = 0L, durationMs = 1_000L, originalDurationMs = 1_000L
                        ),
                        MediaClip(
                            id = "ov2", title = "OV2",
                            startTimelineMs = 2_000L, durationMs = 1_000L, originalDurationMs = 1_000L
                        )
                    )
                )
            )
        )
        val viewModel = newViewModel()
        viewModel.openProject(project)

        viewModel.toggleClipSelection("ov1")
        viewModel.toggleClipSelection("ov2")
        viewModel.copySelectedOverlays()
        viewModel.seekTo(3_000L)
        viewModel.pasteOverlays()

        val clips = viewModel.currentProject.value!!.overlayTracks.first().clips
        assertEquals(4, clips.size)
        val pasted = clips
            .filter { it.id.startsWith("paste_") && (it.title == "OV1" || it.title == "OV2") }
        assertEquals(3_000L, pasted.first { it.title == "OV1" }.startTimelineMs)
        assertEquals(5_000L, pasted.first { it.title == "OV2" }.startTimelineMs)
    }

    @Test
    fun `overlay tracks can be reordered`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p9",
            title = "Reorder",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 3_000L, originalDurationMs = 3_000L)
            ),
            overlayTracks = listOf(
                VideoTrack(id = "track_a", name = "OV A", isMainTrack = false, clips = emptyList()),
                VideoTrack(id = "track_b", name = "OV B", isMainTrack = false, clips = emptyList())
            )
        )
        viewModel.openProject(project)

        viewModel.moveOverlayTrack(0, 1)
        assertEquals(
            listOf("track_b", "track_a"),
            viewModel.currentProject.value!!.overlayTracks.map { it.id }
        )
    }

    @Test
    fun `audio clip moves with snap and clamp`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p12",
            title = "Audio move",
            clips = listOf(
                MediaClip(id = "main", title = "Main", durationMs = 5_000L, originalDurationMs = 5_000L)
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "a1", title = "Music 1", artist = "Artist",
                    startTimelineMs = 0L, durationMs = 1_000L
                ),
                AudioClip(
                    id = "a2", title = "Music 2", artist = "Artist",
                    startTimelineMs = 2_000L, durationMs = 1_000L
                )
            )
        )
        viewModel.openProject(project)

        // Overlapping a2 pushes a1 to its end.
        viewModel.moveAudioClip("a1", 1_200L)
        var audio = viewModel.currentProject.value!!.audioTracks
        assertEquals(3_000L, audio.first { it.id == "a1" }.startTimelineMs)

        // Negative drop clamps to the project start.
        viewModel.moveAudioClip("a1", -1_000L)
        audio = viewModel.currentProject.value!!.audioTracks
        assertEquals(0L, audio.first { it.id == "a1" }.startTimelineMs)

        // Past the end clamps to projectDuration - clipDuration.
        viewModel.moveAudioClip("a1", 9_000L)
        audio = viewModel.currentProject.value!!.audioTracks
        assertEquals(4_000L, audio.first { it.id == "a1" }.startTimelineMs)
    }

    @Test
    fun `selected main clips move as one contiguous block`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p13",
            title = "Block move",
            clips = listOf(
                MediaClip(id = "a", title = "A", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "b", title = "B", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "c", title = "C", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "d", title = "D", durationMs = 1_000L, originalDurationMs = 1_000L)
            )
        )
        viewModel.openProject(project)

        viewModel.toggleClipSelection("a")
        viewModel.toggleClipSelection("b")
        viewModel.moveSelectedMainClips(1)
        assertEquals(
            listOf("c", "a", "b", "d"),
            viewModel.currentProject.value!!.clips.map { it.id }
        )

        viewModel.moveSelectedMainClips(-1)
        assertEquals(
            listOf("a", "b", "c", "d"),
            viewModel.currentProject.value!!.clips.map { it.id }
        )
    }

    @Test
    fun `clip attributes paste to the whole selection`() {
        val viewModel = newViewModel()
        val project = VideoProject(
            id = "p14",
            title = "Paste style",
            clips = listOf(
                MediaClip(
                    id = "a", title = "A", durationMs = 1_000L, originalDurationMs = 1_000L,
                    filter = VideoFilter.CYBERPUNK,
                    brightness = 0.2f,
                    contrast = 1.3f,
                    saturation = 1.4f,
                    vignette = 0.3f,
                    speed = 2f,
                    volume = 0.5f
                ),
                MediaClip(id = "b", title = "B", durationMs = 1_000L, originalDurationMs = 1_000L),
                MediaClip(id = "c", title = "C", durationMs = 1_000L, originalDurationMs = 1_000L)
            )
        )
        viewModel.openProject(project)

        viewModel.toggleClipSelection("a")
        viewModel.toggleClipSelection("b")
        viewModel.copySelectedClipAttributes()
        viewModel.pasteClipAttributes()

        val clips = viewModel.currentProject.value!!.clips
        val pasted = clips.first { it.id == "b" }
        val untouched = clips.first { it.id == "c" }
        assertEquals(VideoFilter.CYBERPUNK, pasted.filter)
        assertEquals(0.2f, pasted.brightness, 0.0001f)
        assertEquals(1.3f, pasted.contrast, 0.0001f)
        assertEquals(1.4f, pasted.saturation, 0.0001f)
        assertEquals(0.3f, pasted.vignette, 0.0001f)
        assertEquals(2f, pasted.speed, 0.0001f)
        assertEquals(0.5f, pasted.volume, 0.0001f)
        assertEquals(VideoFilter.NONE, untouched.filter)
    }
}
