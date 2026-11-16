package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.model.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Independent audio rows must be selectable and expose their real mute action. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimelineAudioTrackTest {

    @get:Rule
    val compose = createComposeRule()

    private val audio = AudioClip(
        id = "music",
        title = "Music",
        artist = "Test",
        uriString = "content://audio/music",
        durationMs = 2_000L,
        originalDurationMs = 2_000L
    )

    @Test
    fun `tapping an audio clip reports its id`() {
        val selected = mutableListOf<String>()
        compose.setContent {
            TimelineAudioTrack(
                tracks = listOf(audio),
                msToDp = 0.1f,
                onSelectAudio = { selected += it }
            )
        }

        compose.onNodeWithTag("timeline_audio_clip_music").performClick()
        compose.waitForIdle()
        assertEquals(listOf("music"), selected)
    }

    @Test
    fun `mute button reports the clip and requested state`() {
        val muted = mutableListOf<Pair<String, Boolean>>()
        compose.setContent {
            TimelineAudioTrack(
                tracks = listOf(audio),
                msToDp = 0.1f,
                onToggleMute = { id, value -> muted += id to value }
            )
        }

        compose.onNodeWithTag("timeline_audio_mute_music").performClick()
        compose.waitForIdle()
        assertEquals(listOf("music" to true), muted)
    }
}
