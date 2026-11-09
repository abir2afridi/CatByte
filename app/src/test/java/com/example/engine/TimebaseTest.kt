package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Timing is the contract between preview, timeline and export. These tests pin
 * the 1001-based rates, because that is where millisecond maths used to drift.
 */
class TimebaseTest {

    @Test
    fun `rational normalises through gcd`() {
        assertEquals(Rational(25), Rational.of(50, 2))
        assertEquals(Rational(60_000, 1_001), Rational.of(60_000, 1_001))
        assertEquals(Rational(30), Rational.of(60, 2))
    }

    @Test
    fun `frame duration keeps the 1001 denominator`() {
        assertEquals(33_366L, Rational.FPS_2997.frameDurationUs().value)
        assertEquals(33_333L, Rational.FPS_30.frameDurationUs().value)
    }

    @Test
    fun `twenty nine ninety seven frames do not collapse into milliseconds`() {
        assertEquals(1_000L, TimeUs.fromFrames(30, Rational.FPS_30).toMs())
        assertEquals(1_001L, TimeUs.fromFrames(30, Rational.FPS_2997).toMs())
    }

    @Test
    fun `long timelines round trip every frame`() {
        val fps = Rational.FPS_2997
        val frames = 53_946L // 30 minutes at 29.97
        val time = TimeUs.fromFrames(frames, fps)
        assertEquals(frames, time.toFrame(fps))
        assertEquals(time, time.snapToFrame(fps))
    }

    @Test
    fun `snap rounds down up and nearest`() {
        val slightlyLate = TimeUs(500_001L)
        assertEquals(500_000L, slightlyLate.snapToFrame(Rational.FPS_30, FrameRounding.DOWN).value)
        assertEquals(533_333L, slightlyLate.snapToFrame(Rational.FPS_30, FrameRounding.UP).value)
        assertEquals(500_000L, slightlyLate.snapToFrame(Rational.FPS_30, FrameRounding.NEAREST).value)

        val overHalf = TimeUs(520_000L)
        assertEquals(533_333L, overHalf.snapToFrame(Rational.FPS_30, FrameRounding.NEAREST).value)
    }

    @Test
    fun `timecode formats hours and frames`() {
        assertEquals("00:00:00:05", Timecode.format(TimeUs.fromFrames(5, Rational.FPS_30), Rational.FPS_30))
        assertEquals("01:00:00:00", Timecode.format(TimeUs.fromFrames(3_600L * 30L, Rational.FPS_30), Rational.FPS_30))
        assertEquals("01:05", Timecode.short(TimeUs.fromMs(65_000L), Rational.FPS_30))
    }

    @Test
    fun `timecode parses back to the same instant`() {
        val parsed = Timecode.parse("00:01:02:15", Rational.FPS_30)
        assertEquals(62_500_000L, parsed?.value)
        assertEquals(TimeUs.fromFrames(5, Rational.FPS_30), Timecode.parse("00:00:00:05", Rational.FPS_30))
        assertNull(Timecode.parse("not a timecode", Rational.FPS_30))
        assertNull(Timecode.parse("00:00:00:99", Rational.FPS_30))
    }

    @Test
    fun `frame rate ladder never exceeds the device`() {
        assertEquals(Rational.FPS_30, FrameRateLadder.resolve(Rational.FPS_60, setOf(30)))
        // Never upscales: a 30 fps project stays 30 on a 60 fps capable device.
        assertEquals(Rational.FPS_30, FrameRateLadder.resolve(Rational.FPS_30, setOf(60)))
        assertEquals(Rational.FPS_24, FrameRateLadder.resolve(Rational.FPS_60, setOf(24)))
    }

    @Test
    fun `arithmetic stays exact under scaling`() {
        val half = TimeUs.fromMs(2_000L) * 0.5
        assertEquals(1_000_000L, half.value)
        assertTrue(TimeUs.fromMs(1_500L) > TimeUs.fromMs(1_000L))
        assertEquals(TimeUs(1_000L), TimeUs.max(TimeUs(1_000L), TimeUs(10L)))
    }
}
