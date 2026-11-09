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

enum class BlendKind { NORMAL, MULTIPLY, SCREEN, OVERLAY, ADD, SUBTRACT, DIFFERENCE }

/**
 * Blend modes as channel maths plus an ARGB composite. `blend` is what the
 * compositor shader mirrors; the Kotlin version keeps it testable off-device.
 */
object BlendMath {

    fun blendChannel(bottom: Float, top: Float, kind: BlendKind): Float {
        val b = bottom.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val out = when (kind) {
            BlendKind.NORMAL -> t
            BlendKind.MULTIPLY -> b * t
            BlendKind.SCREEN -> 1f - (1f - b) * (1f - t)
            BlendKind.OVERLAY -> if (b < 0.5f) 2f * b * t else 1f - 2f * (1f - b) * (1f - t)
            BlendKind.ADD -> b + t
            BlendKind.SUBTRACT -> b - t
            BlendKind.DIFFERENCE -> abs(b - t)
        }
        return out.coerceIn(0f, 1f)
    }

    fun blend(bottom: Int, top: Int, kind: BlendKind, opacity: Float = 1f): Int {
        val op = opacity.coerceIn(0f, 1f)
        if (op <= 0f) return bottom
        val topAlpha = ((top ushr 24) and 0xFF) / 255f * op
        if (topAlpha <= 0f) return bottom
        val bottomAlpha = ((bottom ushr 24) and 0xFF) / 255f
        val outAlpha = topAlpha + bottomAlpha * (1f - topAlpha)
        if (outAlpha <= 0f) return 0

        fun channel(shift: Int): Int {
            val cb = ((bottom shr shift) and 0xFF) / 255f
            val ct = ((top shr shift) and 0xFF) / 255f
            val mixed = blendChannel(cb, ct, kind)
            val out = (mixed * topAlpha + cb * bottomAlpha * (1f - topAlpha)) / outAlpha
            return (out * 255f + 0.5f).toInt().coerceIn(0, 255)
        }

        val r = channel(16)
        val g = channel(8)
        val b = channel(0)
        val a = (outAlpha * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}

/**
 * Chroma key parameters. [tolerance] and [softness] are distances in the
 * normalised Cb/Cr plane (0..~1), not degrees, so they behave the same on every
 * device. [spill] 0 removes the key-colour cast completely, 1 leaves it alone.
 */
data class ChromaSpec(
    val keyR: Float = 0f,
    val keyG: Float = 1f,
    val keyB: Float = 0f,
    val tolerance: Float = 0.25f,
    val softness: Float = 0.2f,
    val spill: Float = 0.3f,
)

object ChromaKey {

    private fun chroma(r: Float, g: Float, b: Float): FloatArray {
        val cb = 0.5f - 0.168736f * r - 0.331264f * g + 0.5f * b
        val cr = 0.5f + 0.5f * r - 0.418688f * g - 0.081312f * b
        return floatArrayOf(cb, cr)
    }

    /** 0 = fully keyed out, 1 = fully opaque. */
    fun alphaFor(r: Float, g: Float, b: Float, spec: ChromaSpec): Float {
        val key = chroma(spec.keyR, spec.keyG, spec.keyB)
        val px = chroma(r, g, b)
        val distance = hypot(px[0] - key[0], px[1] - key[1])
        val inner = spec.tolerance.coerceIn(0f, 1f)
        val outer = (inner + spec.softness.coerceIn(0f, 1f)).coerceAtMost(1.5f)
        if (outer <= inner) return if (distance <= inner) 0f else 1f
        val t = ((distance - inner) / (outer - inner)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Pulls the key-colour cast out of the remaining pixels (green spill). */
    fun suppressSpill(argb: Int, spec: ChromaSpec): Int {
        val a = (argb ushr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        // Only the dominant key channel can spill; for a green key that is G.
        if (spec.keyG < spec.keyR || spec.keyG < spec.keyB) {
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        val limit = maxOf(r, b).toFloat()
        val kept = limit + (g - limit) * spec.spill.coerceIn(0f, 1f)
        val newG = kept.coerceIn(0f, 255f).toInt()
        return (a shl 24) or (r shl 16) or (newG shl 8) or b
    }

    fun apply(argb: Int, spec: ChromaSpec): Int {
        val a = (argb ushr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val alpha = alphaFor(r / 255f, g / 255f, b / 255f, spec)
        val cleaned = suppressSpill(argb, spec)
        val newAlpha = (a * alpha + 0.5f).toInt().coerceIn(0, 255)
        return (newAlpha shl 24) or (cleaned and 0x00FFFFFF)
    }
}
