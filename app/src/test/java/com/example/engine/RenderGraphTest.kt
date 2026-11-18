package com.example.engine

import com.example.model.AspectRatioType
import com.example.model.MediaClip
import com.example.model.TransitionType
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderGraphTest {

    private fun clip(
        id: String,
        durationMs: Long = 1_000L,
        transition: TransitionType = TransitionType.NONE,
        transitionMs: Long = 400L,
        startMs: Long = 0L
    ) = MediaClip(
        id = id,
        title = id,
        durationMs = durationMs,
        originalDurationMs = durationMs,
        transition = transition,
        transitionDurationMs = transitionMs,
        startTimelineMs = startMs
    )

    private fun project(
        clips: List<MediaClip>,
        overlayTracks: List<VideoTrack> = emptyList(),
        aspect: AspectRatioType = AspectRatioType.RATIO_9_16,
        resolution: String = "1080P",
        paperStyle: String = "Drafting Grid"
    ) = VideoProject(
        id = "project",
        title = "Project",
        aspectRatio = aspect,
        clips = clips,
        overlayTracks = overlayTracks,
        resolution = resolution,
        fps = 30,
        paperStyle = paperStyle
    )

    @Test
    fun `canvas size follows aspect and resolution`() {
        assertEquals(1080 to 1920, CanvasMath.canvasSize(AspectRatioType.RATIO_9_16, "1080P"))
        assertEquals(1920 to 1080, CanvasMath.canvasSize(AspectRatioType.RATIO_16_9, "1080P"))
        assertEquals(1080 to 1080, CanvasMath.canvasSize(AspectRatioType.RATIO_1_1, "1080P"))
        assertEquals(1080 to 1350, CanvasMath.canvasSize(AspectRatioType.RATIO_4_5, "1080P"))
        assertEquals(2160, CanvasMath.shortSidePx("4K"))
        assertEquals(540, CanvasMath.shortSidePx("540P"))
        assertEquals(1080, CanvasMath.shortSidePx("unknown"))
    }

    @Test
    fun `main track clips are laid out back to back`() {
        val graph = RenderGraphBuilder.build(project(listOf(clip("a"), clip("b"), clip("c"))))
        assertEquals(3, graph.layers.size)
        assertEquals(0L, graph.layers[0].timelineInUs)
        assertEquals(1_000_000L, graph.layers[0].timelineOutUs)
        assertEquals(1_000_000L, graph.layers[1].timelineInUs)
        assertEquals(3_000_000L, graph.durationUs)
        assertEquals(90L, graph.totalFrames())
        assertEquals(1080, graph.canvasWidth)
        assertEquals(1920, graph.canvasHeight)
    }

    @Test
    fun `transition cannot eat more than half of a clip`() {
        val graph = RenderGraphBuilder.build(
            project(listOf(clip("a"), clip("b", transition = TransitionType.FADE_BLACK, transitionMs = 800L)))
        )
        val transition = graph.layers[1].transitionIn
        assertNotNull(transition)
        assertEquals(500_000L, transition!!.durationUs)
        assertEquals(TransitionKind.DIP_TO_BLACK, transition.kind)
    }

    @Test
    fun `first clip cannot blend into anything`() {
        val graph = RenderGraphBuilder.build(
            project(listOf(clip("a", transition = TransitionType.DISSOLVE), clip("b")))
        )
        assertNull(graph.layers[0].transitionIn)
        assertTrue(graph.warnings.any { it.contains("first clip") })
    }

    @Test
    fun `overlay clips sit above the main track`() {
        val overlay = VideoTrack(name = "Overlay 1", isMainTrack = false, clips = listOf(clip("o", startMs = 500L)))
        val graph = RenderGraphBuilder.build(project(listOf(clip("a")), overlayTracks = listOf(overlay)))
        assertEquals(2, graph.layers.size)
        val top = graph.layers.maxByOrNull { it.zOrder }!!
        assertEquals(1, top.zOrder)
        assertEquals(500_000L, top.timelineInUs)
        assertEquals(0, graph.layers.first().zOrder)
    }

    @Test
    fun `layer sampling is half open`() {
        val graph = RenderGraphBuilder.build(project(listOf(clip("a"), clip("b"))))
        assertEquals(1, graph.layersAt(0L).size)
        assertEquals("b", graph.layersAt(1_000_000L).single().clipId)
        assertEquals(0, graph.layersAt(2_000_000L).size)
    }

    @Test
    fun `colour grade lands in the effect stack`() {
        val graded = clip("a").copy(brightness = 0.2f, contrast = 1.3f, vignette = 0.4f)
        val graph = RenderGraphBuilder.build(project(listOf(graded)))
        val grade = graph.layers.single().effects.single { it.id == "color_grade" }
        assertEquals(0.2f, grade.params.getValue("brightness"), 0.001f)
        assertEquals(1.3f, grade.params.getValue("contrast"), 0.001f)
    }

    @Test
    fun `mask is only emitted when enabled`() {
        val plain = RenderGraphBuilder.build(project(listOf(clip("a"))))
        assertNull(plain.layers.single().mask)

        val masked = clip("b").copy(mask = com.example.model.MaskData(type = com.example.model.MaskType.CIRCLE))
        val graph = RenderGraphBuilder.build(project(listOf(masked)))
        assertEquals(com.example.model.MaskType.CIRCLE, graph.layers.single().mask?.kind)
    }

    @Test
    fun `audio fades are clamped without overlapping`() {
        val audio = com.example.model.AudioClip(
            id = "music",
            title = "Music",
            artist = "Artist",
            durationMs = 2_000L,
            originalDurationMs = 2_000L,
            fadeInMs = 4_000L,
            fadeOutMs = 4_000L
        )
        val graph = RenderGraphBuilder.build(project(listOf(clip("a"))).copy(audioTracks = listOf(audio)))
        val spec = graph.audio.single()
        assertEquals(1_000_000L, spec.fadeInUs)
        assertEquals(1_000_000L, spec.fadeOutUs)
    }

    @Test
    fun `audio spec carries source trim speed and effective mute gain`() {
        val audio = com.example.model.AudioClip(
            id = "music",
            title = "Music",
            artist = "Artist",
            uriString = "content://audio/music",
            durationMs = 2_000L,
            originalDurationMs = 10_000L,
            trimStartMs = 2_000L,
            trimEndMs = 6_000L,
            speed = 2f,
            volume = 0.8f,
            isMuted = true
        )
        val graph = RenderGraphBuilder.build(project(listOf(clip("a"))).copy(audioTracks = listOf(audio)))
        val spec = graph.audio.single()
        assertEquals(10_000_000L, spec.sourceDurationUs)
        assertEquals(2_000_000L, spec.sourceInUs)
        assertEquals(6_000_000L, spec.sourceOutUs)
        assertEquals(2f, spec.speed, 0.0001f)
        assertEquals(0f, spec.volume, 0.0001f)
        assertEquals(2_000_000L, spec.timelineOutUs - spec.timelineInUs)
    }

    @Test
    fun `a soloed audio track silences every other audio track`() {
        val music = com.example.model.AudioClip(
            id = "music",
            title = "Music",
            artist = "Artist",
            durationMs = 2_000L,
            originalDurationMs = 2_000L,
            volume = 0.8f
        )
        val voice = music.copy(id = "voice", title = "Voice", isSoloed = true)
        val soloed = RenderGraphBuilder.build(
            project(listOf(clip("a"))).copy(audioTracks = listOf(music, voice))
        )
        assertEquals(0f, soloed.audio.first { it.clipId == "music" }.volume, 0.0001f)
        assertEquals(0.8f, soloed.audio.first { it.clipId == "voice" }.volume, 0.0001f)

        // Without a soloed track every track keeps its own level again.
        val mixed = RenderGraphBuilder.build(
            project(listOf(clip("a"))).copy(audioTracks = listOf(music, voice.copy(isSoloed = false)))
        )
        assertEquals(0.8f, mixed.audio.first { it.clipId == "music" }.volume, 0.0001f)
    }

    @Test
    fun `paper style sets the background`() {
        val graph = RenderGraphBuilder.build(project(listOf(clip("a")), paperStyle = "Blueprint"))
        assertEquals(0xFF0B1E33, graph.backgroundArgb)
        val drafting = RenderGraphBuilder.build(project(listOf(clip("a")), paperStyle = "Drafting Grid"))
        assertEquals(0xFF1A1A1E, drafting.backgroundArgb)
        val parchment = RenderGraphBuilder.build(project(listOf(clip("a")), paperStyle = "Parchment Sketchbook"))
        assertEquals(0xFF2A2118, parchment.backgroundArgb)
    }
}
