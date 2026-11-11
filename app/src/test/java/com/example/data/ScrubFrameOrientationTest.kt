package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrubFrameOrientationTest {

    @Test
    fun `normalises rotations to quarter turns`() {
        assertTrue(ScrubFrameOrientation.normalizedRotation(0) == 0)
        assertTrue(ScrubFrameOrientation.normalizedRotation(90) == 90)
        assertTrue(ScrubFrameOrientation.normalizedRotation(270) == 270)
        assertTrue(ScrubFrameOrientation.normalizedRotation(-90) == 270)
        assertTrue(ScrubFrameOrientation.normalizedRotation(360) == 0)
        // Not a quarter turn: nothing can be applied safely.
        assertTrue(ScrubFrameOrientation.normalizedRotation(45) == 0)
    }

    @Test
    fun `no rotation metadata needs no rotation`() {
        assertFalse(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 0,
                frameWidth = 1920,
                frameHeight = 1080,
                rawWidth = 1920,
                rawHeight = 1080
            )
        )
    }

    @Test
    fun `portrait source encoded landscape is rotated`() {
        // 1920x1080 encoded + 90 degrees: the raw frame is landscape.
        assertTrue(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 90,
                frameWidth = 1920,
                frameHeight = 1080,
                rawWidth = 1920,
                rawHeight = 1080
            )
        )
    }

    @Test
    fun `already rotated frame is left alone`() {
        assertFalse(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 90,
                frameWidth = 1080,
                frameHeight = 1920,
                rawWidth = 1920,
                rawHeight = 1080
            )
        )
    }

    @Test
    fun `unknown source size is assumed to be raw`() {
        assertTrue(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 270,
                frameWidth = 1920,
                frameHeight = 1080,
                rawWidth = 0,
                rawHeight = 0
            )
        )
    }

    @Test
    fun `scaled frame is compared by aspect ratio`() {
        // Hardware scaling produced 1280x720 for a 1920x1080 source: still raw.
        assertTrue(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 90,
                frameWidth = 1280,
                frameHeight = 720,
                rawWidth = 1920,
                rawHeight = 1080
            )
        )
        // ... and a 720x1280 frame for the same source is already upright.
        assertFalse(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 90,
                frameWidth = 720,
                frameHeight = 1280,
                rawWidth = 1920,
                rawHeight = 1080
            )
        )
    }

    @Test
    fun `half turn is applied even though the size cannot reveal it`() {
        assertTrue(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 180,
                frameWidth = 1920,
                frameHeight = 1080,
                rawWidth = 1920,
                rawHeight = 1080
            )
        )
    }

    @Test
    fun `square source is assumed to be raw`() {
        // A square source cannot be told apart by its aspect, so the platform
        // behaviour (raw orientation) wins.
        assertTrue(
            ScrubFrameOrientation.needsRotation(
                rotationDegrees = 90,
                frameWidth = 1080,
                frameHeight = 1080,
                rawWidth = 1080,
                rawHeight = 1080
            )
        )
    }
}
