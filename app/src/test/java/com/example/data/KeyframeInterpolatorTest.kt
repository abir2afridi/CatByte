package com.example.data

import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Behaviour contract of the keyframe evaluation used by preview and export. */
class KeyframeInterpolatorTest {

    private fun scaleKeyframes(): List<Keyframe> = listOf(
        Keyframe(id = "k1", timestampMs = 0L, property = KeyframeProperty.SCALE, value = 1.0f, easing = EasingCurve.LINEAR),
        Keyframe(id = "k2", timestampMs = 1000L, property = KeyframeProperty.SCALE, value = 3.0f, easing = EasingCurve.LINEAR)
    )

    private fun opacityKeyframes(): List<Keyframe> = listOf(
        Keyframe(id = "k3", timestampMs = 500L, property = KeyframeProperty.OPACITY, value = 0.0f, easing = EasingCurve.LINEAR),
        Keyframe(id = "k4", timestampMs = 1500L, property = KeyframeProperty.OPACITY, value = 1.0f, easing = EasingCurve.LINEAR)
    )

    private fun clip(scale: Float = 1.0f) = MediaClip(
        id = "clip",
        title = "Clip",
        scale = scale,
        opacity = 0.75f,
        keyframes = scaleKeyframes() + opacityKeyframes()
    )

    @Test
    fun `empty keyframes return the default value`() {
        val clip = MediaClip(id = "clip", title = "c", scale = 2.0f)
        assertEquals(2.0f, KeyframeInterpolator.getScale(clip, 500L), 0.0001f)
    }

    @Test
    fun `linear interpolation at the midpoint`() {
        val value = KeyframeInterpolator.interpolate(
            keyframes = scaleKeyframes(),
            property = KeyframeProperty.SCALE,
            timestampMs = 500L,
            defaultValue = 1.0f
        )
        assertEquals(2.0f, value, 0.0001f)
    }

    @Test
    fun `before the first keyframe clamps to the first value`() {
        val value = KeyframeInterpolator.interpolate(
            keyframes = scaleKeyframes(),
            property = KeyframeProperty.SCALE,
            timestampMs = -50L,
            defaultValue = 99f
        )
        assertEquals(1.0f, value, 0.0001f)
    }

    @Test
    fun `after the last keyframe holds the last value`() {
        val value = KeyframeInterpolator.interpolate(
            keyframes = scaleKeyframes(),
            property = KeyframeProperty.SCALE,
            timestampMs = 5000L,
            defaultValue = 42f
        )
        assertEquals(3.0f, value, 0.0001f)
    }

    @Test
    fun `other properties are ignored`() {
        val value = KeyframeInterpolator.interpolate(
            keyframes = opacityKeyframes(),
            property = KeyframeProperty.SCALE,
            timestampMs = 500L,
            defaultValue = 5.5f
        )
        assertEquals(5.5f, value, 0.0001f)
    }

    @Test
    fun `unsorted keyframes are evaluated correctly`() {
        val unsorted = opacityKeyframes().asReversed()
        val value = KeyframeInterpolator.interpolate(
            keyframes = unsorted,
            property = KeyframeProperty.OPACITY,
            timestampMs = 1000L,
            defaultValue = 0.5f
        )
        assertEquals(0.5f, value, 0.0001f)
    }

    @Test
    fun `ease in slows the start`() {
        val value = KeyframeInterpolator.interpolate(
            keyframes = listOf(
                Keyframe(id = "a", timestampMs = 0L, property = KeyframeProperty.SCALE, value = 0f, easing = EasingCurve.LINEAR),
                Keyframe(id = "b", timestampMs = 100L, property = KeyframeProperty.SCALE, value = 1f, easing = EasingCurve.EASE_IN)
            ),
            property = KeyframeProperty.SCALE,
            timestampMs = 50L,
            defaultValue = -1f
        )
        // progress 0.5 eased -> 0.25
        assertEquals(0.25f, value, 0.0001f)
    }

    @Test
    fun `eased progress stays in the value range`() {
        val keyframes = listOf(
            Keyframe(id = "a", timestampMs = 0L, property = KeyframeProperty.OPACITY, value = 0f, easing = EasingCurve.BOUNCE),
            Keyframe(id = "b", timestampMs = 1000L, property = KeyframeProperty.OPACITY, value = 1f, easing = EasingCurve.BOUNCE)
        )
        for (t in longArrayOf(0L, 100L, 250L, 400L, 500L, 600L, 800L, 1000L)) {
            val v = KeyframeInterpolator.interpolate(keyframes, KeyframeProperty.OPACITY, t, 0f)
            assertTrue("Bounce value at $t out of range: $v", v >= -0.0001f && v <= 1.0001f)
        }
    }

    @Test
    fun `per property helpers delegate to interpolate`() {
        val c = clip()
        assertEquals(2.0f, KeyframeInterpolator.getScale(c, 500L), 0.0001f)
        assertEquals(0.5f, KeyframeInterpolator.getOpacity(c, 1000L), 0.0001f)
    }
}
