package com.example.engine.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for GL Compositor infrastructure.
 * Uses Robolectric to provide Android context for GLES calls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GLCompositorTest {

    @Test
    fun testGLShaderProgramCreation() {
        val vertex = """
            #version 300 es
            precision mediump float;
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragment = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            uniform sampler2D uTexture;
            out vec4 fragColor;
            void main() {
                fragColor = texture(uTexture, vTexCoord);
            }
        """.trimIndent()

        val program = GLShaderProgram.create(vertex, fragment)
        assertNotNull("Shader program should compile and link", program)
        program?.release()
    }

    @Test
    fun testGLShaderProgramWithUniforms() {
        val vertex = """
            #version 300 es
            precision mediump float;
            layout(location = 0) in vec2 aPos;
            uniform mat4 uProjection;
            uniform float uTime;
            void main() {
                gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
            }
        """.trimIndent()

        val fragment = """
            #version 300 es
            precision mediump float;
            uniform float uIntensity;
            uniform vec4 uParams;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uIntensity, uParams.x, uParams.y, 1.0);
            }
        """.trimIndent()

        val program = GLShaderProgram.create(vertex, fragment)
        assertNotNull(program)
        program?.use()

        // Test uniform setting
        program?.setUniform1f("uIntensity", 0.5f)
        program?.setUniform4f("uParams", 1f, 2f, 3f, 4f)
        program?.setUniformMatrix4fv("uProjection", FloatArray(16) { if (it % 5 == 0) 1f else 0f })

        program?.release()
    }

    @Test
    fun testGLTextureCache() {
        val cache = GLTextureCache(maxMemoryBytes = 1024 * 1024) // 1MB

        // Test basic operations
        assertEquals(0, cache.getTextureCount())
        assertEquals(0L, cache.getMemoryUsage())

        // Cache should handle empty state
        assertEquals(0, cache.getId("nonexistent"))
        assertEquals(0, cache.getTextureCount())

        cache.clear()
    }

    @Test
    fun testGLFramebufferCreation() {
        val fbo = GLFramebufferFactory.forPreview(720, 1280)
        assertNotNull(fbo)
        assertEquals(720, fbo.width)
        assertEquals(1280, fbo.height)
        fbo.release()
    }

    @Test
    fun testGLFramebufferEffectPass() {
        val fbo = GLFramebufferFactory.forEffectPass(720, 1280)
        assertNotNull(fbo)
        assertEquals(2, fbo.colorAttachmentCount)
        fbo.release()
    }

    @Test
    fun testGLFramebufferMaskPass() {
        val fbo = GLFramebufferFactory.forMaskPass(720, 1280)
        assertNotNull(fbo)
        assertEquals(1, fbo.colorAttachmentCount)
        fbo.release()
    }

    @Test
    fun testEffectShadersLibrary() {
        val effects = com.example.engine.EffectShaders.ALL
        assertEquals(35, effects.size) // Should have 35 GLSL effects

        // Verify key effects exist
        val categories = effects.map { it.category }.toSet()
        assert(categories.contains("glitch"))
        assert(categories.contains("motion"))
        assert(categories.contains("retro"))
        assert(categories.contains("glow"))
        assert(categories.contains("stylize"))
        assert(categories.contains("detail"))

        // Verify each effect has valid shader body
        for (effect in effects) {
            assert(effect.body.isNotBlank())
            assert(effect.fragmentSource.contains("fragColor"))
        }
    }
}