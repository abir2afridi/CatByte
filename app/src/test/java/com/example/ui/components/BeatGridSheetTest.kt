package com.example.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The beat grid sheet must expose the real grid actions, not decorative chips. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BeatGridSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private fun sheet(
        detected: Int = 4,
        pinned: List<Long> = listOf(1_000L, 2_500L),
        playhead: Long = 3_240L,
        onCut: () -> Unit = {},
        onPin: () -> Unit = {},
        onUnpin: () -> Unit = {},
        onClear: () -> Unit = {},
        onRemove: (Long) -> Unit = {},
        onClose: () -> Unit = {}
    ) {
        compose.setContent {
            BeatGridSheet(
                detectedBeatCount = detected,
                pinnedBeatsMs = pinned,
                playheadMs = playhead,
                onCutToBeats = onCut,
                onPinAtPlayhead = onPin,
                onRemoveAtPlayhead = onUnpin,
                onClearPinned = onClear,
                onRemovePin = onRemove,
                onClose = onClose
            )
        }
    }

    @Test
    fun `status shows detected and pinned beats plus the playhead`() {
        sheet()

        compose.onNodeWithTag("beat_grid_status").assertTextEquals("4 detected · 2 pinned")
        compose.onNodeWithTag("beat_grid_playhead").assertTextEquals("Playhead 0:03.240")
    }

    @Test
    fun `actions report the tapped control`() {
        var cut = 0
        var pin = 0
        var unpin = 0
        var clear = 0
        var close = 0
        sheet(
            onCut = { cut++ },
            onPin = { pin++ },
            onUnpin = { unpin++ },
            onClear = { clear++ },
            onClose = { close++ }
        )

        compose.onNodeWithTag("beat_grid_cut").performClick()
        compose.onNodeWithTag("beat_grid_pin").performClick()
        compose.onNodeWithTag("beat_grid_unpin").performScrollTo().performClick()
        compose.onNodeWithTag("beat_grid_clear").performScrollTo().performClick()
        compose.onNodeWithTag("beat_grid_close").performClick()
        compose.waitForIdle()

        assertEquals(1, cut)
        assertEquals(1, pin)
        assertEquals(1, unpin)
        assertEquals(1, clear)
        assertEquals(1, close)
    }

    @Test
    fun `cut and pin removal are disabled without enough beats`() {
        sheet(detected = 1, pinned = emptyList(), playhead = 0L)

        compose.onNodeWithTag("beat_grid_cut").assertIsNotEnabled()
        compose.onNodeWithTag("beat_grid_unpin").assertIsNotEnabled()
        compose.onNodeWithTag("beat_grid_clear").assertIsNotEnabled()
        compose.onNodeWithTag("beat_grid_pin").assertIsEnabled()
    }

    @Test
    fun `pinned rows remove the exact beat`() {
        val removed = mutableListOf<Long>()
        sheet(onRemove = { removed += it })

        compose.onNodeWithTag("beat_grid_pin_remove_2500").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf(2_500L), removed)
    }
}
