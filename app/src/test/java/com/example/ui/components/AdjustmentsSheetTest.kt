package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Adjust panel must expose real colour controls: the comparison asks the
 * preview for the original, and the colour channels really write an HSL
 * grade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdjustmentsSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val clip = MediaClip(
        id = "video",
        title = "Video",
        durationMs = 5_000L,
        originalDurationMs = 5_000L
    )

    @Test
    fun `the compare chip reports the toggle`() {
        val toggles = mutableListOf<Boolean>()
        compose.setContent {
            AdjustmentsSheet(
                clip = clip,
                onUpdateAdjustments = { _, _, _, _ -> },
                onCompareChanged = { toggles += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("adjustments_compare").performClick()
        compose.waitForIdle()

        assertEquals(listOf(true), toggles)
    }

    @Test
    fun `the compare chip shows the original state`() {
        compose.setContent {
            AdjustmentsSheet(
                clip = clip,
                onUpdateAdjustments = { _, _, _, _ -> },
                isComparing = true,
                onCompareChanged = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("adjustments_compare")
            .assertTextContains("Original")
    }

    @Test
    fun `the hue slider reports the selected colour channel`() {
        val grades = mutableListOf<List<HslAdjustment>>()
        compose.setContent {
            AdjustmentsSheet(
                clip = clip,
                onUpdateAdjustments = { _, _, _, _ -> },
                onUpdateHsl = { grades += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("adjustments_hsl_green").performClick()
        compose.waitForIdle()

        val slider = compose.onNodeWithTag("adjustments_hsl_hue").performScrollTo()
        slider.performTouchInput {
            down(center)
            moveTo(Offset(center.x * 1.5f, center.y))
            up()
        }
        compose.waitForIdle()

        assertEquals(1, grades.size)
        val grade = grades.single()
        assertEquals(1, grade.size)
        assertEquals(HslChannel.GREEN, grade.single().channel)
        assertTrue("the hue should move to the right", grade.single().hue > 0f)
    }

    @Test
    fun `the reset button removes the grade of the selected channel`() {
        val graded = clip.copy(
            hslAdjustments = listOf(
                HslAdjustment(HslChannel.RED, hue = 30f),
                HslAdjustment(HslChannel.BLUE, saturation = 20f)
            )
        )
        val grades = mutableListOf<List<HslAdjustment>>()
        compose.setContent {
            AdjustmentsSheet(
                clip = graded,
                onUpdateAdjustments = { _, _, _, _ -> },
                onUpdateHsl = { grades += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("adjustments_hsl_reset").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(1, grades.size)
        // The blue channel survives, the red one is gone.
        assertEquals(1, grades.single().size)
        assertEquals(HslChannel.BLUE, grades.single().single().channel)
    }
}
