package com.example.data

import androidx.media3.common.util.UnstableApi
import com.example.engine.RenderGraphBuilder
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.TransitionType
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preview/export parity.
 *
 * Both paths render from `ProjectCompositionFactory`, whose layout now comes
 * from `RenderGraphBuilder`. If the graph and the plan ever disagree, the
 * exported file would cut at a different frame than the preview shows, so these
 * assertions guard the single source of truth.
 */
@UnstableApi
class ProjectCompositionPlanTest {

    private fun clip(
        id: String,
        durationMs: Long = 1_000L,
        uri: String? = "content://clip/$id",
        transition: TransitionType = TransitionType.NONE,
        transitionMs: Long = 400L,
        startMs: Long = 0L
    ) = MediaClip(
        id = id,
        title = id,
        uriString = uri,
        durationMs = durationMs,
        originalDurationMs = durationMs,
        startTimelineMs = startMs,
        transition = transition,
        transitionDurationMs = transitionMs
    )

    private fun project(
        clips: List<MediaClip>,
        overlayTracks: List<VideoTrack> = emptyList(),
        audioTracks: List<AudioClip> = emptyList()
    ) = VideoProject(
        id = "project",
        title = "Project",
        clips = clips,
        overlayTracks = overlayTracks,
        audioTracks = audioTracks,
        fps = 30,
        resolution = "1080P"
    )

    @Test
    fun `plan timeline matches the render graph`() {
        val source = project(listOf(clip("a"), clip("b", durationMs = 2_000L)))
        val graph = RenderGraphBuilder.build(source)
        val plan = ProjectCompositionFactory.plan(source, 1080, 1920, 30)

        assertEquals(graph.durationUs, plan.totalDurationUs)
        assertEquals(2, plan.main.size)
        assertEquals(
            graph.layers.filter { it.zOrder == 0 }.map { it.timelineInUs },
            plan.main.map { it.layer.timelineInUs }
        )
        assertEquals(
            graph.layers.filter { it.zOrder == 0 }.map { it.timelineOutUs },
            plan.main.map { it.layer.timelineOutUs }
        )
    }

    @Test
    fun `clamped transition is identical for preview and export`() {
        val source = project(
            listOf(
                clip("a", durationMs = 1_000L),
                clip("b", durationMs = 2_000L, transition = TransitionType.FADE_BLACK, transitionMs = 800L)
            )
        )
        val plan = ProjectCompositionFactory.plan(source, 1080, 1920, 30)
        val transition = plan.main[1].layer.transitionIn
        assertNotNull(transition)
        // min(1000, 2000) / 2 = 500 ms, never the requested 800 ms.
        assertEquals(500_000L, transition!!.durationUs)
    }

    @Test
    fun `overlay keeps its graph start time`() {
        val overlay = VideoTrack(
            name = "Overlay",
            isMainTrack = false,
            clips = listOf(clip("o", startMs = 500L))
        )
        val plan = ProjectCompositionFactory.plan(project(listOf(clip("a")), overlayTracks = listOf(overlay)), 1080, 1920, 30)

        assertEquals(1, plan.overlays.size)
        assertEquals(500_000L, plan.overlays.single().layer.timelineInUs)
        assertEquals(2, plan.main.size + plan.overlays.size)
    }

    @Test
    fun `plan canvas keeps the project aspect ratio`() {
        val plan = ProjectCompositionFactory.plan(project(listOf(clip("a"))), 1080, 1920, 30)
        val graph = RenderGraphBuilder.build(project(listOf(clip("a"))))
        assertEquals(1080f / 1920f, plan.aspectRatio, 0.0001f)
        assertEquals(graph.canvasWidth.toFloat() / graph.canvasHeight, plan.aspectRatio, 0.0001f)
    }

    @Test
    fun `audio start time comes from the graph`() {
        val audio = AudioClip(
            id = "music",
            title = "Music",
            artist = "Artist",
            uriString = "content://music",
            startTimelineMs = 1_500L,
            durationMs = 2_000L
        )
        val plan = ProjectCompositionFactory.plan(
            project(listOf(clip("a")), audioTracks = listOf(audio)),
            1080,
            1920,
            30
        )
        assertEquals(1, plan.audio.size)
        assertEquals(1_500_000L, plan.audio.single().second.timelineInUs)
    }

    @Test
    fun `placeholder clips become gaps with a warning`() {
        val plan = ProjectCompositionFactory.plan(
            project(listOf(clip("a"), clip("empty", uri = null))),
            1080,
            1920,
            30
        )
        assertEquals(2, plan.main.size)
        assertTrue(plan.warnings.any { it.contains("rendered as a gap") })
    }

    @Test
    fun `audio without media is skipped with a warning`() {
        val audio = AudioClip(
            id = "silent",
            title = "Library track",
            artist = "Artist",
            uriString = null,
            durationMs = 2_000L
        )
        val plan = ProjectCompositionFactory.plan(
            project(listOf(clip("a")), audioTracks = listOf(audio)),
            1080,
            1920,
            30
        )
        assertTrue(plan.audio.isEmpty())
        assertTrue(plan.warnings.any { it.contains("has no media") })
    }

    @Test
    fun `main track plan is contiguous`() {
        val plan = ProjectCompositionFactory.plan(
            project(listOf(clip("a"), clip("b"), clip("c"))),
            1080,
            1920,
            30
        )
        assertEquals(0L, plan.main.first().layer.timelineInUs)
        plan.main.zipWithNext().forEach { (first, second) ->
            assertEquals(first.layer.timelineOutUs, second.layer.timelineInUs)
        }
    }
}
