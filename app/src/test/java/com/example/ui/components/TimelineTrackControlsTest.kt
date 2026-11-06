package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Track header controls must be real: a mute/lock/solo tap has to reach the
 * project, not a local `remember` toggle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimelineTrackControlsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `track header buttons report which control was tapped`() {
        val taps = mutableListOf<String>()
        compose.setContent {
            TimelineTrackHeaders(
                rows = listOf(
                    TimelineHeaderRow(
                        title = "Main",
                        heightDp = 67.dp,
                        isMuted = false,
                        isLocked = true,
                        isSolo = false,
                        canMute = true,
                        canLock = true,
                        canSolo = true,
                        onMute = { taps += "mute" },
                        onLock = { taps += "lock" },
                        onSolo = { taps += "solo" }
                    )
                )
            )
        }

        compose.onNodeWithTag("track_header_Mute track").performClick()
        compose.onNodeWithTag("track_header_Lock track").performClick()
        compose.onNodeWithTag("track_header_Solo track").performClick()
        compose.waitForIdle()

        assertEquals(listOf("mute", "lock", "solo"), taps)
    }
}
