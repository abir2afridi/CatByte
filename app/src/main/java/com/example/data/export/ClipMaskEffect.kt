package com.example.data.export

import android.opengl.GLES30
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.example.engine.MaskKind
import com.example.engine.MaskMath
import com.example.engine.MaskSpec
import com.example.model.MaskData
import com.example.model.MaskType

/**
 * Burns a clip mask into the pixels, for both the CompositionPlayer preview
 * and the exported file.
 *
 * The fragment shader reproduces [MaskMath.coverage] exactly - same shapes,
 * same feather smoothstep, same invert - so what the user drags in the
 * preview is what lands in the MP4.
 *
 * [backgroundColor] decides what the masked-out area shows:
 *
 *  - `null` keeps the pixels transparent, so an overlay clip reveals the
 *    video underneath it;
 *  - a colour fills the masked-out area with the canvas background, which is
 *    what a main-track clip needs - nothing is composited below it.
 */
@UnstableApi
class ClipMaskEffect(
    private val mask: MaskData,
    private val backgroundColor: Int? = null
) : GlEffect {

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        mask.type == MaskType.NONE

    override fun toGlShaderProgram(context: android.content.Context, useHdr: Boolean): GlShaderProgram =
        MaskShaderProgram(useHdr, mask, backgroundColor)
}

@UnstableApi
internal class MaskShaderProgram(
    useHdr: Boolean,
    mask: MaskData,
    backgroundColor: Int? = null
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {

    private val program: GlQuadProgram = try {
        GlQuadProgram(GlQuadProgram.VERTEX_SOURCE, FRAGMENT_SOURCE).apply {
            use()
            setInt("uKind", kindOf(mask))
            setVec2("uCenter", mask.posX, mask.posY)
            setVec2("uSize", mask.width.coerceAtLeast(0.001f), mask.height.coerceAtLeast(0.001f))
            setFloat("uRotationRad", Math.toRadians(mask.rotation.toDouble()).toFloat())
            setFloat("uFeather", mask.feather.coerceIn(0f, 1f))
            setFloat("uInvert", if (mask.isInverted) 1f else 0f)
            setFloat("uBands", FILMSTRIP_BANDS.toFloat())
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

    private fun kindOf(mask: MaskData): Int = when (mask.type) {
        MaskType.RECTANGLE -> KIND_RECTANGLE
        MaskType.CIRCLE -> KIND_CIRCLE
        MaskType.SPLIT -> KIND_SPLIT
        MaskType.MIRROR -> KIND_MIRROR
        MaskType.FILMSTRIP -> KIND_FILMSTRIP
        MaskType.NONE -> KIND_RECTANGLE
    }

    companion object {
        const val KIND_RECTANGLE = 1
        const val KIND_CIRCLE = 2
        const val KIND_SPLIT = 3
        const val KIND_MIRROR = 4
        const val KIND_FILMSTRIP = 5
        const val FILMSTRIP_BANDS = 6

        /**
         * The mask coverage of [MaskMath], expressed in ES3. Split uses the
         * horizontal centre exactly like [MaskMath.split] (value = uv.y).
         */
        val FRAGMENT_SOURCE = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform int uKind;
            uniform vec2 uCenter;
            uniform vec2 uSize;
            uniform float uRotationRad;
            uniform float uFeather;
            uniform float uInvert;
            uniform float uBands;
            uniform vec3 uBackground;
            uniform float uUseBackground;
            in vec2 vUv;
            out vec4 fragColor;

            float featherStep(float edge, float feather) {
              if (feather <= 0.0) return edge >= 0.0 ? 1.0 : 0.0;
              float t = clamp(edge / feather, 0.0, 1.0);
              return t * t * (3.0 - 2.0 * t);
            }

            void main() {
              vec2 uv = vUv;
              float raw;
              if (uKind == ${KIND_FILMSTRIP}) {
                int bands = int(max(uBands, 1.0));
                float band = floor(clamp(uv.y * float(bands), 0.0, float(bands) - 1.0));
                raw = mod(band, 2.0) < 1.0 ? 1.0 : 0.0;
              } else {
                vec2 d = uv - uCenter;
                vec2 lp = uRotationRad == 0.0
                    ? d
                    : vec2(d.x * cos(uRotationRad) + d.y * sin(uRotationRad),
                           -d.x * sin(uRotationRad) + d.y * cos(uRotationRad));
                if (uKind == ${KIND_RECTANGLE}) {
                  vec2 halfSize = uSize * 0.5;
                  float edge = min(halfSize.x - abs(lp.x), halfSize.y - abs(lp.y));
                  raw = featherStep(edge, uFeather);
                } else if (uKind == ${KIND_CIRCLE}) {
                  float radius = min(uSize.x, uSize.y) * 0.5;
                  raw = featherStep(radius - length(lp), uFeather);
                } else if (uKind == ${KIND_SPLIT}) {
                  raw = featherStep(uv.y - uCenter.x, uFeather);
                } else {
                  raw = featherStep(uSize.x * 0.5 - abs(lp.x), uFeather);
                }
              }
              float alpha = uInvert > 0.5 ? 1.0 - raw : raw;
              vec4 color = texture(uTexSampler, uv);
              if (uUseBackground > 0.5) {
                // A main-track clip has nothing behind it: the masked-out
                // area shows the canvas background instead of a hole.
                fragColor = vec4(mix(uBackground, color.rgb, alpha), 1.0);
              } else {
                fragColor = vec4(color.rgb * alpha, color.a * alpha);
              }
            }
            """.trimIndent()
    }
}

/** JVM-side mirror used by tests to assert shader/uniform parity with Kotlin. */
internal fun maskSpecOf(mask: MaskData): MaskSpec = MaskSpec(
    kind = when (mask.type) {
        MaskType.RECTANGLE -> MaskKind.RECTANGLE
        MaskType.CIRCLE -> MaskKind.CIRCLE
        MaskType.SPLIT -> MaskKind.SPLIT
        MaskType.MIRROR -> MaskKind.MIRROR
        MaskType.FILMSTRIP -> MaskKind.FILMSTRIP
        MaskType.NONE -> MaskKind.RECTANGLE
    },
    centerX = mask.posX,
    centerY = mask.posY,
    width = mask.width,
    height = mask.height,
    rotationDeg = mask.rotation,
    feather = mask.feather,
    invert = mask.isInverted,
    bands = MaskShaderProgram.FILMSTRIP_BANDS
)
