package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline audio DSP kernels for the clip-level audio tools ("Audio effects",
 * "Enhance voice", "Reduce noise").
 *
 * Every kernel is pure Kotlin, stateful across buffer chunks, and operates on
 * interleaved 16-bit PCM in place, so the same code runs in a JVM unit test
 * and inside a Media3 [androidx.media3.common.audio.AudioProcessor] on the
 * device. Channel state is kept per channel, so mono, stereo and 5.1 all work.
 */
object AudioDsp {

    /** Processes interleaved [frames] frames of [samples] in place. */
    fun interface SampleKernel {
        fun process(samples: ShortArray, frames: Int, channels: Int)
    }

    // ------------------------------------------------------------------
    // Biquad building block (RBJ audio EQ cookbook)
    // ------------------------------------------------------------------

    /** One biquad section with per-channel state. */
    class Biquad(b0: Float, b1: Float, b2: Float, a0: Float, a1: Float, a2: Float) {
        private val b = floatArrayOf(b0 / a0, b1 / a0, b2 / a0)
        private val a = floatArrayOf(a1 / a0, a2 / a0)
        private var x1 = FloatArray(0)
        private var x2 = FloatArray(0)
        private var y1 = FloatArray(0)
        private var y2 = FloatArray(0)

        fun process(sample: Float, channel: Int): Float {
            if (channel >= x1.size) {
                // First use or a wider stream: grow all state arrays, keeping
                // the samples already processed for the existing channels.
                fun grow(arr: FloatArray): FloatArray =
                    arr.copyOf(maxOf(arr.size * 2, channel + 1))
                x1 = grow(x1); x2 = grow(x2); y1 = grow(y1); y2 = grow(y2)
            }
            val y = b[0] * sample + b[1] * x1[channel] + b[2] * x2[channel] -
                a[0] * y1[channel] - a[1] * y2[channel]
            x2[channel] = x1[channel]; x1[channel] = sample
            y2[channel] = y1[channel]; y1[channel] = y
            return y
        }

        fun reset() {
            x1 = FloatArray(0); x2 = FloatArray(0); y1 = FloatArray(0); y2 = FloatArray(0)
        }

