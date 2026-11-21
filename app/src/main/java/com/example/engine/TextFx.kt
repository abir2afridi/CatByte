package com.example.engine

import kotlin.math.abs
import kotlin.math.sin

/**
 * The ONE source of truth for text-overlay animation maths.
 *
 * Both renderers - the Compose fallback preview
 * (`ui/canvas/CanvasOverlayLayers`) and the burned-in export
 * (`data/export/TimelineCanvasOverlay`) - consume these constants and
 * functions, so the preview shows exactly what the exported file will
 * contain. Changing a value here changes BOTH; never re-derive the maths
 * locally in a renderer.
 */
object TextFx {

    /** Font size = canvas width * this fraction (all text styles). */
    const val TITLE_WIDTH_FRACTION = 0.085f

    /** Characters revealed per unit of animation progress (typewriter). */
    const val TYPEWRITER_SPEED = 1.6f

    /** Entry-animation durations, in milliseconds. */
    const val POP_UP_MS = 320f
    const val FADE_IN_MS = 320f

    /** Pop-up: the text springs from [POP_UP_FROM]x to 1x. */
    const val POP_UP_FROM = 0.6f

    /** Wave bounce: vertical offset = height * fraction * sin(local/period). */
    const val WAVE_HEIGHT_FRACTION = 0.018f
    const val WAVE_PERIOD_MS = 125f

    /** Neon pulse breathing curve. */
    const val NEON_PULSE_BASE = 0.72f
    const val NEON_PULSE_RANGE = 0.28f
    const val NEON_PULSE_PERIOD_MS = 210f

    /** Neon glow shadow blur = canvas width * this fraction. */
    const val NEON_SHADOW_WIDTH_FRACTION = 0.012f

    /** Typewriter: how many characters of [text] are visible at [progress] (0..1). */
    fun typewriterChars(text: String, progress: Float): Int {
        if (text.isEmpty()) return 0
        val p = progress.coerceIn(0f, 1f)
        return (text.length * (p * TYPEWRITER_SPEED)).toInt().coerceIn(1, text.length)
    }

    /** Pop-up entry scale at [localMs] (settles at 1). */
    fun popUpScale(localMs: Long): Float {
        val p = (localMs / POP_UP_MS).coerceIn(0f, 1f)
        return POP_UP_FROM + (1f - POP_UP_FROM) * p
    }

    /** Fade-in alpha at [localMs] (clamped to 0..1). */
    fun fadeInAlpha(localMs: Long): Float =
        (localMs / FADE_IN_MS).coerceIn(0f, 1f)

    /** Neon-pulse breathing alpha at [localMs]. */
    fun neonPulseAlpha(localMs: Long): Float =
        NEON_PULSE_BASE + NEON_PULSE_RANGE * sin(localMs / NEON_PULSE_PERIOD_MS).toFloat()

    /** Wave bounce vertical offset in pixels at [localMs]. */
    fun waveOffsetY(localMs: Long, canvasHeightPx: Float): Float =
        sin(localMs / WAVE_PERIOD_MS).toFloat() * canvasHeightPx * WAVE_HEIGHT_FRACTION

    /** Title font size in pixels for a canvas of [canvasWidthPx]. */
    fun titleTextSizePx(canvasWidthPx: Float): Float =
        canvasWidthPx * TITLE_WIDTH_FRACTION

    /** Letter-spacing in pixels for a font size of [fontSizePx] and an em value. */
    fun letterSpacingPx(fontSizePx: Float, em: Float): Float =
        fontSizePx * em

    /** [Test hook] The exported-file end frame is EXCLUSIVE - see both renderers. */
    fun isVisibleAt(timeMs: Long, startMs: Long, durationMs: Long): Boolean =
        timeMs >= startMs && timeMs < startMs + abs(durationMs).coerceAtLeast(1L)
}
