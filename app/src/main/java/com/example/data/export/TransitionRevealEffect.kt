package com.example.data.export

import android.content.Context
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/**
 * The reveal transitions: dissolve, page turn, paper tear, pencil wipe and
 * ink bleed.
 *
 * All of them are drawn by one fragment shader that reveals the clip's own
 * pixels over the transition window - the picture is uncovered from black
 * with an edge that is characteristic for the transition. The progress curve
 * is computed in Kotlin ([TransitionRevealEffect.progressAt]) so the preview
 * and the export agree and so it can be unit tested.
 */
@UnstableApi
class TransitionRevealEffect(
    /** One of the `KIND_*` constants of [TransitionRevealShaderProgram]. */
    val kind: Int,
    private val clipDurationUs: Long,
    private val transitionDurationUs: Long
) : GlEffect {

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        transitionDurationUs <= 0L

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        TransitionRevealShaderProgram(
            useHdr = useHdr,
            kind = kind,
            clipDurationUs = clipDurationUs,
            transitionDurationUs = transitionDurationUs
        )

    companion object {

        /**
         * How much of the clip is visible at [timeUs].
         *
         * The transition runs at the start and at the end of the clip: 0 means
         * "nothing visible yet", 1 means "the whole picture".
         */
        fun progressAt(timeUs: Long, clipDurationUs: Long, transitionDurationUs: Long): Float {
            if (transitionDurationUs <= 0L) return 1f
            val local = timeUs.coerceIn(0L, clipDurationUs)
            val remaining = (clipDurationUs - local).coerceAtLeast(0L)
            return when {
                local < transitionDurationUs -> local.toFloat() / transitionDurationUs
                remaining < transitionDurationUs -> remaining.toFloat() / transitionDurationUs
                else -> 1f
            }
        }
    }
}

@UnstableApi
internal class TransitionRevealShaderProgram(
    useHdr: Boolean,
    private val kind: Int,
    private val clipDurationUs: Long,
    private val transitionDurationUs: Long
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {

    private var aspect = 1f

    private val program: GlQuadProgram = try {
        GlQuadProgram(GlQuadProgram.VERTEX_SOURCE, FRAGMENT_SOURCE).apply {
            use()
            setInt("uKind", kind)
            setFloat("uProgress", 1f)
            setFloat("uAspect", 1f)
            setSampler("uTexSampler", /* texId= */ 0, /* unit= */ 0)
        }
    } catch (error: IllegalStateException) {
        throw VideoFrameProcessingException(error)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        aspect = if (inputHeight > 0) inputWidth.toFloat() / inputHeight.toFloat() else 1f
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            program.use()
            program.setFloat(
                "uProgress",
                TransitionRevealEffect.progressAt(
                    timeUs = presentationTimeUs,
                    clipDurationUs = clipDurationUs,
                    transitionDurationUs = transitionDurationUs
                )
            )
            program.setFloat("uAspect", aspect)
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
        const val KIND_DISSOLVE = 0
        const val KIND_PAGE_FLIP = 1
        const val KIND_PAPER_TEAR = 2
        const val KIND_PENCIL_WIPE = 3
        const val KIND_INK_BLEED = 4

        val FRAGMENT_SOURCE = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform int uKind;
            uniform float uProgress;
            uniform float uAspect;
            in vec2 vUv;
            out vec4 fragColor;

            // Cheap deterministic hash, used for the dissolve and for the
            // noise that gives the torn edges their fibre.
            float hash21(vec2 p) {
              return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            float valueNoise(vec2 p) {
              vec2 cell = floor(p);
              vec2 f = fract(p);
              f = f * f * (3.0 - 2.0 * f);
              float a = hash21(cell);
              float b = hash21(cell + vec2(1.0, 0.0));
              float c = hash21(cell + vec2(0.0, 1.0));
              float d = hash21(cell + vec2(1.0, 1.0));
              return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            void main() {
              vec2 uv = vUv;
              float reveal = 0.0;
              float shade = 1.0;

              if (uKind == ${KIND_DISSOLVE}) {
                // Every pixel gets its own threshold, so the picture appears
                // as a cloud of dots instead of a moving edge.
                float threshold = hash21(floor(gl_FragCoord.xy));
                reveal = step(threshold, uProgress);
              } else if (uKind == ${KIND_PAPER_TEAR}) {
                // A torn edge: the wipe line wanders with the paper's fibre.
                float edge = uProgress * 1.3 - 0.15
                    + (valueNoise(vec2(uv.y * 9.0, 3.0)) - 0.5) * 0.24;
                reveal = step(uv.x, edge);
              } else if (uKind == ${KIND_PENCIL_WIPE}) {
                // A scribbled diagonal edge.
                float scribble = sin(uv.y * 38.0) * 0.02
                    + (valueNoise(vec2(uv.y * 14.0, 7.0)) - 0.5) * 0.05;
                float edge = uProgress * 1.6 - 0.3 + scribble;
                reveal = step((uv.x + uv.y) * 0.5, edge);
              } else if (uKind == ${KIND_INK_BLEED}) {
                // An ink blot spreading out of the centre.
                vec2 centred = (uv - 0.5) * vec2(uAspect, 1.0);
                float radius = uProgress * 0.95;
                float wobble = (valueNoise(uv * 6.0) - 0.5) * 0.18 * uProgress;
                float distance = length(centred) - wobble;
                reveal = 1.0 - smoothstep(radius - 0.05, radius + 0.05, distance);
              } else if (uKind == ${KIND_PAGE_FLIP}) {
                // The page turns over towards the left: the front of the
                // sheet shrinks while its back - mirrored and dimmer - shows
                // next to it. Everything past the page is empty.
                float width = max(cos((1.0 - uProgress) * 1.5707963), 0.001);
                if (uv.x <= width) {
                  uv = vec2(uv.x / width, uv.y);
                } else if (uv.x <= 2.0 * width) {
                  uv = vec2((2.0 * width - uv.x) / width, uv.y);
                  shade = 0.55;
                } else {
                  fragColor = vec4(0.0, 0.0, 0.0, 0.0);
                  return;
                }
                reveal = 1.0;
              } else {
                reveal = uProgress;
              }

              vec4 color = texture(uTexSampler, uv);
              fragColor = vec4(color.rgb * reveal * shade, color.a * reveal);
            }
            """.trimIndent()
    }
}
