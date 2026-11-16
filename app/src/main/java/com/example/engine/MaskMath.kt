package com.example.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Mask shapes available to a layer. */
enum class MaskKind { RECTANGLE, CIRCLE, SPLIT, MIRROR, FILMSTRIP }

data class MaskSpec(
    val kind: MaskKind = MaskKind.RECTANGLE,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val width: Float = 0.6f,
    val height: Float = 0.6f,
    val rotationDeg: Float = 0f,
    /** Soft edge width in normalised units; 0 is a hard cut. */
    val feather: Float = 0.02f,
    val invert: Boolean = false,
    /** Band count for [MaskKind.FILMSTRIP]. */
    val bands: Int = 6,
) {
    // Aliases matching the editor model vocabulary (posX/posY/inverted) so the
    // render-graph and gesture layers stay in sync without mapping code.
    val posX: Float get() = centerX
    val posY: Float get() = centerY
    val inverted: Boolean get() = invert

    companion object {
        fun of(
            centerX: Float = 0.5f,
            centerY: Float = 0.5f,
            width: Float = 0.6f,
            height: Float = 0.6f,
            rotationDeg: Float = 0f,
            feather: Float = 0.02f,
            invert: Boolean = false,
            bands: Int = 6,
        ) = MaskSpec(
            kind = MaskKind.RECTANGLE,
            centerX = centerX,
            centerY = centerY,
            width = width,
            height = height,
            rotationDeg = rotationDeg,
            feather = feather,
            invert = invert,
            bands = bands,
        )
    }
}

/**
 * Mask coverage in normalised coordinates. Produces the alpha the compositor
 * multiplies into a layer, so the same maths serves preview and export.
 */
object MaskMath {

    fun coverage(spec: MaskSpec, u: Float, v: Float): Float {
        val raw = when (spec.kind) {
            MaskKind.RECTANGLE -> rectangle(spec, u, v)
            MaskKind.CIRCLE -> circle(spec, u, v)
            MaskKind.SPLIT -> split(spec, u, spec.centerX)
            MaskKind.MIRROR -> mirror(spec, u)
            MaskKind.FILMSTRIP -> filmstrip(spec, v)
        }
        val alpha = if (spec.invert) 1f - raw else raw
        return alpha.coerceIn(0f, 1f)
    }

    private fun smoothstep(edgeDistance: Float, feather: Float): Float {
        if (feather <= 0f) return if (edgeDistance >= 0f) 1f else 0f
        val t = (edgeDistance / feather).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun local(spec: MaskSpec, u: Float, v: Float): Pair<Float, Float> {
        val dx = u - spec.centerX
        val dy = v - spec.centerY
        if (spec.rotationDeg == 0f) return dx to dy
        val rad = Math.toRadians(spec.rotationDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return (dx * c + dy * s) to (-dx * s + dy * c)
    }

    private fun rectangle(spec: MaskSpec, u: Float, v: Float): Float {
        val (x, y) = local(spec, u, v)
        val halfW = spec.width / 2f
        val halfH = spec.height / 2f
        val edge = min(halfW - abs(x), halfH - abs(y))
        return smoothstep(edge, spec.feather)
    }

    private fun circle(spec: MaskSpec, u: Float, v: Float): Float {
        val (x, y) = local(spec, u, v)
        val radius = min(spec.width, spec.height) / 2f
        return smoothstep(radius - hypot(x, y), spec.feather)
    }

    private fun split(spec: MaskSpec, value: Float, center: Float): Float {
        val edge = value - center
        return smoothstep(edge, spec.feather)
    }

    private fun mirror(spec: MaskSpec, u: Float): Float {
        val (x, _) = local(spec, u, spec.centerY)
        return smoothstep(spec.width / 2f - abs(x), spec.feather)
    }

    private fun filmstrip(spec: MaskSpec, v: Float): Float {
        val bands = spec.bands.coerceAtLeast(1)
        val band = (v * bands).toInt().coerceIn(0, bands - 1)
        return if (band % 2 == 0) 1f else 0f
    }
}
