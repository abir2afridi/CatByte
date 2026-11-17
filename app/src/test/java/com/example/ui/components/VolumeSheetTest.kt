package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The clip volume panel must expose the real audio controls: the on-device
 * noise gate is enabled and the clip fades commit real millisecond values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VolumeSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `reduce noise is enabled because the on-device gate is real`() {
        compose.setContent {
            VolumeSheet(
                currentVolume = 1f,
                isNoiseReductionEnabled = false,
                canReduceNoise = true,
                onVolumeChange = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("volume_toggle_reduce_noise").assertIsEnabled()
    }

    @Test
    fun `fade slider commits one fade value when the gesture ends`() {
        val fades = mutableListOf<Pair<Long, Long>>()
        compose.setContent {
            VolumeSheet(
                currentVolume = 1f,
                canReduceNoise = true,
                audioFadeInMs = 0L,
                audioFadeOutMs = 0L,
                maxFadeMs = 4_000L,
                onVolumeChange = {},
                onAudioFadesChange = { fadeIn, fadeOut -> fades += fadeIn to fadeOut },
                onClose = {}
            )
        }

        val slider = compose.onNodeWithTag("volume_fade_in").performScrollTo()
        slider.performTouchInput {
            down(center)
            moveTo(Offset(center.x * 1.5f, center.y))
            up()
        }
        compose.waitForIdle()

        assertEquals(1, fades.size)
        val (fadeIn, fadeOut) = fades.single()
        assertTrue("fade in $fadeIn should be inside the clip", fadeIn in 0L..4_000L)
        assertEquals(0L, fadeOut)
    }
}
