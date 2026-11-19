package com.example.data.export

import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.example.model.EffectFamily
import com.example.model.EffectSpec

/**
 * True video-interacting effects, burned into the pixels by the GPU.
 *
 * Unlike the overlay families (confetti, particles, frames - which correctly
 * sit ON TOP of the picture), these families MODIFY THE VIDEO ITSELF: the
 * fragment shader samples the real frame and blends the effect into it with
 * proper blend maths (screen brightens the footage, vignette multiplies its
 * edges, grid overlays its pixels). A translucent overlay cannot do that,
 * which is exactly why these kinds live here.
 *
 * Families handled: LIGHT_LEAK, VIGNETTE, STROBE, SPOTLIGHT, FILM_BURN,
 * PLASMA, GRID, BEAMS.
 */
@UnstableApi
class EffectBlendShader(
    private val family: EffectFamily,
    private val spec: EffectSpec,
    private val intensityScale: Float = 1f
) : GlEffect {

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = !family.isShaderFamily

    override fun toGlShaderProgram(context: android.content.Context, useHdr: Boolean): GlShaderProgram =
        EffectBlendShaderProgram(useHdr, family, spec, intensityScale)
}

/** True for families that must blend with the video instead of drawing over it. */
val EffectFamily.isShaderFamily: Boolean
    get() = this in EFFECT_FAMILY_SHADER_SET

/** Families that must blend with the video instead of drawing over it. */
val EFFECT_FAMILY_SHADER_SET: Set<EffectFamily> = setOf(
    EffectFamily.LIGHT_LEAK,
    EffectFamily.VIGNETTE,
    EffectFamily.STROBE,
    EffectFamily.SPOTLIGHT,
    EffectFamily.FILM_BURN,
    EffectFamily.PLASMA,
    EffectFamily.GRID,
    EffectFamily.BEAMS
)

/** Pure routing helpers (JVM-testable). */
object EffectBlendShaderUtil {

    val shaderFamilies: Set<EffectFamily> = EFFECT_FAMILY_SHADER_SET

    /** The active shader specs inside a clip's timeline span, in placement order. */
    fun activeShaderSpecs(
        effects: List<com.example.model.TimelineEffect>,
        clipInUs: Long,
        clipOutUs: Long
    ): List<com.example.model.TimelineEffect> =
        effects.filter { effect ->
            effect.effectType.spec.family in shaderFamilies &&
                effect.startTimelineMs * 1000L < clipOutUs &&
                (effect.startTimelineMs + effect.durationMs) * 1000L > clipInUs
        }
}

