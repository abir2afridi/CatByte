package com.example.engine

import kotlin.math.floor
import kotlin.math.min

/**
 * A 3D colour lookup table. [data] is `size * size * size * 3` floats in
 * `.cube` ordering: red varies fastest, then green, then blue.
 */
class Lut3D(val size: Int, val data: FloatArray) {

    init {
        require(size >= 2) { "LUT size must be at least 2, was $size" }
        require(data.size >= size * size * size * 3) {
            "LUT data has ${data.size} values, expected ${size * size * size * 3}"
        }
    }

    /** Trilinear sample; [r], [g], [b] are 0..1 and clamped. */
    fun sample(r: Float, g: Float, b: Float): FloatArray {
        val maxIndex = size - 1
        val x = r.coerceIn(0f, 1f) * maxIndex
        val y = g.coerceIn(0f, 1f) * maxIndex
        val z = b.coerceIn(0f, 1f) * maxIndex
        val x0 = floor(x).toInt().coerceIn(0, maxIndex)
        val y0 = floor(y).toInt().coerceIn(0, maxIndex)
        val z0 = floor(z).toInt().coerceIn(0, maxIndex)
        val x1 = min(x0 + 1, maxIndex)
        val y1 = min(y0 + 1, maxIndex)
        val z1 = min(z0 + 1, maxIndex)
        val fx = x - x0
        val fy = y - y0
        val fz = z - z0

        val c000 = entry(x0, y0, z0)
        val c100 = entry(x1, y0, z0)
        val c010 = entry(x0, y1, z0)
        val c110 = entry(x1, y1, z0)
        val c001 = entry(x0, y0, z1)
        val c101 = entry(x1, y0, z1)
        val c011 = entry(x0, y1, z1)
        val c111 = entry(x1, y1, z1)

        return FloatArray(3) { channel ->
            val c00 = lerp(c000[channel], c100[channel], fx)
            val c10 = lerp(c010[channel], c110[channel], fx)
            val c01 = lerp(c001[channel], c101[channel], fx)
            val c11 = lerp(c011[channel], c111[channel], fx)
            val c0 = lerp(c00, c10, fy)
            val c1 = lerp(c01, c11, fy)
            lerp(c0, c1, fz)
        }
    }

    private fun entry(r: Int, g: Int, b: Int): FloatArray {
        val base = (r + g * size + b * size * size) * 3
        return floatArrayOf(data[base], data[base + 1], data[base + 2])
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    companion object {
        /** Neutral LUT used as a fallback and for tests. */
        fun identity(size: Int = 33): Lut3D {
            val values = FloatArray(size * size * size * 3)
            val maxIndex = (size - 1).toFloat()
            var index = 0
            for (b in 0 until size) {
                for (g in 0 until size) {
                    for (r in 0 until size) {
                        values[index++] = r / maxIndex
                        values[index++] = g / maxIndex
                        values[index++] = b / maxIndex
                    }
                }
            }
            return Lut3D(size, values)
        }
    }
}

/** `.cube` parser (Adobe/Resolve 3D and 1D LUTs). */
object LutCubeParser {

