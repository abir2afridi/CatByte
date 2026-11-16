package com.example.engine

import kotlin.math.abs

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
