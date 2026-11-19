package com.example.data.export

import androidx.media3.common.util.UnstableApi
import com.example.engine.ChromaKey
import com.example.model.ChromaKeyData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chroma key is burned into the clip's pixels by a GPU shader. The
 * shader has to agree with the Kotlin chroma maths, otherwise the preview
 * and the export would key different pixels.
 */
@UnstableApi
class ClipChromaKeyEffectTest {

    @Test
    fun `a disabled chroma key does nothing`() {
        val effect = ClipChromaKeyEffect(ChromaKeyData(isEnabled = false))
        assertTrue(effect.isNoOp(1080, 1920))
    }

    @Test
    fun `an enabled chroma key is not a no-op`() {
        val effect = ClipChromaKeyEffect(ChromaKeyData(isEnabled = true))
        assertFalse(effect.isNoOp(1080, 1920))
    }

    @Test
    fun `the model key colour maps onto the renderer spec`() {
        val spec = ClipChromaKeyEffect.specFor(
            ChromaKeyData(
                isEnabled = true,
                keyColorHex = 0xFF00FF00,
                tolerance = 0.3f,
                softness = 0.15f,
                spill = 0.4f
            )
        )

        assertEquals(0f, spec.keyR, 0.001f)
        assertEquals(1f, spec.keyG, 0.001f)
        assertEquals(0f, spec.keyB, 0.001f)
        assertEquals(0.3f, spec.tolerance, 0.001f)
        assertEquals(0.15f, spec.softness, 0.001f)
        assertEquals(0.4f, spec.spill, 0.001f)
    }

    @Test
    fun `the key colour itself is keyed out`() {
        val spec = ClipChromaKeyEffect.specFor(ChromaKeyData(isEnabled = true))
        assertEquals(0f, ChromaKey.alphaFor(spec.keyR, spec.keyG, spec.keyB, spec), 0.001f)
        assertEquals(1f, ChromaKey.alphaFor(1f, 0f, 0f, spec), 0.001f)
    }

    @Test
    fun `only a green dominant key can spill`() {
        assertEquals(
            1f,
            ChromaKeyShaderProgram.spillChannelOf(
                ClipChromaKeyEffect.specFor(ChromaKeyData(keyColorHex = 0xFF00FF00))
            ),
            0.001f
        )
        assertEquals(
            0f,
            ChromaKeyShaderProgram.spillChannelOf(
                ClipChromaKeyEffect.specFor(ChromaKeyData(keyColorHex = 0xFF0000FF))
            ),
            0.001f
        )
    }

    @Test
    fun `the shader uses the same maths as the Kotlin chroma key`() {
        val source = ChromaKeyShaderProgram.FRAGMENT_SOURCE
        // The YCbCr coefficients, the tolerance ramp and the spill rule are
        // the parts of the shader that can silently drift away from
        // ChromaKey.
        assertTrue(source.contains("0.168736"))
        assertTrue(source.contains("0.331264"))
        assertTrue(source.contains("0.418688"))
        assertTrue(source.contains("0.081312"))
        assertTrue(source.contains("smoothstep(inner, outer, distance)"))
        assertTrue(source.contains("mix(uBackground, color, alpha)"))
    }
}
