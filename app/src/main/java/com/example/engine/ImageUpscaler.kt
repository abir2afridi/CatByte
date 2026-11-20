package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** A resized image: ARGB pixels plus their dimensions. */
class UpscaledImage(
    val pixels: IntArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpscaledImage) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}

/**
 * A high quality on-device image resampler used by the AI Lab's "AI Upscaler".
 *
 * The filter is a separable Lanczos-3 kernel, which is the same class of
 * reconstruction filter professional scalers use: it keeps edges crisp instead
 * of the blur a bilinear or nearest-neighbour resize produces.
 *
 * The resampling happens in premultiplied-alpha space so that transparent
 * pixels do not bleed their colour into the visible edges.
 *
 * The implementation is pure Kotlin over an IntArray, so it is unit-tested on
 * the JVM without a device.
 */
object ImageUpscaler {

    /** Largest integer upscale factor the tool allows. */
    const val MAX_FACTOR = 4

    /** Number of Lanczos lobes on each side of the kernel. */
    private const val LOBES = 3

    /**
     * Resizes [pixels] by an integer [factor] using a Lanczos-3 kernel.
     *
     * Returns the original buffer (copied) when the factor is 1.
     */
    fun upscale(pixels: IntArray, width: Int, height: Int, factor: Int): UpscaledImage {
        require(width > 0 && height > 0) { "image dimensions must be positive" }
        require(pixels.size == width * height) { "pixel buffer does not match the dimensions" }
        val scale = factor.coerceIn(1, MAX_FACTOR)
        if (scale == 1) return UpscaledImage(pixels.copyOf(), width, height)

        val outWidth = width * scale
        val outHeight = height * scale

        // Pass 1: horizontal. Source is straight ARGB, output is premultiplied float.
        val horizontal = FloatArray(outWidth * height * 4)
        for (y in 0 until height) {
            val rowIn = y * width * 4
            val rowOut = y * outWidth * 4
            for (x in 0 until outWidth) {
                val center = (x + 0.5f) / scale - 0.5f
                val start = ceil(center - LOBES).toInt()
                val end = floor(center + LOBES).toInt()
                var pr = 0f
                var pg = 0f
                var pb = 0f
                var pa = 0f
                var total = 0f
                for (i in start..end) {
                    val weight = lanczos(center - i)
                    if (weight == 0f) continue
                    val sx = i.coerceIn(0, width - 1)
                    val index = rowIn + sx * 4
                    val alpha = (pixels[index + 3] and 0xFF) / 255f
                    pr += weight * ((pixels[index] and 0xFF) / 255f) * alpha
                    pg += weight * ((pixels[index + 1] and 0xFF) / 255f) * alpha
                    pb += weight * ((pixels[index + 2] and 0xFF) / 255f) * alpha
                    pa += weight * alpha
                    total += weight
                }
                val norm = if (total != 0f) total else 1f
                val out = rowOut + x * 4
                horizontal[out] = pr / norm
                horizontal[out + 1] = pg / norm
                horizontal[out + 2] = pb / norm
                horizontal[out + 3] = pa / norm
            }
        }

        // Pass 2: vertical, still premultiplied.
        val output = IntArray(outWidth * outHeight)
        for (y in 0 until outHeight) {
            val center = (y + 0.5f) / scale - 0.5f
            val start = ceil(center - LOBES).toInt()
            val end = floor(center + LOBES).toInt()
            for (x in 0 until outWidth) {
                var pr = 0f
                var pg = 0f
                var pb = 0f
                var pa = 0f
                var total = 0f
                for (i in start..end) {
                    val weight = lanczos(center - i)
                    if (weight == 0f) continue
                    val sy = i.coerceIn(0, height - 1)
                    val index = (sy * outWidth + x) * 4
                    pr += weight * horizontal[index]
                    pg += weight * horizontal[index + 1]
                    pb += weight * horizontal[index + 2]
                    pa += weight * horizontal[index + 3]
                    total += weight
                }
                val norm = if (total != 0f) total else 1f
                val alpha = (pa / norm).coerceIn(0f, 1f)
                val r = if (alpha > 0.0001f) (pr / norm / alpha).coerceIn(0f, 1f) else 0f
                val g = if (alpha > 0.0001f) (pg / norm / alpha).coerceIn(0f, 1f) else 0f
                val b = if (alpha > 0.0001f) (pb / norm / alpha).coerceIn(0f, 1f) else 0f
                output[y * outWidth + x] = argb(r, g, b, alpha)
            }
        }
        return UpscaledImage(output, outWidth, outHeight)
    }

    /** The Lanczos-3 kernel: sinc(x) * sinc(x / 3) inside the support. */
    fun lanczos(x: Float): Float {
        if (x == 0f) return 1f
        val ax = abs(x)
        if (ax >= LOBES.toFloat()) return 0f
        val pix = PI.toFloat() * x
        val first = sin(pix) / pix
        val pix3 = pix / LOBES.toFloat()
        val second = sin(pix3) / pix3
        return first * second
    }

    /** Packs normalised channels into an ARGB integer. */
    private fun argb(r: Float, g: Float, b: Float, a: Float): Int {
        val ri = (r * 255f + 0.5f).roundToInt().coerceIn(0, 255)
        val gi = (g * 255f + 0.5f).roundToInt().coerceIn(0, 255)
        val bi = (b * 255f + 0.5f).roundToInt().coerceIn(0, 255)
        val ai = (a * 255f + 0.5f).roundToInt().coerceIn(0, 255)
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }
}
