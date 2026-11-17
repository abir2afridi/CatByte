package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Per-audio mixer state, selection, history and persistence behaviour. */
class AudioMixerTest {

    private fun viewModel(): Pair<EditorViewModel, VideoProject> {
        val project = VideoProject(
            id = "audio_project",
            title = "Audio",
            clips = listOf(
                MediaClip(
                    id = "video",
                    title = "Video",
                    durationMs = 5_000L,
                    originalDurationMs = 5_000L
                )
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "music",
                    title = "Music",
                    artist = "Test",
                    uriString = "content://audio/music",
                    durationMs = 10_000L,
                    originalDurationMs = 10_000L
                )
            )
        )
        val viewModel = EditorViewModel(repository = ProjectRepository())
        viewModel.openProject(project)
        return viewModel to project
    }

    @Test
    fun `volume changes only the selected audio clip and supports undo redo`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioVolume("music", 0.35f)
        assertEquals(0.35f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)

        viewModel.undo()
        assertEquals(1f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
        viewModel.redo()
        assertEquals(0.35f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
    }

    @Test
    fun `rapid volume slider stream collapses to one undo step`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioVolume("music", 0.8f)
        viewModel.setAudioVolume("music", 0.6f)
        viewModel.setAudioVolume("music", 0.4f)
        viewModel.undo()
        assertEquals(1f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
    }

    @Test
    fun `mute preserves the stored volume`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioVolume("music", 0.4f)
        viewModel.setAudioClipMuted("music", true)
        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertTrue(audio.isMuted)
        assertEquals(0.4f, audio.volume, 0.0001f)
    }

    @Test
    fun `auto volume is refused while the waveform is not decoded`() {
        val (viewModel, _) = viewModel()
        assertFalse(viewModel.normaliseAudioVolume("music"))
        assertEquals(1f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
    }

    @Test
    fun `auto volume boosts a quiet waveform towards the target loudness`() {
        val (viewModel, project) = viewModel()
        val quiet = project.audioTracks.single().copy(waveform = List(48) { 0.1f })
        viewModel.openProject(project.copy(audioTracks = listOf(quiet)))

        assertTrue(viewModel.normaliseAudioVolume("music"))

        assertEquals(2f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
    }

    @Test
    fun `auto volume attenuates a loud waveform and supports undo`() {
        val (viewModel, project) = viewModel()
        val loud = project.audioTracks.single().copy(waveform = List(48) { 0.4f })
        viewModel.openProject(project.copy(audioTracks = listOf(loud)))

        assertTrue(viewModel.normaliseAudioVolume("music"))

        assertEquals(0.5f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
        viewModel.undo()
        assertEquals(1f, viewModel.currentProject.value!!.audioTracks.single().volume, 0.0001f)
    }

    @Test
    fun `fades are stored on the output timeline`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioFades("music", 400L, 700L)
        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(400L, audio.fadeInMs)
        assertEquals(700L, audio.fadeOutMs)
    }

    @Test
    fun `speed preserves source trim and retimes the clip`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioTrim("music", 2_000L, 6_000L)
        viewModel.setAudioSpeed("music", 2f)
        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(2_000L, audio.trimStartMs)
        assertEquals(6_000L, audio.trimEndMs)
        assertEquals(2f, audio.speed, 0.0001f)
        assertEquals(2_000L, audio.durationMs)
    }

    @Test
    fun `legacy speed edit materialises source duration before changing speed`() {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        viewModel.openProject(
            VideoProject(
                id = "legacy_audio",
                title = "Legacy",
                audioTracks = listOf(
                    AudioClip(
                        id = "legacy",
                        title = "Legacy",
                        artist = "Test",
                        uriString = "content://audio/legacy",
                        durationMs = 4_000L,
                        originalDurationMs = 0L,
                        speed = 2f
                    )
                )
            )
        )

        viewModel.setAudioSpeed("legacy", 1f)

        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(8_000L, audio.originalDurationMs)
        assertEquals(8_000L, audio.durationMs)
        assertEquals(1f, audio.speed, 0.0001f)
    }

    @Test
    fun `legacy trim and reset keep the inferred source duration`() {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        viewModel.openProject(
            VideoProject(
                id = "legacy_audio",
                title = "Legacy",
                audioTracks = listOf(
                    AudioClip(
                        id = "legacy",
                        title = "Legacy",
                        artist = "Test",
                        uriString = "content://audio/legacy",
                        durationMs = 4_000L,
                        originalDurationMs = 0L,
                        speed = 2f
                    )
                )
            )
        )

        viewModel.setAudioTrim("legacy", 1_000L, 3_000L)
        var audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(8_000L, audio.originalDurationMs)
        assertEquals(1_000L, audio.durationMs)

        viewModel.resetAudioMixer("legacy")
        audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(8_000L, audio.originalDurationMs)
        assertEquals(8_000L, audio.durationMs)
    }

    @Test
    fun `trim retimes the output clip`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioTrim("music", 1_000L, 5_000L)
        assertEquals(4_000L, viewModel.currentProject.value!!.audioTracks.single().durationMs)
    }

    @Test
    fun `timeline start is clamped to zero`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioTimelineStart("music", -900L)
        assertEquals(0L, viewModel.currentProject.value!!.audioTracks.single().startTimelineMs)
    }

    @Test
    fun `reset restores gain mute fades speed and trim`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioVolume("music", 0.2f)
        viewModel.setAudioClipMuted("music", true)
        viewModel.setAudioFades("music", 500L, 600L)
        viewModel.setAudioSpeed("music", 2f)
        viewModel.setAudioTrim("music", 1_000L, 4_000L)
        viewModel.resetAudioMixer("music")

        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(1f, audio.volume, 0.0001f)
        assertFalse(audio.isMuted)
        assertEquals(0L, audio.fadeInMs)
        assertEquals(0L, audio.fadeOutMs)
        assertEquals(1f, audio.speed, 0.0001f)
        assertEquals(0L, audio.trimStartMs)
        assertEquals(0L, audio.trimEndMs)
        assertEquals(10_000L, audio.durationMs)
    }

    @Test
    fun `audio and video selection are mutually exclusive`() {
        val (viewModel, _) = viewModel()
        viewModel.selectAudioClip("music")
        assertEquals("music", viewModel.selectedAudioId.value)
        assertNull(viewModel.selectedClipId.value)

        viewModel.selectClip("video")
        assertNull(viewModel.selectedAudioId.value)
        assertEquals("video", viewModel.selectedClipId.value)
    }

    @Test
    fun `deleting the selected audio clears its selection`() {
        val (viewModel, _) = viewModel()
        viewModel.selectAudioClip("music")
        viewModel.deleteAudioTrack("music")
        assertNull(viewModel.selectedAudioId.value)
        assertTrue(viewModel.currentProject.value!!.audioTracks.isEmpty())
    }

    @Test
    fun `mixer opens the first track when nothing is selected`() {
        val (viewModel, _) = viewModel()
        assertTrue(viewModel.openAudioMixer())
        assertEquals("music", viewModel.selectedAudioId.value)
        assertEquals(ActiveToolSheet.AUDIO_MIXER, viewModel.activeSheet.value)
    }

    @Test
    fun `opening the mixer clears text and video selections`() {
        val (viewModel, _) = viewModel()
        viewModel.toggleClipSelection("video")
        viewModel.selectText("title")

        assertTrue(viewModel.openAudioMixer())

        assertEquals("music", viewModel.selectedAudioId.value)
        assertNull(viewModel.selectedClipId.value)
        assertTrue(viewModel.selectedClipIds.value.isEmpty())
        assertNull(viewModel.selectedTextId.value)
    }
}
