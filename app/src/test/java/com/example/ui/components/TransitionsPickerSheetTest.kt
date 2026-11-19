package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.example.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The transition picker used to show decorative category tabs and a
 * duration slider that changed nothing. The tabs must really filter the
 * offered transitions, the slider must report a real length and every
 * callback must carry the clip the picker was opened for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransitionsPickerSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `picking a transition reports the clip and the transition`() {
        val picks = mutableListOf<Pair<String, TransitionType>>()
        compose.setContent {
            TransitionsPickerSheet(
                clipId = "clip_7",
                currentTransition = TransitionType.NONE,
                currentDurationMs = 400L,
                onSelectTransition = { clipId, transition -> picks += clipId to transition },
                onDurationChanged = { _, _ -> },
                onClose = {}
            )
        }

        compose.onNodeWithTag("transition_dissolve").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("clip_7" to TransitionType.DISSOLVE), picks)
    }

    @Test
    fun `the category tab really filters the offered transitions`() {
        compose.setContent {
            TransitionsPickerSheet(
                clipId = "clip_7",
                currentTransition = TransitionType.NONE,
                currentDurationMs = 400L,
                onSelectTransition = { _, _ -> },
                onDurationChanged = { _, _ -> },
                onClose = {}
            )
        }

        // "All" starts with the basic transitions.
        compose.onNodeWithTag("transition_tab_all").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("transition_dissolve").assertExists()

        // "Style" really replaces the offered set with the torn-edge
        // transitions - the basic ones are gone.
        compose.onNodeWithTag("transition_tab_style").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("transition_paper_tear").assertExists()
        compose.onNodeWithTag("transition_pencil_wipe").assertExists()
        compose.onNodeWithTag("transition_ink_bleed").assertExists()
        compose.onNodeWithTag("transition_dissolve").assertDoesNotExist()
    }

    @Test
    fun `the basic tab offers the basic transitions`() {
        compose.setContent {
            TransitionsPickerSheet(
                clipId = "clip_7",
                currentTransition = TransitionType.NONE,
                currentDurationMs = 400L,
                onSelectTransition = { _, _ -> },
                onDurationChanged = { _, _ -> },
                onClose = {}
            )
        }

        compose.onNodeWithTag("transition_tab_basic").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("transition_dissolve").assertExists()
        compose.onNodeWithTag("transition_fade_black").assertExists()
        compose.onNodeWithTag("transition_flash_white").assertExists()
        compose.onNodeWithTag("transition_page_flip").assertDoesNotExist()
    }

    @Test
    fun `the duration slider reports the clip and a real length`() {
        val durations = mutableListOf<Pair<String, Long>>()
        compose.setContent {
            TransitionsPickerSheet(
                clipId = "clip_7",
                currentTransition = TransitionType.NONE,
                currentDurationMs = 400L,
                onSelectTransition = { _, _ -> },
                onDurationChanged = { clipId, durationMs -> durations += clipId to durationMs },
                onClose = {}
            )
        }

        compose.onNodeWithTag("transition_duration").performTouchInput {
            down(center)
            moveTo(Offset(center.x * 1.5f, center.y))
            up()
        }
        compose.waitForIdle()

        assertTrue("the slider must report a duration", durations.isNotEmpty())
        assertEquals("clip_7", durations.last().first)
        assertTrue(
            "the reported duration must be longer than the old one, got ${durations.last().second}",
            durations.last().second > 400L
        )
    }
}
