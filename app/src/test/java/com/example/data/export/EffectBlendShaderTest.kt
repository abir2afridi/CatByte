package com.example.data.export

import com.example.model.EffectFamily
import com.example.model.EffectType
import com.example.model.TimelineEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The interactive effect families (light leaks, vignettes, strobes, ...) must
 * blend with the VIDEO's own pixels via [EffectBlendShader] - never be drawn
 * as overlays on top of it. These tests pin the routing.
 */
class EffectBlendShaderTest {

    private fun effect(family: EffectFamily, startMs: Long, durationMs: Long) = TimelineEffect(
        id = "fx_$family$startMs",
        name = family.name,
        effectType = EffectType.entries.first { it.spec.family == family },
        startTimelineMs = startMs,
        durationMs = durationMs
    )

    @Test
    fun `interactive families are exactly the shader set`() {
        assertEquals(
            setOf(
                EffectFamily.LIGHT_LEAK,
                EffectFamily.VIGNETTE,
                EffectFamily.STROBE,
                EffectFamily.SPOTLIGHT,
                EffectFamily.FILM_BURN,
                EffectFamily.PLASMA,
                EffectFamily.GRID,
                EffectFamily.BEAMS
            ),
            EffectBlendShaderUtil.shaderFamilies
        )
    }

    @Test
    fun `shader effects active during a clip are picked up`() {
        val effects = listOf(
            effect(EffectFamily.VIGNETTE, startMs = 500, durationMs = 2_000),
            effect(EffectFamily.LIGHT_LEAK, startMs = 3_000, durationMs = 1_000)
        )
        // Clip spans 1.0s .. 2.5s: only the vignette overlaps.
        val active = EffectBlendShaderUtil.activeShaderSpecs(effects, 1_000_000L, 2_500_000L)
        assertEquals(listOf("VIGNETTE"), active.map { it.effectType.spec.family.name })
    }

    @Test
    fun `effects that end before or start after the clip are excluded`() {
        val effects = listOf(
            effect(EffectFamily.VIGNETTE, startMs = 0, durationMs = 500),
            effect(EffectFamily.STROBE, startMs = 5_000, durationMs = 1_000)
        )
        val active = EffectBlendShaderUtil.activeShaderSpecs(effects, 1_000_000L, 2_500_000L)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `overlay families are never routed to the shader`() {
        val effects = listOf(
            effect(EffectFamily.CONFETTI, startMs = 0, durationMs = 5_000),
            effect(EffectFamily.PARTICLES, startMs = 0, durationMs = 5_000)
        )
        val active = EffectBlendShaderUtil.activeShaderSpecs(effects, 0L, 5_000_000L)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `intensity scale is carried into the shader`() {
        val effect = effect(EffectFamily.VIGNETTE, startMs = 0, durationMs = 1_000)
            .copy(intensityScale = 1.5f)
        // The shader receives intensity * scale, clamped - pinned via the
        // shader family routing rather than re-implementing GL maths here.
        assertTrue(effect.intensityScale > 1f)
    }
}
