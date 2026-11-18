package com.example.engine

import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The slow zoom is a plain pair of scale keyframes, shared by preview and export. */
class KenBurnsTest {

    @Test
    fun `the move is a scale ramp from the clip scale to a slow push-in`() {
        val frames = KenBurns.keyframes(4_000L)

        assertEquals(2, frames.size)
        assertTrue(frames.all { it.property == KeyframeProperty.SCALE })
        assertEquals(0L, frames[0].timestampMs)
        assertEquals(4_000L, frames[1].timestampMs)
        assertEquals(KenBurns.START_SCALE, frames[0].value, 0.0001f)
        assertEquals(KenBurns.END_SCALE, frames[1].value, 0.0001f)
        assertTrue(KenBurns.END_SCALE > KenBurns.START_SCALE)
    }

    @Test
    fun `an applied move is recognised for its own duration`() {
        assertTrue(KenBurns.isApplied(KenBurns.keyframes(4_000L), 4_000L))
        assertFalse(KenBurns.isApplied(KenBurns.keyframes(4_000L), 5_000L))
        assertFalse(KenBurns.isApplied(emptyList(), 4_000L))
        assertFalse(KenBurns.isApplied(KenBurns.keyframes(4_000L) + KenBurns.keyframes(4_000L), 4_000L))
    }

    @Test
    fun `the move starts from the clip's own scale`() {
        val frames = KenBurns.keyframes(4_000L, startScale = 1.3f)

        assertEquals(1.3f, frames[0].value, 0.0001f)
        assertEquals(1.3f * 1.12f, frames[1].value, 0.0001f)
        assertTrue(KenBurns.isApplied(frames, 4_000L, startScale = 1.3f))
        assertFalse(KenBurns.isApplied(frames, 4_000L, startScale = 1f))
    }

    @Test
    fun `edited scale or position keyframes block the automatic move`() {
        val opacityOnly = Keyframe(
            timestampMs = 1_000L,
            property = KeyframeProperty.OPACITY,
            value = 0.5f
        )
        assertFalse(KenBurns.hasConflictingKeyframes(listOf(opacityOnly)))
        assertFalse(KenBurns.hasConflictingKeyframes(emptyList()))

        assertTrue(
            KenBurns.hasConflictingKeyframes(
                listOf(Keyframe(timestampMs = 1_000L, property = KeyframeProperty.SCALE, value = 1.4f))
            )
        )
        assertTrue(
            KenBurns.hasConflictingKeyframes(
                listOf(Keyframe(timestampMs = 1_000L, property = KeyframeProperty.POSITION_X, value = 0.3f))
            )
        )
    }
}
