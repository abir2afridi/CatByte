package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LutCubeTest {

    private fun identityCube(size: Int = 2): String {
        val builder = StringBuilder("TITLE \"identity\"\nLUT_3D_SIZE $size\n")
        val maxIndex = (size - 1).toFloat()
        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    builder.append("${r / maxIndex} ${g / maxIndex} ${b / maxIndex}\n")
                }
            }
        }
        return builder.toString()
    }

    @Test
    fun `identity cube parses and samples exactly`() {
        val lut = LutCubeParser.parse(identityCube(2))
        assertNotNull(lut)
        assertEquals(2, lut!!.size)

        val black = lut.sample(0f, 0f, 0f)
        assertEquals(0f, black[0], 0.001f)
        assertEquals(0f, black[2], 0.001f)

        val white = lut.sample(1f, 1f, 1f)
        assertEquals(1f, white[0], 0.001f)
        assertEquals(1f, white[2], 0.001f)

        val mid = lut.sample(0.5f, 0.5f, 0.5f)
        assertEquals(0.5f, mid[0], 0.001f)
        assertEquals(0.5f, mid[1], 0.001f)
        assertEquals(0.5f, mid[2], 0.001f)
    }

    @Test
    fun `comments and blank lines are ignored`() {
        val text = "# a comment\n\n" + identityCube(2) + "\n# trailing\n"
        assertNotNull(LutCubeParser.parse(text))
    }

    @Test
    fun `malformed cube returns null`() {
        assertNull(LutCubeParser.parse("LUT_3D_SIZE 2\n0 0 0\n"))
        assertNull(LutCubeParser.parse("nothing here"))
        assertNull(LutCubeParser.parse(""))
    }

    @Test
    fun `intensity zero returns the source colour`() {
        val lut = Lut3D.identity(2)
        val result = LutApply.apply(lut, 0.2f, 0.4f, 0.6f, 0f)
        assertEquals(0.2f, result[0], 0.001f)
        assertEquals(0.4f, result[1], 0.001f)
        assertEquals(0.6f, result[2], 0.001f)
    }

    @Test
    fun `half intensity moves half way`() {
        val inverted = Lut3D(2, FloatArray(24) { index -> if (index % 3 == 0) 1f else 0f })
        val result = LutApply.apply(inverted, 0f, 0f, 0f, 0.5f)
        assertEquals(0.5f, result[0], 0.001f)
    }

    @Test
    fun `baking an identity chain is still an identity`() {
        val baked = LutApply.bake(listOf(Lut3D.identity(2)), intensity = 1f, size = 4)
        val sampled = baked.sample(0.5f, 0.25f, 0.75f)
        assertEquals(0.5f, sampled[0], 0.02f)
        assertEquals(0.25f, sampled[1], 0.02f)
        assertEquals(0.75f, sampled[2], 0.02f)
    }

    @Test
    fun `texture budget guards against absurd luts`() {
        assertTrue(LutApply.isAcceptableSize(Lut3D.identity(33)))
        assertTrue(!LutApply.isAcceptableSize(Lut3D.identity(65)))
        assertTrue(LutApply.estimatedTextureBytes(Lut3D.identity(33)) > 0L)
    }

    @Test
    fun `shader library covers the promised effect count`() {
        assertTrue("expected at least 30 effects", EffectShaders.ALL.size >= 30)
        assertEquals(EffectShaders.ALL.size, EffectShaders.ALL.map { it.id }.distinct().size)
        EffectShaders.ALL.forEach { effect ->
            assertTrue(effect.displayName.isNotBlank())
            assertTrue(effect.category.isNotBlank())
            assertTrue(effect.body.isNotBlank())
            val source = effect.fragmentSource
            assertTrue("${effect.id} version", source.contains("#version 300 es"))
            assertTrue("${effect.id} intensity", source.contains("uIntensity"))
            assertTrue("${effect.id} output", source.contains("fragColor"))
        }
        assertTrue(EffectShaders.categories().size >= 5)
        assertNotNull(EffectShaders.find("rgb_split"))
        assertNull(EffectShaders.find("does_not_exist"))
    }

    @Test
    fun `legacy effect enum still resolves to a shader`() {
        assertNotNull(EffectShaders.forLegacyEffect("RGB_SPLIT"))
        assertNotNull(EffectShaders.forLegacyEffect("SHAKE"))
        assertNull(EffectShaders.forLegacyEffect("UNKNOWN"))
        assertTrue(EffectShaders.maskPreviewBody("CIRCLE", 0.1f).contains("outColor.a"))
    }
}
