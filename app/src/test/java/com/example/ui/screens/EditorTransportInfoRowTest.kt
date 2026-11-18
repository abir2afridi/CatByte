package com.example.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.model.TimelineMarker
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The strip below the transport bar carries the fullscreen button, the aspect
 * ratio chip, the live timecode and the marker jump buttons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorTransportInfoRowTest {

    @get:Rule
    val compose = createComposeRule()

    private val markers = listOf(
        TimelineMarker(id = "m1", atMs = 2_000L, label = "Intro"),
        TimelineMarker(id = "m2", atMs = 5_000L, label = "Hook")
    )

    @Test
    fun `marker buttons ask for the previous and the next marker`() {
        var previous = 0
        var next = 0
        compose.setContent {
            EditorTransportInfoRow(
                aspectRatioLabel = "1:1",
                currentPositionMs = 3_000L,
                totalDurationMs = 30_000L,
                markers = markers,
                onFullscreen = {},
                onSeek = {},
                onJumpToMarker = {},
                onPreviousMarker = { previous++ },
                onNextMarker = { next++ }
            )
        }

        compose.onNodeWithTag("editor_next_marker_button").performClick()
        compose.onNodeWithTag("editor_previous_marker_button").performClick()
        compose.waitForIdle()

        assertEquals(1, next)
        assertEquals(1, previous)
    }

    @Test
    fun `marker buttons stay hidden while the project has no markers`() {
        compose.setContent {
            EditorTransportInfoRow(
                aspectRatioLabel = "1:1",
                currentPositionMs = 3_000L,
                totalDurationMs = 30_000L,
                markers = emptyList(),
                onFullscreen = {},
                onSeek = {},
                onJumpToMarker = {}
            )
        }

        compose.onNodeWithTag("editor_next_marker_button").assertDoesNotExist()
        compose.onNodeWithTag("editor_previous_marker_button").assertDoesNotExist()
    }

    @Test
    fun `the timecode opens the jump dialog with the project markers`() {
        val jumps = mutableListOf<String>()
        compose.setContent {
            EditorTransportInfoRow(
                aspectRatioLabel = "1:1",
                currentPositionMs = 3_000L,
                totalDurationMs = 30_000L,
                markers = markers,
                onFullscreen = {},
                onSeek = {},
                onJumpToMarker = { jumps += it }
            )
        }

        compose.onNodeWithTag("editor_timecode_chip").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("editor_jump_marker_m2").performClick()
        compose.waitForIdle()

        assertEquals(listOf("m2"), jumps)
    }
}
