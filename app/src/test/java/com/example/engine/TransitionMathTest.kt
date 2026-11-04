package com.example.engine

import com.example.model.EasingCurve
import com.example.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionMathTest {

    @Test
    fun `transition duration is clamped to half of the shorter neighbour`() {
        assertEquals(500_000L, TransitionMath.clampDurationUs(800_000L, 1_000_000L, 2_000_000L))
        assertEquals(2_000_000L, TransitionMath.clampDurationUs(2_000_000L, 4_000_000L, 4_000_000L))
        assertEquals(0L, TransitionMath.clampDurationUs(0L, 1_000_000L, 1_000_000L))
        assertEquals(0L, TransitionMath.clampDurationUs(400_000L, 0L, 1_000_000L))
    }

    @Test
    fun `progress is clamped`() {
        assertEquals(0f, TransitionMath.progress(-5L, 1_000L), 0.001f)
        assertEquals(0.5f, TransitionMath.progress(500L, 1_000L), 0.001f)
        assertEquals(1f, TransitionMath.progress(2_000L, 1_000L), 0.001f)
        assertEquals(1f, TransitionMath.progress(10L, 0L), 0.001f)
    }

    @Test
    fun `easing curves stay in range and are monotonic at the ends`() {
        assertEquals(0.25f, TransitionMath.ease(EasingCurve.LINEAR, 0.25f), 0.001f)
        assertEquals(0.5f, TransitionMath.ease(EasingCurve.EASE_IN_OUT, 0.5f), 0.001f)
        assertEquals(1f, TransitionMath.ease(EasingCurve.BOUNCE, 1f), 0.01f)
        assertEquals(0f, TransitionMath.ease(EasingCurve.EASE_IN, 0f), 0.001f)
        assertEquals(1f, TransitionMath.ease(EasingCurve.EASE_OUT, 1f), 0.001f)
    }

    @Test
    fun `cross dissolve alphas sum to one`() {
        val frame = TransitionMath.frame(TransitionKind.CROSS_DISSOLVE, 0.3f)
        assertEquals(0.7f, frame.outgoingAlpha, 0.001f)
        assertEquals(0.3f, frame.incomingAlpha, 0.001f)
        assertEquals(1f, frame.outgoingAlpha + frame.incomingAlpha, 0.001f)
    }

    @Test
    fun `dip to black reaches a full fill in the middle`() {
        val frame = TransitionMath.frame(TransitionKind.DIP_TO_BLACK, 0.5f)
        assertEquals(0f, frame.outgoingAlpha, 0.001f)
        assertEquals(0f, frame.incomingAlpha, 0.001f)
        assertEquals(1f, frame.fill, 0.001f)
    }

    @Test
    fun `push slides both layers`() {
        val frame = TransitionMath.frame(TransitionKind.PUSH_LEFT, 0.5f)
        assertEquals(-0.5f, frame.outgoingOffsetX, 0.001f)
        assertEquals(0.5f, frame.incomingOffsetX, 0.001f)
        assertEquals(1f, frame.incomingAlpha, 0.001f)
    }

    @Test
    fun `cut flips on the midpoint`() {
        assertEquals(1f, TransitionMath.frame(TransitionKind.CUT, 0.4f).outgoingAlpha, 0.001f)
        assertEquals(1f, TransitionMath.frame(TransitionKind.CUT, 0.6f).incomingAlpha, 0.001f)
    }

    @Test
    fun `legacy transitions map to renderer kinds`() {
        assertEquals(TransitionKind.DIP_TO_BLACK, TransitionMath.fromLegacy(TransitionType.FADE_BLACK))
        assertEquals(TransitionKind.PUSH_LEFT, TransitionMath.fromLegacy(TransitionType.WHIP_PAN))
        assertEquals(TransitionKind.CUT, TransitionMath.fromLegacy(TransitionType.NONE))
        assertEquals(TransitionKind.CROSS_DISSOLVE, TransitionMath.fromLegacy(TransitionType.DISSOLVE))
    }

    @Test
    fun `every transition has a shader`() {
        TransitionKind.entries.forEach { kind ->
            val shader = TransitionMath.shaderFor(kind)
            assertTrue("missing version for $kind", shader.contains("#version 300 es"))
            assertTrue("missing progress uniform for $kind", shader.contains("uProgress"))
            assertTrue("missing fill uniform for $kind", shader.contains("uFillColor"))
        }
    }

    @Test
    fun `two frame transitions are flagged for the compositor`() {
        assertTrue(TransitionCatalog.requiresTwoFrames(TransitionKind.CROSS_DISSOLVE))
        assertEquals(TransitionKind.entries.size, TransitionCatalog.ordered().size)
        assertTrue(TransitionCatalog.supportsFill(TransitionKind.FLASH_WHITE))
    }
}
