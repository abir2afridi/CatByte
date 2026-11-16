package com.example.data.export

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import com.example.model.TransitionType

/** Builds the transition effects that are burned into the exported video. */
@UnstableApi
object TransitionMapper {

    /**
     * Returns effects for one clip using [transition].
     *
     * Presentation timestamps inside an androidx.media3.transformer.EditedMediaItem
     * are clip-local (they start at zero), so every transition is evaluated against
     * [clipDurationUs] rather than absolute timeline positions. This is what makes
     * the live CompositionPlayer preview match Transformer's export.
     */
    fun effectsFor(
        transition: TransitionType,
        clipDurationUs: Long,
        transitionDurationMs: Long
    ): List<GlEffect> {
        if (transition == TransitionType.NONE) return emptyList()
        val clipUs = clipDurationUs.coerceAtLeast(1L)
        val durationUs = (transitionDurationMs.coerceIn(80L, 2000L) * 1000L)
            .coerceAtMost(clipUs / 2L)
            .coerceAtLeast(1L)

        return when (transition) {
            TransitionType.FADE_BLACK, TransitionType.DISSOLVE, TransitionType.PAGE_FLIP,
            TransitionType.PAPER_TEAR, TransitionType.PENCIL_WIPE, TransitionType.INK_BLEED ->
                listOf(fadeEffect(clipUs, durationUs, flashToWhite = false))

            TransitionType.FLASH_WHITE ->
                listOf(fadeEffect(clipUs, durationUs, flashToWhite = true))

            TransitionType.WHIP_PAN -> listOf(
                fadeEffect(clipUs, durationUs, flashToWhite = false),
                slideEffect(clipUs, durationUs, horizontal = true)
            )

            TransitionType.ZOOM_IN -> listOf(
                fadeEffect(clipUs, durationUs, flashToWhite = false),
                zoomEffect(clipUs, durationUs)
            )

            TransitionType.GLITCH -> listOf(
                TimeRgbGainEffect(0L, clipUs) { time ->
                    val phase = (time.coerceAtLeast(0L) % 120_000L) / 120_000L
                    val flicker = if (phase < 0.5f) 1.35f else 0.82f
                    floatArrayOf(flicker, 1f, 1.18f)
                }
            )

            TransitionType.NONE -> emptyList()
        }
    }

    /** Fade the clip towards black at the start and back at the end. */
    private fun fadeEffect(
        clipDurationUs: Long,
        durationUs: Long,
        flashToWhite: Boolean
    ): TimeRgbGainEffect = TimeRgbGainEffect(0L, clipDurationUs) { time ->
        val local = time.coerceIn(0L, clipDurationUs)
        val remaining = (clipDurationUs - local).coerceAtLeast(0L)
        var gain = 1f
        if (local < durationUs) {
            val progress = local.toFloat() / durationUs
            gain = if (flashToWhite) 1f + 2.4f * progress else progress
        } else if (remaining < durationUs) {
            val progress = remaining.toFloat() / durationUs
            gain = if (flashToWhite) 1f + 2.4f * progress else progress
        }
        val clamped = gain.coerceIn(0f, 3.5f)
        floatArrayOf(clamped, clamped, clamped)
    }

    /** Slide the clip in from the side. */
    private fun slideEffect(
        clipDurationUs: Long,
        durationUs: Long,
        horizontal: Boolean
    ): TimeTransformEffect = TimeTransformEffect { time ->
        val matrix = Matrix()
        val local = time.coerceIn(0L, clipDurationUs)
        val remaining = (clipDurationUs - local).coerceAtLeast(0L)
        val progress = when {
            local < durationUs -> 1f - (local.toFloat() / durationUs)
            remaining < durationUs -> remaining.toFloat() / durationUs
            else -> 0f
        }
        val offset = progress * 0.35f
        if (horizontal) matrix.setTranslate(offset, 0f) else matrix.setTranslate(0f, offset)
        matrix
    }

    /** Zoom the clip in slightly at the beginning. */
    private fun zoomEffect(
        clipDurationUs: Long,
        durationUs: Long
    ): TimeTransformEffect = TimeTransformEffect { time ->
        val matrix = Matrix()
        val local = time.coerceIn(0L, clipDurationUs)
        val progress = if (local < durationUs) {
            1f - (local.toFloat() / durationUs) * 0.25f
        } else {
            1f
        }
        matrix.setScale(progress, progress, 0f, 0f)
        matrix
    }
}
