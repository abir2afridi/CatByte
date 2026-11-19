package com.example.engine

import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "original" comparison must drop the colour grade of ONE clip and
 * nothing else: the preview variant is what the comparison shows.
 */
class ClipColorAdjustmentsTest {

    private fun clip(id: String) = MediaClip(
        id = id,
        title = id,
        durationMs = 2_000L,
        originalDurationMs = 2_000L,
        brightness = 0.2f,
        contrast = 1.3f,
        saturation = 0.7f,
        vignette = 0.4f,
        hslAdjustments = listOf(HslAdjustment(HslChannel.RED, hue = 30f))
    )

    private fun project(): VideoProject = VideoProject(
        id = "grade",
        title = "Grade",
        clips = listOf(clip("main")),
        overlayTracks = listOf(
            VideoTrack(name = "PIP", isMainTrack = false, clips = listOf(clip("pip")))
        )
    )

    @Test
    fun `neutralizing a clip resets every colour control`() {
        val neutral = ClipColorAdjustments.neutralized(clip("main"))
        assertEquals(0f, neutral.brightness, 0f)
        assertEquals(1f, neutral.contrast, 0f)
        assertEquals(1f, neutral.saturation, 0f)
        assertEquals(0f, neutral.vignette, 0f)
        assertTrue(neutral.hslAdjustments.isEmpty())
        // The other edits of the clip are untouched.
        assertEquals(2_000L, neutral.durationMs)
    }

    @Test
    fun `only the compared clip loses its grade`() {
        val source = project()
        val preview = ClipColorAdjustments.withoutAdjustments(source, "main")

        val main = preview.clips.single()
        assertEquals(0f, main.brightness, 0f)
        assertEquals(1f, main.contrast, 0f)
        assertTrue(main.hslAdjustments.isEmpty())

        // The overlay clip keeps its own grade.
        val pip = preview.overlayTracks.single().clips.single()
        assertEquals(0.2f, pip.brightness, 0f)
        assertEquals(1, pip.hslAdjustments.size)
    }

    @Test
    fun `the source project is never modified`() {
        val source = project()
        ClipColorAdjustments.withoutAdjustments(source, "main")
        assertEquals(0.2f, source.clips.single().brightness, 0f)
        assertEquals(1, source.clips.single().hslAdjustments.size)
    }

    @Test
    fun `an unknown clip leaves the project untouched`() {
        val source = project()
        assertSame(source, ClipColorAdjustments.withoutAdjustments(source, "missing"))
        assertSame(source, ClipColorAdjustments.withoutAdjustments(source, null))
        assertSame(source, ClipColorAdjustments.withoutAdjustments(source, ""))
    }
}