    fun parse(text: String): Lut3D? = runCatching {
        var size = 0
        var oneDimensional = false
        val values = mutableListOf<Float>()
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) return@forEach
            when {
                line.startsWith("LUT_3D_SIZE", ignoreCase = true) -> {
                    size = line.substringAfter("LUT_3D_SIZE").trim().toInt()
                    oneDimensional = false
                }
                line.startsWith("LUT_1D_SIZE", ignoreCase = true) -> {
                    size = line.substringAfter("LUT_1D_SIZE").trim().toInt()
                    oneDimensional = true
                }
                line.startsWith("TITLE", ignoreCase = true) -> Unit
                line.startsWith("DOMAIN_MIN", ignoreCase = true) -> Unit
                line.startsWith("DOMAIN_MAX", ignoreCase = true) -> Unit
                line.first().isDigit() || line.startsWith("-") -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size < 3) return@forEach
                    parts.take(3).forEach { part -> values += part.toFloat() }
                }
            }
        }
        if (size < 2) return null
        if (oneDimensional) {
            // A 1D LUT is expanded into a 3D table so one sampler covers both.
            val table = Lut3D.identity(size.coerceAtLeast(2))
            val channels = values.chunked(3)
            if (channels.size < size) return null
            val data = FloatArray(table.data.size)
            val maxIndex = (table.size - 1).toFloat()
            var index = 0
            for (b in 0 until table.size) {
                for (g in 0 until table.size) {
                    for (r in 0 until table.size) {
                        data[index++] = sampleChannel(channels, r / maxIndex, 0)
                        data[index++] = sampleChannel(channels, g / maxIndex, 1)
                        data[index++] = sampleChannel(channels, b / maxIndex, 2)
                    }
                }
            }
            return Lut3D(table.size, data)
        }
        val expected = size * size * size * 3
        if (values.size < expected) return null
        return Lut3D(size, values.take(expected).toFloatArray())
    }.getOrNull()

    private fun sampleChannel(entries: List<List<Float>>, position: Float, channel: Int): Float {
        val maxIndex = entries.size - 1
        if (maxIndex <= 0) return position
        val x = position.coerceIn(0f, 1f) * maxIndex
        val lower = floor(x).toInt().coerceIn(0, maxIndex)
        val upper = min(lower + 1, maxIndex)
        val fraction = x - lower
        val a = entries[lower].getOrNull(channel) ?: position
        val b = entries[upper].getOrNull(channel) ?: position
        return a + (b - a) * fraction
    }
}

/** A named grade preset built on top of a LUT. */
data class LutPreset(
    val id: String,
    val displayName: String,
    val lut: Lut3D,
    val defaultIntensity: Float = 1f
)

object LutApply {

    /** Mixes a graded colour back towards the original by [intensity] (0..1). */
    fun apply(lut: Lut3D, r: Float, g: Float, b: Float, intensity: Float): FloatArray {
        val amount = intensity.coerceIn(0f, 1f)
        if (amount <= 0f) return floatArrayOf(r, g, b)
        val sampled = lut.sample(r, g, b)
        return floatArrayOf(
            r + (sampled[0] - r) * amount,
            g + (sampled[1] - g) * amount,
            b + (sampled[2] - b) * amount
        )
    }

    /** Converts the LUT into a 3D texture payload (RGB float, no padding). */
    fun toTexturePayload(lut: Lut3D): FloatArray = lut.data.copyOf(lut.size * lut.size * lut.size * 3)

    /**
     * Bakes a stack of LUTs into one table, so a chain of grades still costs a
     * single texture lookup per pixel.
     */
    fun bake(luts: List<Lut3D>, intensity: Float = 1f, size: Int = 17): Lut3D {
        if (luts.isEmpty()) return Lut3D.identity(size)
        val baked = FloatArray(size * size * size * 3)
        val maxIndex = (size - 1).toFloat()
        var index = 0
        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    var color = floatArrayOf(r / maxIndex, g / maxIndex, b / maxIndex)
                    luts.forEach { lut -> color = apply(lut, color[0], color[1], color[2], intensity) }
                    baked[index++] = color[0]
                    baked[index++] = color[1]
                    baked[index++] = color[2]
                }
            }
        }
        return Lut3D(size, baked)
    }

    /** Lookup keys a shader uploads for one LUT. */
    fun shaderParameters(lut: Lut3D, intensity: Float): Map<String, Float> = mapOf(
        "lutSize" to lut.size.toFloat(),
        "lutIntensity" to intensity.coerceIn(0f, 1f)
    )

    /** Guards against absurd user imports (a 64^3 LUT is already 786k floats). */
    fun isAcceptableSize(lut: Lut3D): Boolean = lut.size in 2..64

    fun estimatedTextureBytes(lut: Lut3D): Long =
        lut.size.toLong() * lut.size.toLong() * lut.size.toLong() * 3L * 4L

    fun maxSampleError(a: Lut3D, b: Lut3D): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        var worst = 0f
        a.data.indices.forEach { index ->
            val diff = kotlin.math.abs(a.data[index] - b.data.getOrElse(index) { 0f })
            if (diff > worst) worst = diff
        }
        return worst
    }
}
