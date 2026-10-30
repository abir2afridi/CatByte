package com.example.data

import android.opengl.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlMatrixTransformation
import com.example.model.EffectType

/**
 * Creates Media3 GPU-accelerated [GlEffect] instances for real-time preview and export.
 *
 * Note: Chroma Key (green screen) removal is handled per-frame by
 * [AiCutoutManager] using bitmap processing; matrix-based effects below run
 * on the GPU via Media3.
 */
@UnstableApi
object CustomGlEffects {

    /** Returns a [GlEffect] for zoom blur or shake effect. */
    fun createEffect(type: EffectType): GlEffect {
        return when (type) {
            EffectType.ZOOM_BLUR -> createZoomBlurEffect()
            EffectType.SHAKE -> createShakeEffect()
            else -> createZoomBlurEffect()
        }
    }

    private fun createZoomBlurEffect(): GlMatrixTransformation {
        return GlMatrixTransformation { presentationTimeUs ->
            val scale = 1.05f + 0.05f * kotlin.math.sin(presentationTimeUs / 100_000.0).toFloat()
            val matrix = FloatArray(16)
            Matrix.setIdentityM(matrix, 0)
            Matrix.scaleM(matrix, 0, scale, scale, 1.0f)
            matrix
        }
    }

    private fun createShakeEffect(): GlMatrixTransformation {
        return GlMatrixTransformation { presentationTimeUs ->
            val dx = 0.03f * kotlin.math.sin(presentationTimeUs / 50_000.0).toFloat()
            val dy = 0.02f * kotlin.math.cos(presentationTimeUs / 40_000.0).toFloat()
            val matrix = FloatArray(16)
            Matrix.setIdentityM(matrix, 0)
            Matrix.translateM(matrix, 0, dx, dy, 0.0f)
            matrix
        }
    }
}
