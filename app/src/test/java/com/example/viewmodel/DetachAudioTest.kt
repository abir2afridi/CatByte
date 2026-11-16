package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Unlink" / detach audio.
 *
 * The bottom-nav chip used to answer with a "not available yet" toast. It now
 * moves the clip's sound onto its own audio track and mutes the video, in a
 * single undoable edit - the CapCut "Unlink" behaviour.
 */
class DetachAudioTest {

    private fun clip(
        id: String,
        title: String = id,
        uri: String? = "content://clip/$id",
        durationMs: Long = 3_000L,
        muted: Boolean = false
    ) = MediaClip(
        id = id,
        title = title,
        uriString = uri,
        durationMs = durationMs,
        originalDurationMs = durationMs,
        isMuted = muted
    )

    private fun viewModelWith(vararg clips: MediaClip): EditorViewModel {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val project = ProjectRepository().createNewProject("Seeded", initialClips = clips.toList())
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `detach moves the sound to its own track and mutes the clip`() {
        val viewModel = viewModelWith(clip("clip_1"))
        viewModel.selectClip("clip_1")

        assertTrue(viewModel.detachAudioFromSelectedClip())

        val project = viewModel.currentProject.value!!
        assertEquals(1, project.audioTracks.size)
        val audio = project.audioTracks.single()
        assertEquals("content://clip/clip_1", audio.uriString)
        assertEquals(0L, audio.startTimelineMs)
        assertEquals(3_000L, audio.durationMs)
        assertEquals("clip_1 (Audio)", audio.title)
        assertTrue("the video must stay silent after unlinking", project.clips.single().isMuted)
    }

    @Test
    fun `detach keeps the timeline offset of a later clip`() {
        val viewModel = viewModelWith(
            clip("clip_1", durationMs = 3_000L),
            clip("clip_2", durationMs = 2_000L)
        )
        viewModel.selectClip("clip_2")

        assertTrue(viewModel.detachAudioFromSelectedClip())

        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals("clip_2 starts at 3s on the timeline", 3_000L, audio.startTimelineMs)
        assertEquals(2_000L, audio.durationMs)
    }

    @Test
    fun `detach twice is a no-op`() {
        val viewModel = viewModelWith(clip("clip_1"))
        viewModel.selectClip("clip_1")

        assertTrue(viewModel.detachAudioFromSelectedClip())
        assertFalse(viewModel.detachAudioFromSelectedClip())

        assertEquals(1, viewModel.currentProject.value!!.audioTracks.size)
    }

    @Test
    fun `detach without a media source is rejected`() {
        val viewModel = viewModelWith(clip("clip_1", uri = null))
        viewModel.selectClip("clip_1")

        assertFalse(viewModel.detachAudioFromSelectedClip())
        assertTrue(viewModel.currentProject.value!!.audioTracks.isEmpty())
    }

    @Test
    fun `detach needs a selection`() {
        val viewModel = viewModelWith(clip("clip_1"))

        assertFalse(viewModel.detachAudioFromSelectedClip())
        assertTrue(viewModel.currentProject.value!!.audioTracks.isEmpty())
    }

    @Test
    fun `undo puts the sound back on the clip`() {
        val viewModel = viewModelWith(clip("clip_1"))
        viewModel.selectClip("clip_1")
        assertTrue(viewModel.detachAudioFromSelectedClip())

        viewModel.undo()

        val project = viewModel.currentProject.value!!
        assertTrue("the detached track must disappear", project.audioTracks.isEmpty())
        assertFalse("the clip must be audible again", project.clips.single().isMuted)
    }
}
