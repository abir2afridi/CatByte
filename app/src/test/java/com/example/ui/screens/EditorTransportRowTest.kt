package com.example.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The transport row is the fastest way to reach a timeline marker, so the
 * jump buttons have to show up as soon as the project has markers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorTransportRowTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `marker buttons jump to the previous and the next marker`() {
        var previous = 0
        var next = 0
        compose.setContent {
            EditorTransportRow(
                isPlaying = false,
                canUndo = false,
                canRedo = false,
                onTogglePlay = {},
                onFullscreen = {},
                onUndo = {},
                onRedo = {},
                markerCount = 3,
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
            EditorTransportRow(
                isPlaying = false,
                canUndo = false,
                canRedo = false,
                onTogglePlay = {},
                onFullscreen = {},
                onUndo = {},
                onRedo = {},
                markerCount = 0,
                onPreviousMarker = {},
                onNextMarker = {}
            )
        }

        compose.onNodeWithTag("editor_next_marker_button").assertDoesNotExist()
        compose.onNodeWithTag("editor_previous_marker_button").assertDoesNotExist()
    }
}