@UnstableApi
internal class EffectBlendShaderProgram(
    useHdr: Boolean,
    family: EffectFamily,
    spec: EffectSpec,
    intensityScale: Float
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {

    private val program: GlQuadProgram = try {
        GlQuadProgram(GlQuadProgram.VERTEX_SOURCE, FRAGMENT_SOURCE).apply {
            use()
            setInt("uKind", kindOf(family))
            setVec3(
                "uColorA",
                ((spec.colorA shr 16) and 0xFF) / 255f,
                ((spec.colorA shr 8) and 0xFF) / 255f,
                (spec.colorA and 0xFF) / 255f
            )
            setVec3(
                "uColorB",
                ((spec.colorB shr 16) and 0xFF) / 255f,
                ((spec.colorB shr 8) and 0xFF) / 255f,
                (spec.colorB and 0xFF) / 255f
            )
            setFloat("uIntensity", (spec.intensity * intensityScale).coerceIn(0.05f, 1.6f))
            setFloat("uSpeed", spec.speed.coerceIn(0.05f, 1.5f))
            val seed = ((spec.colorA xor spec.colorB) and 0xFF) / 255f
            setVec3("uSeed", seed, seed * 0.7f, seed * 1.3f)
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

    private fun kindOf(family: EffectFamily): Int = when (family) {
        EffectFamily.LIGHT_LEAK -> KIND_LIGHT_LEAK
        EffectFamily.VIGNETTE -> KIND_VIGNETTE
        EffectFamily.STROBE -> KIND_STROBE
        EffectFamily.SPOTLIGHT -> KIND_SPOTLIGHT
        EffectFamily.FILM_BURN -> KIND_FILM_BURN
        EffectFamily.PLASMA -> KIND_PLASMA
        EffectFamily.GRID -> KIND_GRID
        EffectFamily.BEAMS -> KIND_BEAMS
        else -> KIND_LIGHT_LEAK
    }

    companion object {
        const val KIND_LIGHT_LEAK = 0
        const val KIND_VIGNETTE = 1
        const val KIND_STROBE = 2
        const val KIND_SPOTLIGHT = 3
        const val KIND_FILM_BURN = 4
        const val KIND_PLASMA = 5
        const val KIND_GRID = 6
        const val KIND_BEAMS = 7

        const val FRAGMENT_SOURCE = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform int uKind;
            uniform vec3 uColorA;
            uniform vec3 uColorB;
            uniform float uIntensity;
            uniform float uSpeed;
            uniform float uTime;
            in vec2 vUv;
            out vec4 fragColor;

            // Screen blend: brightens the footage where the effect is strong.
            vec3 screenBlend(vec3 video, vec3 fx) {
                return 1.0 - (1.0 - video) * (1.0 - fx);
            }
            // Soft light blend: colours the footage while keeping its detail.
            vec3 softBlend(vec3 video, vec3 fx) {
                vec3 lo = 2.0 * video * fx + video * video * (1.0 - 2.0 * fx);
                vec3 hi = sqrt(video) * (2.0 * fx - 1.0) + 2.0 * video * (1.0 - fx);
                return mix(lo, hi, step(0.5, fx));
            }

            void main() {
                vec4 video = texture(uTexSampler, vUv);
                float t = uTime * (0.35 + uSpeed * 1.6);
                vec3 fx = vec3(0.0);
                float mixAmount = 1.0;

                if (uKind == ${KIND_LIGHT_LEAK}) {
                    // Warm wash from the top-left corner, pulsing.
                    float pulse = 0.45 + 0.55 * sin(t * 2.0);
                    float corner = smoothstep(1.1, 0.0, length(vUv - vec2(0.05, 0.05)));
                    fx = mix(uColorA, uColorB, vUv.x) * pulse * corner;
                    mixAmount = 0.85;
                } else if (uKind == ${KIND_VIGNETTE}) {
                    // Darkens the video's own edges towards the frame centre.
                    float d = length((vUv - 0.5) * vec2(1.15, 1.0));
                    float dark = smoothstep(0.35, 0.85, d) * uIntensity;
                    fragColor = vec4(video.rgb * (1.0 - dark), video.a);
                    return;
                } else if (uKind == ${KIND_STROBE}) {
                    // Full-frame flash: pushes the real pixels towards the colour.
                    float period = 1.4 - uSpeed;
                    float phase = fract(uTime / max(period, 0.12));
                    float flash = 1.0 - smoothstep(0.0, 0.18, phase);
                    fx = mix(uColorA, vec3(1.0), 0.35) * flash;
                    mixAmount = 1.0;
                } else if (uKind == ${KIND_SPOTLIGHT}) {
                    // A light spot wandering over the actual footage.
                    vec2 centre = vec2(
                        0.5 + 0.3 * sin(t * 2.2),
                        0.5 + 0.3 * cos(t * 1.7)
                    );
                    float d = length(vUv - centre);
                    fx = uColorA * smoothstep(0.45, 0.0, d);
                    mixAmount = 0.9;
                } else if (uKind == ${KIND_FILM_BURN}) {
                    // Warm drifting blotches burning into the frame edges.
                    float drift = uTime * (0.2 + uSpeed);
                    float blob = smoothstep(0.5, 0.0,
                        length(vUv - vec2(0.3 + 0.25 * sin(drift * 3.1), 0.4 + 0.3 * cos(drift * 2.3))));
                    float blob2 = smoothstep(0.55, 0.0,
                        length(vUv - vec2(0.7 - 0.2 * sin(drift * 2.2), 0.7 - 0.25 * cos(drift * 3.4))));
                    float flicker = 0.7 + 0.3 * sin(uTime * 8.0);
                    fx = (blob * uColorA + blob2 * uColorB) * flicker * 0.8;
                    mixAmount = 1.0;
                } else if (uKind == ${KIND_PLASMA}) {
                    // Two soft colour fields drifting over the picture.
                    vec2 c1 = vec2(0.5 + 0.36 * sin(t * 2.0), 0.5 + 0.36 * cos(t * 1.3));
                    vec2 c2 = vec2(0.5 + 0.36 * cos(t * 1.6), 0.5 + 0.36 * sin(t * 2.1));
                    float g1 = smoothstep(0.6, 0.0, length(vUv - c1));
                    float g2 = smoothstep(0.6, 0.0, length(vUv - c2));
                    fx = g1 * uColorA + g2 * uColorB;
                    mixAmount = 0.7;
                } else if (uKind == ${KIND_GRID}) {
                    // Overlay-blended grid: interacts with the video's brightness.
                    float cell = 1.0 / (6.0 + uIntensity * 10.0);
                    vec2 g = abs(fract(vUv / cell) - 0.5);
                    float line = smoothstep(0.5, 0.46, max(g.x, g.y));
                    fx = uColorA * line;
                    mixAmount = 0.9;
                } else if (uKind == ${KIND_BEAMS}) {
                    // Rotating beams sweeping the real footage.
                    vec2 rel = vUv - 0.5;
                    float angle = atan(rel.y, rel.x);
                    float spin = t * 0.8;
                    float beam = cos((angle - spin) * 5.0);
                    beam = pow(max(beam, 0.0), 6.0);
                    fx = mix(uColorA, uColorB, 0.5 + 0.5 * sin(angle * 2.0)) * beam;
                    mixAmount = 0.8;
                }

                vec3 blended = screenBlend(video.rgb, fx * uIntensity);
                vec3 soft = softBlend(video.rgb, fx * uIntensity);
                // Strobe and burn read better as screen; plasma/grid as soft light.
                vec3 result = (uKind == ${KIND_PLASMA} || uKind == ${KIND_GRID})
                    ? mix(video.rgb, soft, clamp(mixAmount, 0.0, 1.0))
                    : mix(video.rgb, blended, clamp(mixAmount, 0.0, 1.0));
                fragColor = vec4(result, video.a);
            }
"""
    }
}
