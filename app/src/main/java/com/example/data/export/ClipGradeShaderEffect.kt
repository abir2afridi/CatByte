package com.example.data.export

import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import com.example.model.MediaClip

/**
 * The per-pixel part of the Adjust panel that a 4x4 colour matrix cannot
 * express: highlights/shadows tone mapping, unsharp-mask sharpen, animated
 * film grain, disc blur and the radial relight.
 *
 * Runs inside the shared composition, so the CompositionPlayer preview and
 * the Transformer export burn the exact same pixels. Neutral values are a
 * true no-op ([isNoOp]) so untouched clips never pay for the pass.
 */
@UnstableApi
class ClipGradeShaderEffect(private val clip: MediaClip) : GlEffect {

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = !grade.needsShader

    /** The shader inputs derived from the clip (JVM-testable mirror below). */
    val grade: GradeShaderSpec = specOf(clip)

    override fun toGlShaderProgram(
        context: android.content.Context,
        useHdr: Boolean
    ): androidx.media3.effect.GlShaderProgram = GradeShaderProgram(useHdr, grade)
}

/** Pure data the shader consumes; mirrors keep it unit-testable off the GPU. */
data class GradeShaderSpec(
    val highlights: Float,
    val shadows: Float,
    val sharpen: Float,
    val grain: Float,
    val blur: Float,
    val relight: Float
) {
    val needsShader: Boolean
        get() = highlights != 0f || shadows != 0f || sharpen != 0f ||
            grain != 0f || blur != 0f || relight != 0f
}

/** The gentle skin-smooth blur floor of the Retouch preset. */
internal const val RETOUCH_BLUR = 0.18f

/** JVM mirror of the shader inputs for a clip. */
internal fun specOf(clip: MediaClip): GradeShaderSpec = GradeShaderSpec(
    highlights = clip.highlights.coerceIn(-1f, 1f),
    shadows = clip.shadows.coerceIn(-1f, 1f),
    sharpen = clip.sharpen.coerceIn(0f, 1f),
    grain = clip.grain.coerceIn(0f, 1f),
    // Retouch is a gentle skin-smooth: it raises the blur floor without
    // touching the user's blur slider value.
    blur = maxOf(
        clip.blurAmount.coerceIn(0f, 1f),
        if (clip.retouch) RETOUCH_BLUR else 0f
    ),
    relight = clip.relight.coerceIn(-1f, 1f)
)

@UnstableApi
internal class GradeShaderProgram(
    useHdr: Boolean,
    spec: GradeShaderSpec
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {

    private val program: GlQuadProgram = try {
        GlQuadProgram(GlQuadProgram.VERTEX_SOURCE, FRAGMENT_SOURCE).apply {
            use()
            setVec2("uTexel", 1f / 1920f, 1f / 1080f)
            setFloat("uHighlights", spec.highlights)
            setFloat("uShadows", spec.shadows)
            setFloat("uSharpen", spec.sharpen)
            setFloat("uGrain", spec.grain)
            setFloat("uBlur", spec.blur)
            setFloat("uRelight", spec.relight)
            setSampler("uTexSampler", /* texId= */ 0, /* unit= */ 0)
        }
    } catch (error: IllegalStateException) {
        throw VideoFrameProcessingException(error)
    }

    private var texelX = 1f / 1920f
    private var texelY = 1f / 1080f

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        if (inputWidth > 0 && inputHeight > 0) {
            texelX = 1f / inputWidth
            texelY = 1f / inputHeight
        }
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            program.use()
            program.setVec2("uTexel", texelX, texelY)
            program.setFloat("uTime", presentationTimeUs / 1_000_000f)
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
        val FRAGMENT_SOURCE = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform vec2 uTexel;
            uniform float uHighlights;
            uniform float uShadows;
            uniform float uSharpen;
            uniform float uGrain;
            uniform float uBlur;
            uniform float uRelight;
            uniform float uTime;
            in vec2 vUv;
            out vec4 fragColor;

            float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

            float hash(vec2 p) {
              p = fract(p * vec2(443.897, 441.423));
              p += dot(p, p + 19.19);
              return fract((p.x + p.y) * p.x);
            }

            void main() {
              vec2 uv = vUv;
              vec4 src = texture(uTexSampler, uv);

              // Disc blur (12 taps) when the blur slider or retouch is up.
              vec3 color = src.rgb;
              if (uBlur > 0.0) {
                float radius = uBlur * 6.0;
                vec3 sum = vec3(0.0);
                for (int i = 0; i < 12; i++) {
                  float a = float(i) * 0.5236;
                  vec2 off = vec2(cos(a), sin(a)) * uTexel * radius;
                  sum += texture(uTexSampler, uv + off).rgb;
                }
                color = sum / 12.0;
              }

              // Unsharp-mask sharpen against a 4-neighbour average.
              if (uSharpen > 0.0) {
                vec3 blurN = (texture(uTexSampler, uv + vec2(uTexel.x, 0.0)).rgb +
                              texture(uTexSampler, uv - vec2(uTexel.x, 0.0)).rgb +
                              texture(uTexSampler, uv + vec2(0.0, uTexel.y)).rgb +
                              texture(uTexSampler, uv - vec2(0.0, uTexel.y)).rgb) * 0.25;
                color += (color - blurN) * uSharpen * 1.5;
              }

              // Highlight / shadow tone mapping in luminance bands.
              float l = luma(color);
              float highlightMask = smoothstep(0.5, 1.0, l);
              float shadowMask = 1.0 - smoothstep(0.0, 0.5, l);
              color *= 1.0 + uHighlights * 0.35 * highlightMask;
              color *= 1.0 + uShadows * 0.35 * shadowMask;

              // Relight: soft radial key light from the upper-left; negative
              // values dim the centre like a reversed vignette.
              if (uRelight != 0.0) {
                vec2 d = uv - vec2(0.38, 0.32);
                float light = exp(-dot(d, d) * 6.0);
                color *= 1.0 + uRelight * 0.4 * light;
              }

              // Animated film grain.
              if (uGrain > 0.0) {
                float n = hash(uv * vec2(1920.0, 1080.0) + vec2(uTime * 61.0, uTime * 83.0)) - 0.5;
                color += n * uGrain * 0.12;
              }

              fragColor = vec4(clamp(color, 0.0, 1.0), src.a);
            }
            """.trimIndent()
    }
}
