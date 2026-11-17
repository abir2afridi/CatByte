package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

/**
 * DSP checks for the offline audio tools. Everything runs at a small sample
 * rate so the timing numbers stay readable (10 kHz = 10 samples per ms).
 */
class AudioDspTest {

    private val sr = 10_000

    private fun assertNear(expected: Float, actual: Float, delta: Float) {
        assertTrue("expected $expected ±$delta, got $actual", abs(expected - actual) <= delta)
    }

    private fun sine(hz: Float, samples: Int, amplitude: Float = 0.8f): ShortArray =
        ShortArray(samples) { i ->
            AudioDsp.clampToShort(amplitude * sin(2.0 * Math.PI * hz * i / sr).toFloat() * 32768f)
        }

    private fun amplitude(samples: ShortArray, from: Int = 0, to: Int = samples.size): Float =
        (from until to).maxOf { abs(samples[it].toFloat()) } / 32768f

    // ------------------------------------------------------------------
    // Echo
    // ------------------------------------------------------------------

    @Test
    fun `echo copies the impulse after the delay with scaled amplitude`() {
        val delayMs = 100f
        val echo = AudioDsp.Echo(sr, channels = 1, delayMs = delayMs, feedback = 0.5f, intensity = 1f)
        val samples = ShortArray(4000)
        samples[0] = 10000
        echo.process(samples, samples.size, 1)

        // Dry impulse survives.
        assertNear(10000f, samples[0].toFloat(), 2f)
        // First tap at 100 ms: dry * wet = 10000 * 0.45.
        assertNear(4500f, samples[1000].toFloat(), 60f)
        // Second tap at 200 ms: dry * wet * feedback... the buffer holds
        // dry + delayed*fb, so the second tap is 4500 * 0.5.
        assertNear(2250f, samples[2000].toFloat(), 60f)
        // Silence everywhere else (comb residue of a pure impulse is 0).
        for (i in samples.indices) {
            if (i !in intArrayOf(0, 1000, 2000, 3000)) {
                assertTrue("unexpected sample $i = ${samples[i]}", abs(samples[i].toInt()) <= 2)
            }
        }
    }

    @Test
    fun `echo intensity zero is a no-op`() {
        val echo = AudioDsp.Echo(sr, channels = 1, delayMs = 50f, feedback = 0.5f, intensity = 0f)
        val original = sine(440f, 1000)
        val processed = original.copyOf()
        echo.process(processed, processed.size, 1)
        assertTrue(original.contentEquals(processed))
    }

    // ------------------------------------------------------------------
    // Reverb
    // ------------------------------------------------------------------

    @Test
    fun `reverb extends a burst with a decaying tail`() {
        val reverb = AudioDsp.Reverb(sr, channels = 1, room = 0.6f)
        val samples = ShortArray(6000)
        // 50 ms burst of 1 kHz.
        for (i in 0 until 500) {
            samples[i] = AudioDsp.clampToShort(0.8f * sin(2.0 * Math.PI * 1000 * i / sr).toFloat() * 32768f)
        }
        val burstEnd = 500
        val input = samples.copyOf()
        reverb.process(samples, samples.size, 1)

        // Dry part is untouched in shape (wet adds energy, so compare signs).
        // At the dry sine's zero crossings the wet tail dominates, so only
        // samples where the dry signal is well above the wet level are checked.
        for (i in 0 until burstEnd step 37) {
            if (abs(input[i].toInt()) < 8192) continue
            assertTrue(
                "dry part changed sign at $i",
                samples[i].toInt() * input[i].toInt() > 0 || abs(samples[i].toInt()) < 100
            )
        }
        // Tail after the burst: still audible right after, clearly decayed later.
        val early = amplitude(samples, burstEnd, burstEnd + 300)
        val late = amplitude(samples, burstEnd + 2500, burstEnd + 2800)
        assertTrue("expected reverb tail, early=$early", early > 0.01f)
        assertTrue("tail should decay, early=$early late=$late", early > late * 2f)
        // Bounded and finite.
        assertTrue(amplitude(samples) <= 1.0f)
    }

    @Test
    fun `reverb stereo channels do not cross-contaminate`() {
        val reverb = AudioDsp.Reverb(sr, channels = 2, room = 0.5f)
        // Impulse on the LEFT channel only.
        val samples = ShortArray(4000)
        samples[0] = 16000 // frame 0, channel 0
        reverb.process(samples, samples.size / 2, 2)
        // The right channel stays silent (its combs only saw zeros).
        for (frame in 0 until samples.size / 2) {
            val right = samples[frame * 2 + 1]
            assertTrue("right channel contaminated at frame $frame: $right", abs(right.toInt()) <= 2)
        }
    }

    // ------------------------------------------------------------------
    // Robot
    // ------------------------------------------------------------------

