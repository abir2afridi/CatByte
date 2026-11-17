package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unified main-track/overlay clipboard: copy, cut and paste at the playhead. */
class ClipboardFeatureTest {

    private fun newViewModel(): EditorViewModel = EditorViewModel(repository = ProjectRepository())

    private fun project(): VideoProject = VideoProject(
        id = "clipboard",
        title = "Clipboard",
        clips = listOf(
            MediaClip(id = "a", title = "A", durationMs = 1_000L, originalDurationMs = 1_000L),
            MediaClip(id = "b", title = "B", durationMs = 1_000L, originalDurationMs = 1_000L),
            MediaClip(id = "c", title = "C", durationMs = 1_000L, originalDurationMs = 1_000L)
        ),
        overlayTracks = listOf(
            VideoTrack(
                id = "ov_track",
                name = "Overlay",
                isMainTrack = false,
                clips = listOf(
                    MediaClip(
                        id = "ov1",
                        title = "OV1",
                        startTimelineMs = 1_000L,
                        durationMs = 1_000L,
                        originalDurationMs = 1_000L
                    )
                )
            )
        )
    )

    @Test
    fun `copying main clips pastes them at the playhead in order`() {
        val vm = newViewModel()
        vm.openProject(project())
        vm.toggleClipSelection("a")
        vm.toggleClipSelection("b")
        vm.copySelectedClips()

        assertEquals(2, vm.clipboardCount.value)
        vm.seekTo(3_000L)
        vm.pasteClips()

        val clips = vm.currentProject.value!!.clips
        assertEquals(5, clips.size)
        assertEquals(listOf("a", "b", "c"), clips.take(3).map { it.id })
        assertEquals(listOf("A", "B"), clips.drop(3).map { it.title })
        assertTrue(clips.drop(3).all { it.id.startsWith("paste_") })
        assertEquals(2, vm.selectedClipIds.value.size)

        vm.undo()
        assertEquals(3, vm.currentProject.value!!.clips.size)
    }

    @Test
    fun `cut removes the selection and paste restores a fresh copy`() {
        val vm = newViewModel()
        vm.openProject(project())
        vm.selectClip("b")

        vm.cutSelectedClips()
        assertEquals(listOf("a", "c"), vm.currentProject.value!!.clips.map { it.id })
        assertEquals(1, vm.clipboardCount.value)

        vm.seekTo(1_000L)
        vm.pasteClips()
        val clips = vm.currentProject.value!!.clips
        assertEquals("a", clips[0].id)
        assertTrue(clips[1].id.startsWith("paste_"))
        assertEquals("c", clips[2].id)
        assertEquals("B", clips[1].title)

        vm.undo()
        assertEquals(listOf("a", "c"), vm.currentProject.value!!.clips.map { it.id })
        vm.undo()
        assertEquals(listOf("a", "b", "c"), vm.currentProject.value!!.clips.map { it.id })
    }

    @Test
    fun `mixed clipboard keeps main insertion and overlay offsets`() {
        val vm = newViewModel()
        vm.openProject(project())
        vm.toggleClipSelection("b")
        vm.toggleClipSelection("ov1")
        vm.copySelectedClips()

        assertEquals(2, vm.clipboardCount.value)
        vm.seekTo(2_000L)
        vm.pasteClips()

        val project = vm.currentProject.value!!
        assertEquals(4, project.clips.size)
        assertEquals("B", project.clips[2].title)
        val overlays = project.overlayTracks.first().clips
        assertEquals(2, overlays.size)
        assertTrue(overlays.any { it.title == "OV1" && it.startTimelineMs == 2_000L })
    }
}
