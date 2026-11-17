package com.example.engine

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Linear-interpolation resampler with mono downmix, for feeding speech
 * recognisers (which want 16 kHz mono) from whatever rate the decoder gave.
 *
 * Pure Kotlin so the tap positions, rounding and length invariants are unit
 * tested without Android audio APIs.
 */
object Resampler {

    /**
     * Converts interleaved [input] (16-bit PCM, [inChannels]) at [inRate] to
     * mono at [outRate].
     *
     * @return a mono ShortArray of exactly `ceil(duration * outRate)` samples.
     */
    fun toMonoAtRate(
        input: ShortArray,
        inChannels: Int,
        inRate: Int,
        outRate: Int
    ): ShortArray {
        require(inChannels >= 1) { "invalid channel count $inChannels" }
        require(inRate > 0 && outRate > 0) { "invalid rates $inRate -> $outRate" }
        if (input.isEmpty()) return ShortArray(0)

        val frames = input.size / inChannels
        val ratio = outRate.toDouble() / inRate
        val outFrames = (frames * ratio).roundToInt().coerceAtLeast(0)
        if (outFrames == 0) return ShortArray(0)

        val out = ShortArray(outFrames)
        val step = 1.0 / ratio // input frames per output sample
        var sourcePos = 0.0
        for (i in 0 until outFrames) {
            val mono = downmixedSampleAt(input, inChannels, frames, sourcePos)
            out[i] = mono
            sourcePos += step
        }
        return out
    }

    /**
     * The mono value at a fractional [framePos]: linear interpolation between
     * the two neighbouring frames, each downmixed by averaging its channels.
     */
    private fun downmixedSampleAt(
        input: ShortArray,
        channels: Int,
        frames: Int,
        framePos: Double
    ): Short {
        val index = framePos.toInt().coerceIn(0, frames - 1)
        val nextIndex = (index + 1).coerceAtMost(frames - 1)
        val fraction = (framePos - index).toFloat().coerceIn(0f, 1f)

        val a = monoAt(input, channels, index)
        val b = monoAt(input, channels, nextIndex)
        val value = a + (b - a) * fraction
        return value.roundToInt().coerceIn(-32768, 32767).toShort()
    }

    private fun monoAt(input: ShortArray, channels: Int, frame: Int): Float {
        var sum = 0f
        val base = frame * channels
        for (ch in 0 until channels) {
            sum += input[base + ch]
        }
        return sum / channels
    }

    /** True when the two rates are close enough to skip resampling. */
    fun isCloseEnough(rateA: Int, rateB: Int): Boolean = abs(rateA - rateB) <= 1
}
