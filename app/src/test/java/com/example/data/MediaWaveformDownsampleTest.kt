package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The waveform pyramid: peaks-preserving downsampling and interpolated
 * upsampling. The decode itself is device-only; the pyramid maths is pure.
 */
class MediaWaveformDownsampleTest {

    @Test
    fun `downsampling keeps the loudest peak per group`() {
        val fine = listOf(0.1f, 0.9f, 0.2f, 0.3f, 0.05f, 0.7f, 0.4f, 0.6f)
        val coarse = MediaWaveform.downsample(fine, 4)
        assertEquals(4, coarse.size)
        assertEquals(0.9f, coarse[0], 1e-6f) // max(0.1, 0.9)
        assertEquals(0.3f, coarse[1], 1e-6f) // max(0.2, 0.3)
        assertEquals(0.7f, coarse[2], 1e-6f) // max(0.05, 0.7)
        assertEquals(0.6f, coarse[3], 1e-6f) // max(0.4, 0.6)
    }

    @Test
    fun `upsampling interpolates smoothly`() {
        val fine = listOf(0f, 1f)
        val up = MediaWaveform.downsample(fine, 5)
        assertEquals(5, up.size)
        assertEquals(0f, up[0], 1e-6f)
        assertEquals(0.5f, up[2], 1e-6f)
        assertEquals(1f, up[4], 1e-6f)
    }

    @Test
    fun `identity when the bucket count matches`() {
        val fine = listOf(0.2f, 0.5f, 0.8f)
        assertEquals(fine, MediaWaveform.downsample(fine, 3))
    }

    @Test
    fun `downsampled peaks never exceed any source value`() {
        val fine = (1..1200).map { (it % 97) / 97f }
        val coarse = MediaWaveform.downsample(fine, 48)
        assertEquals(48, coarse.size)
        assertTrue(coarse.all { it in 0f..1f })
        // Every coarse bucket must dominate the values it summarises: the
        // global maximum survives.
        assertEquals(fine.maxOrNull()!!, coarse.maxOrNull()!!, 1e-6f)
    }

    @Test
    fun `empty input stays empty`() {
        assertTrue(MediaWaveform.downsample(emptyList(), 48).isEmpty())
    }
}
