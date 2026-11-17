package com.example.data.export

import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/**
 * Real directional motion blur, burned into the pixels by the GPU for both
 * the CompositionPlayer preview and the exported file.
 *
 * The fragment shader samples the frame [TAPS] times along the blur axis with
 * a Gaussian-ish weight profile - the same blur a camera would record for a
 * moving subject - instead of faking it with a translucent overlay.
 *
 * @param strength 0..1 blur length as a fraction of the frame width.
 */
@UnstableApi
class MotionBlurEffect(private val strength: Float) : GlEffect {

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = strength <= 0.001f

    override fun toGlShaderProgram(context: android.content.Context, useHdr: Boolean): GlShaderProgram =
        MotionBlurShaderProgram(useHdr, strength.coerceIn(0f, 1f))
}

@UnstableApi
internal class MotionBlurShaderProgram(
    useHdr: Boolean,
    private val strength: Float
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {

    private val program: GlQuadProgram = try {
        GlQuadProgram(GlQuadProgram.VERTEX_SOURCE, FRAGMENT_SOURCE).apply {
            use()
            setFloat("uStrength", strength.coerceIn(0.01f, 1f))
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
            // Normalised blur length follows the actual frame size.
            program.setFloat(
                "uStep",
                MotionBlurEffectUtil.normalizedStep(strength)
            )
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
        /** Tap count: more taps = smoother trail at linear cost. */
        const val TAPS = 17

        const val FRAGMENT_SOURCE = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform float uStep;      // one tap offset in normalised u (already scaled)
            in vec2 vUv;
            out vec4 fragColor;

            void main() {
                const int TAPS = ${TAPS - 1};
                vec4 sum = texture(uTexSampler, vUv) * 2.0;
                float total = 2.0;
                for (int i = 1; i <= TAPS; i++) {
                    float fi = float(i);
                    // Gaussian-ish weights: the centre dominates, the trail fades.
                    float weight = exp(-fi * fi / (float(TAPS) * 0.7));
                    float offset = uStep * fi;
                    sum += texture(uTexSampler, vec2(vUv.x + offset, vUv.y)) * weight;
                    sum += texture(uTexSampler, vec2(vUv.x - offset, vUv.y)) * weight;
                    total += weight * 2.0;
                }
                fragColor = sum / total;
            }
"""
    }
}

/** Pure helpers for the motion blur (JVM-testable). */
object MotionBlurEffectUtil {

    /**
     * Distance between two taps in normalised texture coordinates. The full
     * trail spans [strength] * 8% of the frame width, split over the taps.
     */
    fun normalizedStep(strength: Float, taps: Int = MotionBlurShaderProgram.TAPS - 1): Float =
        (strength * 0.08f / taps.coerceAtLeast(1)).coerceIn(0.0005f, 0.02f)
}
