package com.example.data.export

import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-pixel grade effect: neutral clips must be a true no-op, retouch
 * raises the blur floor without touching the blur slider, and the shader
 * carries the exact uniforms the spec mirror produces.
 */
class ClipGradeShaderEffectTest {

    @Test
    fun `neutral clip is a no-op`() {
        val effect = ClipGradeShaderEffect(MediaClip(id = "c1", title = "Plain"))
        assertTrue(effect.isNoOp(1920, 1080))
        assertTrue(!effect.grade.needsShader)
    }

    @Test
    fun `each slider alone activates the pass`() {
        listOf(
            MediaClip(id = "c", title = "t", highlights = 0.3f),
            MediaClip(id = "c", title = "t", shadows = -0.5f),
            MediaClip(id = "c", title = "t", sharpen = 0.4f),
            MediaClip(id = "c", title = "t", grain = 0.2f),
            MediaClip(id = "c", title = "t", blurAmount = 0.5f),
            MediaClip(id = "c", title = "t", relight = 0.6f),
            MediaClip(id = "c", title = "t", retouch = true)
        ).forEach { clip ->
            assertFalse(
                "expected ${clip.highlights}/${clip.shadows}/${clip.sharpen}/${clip.grain}/" +
                    "${clip.blurAmount}/${clip.relight}/${clip.retouch} to need the shader",
                ClipGradeShaderEffect(clip).isNoOp(1920, 1080)
            )
        }
    }

    @Test
    fun `retouch raises the blur floor without changing the slider`() {
        val spec = specOf(MediaClip(id = "c", title = "t", retouch = true))
        assertEquals(RETOUCH_BLUR, spec.blur, 1e-6f)
        val withUserBlur = specOf(MediaClip(id = "c", title = "t", retouch = true, blurAmount = 0.6f))
        assertEquals(0.6f, withUserBlur.blur, 1e-6f)
    }

    @Test
    fun `values clamp into their ranges`() {
        val spec = specOf(MediaClip(id = "c", title = "t", highlights = 4f, sharpen = -2f))
        assertEquals(1f, spec.highlights, 1e-6f)
        assertEquals(0f, spec.sharpen, 1e-6f)
    }

    @Test
    fun `shader carries the uniforms the spec feeds`() {
        val source = com.example.data.export.GradeShaderProgram.FRAGMENT_SOURCE
        // String-parity on the uniform set: every spec field must have a
        // matching uniform declaration.
        listOf(
            "uHighlights", "uShadows", "uSharpen", "uGrain", "uBlur", "uRelight", "uTexel", "uTime"
        ).forEach { uniform ->
            assertTrue("missing uniform $uniform", source.contains(uniform))
        }
        // The luminance-band tone mapping must be present verbatim.
        assertTrue(source.contains("smoothstep(0.5, 1.0, l)"))
    }
}