    @Test
    fun `robot ring modulation keeps energy bounded and changes the signal`() {
        val robot = AudioDsp.Robot(sr, carrierHz = 50f, intensity = 1f)
        val samples = sine(200f, 5000)
        val before = samples.copyOf()
        robot.process(samples, samples.size, 1)
        assertTrue("expected the signal to change", !before.contentEquals(samples))
        assertTrue("output must stay in range", amplitude(samples) <= 1.0f)
        // The input sine is near zero for only ~2% of samples; multiplying by
        // a 50 Hz carrier adds dips wherever EITHER factor is small, so the
        // output must dip far more often while keeping strong peaks.
        val nearZero = (0 until samples.size).count { abs(samples[it].toInt()) < 1000 }
        val inputNearZero = (0 until before.size).count { abs(before[it].toInt()) < 1000 }
        // The dip count at this exact rate lands on a round number, so the
        // requirement stays relative: far more dips than the input itself.
        assertTrue(
            "expected modulation dips, got $nearZero vs input $inputNearZero",
            nearZero > inputNearZero * 2
        )
        assertTrue("expected strong peaks to survive", amplitude(samples) > 0.2f)
    }

    @Test
    fun `robot with zero intensity is a no-op`() {
        val robot = AudioDsp.Robot(sr, carrierHz = 60f, intensity = 0f)
        val original = sine(300f, 900)
        val processed = original.copyOf()
        robot.process(processed, processed.size, 1)
        assertTrue(original.contentEquals(processed))
    }

    // ------------------------------------------------------------------
    // Noise gate
    // ------------------------------------------------------------------

    @Test
    fun `gate passes loud audio almost unchanged and ducks silence`() {
        val gate = AudioDsp.NoiseGate(sr, thresholdDb = -30f, attackMs = 1f, releaseMs = 50f, holdMs = 10f)
        // 100 ms loud, then 100 ms very quiet hum, then 100 ms loud.
        val samples = ShortArray(3000)
        for (i in 0 until 1000) samples[i] = AudioDsp.clampToShort(0.6f * sin(2.0 * Math.PI * 200 * i / sr).toFloat() * 32768f)
        for (i in 1000 until 2000) samples[i] = AudioDsp.clampToShort(0.01f * sin(2.0 * Math.PI * 200 * i / sr).toFloat() * 32768f)
        for (i in 2000 until 3000) samples[i] = AudioDsp.clampToShort(0.6f * sin(2.0 * Math.PI * 200 * i / sr).toFloat() * 32768f)
        gate.process(samples, samples.size, 1)

        val loudAmplitude = 0.6f // amplitude() is normalised to 0..1
        // Loud start passes (attack settles within a few ms).
        val startPass = amplitude(samples, 100, 900)
        assertTrue("loud start was ducked: $startPass", startPass > loudAmplitude * 0.8f)
        // Quiet middle is ducked to near the floor.
        val middle = amplitude(samples, 1400, 1900)
        assertTrue("quiet middle not ducked: $middle", middle < loudAmplitude * 0.1f)
        // Loud end re-opens.
        val endPass = amplitude(samples, 2100, 2900)
        assertTrue("loud end was ducked: $endPass", endPass > loudAmplitude * 0.8f)
    }

    @Test
    fun `gate holds open through short dips`() {
        val gate = AudioDsp.NoiseGate(sr, thresholdDb = -30f, attackMs = 1f, releaseMs = 100f, holdMs = 30f)
        // Loud 50 ms, 10 ms dip, loud 50 ms: the 30 ms hold must bridge it.
        val samples = ShortArray(1100)
        fun loud(i: Int) = AudioDsp.clampToShort(0.5f * sin(2.0 * Math.PI * 200 * i / sr).toFloat() * 32768f)
        for (i in 0 until 500) samples[i] = loud(i)
        for (i in 500 until 600) samples[i] = 0
        for (i in 600 until 1100) samples[i] = loud(i)
        gate.process(samples, samples.size, 1)

        val loudAmp = 0.5f // amplitude() is normalised to 0..1
        val duringDip = amplitude(samples, 520, 590)
        // During the dip there is no signal to pass anyway; what matters is
        // that the sine right after the dip is NOT released.
        val afterDip = amplitude(samples, 610, 1000)
        assertTrue("gate released through the dip: after=$afterDip", afterDip > loudAmp * 0.7f)
        assertTrue(duringDip < loudAmp * 0.2f)
    }

    // ------------------------------------------------------------------
    // Voice enhance EQ
    // ------------------------------------------------------------------

