package com.example.ui.audio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.model.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The mixer sheet must expose real per-track actions, not decorative controls. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioMixerSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val audio = AudioClip(
        id = "music",
        title = "Music",
        artist = "Test",
        uriString = "content://audio/music",
        durationMs = 4_000L,
        originalDurationMs = 4_000L
    )

    @Test
    fun `mute reset and delete report the selected track`() {
        val muted = mutableListOf<Pair<String, Boolean>>()
        val reset = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { id, value -> muted += id to value },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = { reset += it },
                onDelete = { deleted += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_mute").performClick()
        compose.onNodeWithTag("audio_mixer_reset").performScrollTo().performClick()
        compose.onNodeWithTag("audio_mixer_delete").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("music" to true), muted)
        assertEquals(listOf("music"), reset)
        assertEquals(listOf("music"), deleted)
        assertTrue(compose.onAllNodesWithTag("audio_mixer_sheet").fetchSemanticsNodes().isNotEmpty())
    }
}
