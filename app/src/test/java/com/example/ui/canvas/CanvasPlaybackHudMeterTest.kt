package com.example.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.player.PlaybackAudioMeter
import com.example.model.MediaClip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The canvas meter must draw measured peaks, not a fixed decoration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CanvasPlaybackHudMeterTest {

    @get:Rule
    val compose = createComposeRule()

    private val clip = MediaClip(id = "clip", title = "Clip")

    @After
    fun tearDown() {
        PlaybackAudioMeter.reset()
    }

    private fun meterNodes() = compose.onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
    ).fetchSemanticsNodes()

    @Test
    fun `no meter is drawn while the preview is paused`() {
        PlaybackAudioMeter.publish(0.8f, 0.6f)

        compose.setContent {
            Box {
                PlaybackMeterStrip(visible = false)
            }
        }
        compose.waitForIdle()

        assertTrue(meterNodes().isEmpty())
    }

    @Test
    fun `playing audio draws both measured channels`() {
        PlaybackAudioMeter.publish(0.8f, 0.6f)

        compose.setContent {
            Box {
                PlaybackMeterStrip(visible = true)
            }
        }
        compose.waitForIdle()

        val bars = meterNodes()
        assertEquals(2, bars.size)
        val values = bars.map {
            it.config[SemanticsProperties.ProgressBarRangeInfo].current
        }
        // The bars snap up to the published peaks.
        assertEquals(0.8f, values.max(), 0.01f)
        assertEquals(0.6f, values.min(), 0.01f)
    }
}