    @Test
    fun `voice enhance cuts rumble and boosts presence`() {
        val cycles = 30

        fun measureAt(freq: Float): Float {
            val kernel = AudioDsp.VoiceEnhance(sr, presenceDb = 4f)
            var peakIn = 0f
            var peakOut = 0f
            val chunk = ShortArray(sr / 10)
            var phase = 0.0
            repeat(cycles) {
                for (i in chunk.indices) {
                    val v = (0.5 * sin(2.0 * Math.PI * freq * phase / sr)).toFloat()
                    chunk[i] = AudioDsp.clampToShort(v * 32768f)
                    if (abs(v) > peakIn) peakIn = abs(v)
                    phase += 1.0
                }
                kernel.process(chunk, chunk.size, 1)
                val peak = amplitude(chunk)
                if (peak > peakOut) peakOut = peak
            }
            return peakOut / peakIn
        }

        val lowRatio = measureAt(40f)    // below the 90 Hz high-pass
        val highRatio = measureAt(4000f) // in the presence shelf
        assertTrue("rumble not cut: ratio=$lowRatio", lowRatio < 0.7f)
        assertTrue("presence not boosted: ratio=$highRatio", highRatio > 1.05f)
    }

    // ------------------------------------------------------------------
    // Biquad
    // ------------------------------------------------------------------

    @Test
    fun `high pass biquad rejects dc and passes high frequencies`() {
        val hpf = AudioDsp.Biquad.highPass(sr, 90f)
        // DC input: output must decay towards zero.
        var last = 1f
        repeat(2000) { last = hpf.process(1f, 0) }
        assertTrue("dc leaked through: $last", abs(last) < 0.05f)

        // 4 kHz at 10 kHz sample rate is near Nyquist: must pass largely.
        val hpf2 = AudioDsp.Biquad.highPass(sr, 90f)
        var peak = 0f
        for (i in 0 until 2000) {
            val x = sin(2.0 * Math.PI * 4000 * i / sr).toFloat()
            val y = hpf2.process(x, 0)
            if (i > 1000 && abs(y) > peak) peak = abs(y)
        }
        assertTrue("high frequency attenuated: $peak", peak > 0.7f)
    }

    @Test
    fun `biquad state stays per channel`() {
        val biquad = AudioDsp.Biquad.highPass(sr, 90f)
        // Feed channel 0 with DC to charge its state, keep channel 1 idle.
        repeat(500) { biquad.process(1f, 0) }
        val fresh = AudioDsp.Biquad.highPass(sr, 90f)
        val a = biquad.process(0.5f, 1)
        val b = fresh.process(0.5f, 1)
        assertNear(b, a, 1e-6f)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @Test
    fun `db conversions round trip`() {
        assertNear(1f, AudioDsp.dbToLinear(0f), 1e-6f)
        assertNear(0.5f, AudioDsp.dbToLinear(-6.0206f), 1e-4f)
        assertNear(-6.0206f, AudioDsp.linearToDb(0.5f), 1e-3f)
        assertEquals(-999f, AudioDsp.linearToDb(0f), 1e-6f)
    }

    @Test
    fun `clamp to short saturates instead of wrapping`() {
        assertEquals(32767, AudioDsp.clampToShort(40000f).toInt())
        assertEquals(-32768, AudioDsp.clampToShort(-40000f).toInt())
        assertEquals(1000, AudioDsp.clampToShort(1000.4f).toInt())
    }

    // ------------------------------------------------------------------
    // Isolate voice
    // ------------------------------------------------------------------

    @Test
    fun `isolate voice collapses bass far more than speech band tones`() {
        // Real export rate, so every test tone is below Nyquist.
        val hsr = 44_100
        val samplesIn = 44_100
        val bass = ShortArray(samplesIn) { i ->
            AudioDsp.clampToShort(0.8f * sin(2.0 * Math.PI * 60.0 * i / hsr).toFloat() * 32768f)
        }
        val voice = ShortArray(samplesIn) { i ->
            AudioDsp.clampToShort(0.8f * sin(2.0 * Math.PI * 900.0 * i / hsr).toFloat() * 32768f)
        }

        val isolate = AudioDsp.IsolateVoice(hsr)
        isolate.process(bass, bass.size, 1)
        val isolate2 = AudioDsp.IsolateVoice(hsr)
        isolate2.process(voice, voice.size, 1)

        val bassOut = amplitude(bass, samplesIn / 2)
        val voiceOut = amplitude(voice, samplesIn / 2)
        // The 60 Hz rumble is heavily rejected; a speech-band tone survives.
        assertTrue("bass $bassOut should be well below the voice", bassOut < voiceOut * 0.25f)
        assertTrue("voice $voiceOut should still be audible", voiceOut > 0.2f)
    }

    @Test
    fun `isolate voice rejects content above the speech band`() {
        val hsr = 44_100
        val samplesIn = 44_100
        val cymbal = ShortArray(samplesIn) { i ->
            AudioDsp.clampToShort(0.8f * sin(2.0 * Math.PI * 8_000.0 * i / hsr).toFloat() * 32768f)
        }
        AudioDsp.IsolateVoice(hsr).process(cymbal, cymbal.size, 1)
        // A 2nd-order roll-off at 5.2 kHz gives ~-9 dB at 8 kHz; the +3 dB
        // presence shelf partially offsets it, so expect roughly halved.
        val out = amplitude(cymbal, samplesIn / 2)
        assertTrue("8 kHz should be attenuated, got $out", out < 0.45f)
    }
}
