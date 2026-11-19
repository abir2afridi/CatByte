package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.example.model.ClipAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The animation panel must not show controls that do nothing: the duration
 * slider has to report a real length and the presets have to be selected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnimationSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the duration slider reports the new length`() {
        val durations = mutableListOf<Long>()
        compose.setContent {
            AnimationSheet(
                current = ClipAnimation.ZOOM_IN,
                currentDurationMs = 600L,
                onSelect = {},
                onDurationChanged = { durations += it },
                onClose = {}
            )
        }

        val slider = compose.onNodeWithTag("animation_duration")
        slider.performTouchInput {
            down(center)
            moveTo(Offset(center.x * 1.5f, center.y))
            up()
        }
        compose.waitForIdle()

        assertTrue("the slider must report a duration", durations.isNotEmpty())
        assertTrue(
            "the reported duration must be longer than the old one, got ${durations.last()}",
            durations.last() > 600L
        )
    }

    @Test
    fun `the animation presets report the selection`() {
        val selected = mutableListOf<ClipAnimation>()
        compose.setContent {
            AnimationSheet(
                current = ClipAnimation.NONE,
                currentDurationMs = 600L,
                onSelect = { selected += it },
                onDurationChanged = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("animation_fade_in").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf(ClipAnimation.FADE_IN), selected)
    }
}
