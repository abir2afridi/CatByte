package com.example.data.export

import androidx.media3.common.util.UnstableApi
import com.example.engine.MaskKind
import com.example.engine.MaskMath
import com.example.model.MaskData
import com.example.model.MaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mask is burned into the clip's pixels by a GPU shader. The shader has
 * to agree with the Kotlin mask maths, otherwise the preview and the export
 * would show different masks - and the export would silently differ from
 * the preview.
 */
@UnstableApi
class ClipMaskEffectTest {

    @Test
    fun `a clip without a mask does nothing`() {
        val effect = ClipMaskEffect(MaskData(type = MaskType.NONE))
        assertTrue(effect.isNoOp(1080, 1920))
    }

    @Test
    fun `a real mask is not a no-op`() {
        val effect = ClipMaskEffect(MaskData(type = MaskType.CIRCLE))
        assertFalse(effect.isNoOp(1080, 1920))
    }

    @Test
    fun `the model mask maps onto the renderer mask`() {
        val mask = MaskData(
            type = MaskType.RECTANGLE,
            posX = 0.4f,
            posY = 0.6f,
            width = 0.5f,
            height = 0.7f,
            rotation = 15f,
            feather = 0.1f,
            isInverted = true
        )

        val spec = maskSpecOf(mask)

        assertEquals(MaskKind.RECTANGLE, spec.kind)
        assertEquals(0.4f, spec.centerX, 0.0001f)
        assertEquals(0.6f, spec.centerY, 0.0001f)
        assertEquals(0.5f, spec.width, 0.0001f)
        assertEquals(0.7f, spec.height, 0.0001f)
        assertEquals(15f, spec.rotationDeg, 0.0001f)
        assertEquals(0.1f, spec.feather, 0.0001f)
        assertTrue(spec.invert)
    }

    @Test
    fun `every mask type has a renderer kind`() {
        assertEquals(MaskKind.CIRCLE, maskSpecOf(MaskData(type = MaskType.CIRCLE)).kind)
        assertEquals(MaskKind.SPLIT, maskSpecOf(MaskData(type = MaskType.SPLIT)).kind)
        assertEquals(MaskKind.MIRROR, maskSpecOf(MaskData(type = MaskType.MIRROR)).kind)
        assertEquals(MaskKind.FILMSTRIP, maskSpecOf(MaskData(type = MaskType.FILMSTRIP)).kind)
    }

    @Test
    fun `the masked area really hides the picture`() {
        val spec = maskSpecOf(MaskData(type = MaskType.RECTANGLE, width = 0.6f, height = 0.6f))
        assertEquals(1f, MaskMath.coverage(spec, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, MaskMath.coverage(spec, 0.95f, 0.95f), 0.001f)
    }

    @Test
    fun `the shader uses the same maths as the Kotlin mask`() {
        val source = MaskShaderProgram.FRAGMENT_SOURCE
        // The feather ramp, the inversion and the background blend are the
        // three parts of the shader that can silently drift away from the
        // Kotlin implementation.
        assertTrue(source.contains("uInvert > 0.5 ? 1.0 - raw : raw"))
        assertTrue(source.contains("t * t * (3.0 - 2.0 * t)"))
        assertTrue(source.contains("mix(uBackground, color.rgb, alpha)"))
    }
}
