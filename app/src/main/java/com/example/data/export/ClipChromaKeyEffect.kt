package com.example.data.export

import android.content.Context
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.example.engine.ChromaSpec
import com.example.model.ChromaKeyData

/**
 * Burns the chroma key (green screen removal) into the clip's pixels.
 *
 * The shader mirrors [com.example.engine.ChromaKeyMath] exactly: the same
 * YCbCr chroma distance, the same tolerance/softness ramp and the same spill
 * suppression, so the preview and the exported file key the same pixels.
 *
 * [backgroundColor] decides what the keyed-out area shows:
 *
 *  - `null` keeps the pixels transparent, so an overlay clip reveals the
 *    video underneath it;
 *  - a colour fills the keyed-out area with the canvas background, which is
 *    what a main-track clip needs - nothing is composited below it.
 */
@UnstableApi
class ClipChromaKeyEffect(
    private val chroma: ChromaKeyData,
    private val backgroundColor: Int? = null
) : GlEffect {

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = !chroma.isEnabled

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        ChromaKeyShaderProgram(useHdr, chroma, backgroundColor)

    companion object {
        /**
         * The shader parameters of a chroma key setting.
         *
         * The key colour is split into the same chroma plane the shader
         * uses, so the model value and the GPU value cannot drift apart.
         */
        fun specFor(chroma: ChromaKeyData): ChromaSpec {
            val hex = chroma.keyColorHex
            return ChromaSpec(
                keyR = ((hex shr 16) and 0xFF) / 255f,
                keyG = ((hex shr 8) and 0xFF) / 255f,
                keyB = (hex and 0xFF) / 255f,
                tolerance = chroma.tolerance.coerceIn(0f, 1f),
                softness = chroma.softness.coerceIn(0f, 1f),
                spill = chroma.spill.coerceIn(0f, 1f)
            )
        }
    }
}

@UnstableApi
internal class ChromaKeyShaderProgram(
    useHdr: Boolean,
    chroma: ChromaKeyData,
    backgroundColor: Int? = null
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {

    private val spec: ChromaSpec = ClipChromaKeyEffect.specFor(chroma)

    private val program: GlQuadProgram = try {
        GlQuadProgram(GlQuadProgram.VERTEX_SOURCE, FRAGMENT_SOURCE).apply {
            use()
            setVec3("uKeyColor", spec.keyR, spec.keyG, spec.keyB)
            setFloat("uTolerance", spec.tolerance)
            setFloat("uSoftness", spec.softness)
            setFloat("uSpill", spec.spill)
            // Only the dominant key channel can spill; a green key spills
            // into the green channel, a blue key into the blue one.
            setFloat("uSpillChannel", spillChannelOf(spec))
            val background = backgroundColor
            if (background != null) {
                setVec3(
                    "uBackground",
                    ((background shr 16) and 0xFF) / 255f,
                    ((background shr 8) and 0xFF) / 255f,
                    (background and 0xFF) / 255f
                )
                setFloat("uUseBackground", 1f)
            } else {
                setVec3("uBackground", 0f, 0f, 0f)
                setFloat("uUseBackground", 0f)
            }
            setSampler("uTexSampler", /* texId= */ 0, /* unit= */ 0)
        }
    } catch (error: IllegalStateException) {
        throw VideoFrameProcessingException(error)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size =
        Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            program.use()
            program.setSampler("uTexSampler", inputTexId, /* unit= */ 0)
            program.draw()
        } catch (error: IllegalStateException) {
            throw VideoFrameProcessingException(error, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        program.release()
    }

    companion object {

        /**
         * 1 when the key colour is green dominant (the only channel
         * [ChromaKeyMath.suppressSpill] can pull back), 0 otherwise.
         */
        fun spillChannelOf(spec: ChromaSpec): Float =
            if (spec.keyG < spec.keyR || spec.keyG < spec.keyB) 0f else 1f

        /**
         * The chroma key shader.
         *
         * The YCbCr coefficients, the tolerance ramp and the spill rule are
         * the ES3 twins of [com.example.engine.ChromaKeyMath].
         */
        val FRAGMENT_SOURCE = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform vec3 uKeyColor;
            uniform float uTolerance;
            uniform float uSoftness;
            uniform float uSpill;
            uniform float uSpillChannel;
            uniform vec3 uBackground;
            uniform float uUseBackground;
            in vec2 vUv;
            out vec4 fragColor;

            vec2 chromaOf(vec3 c) {
              float cb = 0.5 - 0.168736 * c.r - 0.331264 * c.g + 0.5 * c.b;
              float cr = 0.5 + 0.5 * c.r - 0.418688 * c.g - 0.081312 * c.b;
              return vec2(cb, cr);
            }

            void main() {
              vec4 source = texture(uTexSampler, vUv);
              vec3 color = source.rgb;

              float distance = length(chromaOf(color) - chromaOf(uKeyColor));
              float inner = uTolerance;
              float outer = min(inner + uSoftness, 1.5);
              float alpha = outer <= inner
                  ? (distance <= inner ? 0.0 : 1.0)
                  : smoothstep(inner, outer, distance);

              // Spill suppression: pull the key colour out of the pixels
              // that survive the key. Only the dominant key channel can
              // spill, so a green key is the one that is cleaned up.
              if (uSpillChannel > 0.5) {
                float limit = max(color.r, color.b);
                color.g = clamp(limit + (color.g - limit) * uSpill, 0.0, 1.0);
              }

              if (uUseBackground > 0.5) {
                fragColor = vec4(mix(uBackground, color, alpha), 1.0);
              } else {
                fragColor = vec4(color * alpha, source.a * alpha);
              }
            }
            """.trimIndent()
    }
}
