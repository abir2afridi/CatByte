package com.example.data.export

import com.example.model.AudioClip
import com.example.model.EffectType
import com.example.model.MediaClip
import com.example.model.StickerOverlay
import com.example.model.StrokeOverlay
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One-track audio export: the copy that is handed to the render pipeline. */
class ExportAudioTrackProjectTest {

    private fun project() = VideoProject(
        id = "track_export",
        title = "Track export",
        clips = listOf(
            MediaClip(
                id = "video",
                title = "Video",
                durationMs = 5_000L,
                originalDurationMs = 5_000L
            )
        ),
        overlayTracks = listOf(
            VideoTrack(
                id = "pip",
                name = "PiP",
                isMainTrack = false,
                clips = listOf(
                    MediaClip(
                        id = "pip_clip",
                        title = "PiP clip",
                        durationMs = 2_000L,
                        originalDurationMs = 2_000L
                    )
                )
            )
        ),
        audioTracks = listOf(
            AudioClip(
                id = "music",
                title = "Music",
                artist = "Test",
                uriString = "content://audio/music",
                startTimelineMs = 4_000L,
                durationMs = 8_000L,
                originalDurationMs = 8_000L,
                volume = 0.6f,
                fadeInMs = 750L,
                fadeOutMs = 1_250L,
                speed = 1.5f,
                isMuted = true,
                isSoloed = true
            ),
            AudioClip(
                id = "voice",
                title = "Voice",
                artist = "Test",
                uriString = "content://audio/voice",
                durationMs = 3_000L,
                originalDurationMs = 3_000L
            )
        ),
        textOverlays = listOf(TextOverlay(id = "text", text = "Hello")),
        stickers = listOf(StickerOverlay(id = "sticker", symbol = "sparkles")),
        strokes = listOf(
            StrokeOverlay(id = "stroke", points = listOf(0.1f, 0.1f, 0.9f, 0.9f))
        ),
        effects = listOf(
            TimelineEffect(id = "effect", name = "RGB", effectType = EffectType.RGB_SPLIT)
        )
    )

    @Test
    fun `an unknown track is reported instead of rendering the whole timeline`() {
        assertNull(singleTrackProject(project(), "missing"))
    }

    @Test
    fun `only the chosen track survives and starts at zero`() {
        val trackProject = singleTrackProject(project(), "music")!!

        val track = trackProject.audioTracks.single()
        assertEquals("music", track.id)
        assertEquals(0L, track.startTimelineMs)
        // The track keeps the sound the mixer produced.
        assertEquals(0.6f, track.volume, 0.0001f)
        assertEquals(750L, track.fadeInMs)
        assertEquals(1_250L, track.fadeOutMs)
        assertEquals(1.5f, track.speed, 0.0001f)
        assertTrue(track.isMuted)
        assertTrue(track.isSoloed)
    }

    @Test
    fun `every visual element is dropped from the export copy`() {
        val trackProject = singleTrackProject(project(), "music")!!

        assertTrue(trackProject.clips.isEmpty())
        assertTrue(trackProject.overlayTracks.isEmpty())
        assertTrue(trackProject.textOverlays.isEmpty())
        assertTrue(trackProject.stickers.isEmpty())
        assertTrue(trackProject.strokes.isEmpty())
        assertTrue(trackProject.effects.isEmpty())
    }

    @Test
    fun `the project-level settings are preserved and the original is untouched`() {
        val original = project()
        val trackProject = singleTrackProject(original, "music")!!

        assertEquals("Track export", trackProject.title)
        assertEquals(original.aspectRatio, trackProject.aspectRatio)
        assertEquals(original.autoDuckEnabled, trackProject.autoDuckEnabled)
        // The original project keeps both tracks and its timeline placement.
        assertEquals(2, original.audioTracks.size)
        assertEquals(4_000L, original.audioTracks.single { it.id == "music" }.startTimelineMs)
    }
}
