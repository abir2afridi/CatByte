package com.example.engine.gl

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import androidx.annotation.Keep
import com.example.engine.EffectShaders
import com.example.engine.GlslEffect
import com.example.engine.ShaderTemplate
import com.example.model.BlendMode
import com.example.model.MaskType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Main GL Compositor that executes the render graph on GPU.
 * Handles: video layers, blend modes, masks, chroma key, GLSL effects, transitions.
 * Single-pass where possible, multi-pass for complex effects.
 */
@Keep
class GLCompositor(
    private val width: Int,
    private val height: Int
) {

    companion object {
        private const val TAG = "GLCompositor"
    }

    // Shader programs
    private var baseProgram: GLShaderProgram? = null
    private var effectProgram: GLShaderProgram? = null
    private var blendProgram: GLShaderProgram? = null
    private var maskProgram: GLShaderProgram? = null
    private var chromaKeyProgram: GLShaderProgram? = null
    private var transitionProgram: GLShaderProgram? = null

    // Geometry
    private var quadVBO: Int = 0
    private var quadVAO: Int = 0

    // Framebuffers for ping-pong rendering
    private var pingFBO: GLFramebuffer? = null
    private var pongFBO: GLFramebuffer? = null
    private var usePing = true

    // Texture cache
    private val textureCache = GLTextureCache(maxMemoryBytes = 96 * 1024 * 1024)

    // Uniform locations (cached)
    private var uModelMatrix: Int = -1
    private var uViewMatrix: Int = -1
    private var uProjectionMatrix: Int = -1
    private var uTexMatrix: Int = -1
    private var uAlpha: Int = -1
    private var uTime: Int = -1
    private var uIntensity: Int = -1
    private var uParams: Int = -1

    // View/Projection matrices (orthographic for 2D)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    init {
        initGL()
    }

    private fun initGL() {
        // Create shader programs
        baseProgram = createBaseProgram()
        effectProgram = createEffectProgram()
        blendProgram = createBlendProgram()
        maskProgram = createMaskProgram()
        chromaKeyProgram = createChromaKeyProgram()
        transitionProgram = createTransitionProgram()

        // Create full-screen quad
        createQuad()

        // Setup matrices
        Matrix.setIdentityM(identityMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)

        // Create ping-pong framebuffers
        pingFBO = GLFramebufferFactory.forEffectPass(width, height)
        pongFBO = GLFramebufferFactory.forEffectPass(width, height)
    }

    private fun createBaseProgram(): GLShaderProgram? {
        val vertex = """
            #version 300 es
            precision mediump float;
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aTexCoord;
            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform mat4 uModel;
            uniform mat3 uTexMatrix;
            out vec2 vTexCoord;
            void main() {
                gl_Position = uProjection * uView * uModel * vec4(aPos, 0.0, 1.0);
                vec3 tc = uTexMatrix * vec3(aTexCoord, 1.0);
                vTexCoord = tc.xy / tc.z;
            }
        """.trimIndent()

        val fragment = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform float uAlpha;
            out vec4 fragColor;
            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                fragColor = vec4(color.rgb, color.a * uAlpha);
            }
        """.trimIndent()

        return GLShaderProgram.create(vertex, fragment)
    }

    private fun createEffectProgram(): GLShaderProgram? {
        val vertex = """
            #version 300 es
            precision mediump float;
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aTexCoord;
            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform mat4 uModel;
            out vec2 vTexCoord;
            void main() {
                gl_Position = uProjection * uView * uModel * vec4(aPos, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        // Use the shader template from EffectShaders
        val fragment = ShaderTemplate.HEADER + """
            uniform sampler2D uTex;
            uniform vec2 uTexel;
            uniform float uTime;
            uniform float uIntensity;
            uniform vec4 uParams;
            in vec2 vTexCoord;
            out vec4 fragColor;
            void main() {
                vec2 uv = vTexCoord;
                vec4 src = texture(uTex, uv);
                vec4 outColor = src;
                // Effect body injected here
                fragColor = mix(src, outColor, clamp(uIntensity, 0.0, 1.0));
            }
        """.trimIndent()

        return GLShaderProgram.create(vertex, fragment)
    }

    private fun createBlendProgram(): GLShaderProgram? {
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
            uniform sampler2D uBase;
            uniform sampler2D uOverlay;
            uniform float uOpacity;
            uniform int uBlendMode;
            out vec4 fragColor;

            vec3 blendNormal(vec3 base, vec3 overlay, float a) { return mix(base, overlay, a); }
            vec3 blendMultiply(vec3 base, vec3 overlay, float a) { return mix(base, base * overlay, a); }
            vec3 blendScreen(vec3 base, vec3 overlay, float a) { return mix(base, 1.0 - (1.0 - base) * (1.0 - overlay), a); }
            vec3 blendOverlay(vec3 base, vec3 overlay, float a) {
                vec3 result = vec3(0.0);
                result = mix(result, 2.0 * base * overlay, step(0.5, base));
                result = mix(result, 1.0 - 2.0 * (1.0 - base) * (1.0 - overlay), step(0.5, base));
                return mix(base, result, a);
            }
            vec3 blendAdd(vec3 base, vec3 overlay, float a) { return mix(base, min(base + overlay, 1.0), a); }

            void main() {
                vec4 base = texture(uBase, vTexCoord);
                vec4 overlay = texture(uOverlay, vTexCoord);
                vec3 result;
                switch(uBlendMode) {
                    case 0: result = blendNormal(base.rgb, overlay.rgb, overlay.a * uOpacity); break;
                    case 1: result = blendMultiply(base.rgb, overlay.rgb, overlay.a * uOpacity); break;
                    case 2: result = blendScreen(base.rgb, overlay.rgb, overlay.a * uOpacity); break;
                    case 3: result = blendOverlay(base.rgb, overlay.rgb, overlay.a * uOpacity); break;
                    case 4: result = blendAdd(base.rgb, overlay.rgb, overlay.a * uOpacity); break;
                    default: result = blendNormal(base.rgb, overlay.rgb, overlay.a * uOpacity);
                }
                fragColor = vec4(result, base.a);
            }
        """.trimIndent()

        return GLShaderProgram.create(vertex, fragment)
    }

    private fun createMaskProgram(): GLShaderProgram? {
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
            uniform sampler2D uMask;
            uniform int uMaskType;
            uniform vec4 uMaskParams; // x=posX, y=posY, z=width, w=height
            uniform float uFeather;
            uniform bool uInverted;
            out vec4 fragColor;

            float maskRect(vec2 uv, vec4 params, float feather) {
                vec2 center = params.xy;
                vec2 size = params.zw * 0.5;
                vec2 d = abs(uv - center) - size;
                float edge = length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
                return 1.0 - smoothstep(-feather, feather, edge);
            }

            float maskCircle(vec2 uv, vec4 params, float feather) {
                vec2 center = params.xy;
                float radius = min(params.z, params.w) * 0.5;
                float d = length(uv - center) - radius;
                return 1.0 - smoothstep(-feather, feather, d);
            }

            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                float maskVal;
                if (uMaskType == 0) { // RECTANGLE
                    maskVal = maskRect(vTexCoord, uMaskParams, uFeather);
                } else if (uMaskType == 1) { // CIRCLE
                    maskVal = maskCircle(vTexCoord, uMaskParams, uFeather);
                } else {
                    maskVal = texture(uMask, vTexCoord).r;
                }
                if (uInverted) maskVal = 1.0 - maskVal;
                fragColor = vec4(color.rgb, color.a * maskVal);
            }
        """.trimIndent()

        return GLShaderProgram.create(vertex, fragment)
    }

    private fun createChromaKeyProgram(): GLShaderProgram? {
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
            uniform vec3 uKeyColor; // RGB in 0-1
            uniform float uSimilarity; // 0-1
            uniform float uBlend; // 0-1 spill suppression
            out vec4 fragColor;

            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                vec3 diff = abs(color.rgb - uKeyColor);
                float maxDiff = max(max(diff.r, diff.g), diff.b);
                float alpha = smoothstep(uSimilarity - 0.05, uSimilarity + 0.05, maxDiff);
                // Spill suppression
                vec3 spill = color.rgb * uBlend;
                vec3 result = mix(color.rgb, spill, alpha);
                fragColor = vec4(result, color.a * (1.0 - alpha));
            }
        """.trimIndent()

        return GLShaderProgram.create(vertex, fragment)
    }

    private fun createTransitionProgram(): GLShaderProgram? {
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
            uniform sampler2D uFrom;
            uniform sampler2D uTo;
            uniform float uProgress; // 0-1
            uniform int uType;
            uniform float uDuration;
            out vec4 fragColor;

            // Crossfade
            vec4 crossfade(vec4 from, vec4 to, float p) { return mix(from, to, p); }

            // Slide
            vec4 slide(vec4 from, vec4 to, float p, vec2 uv) {
                vec2 offset = vec2(p, 0.0);
                vec4 fromShifted = texture(uFrom, uv + offset);
                vec4 toShifted = texture(uTo, uv - (1.0 - p) * vec2(1.0, 0.0));
                return mix(fromShifted, toShifted, p);
            }

            // Wipe
            vec4 wipe(vec4 from, vec4 to, float p, vec2 uv) {
                float edge = uv.x - p;
                float a = smoothstep(-0.02, 0.02, edge);
                return mix(from, to, a);
            }

            // Zoom
            vec4 zoom(vec4 from, vec4 to, float p, vec2 uv) {
                vec2 center = vec2(0.5);
                vec2 fromUV = center + (uv - center) * (1.0 + p);
                vec2 toUV = center + (uv - center) * (2.0 - p);
                vec4 fromZ = texture(uFrom, fromUV);
                vec4 toZ = texture(uTo, toUV);
                return mix(fromZ, toZ, p);
            }

            // Glitch
            vec4 glitch(vec4 from, vec4 to, float p, vec2 uv) {
                float block = floor(uv.y * 20.0);
                float seed = fract(sin(block * 12.34 + p * 50.0) * 43758.5453);
                float shift = (seed - 0.5) * 0.1 * step(0.5, seed) * (1.0 - p);
                vec2 gUV = uv + vec2(shift, 0.0);
                vec4 fromG = texture(uFrom, gUV);
                vec4 toG = texture(uTo, gUV);
                return mix(fromG, toG, p);
            }

            void main() {
                vec4 from = texture(uFrom, vTexCoord);
                vec4 to = texture(uTo, vTexCoord);
                switch(uType) {
                    case 0: fragColor = crossfade(from, to, uProgress); break; // FADE
                    case 1: fragColor = slide(from, to, uProgress, vTexCoord); break; // SLIDE
                    case 2: fragColor = wipe(from, to, uProgress, vTexCoord); break; // WIPE
                    case 3: fragColor = zoom(from, to, uProgress, vTexCoord); break; // ZOOM
                    case 4: fragColor = glitch(from, to, uProgress, vTexCoord); break; // GLITCH
                    default: fragColor = crossfade(from, to, uProgress);
                }
            }
        """.trimIndent()

        return GLShaderProgram.create(vertex, fragment)
    }

    private fun createQuad() {
        // Full-screen quad: position + texcoord
        val vertices = floatArrayOf(
            // x, y, u, v
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )

        val vbo = IntArray(1)
        GLES30.glGenBuffers(1, vbo, 0)
        quadVBO = vbo[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, FloatBuffer.wrap(vertices), GLES30.GL_STATIC_DRAW)

        val vao = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        quadVAO = vao[0]
        GLES30.glBindVertexArray(quadVAO)
        GLES30.glEnableVertexAttribArray(0) // position
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 4 * 4, 0)
        GLES30.glEnableVertexAttribArray(1) // texcoord
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 4 * 4, 2 * 4)
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    /**
     * Renders a single layer with all effects, blend, mask, chroma key.
     * Returns the output texture ID (from ping-pong buffer).
     */
    fun renderLayer(
        layer: LayerInput,
        backgroundTexture: Int = 0
    ): Int {
        var currentTex = layer.textureId
        var currentFBO = getCurrentFBO()

        // 1. Apply chroma key if needed
        if (layer.chromaKey != null) {
            currentFBO = applyChromaKey(currentTex, layer.chromaKey!!)
            currentTex = currentFBO.getColorTexture()
        }

        // 2. Apply GLSL effects
        for (effect in layer.effects) {
            currentFBO = applyEffect(currentTex, effect)
            currentTex = currentFBO.getColorTexture()
        }

        // 3. Apply mask if needed
        if (layer.mask != null) {
            currentFBO = applyMask(currentTex, layer.mask!!)
            currentTex = currentFBO.getColorTexture()
        }

        // 4. Composite onto background with blend mode
        if (backgroundTexture != 0) {
            currentFBO = compositeBlend(backgroundTexture, currentTex, layer.blendMode, layer.opacity)
            currentTex = currentFBO.getColorTexture()
        } else if (layer.opacity < 1f) {
            // Just apply alpha
            currentFBO = applyAlpha(currentTex, layer.opacity)
            currentTex = currentFBO.getColorTexture()
        }

        swapPingPong()
        return currentTex
    }

    /**
     * Renders a transition between two textures.
     */
    fun renderTransition(
        fromTexture: Int,
        toTexture: Int,
        progress: Float,
        transitionType: Int
    ): Int {
        val fbo = getCurrentFBO()
        fbo.bind()

        transitionProgram?.apply {
            use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fromTexture)
            setUniform1i("uFrom", 0)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, toTexture)
            setUniform1i("uTo", 1)

            setUniform1f("uProgress", progress)
            setUniform1i("uType", transitionType)

            drawQuad()
        }

        fbo.unbind()
        swapPingPong()
        return fbo.getColorTexture()
    }

    private fun applyChromaKey(inputTex: Int, chroma: ChromaKeyParams): GLFramebuffer {
        val fbo = getCurrentFBO()
        fbo.bind()

        chromaKeyProgram?.apply {
            use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTex)
            setUniform1i("uTexture", 0)
            setUniform3f("uKeyColor", chroma.keyColorR, chroma.keyColorG, chroma.keyColorB)
            setUniform1f("uSimilarity", chroma.similarity)
            setUniform1f("uBlend", chroma.spillSuppression)

            drawQuad()
        }

        fbo.unbind()
        return fbo
    }

    private fun applyEffect(inputTex: Int, effect: GlslEffect): GLFramebuffer {
        val fbo = getCurrentFBO()
        fbo.bind()

        // Build dynamic fragment shader with effect body
        val fragmentSource = ShaderTemplate.wrap(effect.body)
        val vertexSource = """
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

        val tempProgram = GLShaderProgram.create(vertexSource, fragmentSource)
        tempProgram?.apply {
            use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTex)
            setUniform1i("uTex", 0)
            setUniform2f("uTexel", 1f / width, 1f / height)
            setUniform1f("uTime", effectTime)
            setUniform1f("uIntensity", effect.intensity)
            // Pack params into vec4
            val params = effect.params
            val p0 = params.getOrDefault("amount", params.getOrDefault("radius", params.getOrDefault("strength", params.getOrDefault("size", 0f)))) ?: 0f
            val p1 = params.getOrDefault("grain", 0f)
            val p2 = params.getOrDefault("frequency", 0f)
            val p3 = params.getOrDefault("softness", 0f)
            setUniform4f("uParams", p0, p1, p2, p3)

            drawQuad()
        }
        tempProgram?.release()

        fbo.unbind()
        return fbo
    }

    private fun applyMask(inputTex: Int, mask: MaskParams): GLFramebuffer {
        val fbo = getCurrentFBO()
        fbo.bind()

        maskProgram?.apply {
            use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTex)
            setUniform1i("uTexture", 0)

            // Generate mask texture if needed
            val maskTex = generateMaskTexture(mask)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex)
            setUniform1i("uMask", 1)

            setUniform1i("uMaskType", mask.type.ordinal)
            setUniform4f("uMaskParams", mask.posX, mask.posY, mask.width, mask.height)
            setUniform1f("uFeather", mask.feather)
            setUniform1i("uInverted", if (mask.inverted) 1 else 0)

            drawQuad()
        }

        fbo.unbind()
        return fbo
    }

    private fun compositeBlend(baseTex: Int, overlayTex: Int, blendMode: BlendMode, opacity: Float): GLFramebuffer {
        val fbo = getCurrentFBO()
        fbo.bind()

        blendProgram?.apply {
            use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, baseTex)
            setUniform1i("uBase", 0)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, overlayTex)
            setUniform1i("uOverlay", 1)

            setUniform1f("uOpacity", opacity)
            setUniform1i("uBlendMode", blendMode.ordinal)

            drawQuad()
        }

        fbo.unbind()
        return fbo
    }

    private fun applyAlpha(inputTex: Int, alpha: Float): GLFramebuffer {
        val fbo = getCurrentFBO()
        fbo.bind()

        baseProgram?.apply {
            use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTex)
            setUniform1i("uTexture", 0)
            setUniform1f("uAlpha", alpha)
            setUniformMatrix4fv("uProjection", projectionMatrix)
            setUniformMatrix4fv("uView", viewMatrix)
            setUniformMatrix4fv("uModel", identityMatrix)
            setUniformMatrix3fv("uTexMatrix", identityMatrix.copyOf(9))

            drawQuad()
        }

        fbo.unbind()
        return fbo
    }

    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVAO)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun getCurrentFBO(): GLFramebuffer = if (usePing) pingFBO!! else pongFBO!!

    private fun swapPingPong() {
        usePing = !usePing
    }

    private var effectTime = 0f
    fun updateTime(deltaMs: Float) {
        effectTime += deltaMs / 1000f
    }

    fun getTextureCache(): GLTextureCache = textureCache

    fun resize(newWidth: Int, newHeight: Int) {
        // Recreate framebuffers and update projection
        pingFBO?.release()
        pongFBO?.release()
        pingFBO = GLFramebufferFactory.forEffectPass(newWidth, newHeight)
        pongFBO = GLFramebufferFactory.forEffectPass(newWidth, newHeight)
        Matrix.orthoM(projectionMatrix, 0, 0f, newWidth.toFloat(), newHeight.toFloat(), 0f, -1f, 1f)
    }

    fun release() {
        baseProgram?.release()
        effectProgram?.release()
        blendProgram?.release()
        maskProgram?.release()
        chromaKeyProgram?.release()
        transitionProgram?.release()
        textureCache.clear()
        pingFBO?.release()
        pongFBO?.release()
        if (quadVBO != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVBO), 0)
        if (quadVAO != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVAO), 0)
    }

    private fun generateMaskTexture(mask: MaskParams): Int {
        val size = 256
        val pixels = ByteArray(size * size * 4)
        val centerX = (mask.posX * size).toInt()
        val centerY = (mask.posY * size).toInt()
        val radiusX = (mask.width * size * 0.5).toInt()
        val radiusY = (mask.height * size * 0.5).toInt()
        val featherPx = (mask.feather * size * 0.5).toInt()

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - centerX
                val dy = y - centerY
                var alpha: Float

                if (mask.type == MaskType.CIRCLE) {
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val radius = (radiusX + radiusY) / 2f
                    alpha = if (dist <= radius - featherPx) 1f else if (dist >= radius + featherPx) 0f else 1f - (dist - radius + featherPx) / (2 * featherPx)
                } else {
                    val distX = kotlin.math.abs(dx).toFloat() - radiusX
                    val distY = kotlin.math.abs(dy).toFloat() - radiusY
                    val dist = kotlin.math.max(distX, distY)
                    alpha = if (dist <= -featherPx) 1f else if (dist >= featherPx) 0f else 1f - (dist + featherPx) / (2 * featherPx)
                }

                if (mask.inverted) alpha = 1f - alpha

                val idx = (y * size + x) * 4
                pixels[idx] = 0
                pixels[idx + 1] = 0
                pixels[idx + 2] = 0
                pixels[idx + 3] = (alpha * 255).toInt().toByte()
            }
        }

        return textureCache.createFromPixels("mask_${mask.type}_${mask.posX}_${mask.posY}", size, size, pixels)
    }

    // Data classes for layer input
    data class LayerInput(
        val textureId: Int,
        val blendMode: BlendMode = BlendMode.NORMAL,
        val opacity: Float = 1f,
        val effects: List<GlslEffect> = emptyList(),
        val mask: MaskParams? = null,
        val chromaKey: ChromaKeyParams? = null
    )

    data class MaskParams(
        val type: MaskType,
        val posX: Float,
        val posY: Float,
        val width: Float,
        val height: Float,
        val rotation: Float = 0f,
        val feather: Float = 0f,
        val inverted: Boolean = false
    )

    data class ChromaKeyParams(
        val keyColorR: Float,
        val keyColorG: Float,
        val keyColorB: Float,
        val similarity: Float,
        val spillSuppression: Float
    )
}