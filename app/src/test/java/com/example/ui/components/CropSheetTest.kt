package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The quick rotate/flip actions of the crop panel must really change the
 * clip transform - they used to nudge the zoom and the pan instead, which
 * made the buttons look alive but changed nothing that the renderer saw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CropSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the rotate buttons report the new absolute rotation`() {
        val rotations = mutableListOf<Float>()
        compose.setContent {
            CropSheet(
                sourceWidth = 1080,
                sourceHeight = 1920,
                currentScale = 1f,
                currentPosX = 0.5f,
                currentPosY = 0.5f,
                onApply = { _, _, _ -> },
                currentRotation = 0f,
                onRotationChanged = { rotations += it },
                currentMirrored = false,
                onMirroredChanged = {},
                onReset = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("crop_rotate_90").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("crop_rotate_180").performClick()
        compose.waitForIdle()

        assertEquals(listOf(90f, 270f), rotations)
    }

    @Test
    fun `the rotation wraps around a full turn`() {
        val rotations = mutableListOf<Float>()
        compose.setContent {
            CropSheet(
                sourceWidth = 1080,
                sourceHeight = 1920,
                currentScale = 1f,
                currentPosX = 0.5f,
                currentPosY = 0.5f,
                onApply = { _, _, _ -> },
                currentRotation = 270f,
                onRotationChanged = { rotations += it },
                currentMirrored = false,
                onMirroredChanged = {},
                onReset = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("crop_rotate_90").performClick()
        compose.waitForIdle()

        assertEquals(listOf(0f), rotations)
    }

    @Test
    fun `the flip button toggles the mirror state`() {
        val mirrors = mutableListOf<Boolean>()
        compose.setContent {
            CropSheet(
                sourceWidth = 1080,
                sourceHeight = 1920,
                currentScale = 1f,
                currentPosX = 0.5f,
                currentPosY = 0.5f,
                onApply = { _, _, _ -> },
                currentRotation = 0f,
                onRotationChanged = {},
                currentMirrored = false,
                onMirroredChanged = { mirrors += it },
                onReset = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("crop_flip_h").performClick()
        compose.waitForIdle()

        assertEquals(listOf(true), mirrors)
    }

    @Test
    fun `the rotate and flip actions never touch the crop transform`() {
        var applies = 0
        compose.setContent {
            CropSheet(
                sourceWidth = 1080,
                sourceHeight = 1920,
                currentScale = 1f,
                currentPosX = 0.5f,
                currentPosY = 0.5f,
                onApply = { _, _, _ -> applies++ },
                currentRotation = 0f,
                onRotationChanged = {},
                currentMirrored = false,
                onMirroredChanged = {},
                onReset = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("crop_rotate_90").performClick()
        compose.onNodeWithTag("crop_flip_h").performClick()
        compose.waitForIdle()

        assertTrue("rotation and flip must not rewrite the crop rectangle", applies == 0)
    }
}
