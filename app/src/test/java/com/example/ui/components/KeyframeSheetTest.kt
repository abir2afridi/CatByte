package com.example.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The keyframe navigation buttons must move the playhead to a real
 * keyframe. They used to be empty click handlers that looked alive but did
 * nothing at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyframeSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val clip = MediaClip(
        id = "video",
        title = "Video",
        durationMs = 5_000L,
        originalDurationMs = 5_000L,
        keyframes = listOf(
            Keyframe(id = "k1", timestampMs = 1_000L, property = KeyframeProperty.SCALE, value = 1f),
            Keyframe(id = "k2", timestampMs = 3_000L, property = KeyframeProperty.SCALE, value = 2f)
        )
    )

    @Test
    fun `the previous button seeks to the previous keyframe`() {
        val seeks = mutableListOf<Long>()
        compose.setContent {
            KeyframeSheet(
                clip = clip,
                onAddKeyframe = { _: KeyframeProperty, _: Float, _: EasingCurve -> },
                onRemoveKeyframe = {},
                previousKeyframeMs = 1_000L,
                nextKeyframeMs = 3_000L,
                onSeekTo = { seeks += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("keyframe_prev").performClick()
        compose.waitForIdle()

        assertEquals(listOf(1_000L), seeks)
    }

    @Test
    fun `the next button seeks to the next keyframe`() {
        val seeks = mutableListOf<Long>()
        compose.setContent {
            KeyframeSheet(
                clip = clip,
                onAddKeyframe = { _: KeyframeProperty, _: Float, _: EasingCurve -> },
                onRemoveKeyframe = {},
                previousKeyframeMs = 1_000L,
                nextKeyframeMs = 3_000L,
                onSeekTo = { seeks += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("keyframe_next").performClick()
        compose.waitForIdle()

        assertEquals(listOf(3_000L), seeks)
    }

    @Test
    fun `the buttons are disabled when there is no neighbouring keyframe`() {
        val seeks = mutableListOf<Long>()
        compose.setContent {
            KeyframeSheet(
                clip = clip,
                onAddKeyframe = { _: KeyframeProperty, _: Float, _: EasingCurve -> },
                onRemoveKeyframe = {},
                previousKeyframeMs = null,
                nextKeyframeMs = null,
                onSeekTo = { seeks += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("keyframe_prev").assertIsNotEnabled()
        compose.onNodeWithTag("keyframe_next").assertIsNotEnabled()

        compose.onNodeWithTag("keyframe_prev").performClick()
        compose.onNodeWithTag("keyframe_next").performClick()
        compose.waitForIdle()

        assertEquals(emptyList<Long>(), seeks)
    }
}
