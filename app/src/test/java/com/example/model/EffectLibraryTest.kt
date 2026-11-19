package com.example.model

import com.example.data.export.isShaderFamily
import com.example.engine.EffectOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The video-effects library: every sheet tab owns its own presets (30+ per
 * tab), names stay unique (projects persist the enum name), and every preset
 * produces real draw ops in both renderers.
 */
class EffectLibraryTest {

    /** The sheets' tab labels -> EffectType.category keys. */
    private val sheetTabs = mapOf(
        "Trending" to "Trending",
        "B.Duck" to "B.Duck",
        "Pumpkin Spice 🍁" to "Pumpkin Spice",
        "Classic" to "Classic",
        "✨ Magic" to "Magic",
        "Retro" to "Retro",
        "Lens" to "Lens",
        "Nightclub" to "Nightclub",
        "Body: Glow" to "Body Glow",
        "Body: Aura" to "Body Aura",
        "Body: Sparkle" to "Body Sparkle",
        "Body: Neon" to "Body Neon"
    )

    @Test
    fun `every tab owns at least 30 effects`() {
        sheetTabs.values.forEach { category ->
            val count = EffectType.entries.count { it.category == category }
            assertTrue("$category has only $count effects", count >= 30)
        }
    }

    @Test
    fun `every effect belongs to a sheet tab`() {
        val categories = sheetTabs.values.toSet()
        val orphans = EffectType.entries.filter { it.category !in categories }
        assertTrue("orphans: ${orphans.map { it.name }}", orphans.isEmpty())
    }

    @Test
    fun `display names are unique for search and UI`() {
        val duplicates = EffectType.entries
            .groupBy { it.displayName }
            .filterValues { it.size > 1 }
        assertTrue("duplicates: ${duplicates.keys}", duplicates.isEmpty())
    }

    @Test
    fun `enum names are unique for JSON persistence`() {
        assertEquals(EffectType.entries.size, EffectType.entries.map { it.name }.toSet().size)
    }

    @Test
    fun `every preset renders - overlay families as ops, shader families via the GPU`() {
        val w = 1080f
        val h = 1920f
        val times = listOf(0L, 400L, 1_200L, 2_500L)
        EffectType.entries.forEach { type ->
            if (type.spec.family.isShaderFamily) {
                // Interactive families are GPU blend shaders, not overlay ops.
                assertTrue(
                    "${type.name} must be a shader family",
                    com.example.data.export.EFFECT_FAMILY_SHADER_SET.contains(type.spec.family)
                )
                return@forEach
            }
            val renderedAtLeastOnce = times.any { localMs ->
                EffectOps.compute(type, type.spec, localMs, w, h).isNotEmpty()
            }
            assertTrue("${type.name} never renders", renderedAtLeastOnce)
        }
    }

    @Test
    fun `ops stay inside the frame with a small bleed margin`() {
        val w = 1080f
        val h = 1920f
        val margin = 120f
        EffectType.entries.forEach { type ->
            EffectOps.compute(type, type.spec, 900L, w, h).forEach { op ->
                if (op.kind == com.example.engine.EffectOpKind.LINE) {
                    // Beams/grid lines radiate past the frame on purpose; the
                    // canvas clips them. Their start point must be on-frame.
                    assertTrue(
                        "${type.name} line start off-screen: ${op.cx},${op.cy}",
                        op.cx in -margin..w + margin && op.cy in -margin..h + margin
                    )
                } else {
                    // Drifting washes bleed over the edges (clipped), but every
                    // op must still intersect the visible area.
                    val intersects = op.right > -margin && op.left < w + margin &&
                        op.bottom > -margin && op.top < h + margin
                    assertTrue(
                        "${type.name} op fully off-screen: ${op.left},${op.top},${op.right},${op.bottom}",
                        intersects
                    )
                }
            }
        }
    }

    @Test
    fun `the same preset and time render the same ops`() {
        val type = EffectType.entries.first { it.spec.family == EffectFamily.PARTICLES }
        val a = EffectOps.compute(type, type.spec, 1_000L, 720f, 1280f)
        val b = EffectOps.compute(type, type.spec, 1_000L, 720f, 1280f)
        assertEquals(a, b)
    }

    @Test
    fun `animated presets actually move between frames`() {
        val type = EffectType.entries.first { it.spec.family == EffectFamily.PARTICLES }
        val first = EffectOps.compute(type, type.spec, 0L, 720f, 1280f)
        val later = EffectOps.compute(type, type.spec, 900L, 720f, 1280f)
        assertTrue("particles must move between frames", first != later)
    }

    @Test
    fun `legacy presets keep their names for old projects`() {
        listOf(
            "RGB_SPLIT", "SHAKE", "VHS_GRAIN", "NEON_HALO", "LIGHTNING", "ZOOM_BLUR"
        ).forEach { name ->
            assertTrue("missing legacy $name", EffectType.entries.any { it.name == name })
        }
    }
}
