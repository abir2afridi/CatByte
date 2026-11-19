package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.AudioClip
import com.example.model.AudioCompressor
import com.example.model.AudioEffectSettings
import com.example.model.AudioEqualizer
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VoiceEffectType
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
    fun `pan is stored on the track and supports undo redo`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioPan("music", -0.4f)
        assertEquals(-0.4f, viewModel.currentProject.value!!.audioTracks.single().pan, 0.0001f)

        viewModel.undo()
        assertEquals(0f, viewModel.currentProject.value!!.audioTracks.single().pan, 0.0001f)
        viewModel.redo()
        assertEquals(-0.4f, viewModel.currentProject.value!!.audioTracks.single().pan, 0.0001f)
    }

    @Test
    fun `pan is clamped to the stereo field`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioPan("music", 5f)
        assertEquals(1f, viewModel.currentProject.value!!.audioTracks.single().pan, 0.0001f)

        viewModel.setAudioPan("music", Float.NaN)
        assertEquals(0f, viewModel.currentProject.value!!.audioTracks.single().pan, 0.0001f)
    }

    @Test
    fun `equalizer is stored on the track and supports undo redo`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioEqualizer("music", AudioEqualizer(6f, -3.5f, 2f))

        val applied = viewModel.currentProject.value!!.audioTracks.single().equalizer
        assertEquals(6f, applied.lowGainDb, 0.0001f)
        assertEquals(-3.5f, applied.midGainDb, 0.0001f)
        assertEquals(2f, applied.highGainDb, 0.0001f)

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().equalizer.isFlat)

        viewModel.redo()
        assertEquals(6f, viewModel.currentProject.value!!.audioTracks.single().equalizer.lowGainDb, 0.0001f)
    }

    @Test
    fun `equalizer gains are clamped before they reach the render graph`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioEqualizer(
            "music",
            AudioEqualizer(40f, Float.NaN, -40f)
        )

        val eq = viewModel.currentProject.value!!.audioTracks.single().equalizer
        assertEquals(AudioEqualizer.MAX_GAIN_DB, eq.lowGainDb, 0f)
        assertEquals(0f, eq.midGainDb, 0f)
        assertEquals(AudioEqualizer.MIN_GAIN_DB, eq.highGainDb, 0f)
    }

    @Test
    fun `rapid equalizer slider stream collapses to one undo step`() {
        val (viewModel, _) = viewModel()

        viewModel.setAudioEqualizer("music", AudioEqualizer(lowGainDb = 3f))
        viewModel.setAudioEqualizer("music", AudioEqualizer(lowGainDb = 6f))
        viewModel.setAudioEqualizer("music", AudioEqualizer(lowGainDb = 9f))

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().equalizer.isFlat)
    }

    @Test
    fun `equalizer no-change does not create an undo step`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioEqualizer("music", AudioEqualizer())

        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().equalizer.isFlat)
    }

    @Test
    fun `compressor is stored on the track and supports undo redo`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioCompressor(
            "music",
            AudioCompressor(enabled = true, thresholdDb = -24f, ratio = 6f, makeupGainDb = 3f)
        )

        val applied = viewModel.currentProject.value!!.audioTracks.single().compressor
        assertTrue(applied.enabled)
        assertEquals(-24f, applied.thresholdDb, 0.0001f)
        assertEquals(6f, applied.ratio, 0.0001f)
        assertEquals(3f, applied.makeupGainDb, 0.0001f)

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().compressor.isNoOp)

        viewModel.redo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().compressor.enabled)
    }

    @Test
    fun `compressor parameters are clamped before they reach the render graph`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioCompressor(
            "music",
            AudioCompressor(
                enabled = true,
                thresholdDb = 40f,
                ratio = 900f,
                makeupGainDb = -4f
            )
        )

        val compressor = viewModel.currentProject.value!!.audioTracks.single().compressor
        assertEquals(AudioCompressor.MAX_THRESHOLD_DB, compressor.thresholdDb, 0f)
        assertEquals(AudioCompressor.MAX_RATIO, compressor.ratio, 0f)
        assertEquals(AudioCompressor.MIN_MAKEUP_GAIN_DB, compressor.makeupGainDb, 0f)
    }

    @Test
    fun `rapid compressor slider stream collapses to one undo step`() {
        val (viewModel, _) = viewModel()

        viewModel.setAudioCompressor("music", AudioCompressor(enabled = true, ratio = 2f))
        viewModel.setAudioCompressor("music", AudioCompressor(enabled = true, ratio = 4f))
        viewModel.setAudioCompressor("music", AudioCompressor(enabled = true, ratio = 8f))

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().compressor.isNoOp)
    }

    @Test
    fun `compressor no-change does not create an undo step`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioCompressor("music", AudioCompressor())

        assertFalse(viewModel.canUndo.value)
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
    fun `solo is stored on the track and supports undo redo`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioSolo("music", true)
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().isSoloed)

        viewModel.undo()
        assertFalse(viewModel.currentProject.value!!.audioTracks.single().isSoloed)

        viewModel.redo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().isSoloed)
    }

    @Test
    fun `soloed track drops the other tracks out of the rendered mix`() {
        val (viewModel, project) = viewModel()
        val voice = project.audioTracks.single().copy(
            id = "voice",
            title = "Voice",
            isVoiceover = true,
            isSoloed = true
        )
        viewModel.openProject(project.copy(audioTracks = project.audioTracks + voice))

        val graph = com.example.engine.RenderGraphBuilder.build(viewModel.currentProject.value!!)
        assertEquals(0f, graph.audio.first { it.clipId == "music" }.volume, 0.0001f)
        assertEquals(1f, graph.audio.first { it.clipId == "voice" }.volume, 0.0001f)
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
    fun `reset restores gain mute fades speed trim equalizer and compressor`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioVolume("music", 0.2f)
        viewModel.setAudioClipMuted("music", true)
        viewModel.setAudioFades("music", 500L, 600L)
        viewModel.setAudioSpeed("music", 2f)
        viewModel.setAudioTrim("music", 1_000L, 4_000L)
        viewModel.setAudioEqualizer("music", AudioEqualizer(5f, 0f, -4f))
        viewModel.setAudioCompressor("music", AudioCompressor(enabled = true, ratio = 8f))
        viewModel.resetAudioMixer("music")

        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(1f, audio.volume, 0.0001f)
        assertFalse(audio.isMuted)
        assertEquals(0L, audio.fadeInMs)
        assertEquals(0L, audio.fadeOutMs)
        assertEquals(1f, audio.speed, 0.0001f)
        assertEquals(0L, audio.trimStartMs)
        assertEquals(0L, audio.trimEndMs)
        assertTrue(audio.equalizer.isFlat)
        assertTrue(audio.compressor.isNoOp)
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

    @Test
    fun `split at playhead cuts the selected track at output time`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioTrim("music", 1_000L, 5_000L) // 4 s on the timeline
        viewModel.selectAudioClip("music")
        viewModel.seekTo(3_000L)

        assertTrue(viewModel.splitAudioAtPlayhead())

        val tracks = viewModel.currentProject.value!!.audioTracks
        assertEquals(2, tracks.size)
        val first = tracks[0]
        val second = tracks[1]
        // Playhead 3 s into the output; speed 1 maps the cut to source 4 s.
        assertEquals(1_000L, first.trimStartMs)
        assertEquals(4_000L, first.trimEndMs)
        assertEquals(3_000L, first.durationMs)
        assertEquals(0L, first.startTimelineMs)
        assertEquals(4_000L, second.trimStartMs)
        assertEquals(5_000L, second.trimEndMs)
        assertEquals(1_000L, second.durationMs)
        assertEquals(3_000L, second.startTimelineMs)
        assertEquals(second.id, viewModel.selectedAudioId.value)
    }

    @Test
    fun `split maps the playhead through playback speed to source time`() {
        val (viewModel, project) = viewModel()
        val fast = project.audioTracks.single().copy(
            trimStartMs = 0L,
            trimEndMs = 0L,
            speed = 2f,
            durationMs = 5_000L
        )
        viewModel.openProject(project.copy(audioTracks = listOf(fast)))
        viewModel.selectAudioClip("music")
        viewModel.seekTo(2_500L)

        assertTrue(viewModel.splitAudioAtPlayhead())

        val tracks = viewModel.currentProject.value!!.audioTracks
        assertEquals(2, tracks.size)
        // 2.5 s of output at 2x starts at source second 5.
        assertEquals(5_000L, tracks[1].trimStartMs)
        assertEquals(2_500L, tracks[0].durationMs)
        assertEquals(2_500L, tracks[1].durationMs)
    }

    @Test
    fun `split is refused outside the selected track`() {
        val (viewModel, _) = viewModel()
        viewModel.selectAudioClip("music")
        // Place the playhead beyond the 10 s track directly; seekTo clamps to
        // the project duration, which would mask this regression.
        viewModel._currentPositionMs.value = 15_000L

        assertFalse(viewModel.splitAudioAtPlayhead())
        assertEquals(1, viewModel.currentProject.value!!.audioTracks.size)
    }

    @Test
    fun `audio effects persist and undo`() {
        val (viewModel, _) = viewModel()
        val echo = AudioEffectSettings(
            type = VoiceEffectType.ECHO,
            intensity = 0.8f,
            reduceNoise = true
        )

        viewModel.setAudioEffects("music", echo)

        val applied = viewModel.currentProject.value!!.audioTracks.single().audioEffects
        assertEquals(VoiceEffectType.ECHO, applied.type)
        assertEquals(0.8f, applied.intensity, 0.0001f)
        assertTrue(applied.reduceNoise)

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.audioTracks.single().audioEffects.isNoOp)

        viewModel.redo()
        assertEquals(VoiceEffectType.ECHO, viewModel.currentProject.value!!.audioTracks.single().audioEffects.type)
    }

    @Test
    fun `loop count repeats the source window and covers the timeline`() {
        val (viewModel, _) = viewModel()
        viewModel.setAudioTrim("music", 0L, 2_000L) // a two-second window

        viewModel.setAudioLoopCount("music", 4)

        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(4, audio.loopCount)
        assertEquals(8_000L, audio.durationMs)

        viewModel.undo()
        val restored = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(1, restored.loopCount)
        assertEquals(2_000L, restored.durationMs)
    }

    @Test
    fun `loop count is clamped to the supported range`() {
        val (viewModel, _) = viewModel()

        viewModel.setAudioLoopCount("music", 99)

        assertEquals(16, viewModel.currentProject.value!!.audioTracks.single().loopCount)
    }

    @Test
    fun `auto ducking can be turned off and undone`() {
        val (viewModel, _) = viewModel()
        assertTrue(viewModel.currentProject.value!!.autoDuckEnabled)

        viewModel.setAutoDuckEnabled(false)

        assertFalse(viewModel.currentProject.value!!.autoDuckEnabled)

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.autoDuckEnabled)
    }

    @Test
    fun `duck strength is clamped to the supported range`() {
        val (viewModel, _) = viewModel()

        viewModel.setAutoDuckStrength(2f)
        assertEquals(1f, viewModel.currentProject.value!!.autoDuckStrength, 0.0001f)

        viewModel.setAutoDuckStrength(-1f)
        assertEquals(0f, viewModel.currentProject.value!!.autoDuckStrength, 0.0001f)

        viewModel.setAutoDuckStrength(0.5f)
        assertEquals(0.5f, viewModel.currentProject.value!!.autoDuckStrength, 0.0001f)
    }
}