        companion object {
            /** Butterworth high-pass (Q = 0.707). */
            fun highPass(sampleRate: Int, cutoffHz: Float, q: Float = 0.707f): Biquad {
                val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
                val cosW = cos(w0)
                val alpha = sin(w0) / (2f * q)
                return Biquad(
                    b0 = (1f + cosW) / 2f,
                    b1 = -(1f + cosW),
                    b2 = (1f + cosW) / 2f,
                    a0 = 1f + alpha,
                    a1 = -2f * cosW,
                    a2 = 1f - alpha
                )
            }

            /** Butterworth low-pass (Q = 0.707). */
            fun lowPass(sampleRate: Int, cutoffHz: Float, q: Float = 0.707f): Biquad {
                val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
                val cosW = cos(w0)
                val alpha = sin(w0) / (2f * q)
                return Biquad(
                    b0 = (1f - cosW) / 2f,
                    b1 = 1f - cosW,
                    b2 = (1f - cosW) / 2f,
                    a0 = 1f + alpha,
                    a1 = -2f * cosW,
                    a2 = 1f - alpha
                )
            }

            /** High shelf boost/cut at [gainDb]. */
            fun highShelf(sampleRate: Int, cutoffHz: Float, gainDb: Float, slope: Float = 1f): Biquad {
                val a = 10f.pow(gainDb / 40f)
                val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
                val cosW = cos(w0)
                val alpha = sin(w0) / 2f * sqrt((a + 1f / a) * (1f / slope - 1f) + 2f)
                val t = 2f * sqrt(a) * alpha
                return Biquad(
                    b0 = a * ((a + 1f) + (a - 1f) * cosW + t),
                    b1 = -2f * a * ((a - 1f) + (a + 1f) * cosW),
                    b2 = a * ((a + 1f) + (a - 1f) * cosW - t),
                    a0 = (a + 1f) - (a - 1f) * cosW + t,
                    a1 = 2f * ((a - 1f) - (a + 1f) * cosW),
                    a2 = (a + 1f) - (a - 1f) * cosW - t
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Noise gate ("Reduce noise" - honest basic: envelope gate)
    // ------------------------------------------------------------------

    /**
     * A peak-envelope noise gate. Signals below [thresholdDb] (relative to
     * full scale) are faded towards [floorGain]; attacks are near-instant and
     * the release is slow so speech tails and music decay stay natural.
     */
    class NoiseGate(
        sampleRate: Int,
        thresholdDb: Float = -42f,
        attackMs: Float = 2f,
        releaseMs: Float = 120f,
        holdMs: Float = 60f,
        private val floorGain: Float = 0.06f
    ) : SampleKernel {
        private val threshold = dbToLinear(thresholdDb)
        private val attackCoef = timeCoef(attackMs, sampleRate)
        private val releaseCoef = timeCoef(releaseMs, sampleRate)
        private val holdSamples = (holdMs * sampleRate / 1000f).toInt().coerceAtLeast(1)
        private var envelope = 0f
        private var gain = 1f
        private var openCountdown = 0

        override fun process(samples: ShortArray, frames: Int, channels: Int) {
            for (frame in 0 until frames) {
                var peak = 0f
                for (ch in 0 until channels) {
                    val v = abs(samples[frame * channels + ch] / 32768f)
                    if (v > peak) peak = v
                }
                envelope = if (peak > envelope) attackCoef * (peak - envelope) + envelope
                else releaseCoef * (peak - envelope) + envelope

                val target = if (envelope >= threshold) 1f else floorGain
                if (target >= 1f) {
                    openCountdown = holdSamples
                } else if (openCountdown > 0) {
                    openCountdown--
                }
                // Keep the gate open through the hold window so consonants do
                // not chop; after that ease towards the floor.
                val effectiveTarget = if (target == 1f || openCountdown > 0) 1f else floorGain
                val coef = if (effectiveTarget > gain) attackCoef else releaseCoef
                gain = coef * (effectiveTarget - gain) + gain

                for (ch in 0 until channels) {
                    val i = frame * channels + ch
                    samples[i] = clampToShort(samples[i] * gain)
                }
            }
        }

        private fun timeCoef(ms: Float, sampleRate: Int): Float =
            1f - exp(-1f / (ms * sampleRate / 1000f).coerceAtLeast(1f))
    }

    // ------------------------------------------------------------------
    // Voice enhance EQ
    // ------------------------------------------------------------------

    /**
     * Speech clarity preset: a 90 Hz rumble high-pass plus a +4 dB presence
     * shelf at 3 kHz. Nothing is faked - both are real filters.
     */
    class VoiceEnhance(sampleRate: Int, presenceDb: Float = 4f) : SampleKernel {
        private val highPass = Biquad.highPass(sampleRate, 90f)
        private val presence = Biquad.highShelf(sampleRate, 3000f, presenceDb)

        override fun process(samples: ShortArray, frames: Int, channels: Int) {
            for (i in 0 until frames * channels) {
                val x = samples[i] / 32768f
                val y = presence.process(highPass.process(x, i % channels), i % channels)
                samples[i] = clampToShort(y * 32768f)
            }
        }
    }

    /**
     * Vocal isolation ("Isolate voice"): band-limits the signal to the speech
     * range (180 Hz .. 5.2 kHz) with real Butterworth filters and fades below
     * the gate floor, so music bass and cymbal hiss collapse around the voice.
     * It is an honest EQ-based isolation, not a source-separation model.
     */
    class IsolateVoice(sampleRate: Int) : SampleKernel {
        // Cutoffs are clamped below Nyquist so the filters stay stable even
        // for small sample rates in tests and low-quality pipelines. Two
        // cascaded low-pass biquads give a 4th-order roll-off, so cymbal hiss
        // above the speech band collapses even with the +3 dB presence shelf.
        private val highPass = Biquad.highPass(sampleRate, min(180f, sampleRate * 0.2f))
        private val lowPass = Biquad.lowPass(sampleRate, min(5200f, sampleRate * 0.45f))
        private val lowPass2 = Biquad.lowPass(sampleRate, min(5200f, sampleRate * 0.45f))
        private val presence = Biquad.highShelf(sampleRate, min(2400f, sampleRate * 0.22f), 3f)
        private val gate = NoiseGate(sampleRate, thresholdDb = -38f, holdMs = 90f)

        override fun process(samples: ShortArray, frames: Int, channels: Int) {
            for (i in 0 until frames * channels) {
                val x = samples[i] / 32768f
                val channel = i % channels
                val band = lowPass2.process(lowPass.process(highPass.process(x, channel), channel), channel)
                val y = presence.process(band, channel)
                samples[i] = clampToShort(y * 32768f)
            }
            gate.process(samples, frames, channels)
        }
    }

    // ------------------------------------------------------------------
    // Echo
    // ------------------------------------------------------------------

    /** Classic feedback delay. [intensity] 0..1 scales wet and feedback. */
    class Echo(
        sampleRate: Int,
        private val channels: Int,
        delayMs: Float = 250f,
        feedback: Float = 0.35f,
        private val intensity: Float = 1f
    ) : SampleKernel {
        private val delaySamples = (delayMs * sampleRate / 1000f).toInt().coerceAtLeast(1)
        private val buffer = FloatArray(delaySamples * channels)
        private val wet = 0.45f * intensity.coerceIn(0f, 1f)
        private val fb = feedback.coerceIn(0f, 0.7f) * intensity.coerceIn(0f, 1f)
        private var writeIndex = 0

        override fun process(samples: ShortArray, frames: Int, channels: Int) {
            for (i in 0 until frames * this.channels) {
                val dry = samples[i] / 32768f
                val delayed = buffer[writeIndex]
                buffer[writeIndex] = dry + delayed * fb
                writeIndex = (writeIndex + 1) % buffer.size
                samples[i] = clampToShort((dry + delayed * wet) * 32768f)
            }
        }
    }

    // ------------------------------------------------------------------
    // Reverb (Schroeder)
    // ------------------------------------------------------------------

    /**
     * Schroeder reverb: four parallel comb filters into two series all-pass
     * filters per channel. [room] 0..1 scales decay and wet level.
     */
    class Reverb(
        sampleRate: Int,
        private val channels: Int,
        room: Float = 0.5f
    ) : SampleKernel {
        private val scale = sampleRate / 44100f
        private val combDelays = intArrayOf(1117, 1187, 1277, 1357)
            .map { (it * scale).toInt().coerceAtLeast(4) }
        private val allPassDelays = intArrayOf(225, 556)
            .map { (it * scale).toInt().coerceAtLeast(2) }
        private val combFeedback = 0.72f + 0.18f * room.coerceIn(0f, 1f)
        private val wet = 0.22f + 0.3f * room.coerceIn(0f, 1f)
        private val combs = Array(channels) { ch ->
            Array(combDelays.size) { Comb(combDelays[it] + ch * 13, combFeedback) }
        }
        private val allPass = Array(channels) { ch ->
            Array(allPassDelays.size) { AllPass(allPassDelays[it] + ch * 7, 0.7f) }
        }

        override fun process(samples: ShortArray, frames: Int, channels: Int) {
            for (frame in 0 until frames) {
                for (ch in 0 until this.channels) {
                    val i = frame * this.channels + ch
                    val dry = samples[i] / 32768f
                    var acc = 0f
                    combs[ch].forEach { comb -> acc += comb.process(dry) }
                    acc /= combDelays.size
                    allPass[ch].forEach { ap -> acc = ap.process(acc) }
                    samples[i] = clampToShort((dry + acc * wet) * 32768f)
                }
            }
        }

        private class Comb(val delay: Int, val feedback: Float) {
            private val buffer = FloatArray(delay)
            private var index = 0

            fun process(x: Float): Float {
                val y = buffer[index]
                buffer[index] = x + y * feedback
                index = (index + 1) % delay
                return y
            }
        }

        private class AllPass(val delay: Int, val gain: Float) {
            private val buffer = FloatArray(delay)
            private var index = 0

            fun process(x: Float): Float {
                val delayed = buffer[index]
                val y = -gain * x + delayed
                buffer[index] = x + delayed * gain
                index = (index + 1) % delay
                return y
            }
        }
    }

    // ------------------------------------------------------------------
    // Robot (ring modulation)
    // ------------------------------------------------------------------

    /**
     * Ring modulation with a [carrierHz] sine. [intensity] cross-fades
     * between the dry signal and the modulated one.
     */
    class Robot(
        sampleRate: Int,
        carrierHz: Float = 50f,
        private val intensity: Float = 1f
    ) : SampleKernel {
        private val depth = intensity.coerceIn(0f, 1f)
        private val step = 2f * PI.toFloat() * carrierHz / sampleRate
        private var phase = 0f

        override fun process(samples: ShortArray, frames: Int, channels: Int) {
            for (frame in 0 until frames) {
                val carrier = sin(phase)
                phase = (phase + step) % (2f * PI.toFloat())
                for (ch in 0 until channels) {
                    val i = frame * channels + ch
                    val dry = samples[i] / 32768f
                    val modulated = dry * carrier
                    samples[i] = clampToShort(((1f - depth) * dry + depth * modulated) * 32768f)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

    /**
     * dB with a -999 "minus infinity" sentinel for silence and non-positive
     * input, so callers can display and compare the result without handling
     * NaN or -Infinity.
     */
    fun linearToDb(linear: Float): Float =
        if (linear <= 0f) -999f else 20f * (ln(linear) / ln(10f))

    /** Rounds and clamps a float sample into the 16-bit range. */
    fun clampToShort(value: Float): Short =
        value.toInt().coerceIn(-32768, 32767).toShort()
}
