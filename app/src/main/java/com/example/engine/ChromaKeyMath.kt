package com.example.engine

import kotlin.math.hypot

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
