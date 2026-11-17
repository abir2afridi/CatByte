package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.model.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `decoded waveforms render and undecoded ones stay flat`() {
        var current by mutableStateOf(audio)
        compose.setContent {
            TimelineAudioTrack(tracks = listOf(current), msToDp = 0.1f)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("timeline_audio_waveform_music", useUnmergedTree = true)
            .assertDoesNotExist()

        compose.runOnUiThread {
            current = audio.copy(waveform = listOf(0.2f, 0.6f, 0.9f))
        }
        compose.waitForIdle()
        compose.onNodeWithTag("timeline_audio_waveform_music", useUnmergedTree = true)
            .assertExists()
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

    @Test
    fun `timeline drag clamps the clip before the project end`() {
        val moved = mutableListOf<Pair<String, Long>>()
        compose.setContent {
            TimelineAudioTrack(
                tracks = listOf(audio.copy(durationMs = 4_000L, originalDurationMs = 4_000L)),
                msToDp = 0.1f,
                timelineDurationMs = 10_000L,
                onMoveClip = { id, start -> moved += id to start }
            )
        }

        compose.onNodeWithTag("timeline_audio_clip_music").performTouchInput {
            down(center)
            advanceEventTime(700L)
            moveTo(Offset(center.x + 1_000f, center.y))
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf("music" to 6_000L), moved)
    }

    @Test
    fun `successive drags use the latest committed clip position`() {
        var current by mutableStateOf(audio.copy(durationMs = 4_000L, originalDurationMs = 4_000L))
        val moved = mutableListOf<Long>()
        compose.setContent {
            TimelineAudioTrack(
                tracks = listOf(current),
                msToDp = 0.1f,
                timelineDurationMs = 10_000L,
                onMoveClip = { _, start ->
                    moved += start
                    current = current.copy(startTimelineMs = start)
                }
            )
        }

        val clip = compose.onNodeWithTag("timeline_audio_clip_music")
        clip.performTouchInput {
            down(center)
            advanceEventTime(700L)
            moveTo(Offset(center.x + 200f, center.y))
            up()
        }
        compose.waitForIdle()
        clip.performTouchInput {
            down(center)
            advanceEventTime(700L)
            moveTo(Offset(center.x + 200f, center.y))
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(2_000L, 4_000L), moved)
    }

    @Test
    fun `short audio clips keep the mute control inside the clip`() {
        val shortAudio = audio.copy(durationMs = 200L, originalDurationMs = 200L)
        compose.setContent {
            TimelineAudioTrack(
                tracks = listOf(shortAudio),
                msToDp = 0.1f
            )
        }

        val clipBounds = compose.onNodeWithTag("timeline_audio_clip_music")
            .getUnclippedBoundsInRoot()
        val muteBounds = compose.onNodeWithTag("timeline_audio_mute_music")
            .getUnclippedBoundsInRoot()

        assertTrue(muteBounds.left >= clipBounds.left)
        assertTrue(muteBounds.right <= clipBounds.right)
    }
}
