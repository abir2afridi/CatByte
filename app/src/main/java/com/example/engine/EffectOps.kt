package com.example.engine

import com.example.model.EffectFamily
import com.example.model.EffectSpec
import com.example.model.EffectType
import com.example.engine.effects.bokeh
import com.example.engine.effects.cinemaBars
import com.example.engine.effects.confetti
import com.example.engine.effects.edgeGlow
import com.example.engine.effects.grain
import com.example.engine.effects.particles
import com.example.engine.effects.pixelBlocks
import com.example.engine.effects.ripple
import com.example.engine.effects.rings
import com.example.engine.effects.scanlines
import com.example.engine.effects.starfield
import com.example.engine.effects.wave
import kotlin.random.Random

/**
 * One primitive draw instruction for a video effect. All geometry is in
 * absolute pixels for the current frame; both renderers (the Compose preview
 * and the exported-file canvas) consume the exact same list, so preview and
 * output always match.
 */
data class EffectOp(
    val kind: EffectOpKind,
    /** Rect for RECT/GRADIENT_H/RADIAL ops (pixels). */
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    /** Centre for OVAL_FILLED/LINE/CIRCLE_STROKE ops (pixels). */
    val cx: Float = 0f,
    val cy: Float = 0f,
    /** Radius or line end offset (pixels). */
    val radius: Float = 0f,
    val colorA: Int = 0,
    /** Second gradient colour for GRADIENT_H/RADIAL ops (argb). */
    val colorB: Int = 0,
    /** Stroke width for CIRCLE_STROKE/LINE ops (pixels). */
    val strokeWidth: Float = 0f
)

enum class EffectOpKind {
    /** Filled rect from (left,top) to (right,bottom). */
    RECT,
    /** Horizontal linear gradient rect, [colorA] -> transparent-ish [colorB]. */
    GRADIENT_H,
    /** Radial gradient centred in the rect, [colorA] core -> [colorB] edge. */
    RADIAL,
    /** Filled circle at (cx,cy) with [radius]. */
    OVAL_FILLED,
    /** Stroke circle at (cx,cy). */
    CIRCLE_STROKE,
    /** Stroke rect border from (left,top) to (right,bottom). */
    RECT_STROKE,
    /** Line from (cx,cy) to (left,top). */
    LINE
}

/**
 * Turns an [EffectType] plus its [EffectSpec] into the primitive draw ops for
 * one frame. This object is ONLY a dispatcher: every family's maths live in
 * its own file under engine/effects/ (one file per family), so debugging or
 * tuning one effect can never touch another.
 */
object EffectOps {

    /** Frame step for noise so speckles/grain shimmer at a visible rate. */
    internal const val NOISE_FRAME_MS = 40L

    fun compute(
        type: EffectType,
        spec: EffectSpec,
        localMs: Long,
        width: Float,
        height: Float
    ): List<EffectOp> {
        if (width <= 0f || height <= 0f) return emptyList()
        val rng = Random(type.name.hashCode() * 31L + localMs / NOISE_FRAME_MS)
        return when (spec.family) {
            // Interactive families (light leak, vignette, strobe, spotlight,
            // film burn, plasma, grid, beams) render in the GPU via
            // EffectBlendShader - they MODIFY video pixels, so they never
            // produce overlay ops.
            EffectFamily.LIGHT_LEAK, EffectFamily.STROBE, EffectFamily.VIGNETTE,
            EffectFamily.SPOTLIGHT, EffectFamily.FILM_BURN, EffectFamily.PLASMA,
            EffectFamily.GRID, EffectFamily.BEAMS -> emptyList()

            EffectFamily.SCANLINES -> scanlines(spec, localMs, rng, width, height)
            EffectFamily.GRAIN -> grain(spec, localMs, rng, width, height)
            EffectFamily.RINGS -> rings(spec, localMs, width, height)
            EffectFamily.PARTICLES -> particles(spec, localMs, rng, width, height)
            EffectFamily.BOKEH -> bokeh(spec, localMs, rng, width, height)
            EffectFamily.EDGE_GLOW -> edgeGlow(spec, localMs, width, height)
            EffectFamily.CINEMA_BARS -> cinemaBars(spec, localMs, width, height)
            EffectFamily.WAVE -> wave(spec, localMs, width, height)
            EffectFamily.PIXEL_BLOCKS -> pixelBlocks(spec, localMs, rng, width, height)
            EffectFamily.CONFETTI -> confetti(spec, localMs, rng, width, height)
            EffectFamily.STARFIELD -> starfield(spec, localMs, rng, width, height)
            EffectFamily.RIPPLE -> ripple(spec, localMs, width, height)
        }
    }
}

/** Replaces only the alpha channel of an argb colour. */
fun withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
