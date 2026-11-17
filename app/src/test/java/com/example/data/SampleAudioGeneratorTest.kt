package com.example.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The dependency-free library source must be a deterministic, valid PCM WAV. */
class SampleAudioGeneratorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `generated samples are deterministic and expose their real duration`() {
        val first = SampleAudioGenerator.generate("aud_golden", 1_000L)
        val second = SampleAudioGenerator.generate("aud_golden", 1_000L)

        assertEquals(1_000L, first.durationMs)
        assertEquals(first.durationMs, second.durationMs)
        assertTrue(first.samples.contentEquals(second.samples))
        assertEquals(first.waveform, second.waveform)
        assertEquals(SampleAudioGenerator.SAMPLE_RATE, first.samples.size)
        assertTrue(first.waveform.isNotEmpty())
    }

    @Test
    fun `wav writer emits a valid mono 16 bit pcm header`() {
        val generated = SampleAudioGenerator.generate("sfx_pop", 600L)
        val output = temporaryFolder.newFile("pop.wav")

        SampleAudioGenerator.writeWav(output, generated.samples)

        val bytes = output.readBytes()
        assertTrue(bytes.size >= 44)
        assertArrayEquals("RIFF".toByteArray(), bytes.copyOfRange(0, 4))
        assertArrayEquals("WAVE".toByteArray(), bytes.copyOfRange(8, 12))
        assertArrayEquals("fmt ".toByteArray(), bytes.copyOfRange(12, 16))
        assertArrayEquals("data".toByteArray(), bytes.copyOfRange(36, 40))

        assertEquals(1, readLittleEndianShort(bytes, 20))
        assertEquals(1, readLittleEndianShort(bytes, 22))
        assertEquals(SampleAudioGenerator.SAMPLE_RATE, readLittleEndianInt(bytes, 24))
        assertEquals(16, readLittleEndianShort(bytes, 34))
        assertEquals(generated.samples.size * 2, readLittleEndianInt(bytes, 40))
        assertEquals(44L + generated.samples.size * 2L, output.length())
    }

    @Test
    fun `duration is capped before the pcm sample count can overflow`() {
        val generated = SampleAudioGenerator.generate("aud_long", Long.MAX_VALUE)

        assertTrue(generated.durationMs in 1..60_000L)
        assertTrue(generated.samples.isNotEmpty())
        assertTrue(generated.samples.size <= Int.MAX_VALUE)
    }

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun readLittleEndianShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
}
