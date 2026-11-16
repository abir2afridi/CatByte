package com.example.data

import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which clips count as "media that must still resolve". */
class MediaAvailabilityTest {

    private fun project(vararg clips: MediaClip, audio: List<AudioClip> = emptyList()): VideoProject =
        VideoProject(
            id = "p",
            title = "Project",
            clips = clips.toList(),
            audioTracks = audio
        )

    @Test
    fun `clips with a uri are checked`() {
        val refs = MediaAvailability.mediaRefs(
            project(
                MediaClip(id = "a", title = "A", uriString = "content://media/1"),
                MediaClip(id = "b", title = "B", drawableResId = 42)
            )
        )
        assertEquals(listOf("a"), refs.map { it.id })
        assertEquals(false, refs.single().isAudio)
    }

    @Test
    fun `placeholders without media are ignored`() {
        val refs = MediaAvailability.mediaRefs(
            project(
                MediaClip(id = "empty", title = "Empty"),
                MediaClip(id = "blank", title = "Blank", uriString = "  ")
            )
        )
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `overlay clips and audio tracks are included`() {
        val refs = MediaAvailability.mediaRefs(
            VideoProject(
                id = "p",
                title = "P",
                overlayTracks = listOf(
                    VideoTrack(
                        clips = listOf(
                            MediaClip(id = "overlay", title = "PIP", uriString = "file:///tmp/a.mp4")
                        )
                    )
                ),
                audioTracks = listOf(
                    AudioClip(id = "music", title = "Music", artist = "A", uriString = "content://audio/9"),
                    AudioClip(id = "library", title = "Built-in", artist = "A")
                )
            )
        )
        assertEquals(listOf("overlay", "music"), refs.map { it.id })
        assertEquals(true, refs.first { it.id == "music" }.isAudio)
    }
}
