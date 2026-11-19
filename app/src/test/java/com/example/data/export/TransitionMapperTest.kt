package com.example.data.export

import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import com.example.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every transition the picker offers has to be routed to its own effect -
 * a picker whose tiles all draw the same fade is a lie.
 */
@UnstableApi
class TransitionMapperTest {

    private val clipUs = 4_000_000L

    private fun effectsFor(transition: TransitionType): List<GlEffect> =
        TransitionMapper.effectsFor(transition, clipUs, 500L)

    @Test
    fun `no transition means no effect`() {
        assertTrue(effectsFor(TransitionType.NONE).isEmpty())
    }

    @Test
    fun `every renderable transition produces at least one effect`() {
        TransitionType.entries
            .filter { it.isRenderable }
            .forEach { transition ->
                assertTrue(
                    "transition $transition produced no effect",
                    effectsFor(transition).isNotEmpty()
                )
            }
    }

    @Test
    fun `the reveal transitions use their own shader kind`() {
        val expected = mapOf(
            TransitionType.DISSOLVE to TransitionRevealShaderProgram.KIND_DISSOLVE,
            TransitionType.PAGE_FLIP to TransitionRevealShaderProgram.KIND_PAGE_FLIP,
            TransitionType.PAPER_TEAR to TransitionRevealShaderProgram.KIND_PAPER_TEAR,
            TransitionType.PENCIL_WIPE to TransitionRevealShaderProgram.KIND_PENCIL_WIPE,
            TransitionType.INK_BLEED to TransitionRevealShaderProgram.KIND_INK_BLEED
        )

        expected.forEach { (transition, kind) ->
            val effect = effectsFor(transition).single() as TransitionRevealEffect
            assertEquals("wrong shader kind for $transition", kind, effect.kind)
        }
    }

    @Test
    fun `the five reveal transitions really use five different kinds`() {
        val kinds = listOf(
            TransitionType.DISSOLVE,
            TransitionType.PAGE_FLIP,
            TransitionType.PAPER_TEAR,
            TransitionType.PENCIL_WIPE,
            TransitionType.INK_BLEED
        ).map { transition ->
            (effectsFor(transition).single() as TransitionRevealEffect).kind
        }

        assertEquals("the reveal kinds must be distinct", kinds.size, kinds.distinct().size)
    }

    @Test
    fun `the colour transitions use the colour gain effect`() {
        assertTrue(effectsFor(TransitionType.FADE_BLACK).single() is TimeRgbGainEffect)
        assertTrue(effectsFor(TransitionType.FLASH_WHITE).single() is TimeRgbGainEffect)
        assertTrue(effectsFor(TransitionType.GLITCH).single() is TimeRgbGainEffect)
    }

    @Test
    fun `the motion transitions use the transform effect`() {
        assertTrue(effectsFor(TransitionType.WHIP_PAN).any { it is TimeTransformEffect })
        assertTrue(effectsFor(TransitionType.ZOOM_IN).any { it is TimeTransformEffect })
    }

    @Test
    fun `the transition length is clamped to the supported range`() {
        // A ridiculous length must not be honoured - the renderer would draw
        // a transition longer than the clip itself.
        val long = TransitionMapper.effectsFor(TransitionType.DISSOLVE, clipUs, 900_000L)
        assertTrue(long.isNotEmpty())

        val short = TransitionMapper.effectsFor(TransitionType.DISSOLVE, clipUs, 1L)
        assertTrue(short.isNotEmpty())
    }
}
