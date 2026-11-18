package com.example.engine

import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** The export-time loudness boost: real gain, applied to every audio source. */
class AudioBoostTest {

    private fun project() = VideoProject(
        id = "boost",
        title = "Boost",
        clips = listOf(
            MediaClip(
                id = "clip",
                title = "Clip",
                durationMs = 2_000L,
                originalDurationMs = 2_000L,
                volume = 0.5f
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
                        title = "PiP",
                        durationMs = 1_000L,
                        originalDurationMs = 1_000L,
                        volume = 1f
                    )
                )
            )
        ),
        audioTracks = listOf(
            AudioClip(
                id = "music",
                title = "Music",
                artist = "Test",
                durationMs = 2_000L,
                originalDurationMs = 2_000L,
                volume = 1f
            )
        )
    )

    @Test
    fun `no boost leaves the project untouched`() {
        val original = project()
        assertSame(original, AudioBoost.boosted(original, 1f))
        assertSame(original, AudioBoost.boosted(original, 0.5f))
        assertSame(original, AudioBoost.boosted(original, Float.NaN))
    }

    @Test
    fun `the boost raises every audio source by the same gain`() {
        val boosted = AudioBoost.boosted(project(), AudioBoost.SOCIAL_GAIN)
        assertEquals(0.5f * AudioBoost.SOCIAL_GAIN, boosted.clips.single().volume, 0.0001f)
        assertEquals(
            AudioBoost.SOCIAL_GAIN,
            boosted.overlayTracks.single().clips.single().volume,
            0.0001f
        )
        assertEquals(
            AudioBoost.SOCIAL_GAIN,
            boosted.audioTracks.single().volume,
            0.0001f
        )
    }

    @Test
    fun `the gain is capped so a hot mix cannot run away`() {
        val boosted = AudioBoost.boosted(project(), 10f)
        assertEquals(AudioBoost.MAX_GAIN, boosted.audioTracks.single().volume, 0.0001f)
    }

    @Test
    fun `the gain is a real three decibel step`() {
        assertEquals(1.41f, AudioBoost.SOCIAL_GAIN, 0.0001f)
        // 20 * log10(1.41) = 2.99 dB - the boost really is about +3 dB.
        val db = 20f * kotlin.math.log10(AudioBoost.SOCIAL_GAIN)
        assertEquals(3f, db, 0.05f)
        assertEquals(1.41f, AudioBoost.gainFor(boost = true), 0.0001f)
        assertEquals(1f, AudioBoost.gainFor(boost = false), 0.0001f)
    }
}
