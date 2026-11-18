package com.example.data.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The preview peak meter taps real PCM. These tests feed buffers straight into
 * the processor, so the maths is verified without a device or a player.
 */
class PlaybackAudioMeterTest {

    private fun processor(): PlaybackAudioMeter.PeakProcessor =
        PlaybackAudioMeter.PeakProcessor(publishIntervalNanos = 0L)

    private fun configure(
        processor: PlaybackAudioMeter.PeakProcessor,
        channels: Int = 2,
        encoding: Int = C.ENCODING_PCM_16BIT
    ): PlaybackAudioMeter.PeakProcessor {
        processor.configure(AudioProcessor.AudioFormat(44_100, channels, encoding))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        return processor
    }

    private fun pcm16(vararg samples: Short): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        buffer.asShortBuffer().put(samples)
        buffer.position(0)
        buffer.limit(samples.size * 2)
        return buffer
    }

    private fun pcmFloat(vararg samples: Float): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(samples)
        buffer.position(0)
        buffer.limit(samples.size * 4)
        return buffer
    }

    /** Queues one buffer and drains the pass-through output like the sink does. */
    private fun feed(processor: PlaybackAudioMeter.PeakProcessor, buffer: ByteBuffer): ByteBuffer {
        processor.queueInput(buffer)
        val output = processor.output
        val copy = ByteBuffer.allocate(output.remaining())
        copy.put(output)
        copy.flip()
        output.position(output.limit())
        return copy
    }

    @Test
    fun `stereo peaks are reported per channel`() {
        val processor = configure(processor())
        PlaybackAudioMeter.reset()

        feed(
            processor,
            pcm16(
                16_384, 8_192,   // L = 0.5, R = 0.25
                3_276, 32_767    // L = 0.1, R = ~1.0
            )
        )

        val levels = PlaybackAudioMeter.levels.value
        assertEquals(0.5f, levels.left, 0.0001f)
        assertEquals(32_767f / 32_768f, levels.right, 0.0001f)
        assertEquals(levels.right, levels.peak, 0.0001f)
    }

    @Test
    fun `a mono stream drives both bars`() {
        val processor = configure(processor(), channels = 1)
        PlaybackAudioMeter.reset()

        feed(processor, pcm16(0, -32_768, 1_000))

        val levels = PlaybackAudioMeter.levels.value
        assertEquals(1f, levels.left, 0.0001f)
        assertEquals(1f, levels.right, 0.0001f)
    }

    @Test
    fun `float PCM is measured too`() {
        val processor = configure(processor(), encoding = C.ENCODING_PCM_FLOAT)
        PlaybackAudioMeter.reset()

        feed(processor, pcmFloat(-0.75f, 0.25f))

        val levels = PlaybackAudioMeter.levels.value
        assertEquals(0.75f, levels.left, 0.0001f)
        assertEquals(0.25f, levels.right, 0.0001f)
    }

    @Test
    fun `silence drops the meter to zero`() {
        val processor = configure(processor())
        PlaybackAudioMeter.reset()

        feed(processor, pcm16(20_000, -20_000))
        assertTrue(PlaybackAudioMeter.levels.value.peak > 0.5f)

        feed(processor, pcm16(0, 0))
        assertEquals(0f, PlaybackAudioMeter.levels.value.peak, 0.0001f)
        assertTrue(PlaybackAudioMeter.levels.value.isSilent)
    }

    @Test
    fun `a flush silences the meter without changing the audio`() {
        val processor = configure(processor())
        PlaybackAudioMeter.reset()

        feed(processor, pcm16(15_000, 15_000))
        assertTrue(PlaybackAudioMeter.levels.value.peak > 0f)

        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        assertEquals(0f, PlaybackAudioMeter.levels.value.peak, 0.0001f)
    }

    @Test
    fun `the sampled audio passes through byte for byte`() {
        val processor = configure(processor())
        val input = shortArrayOf(1_000, -2_000, 3_000, -32_768)

        val output = feed(processor, pcm16(*input))
        val passed = ShortArray(input.size)
        output.order(ByteOrder.nativeOrder()).asShortBuffer().get(passed)

        assertTrue(input.contentEquals(passed))
    }

    @Test
    fun `an unreadable encoding is passed through instead of failing the sink`() {
        val processor = configure(processor(), channels = 1, encoding = C.ENCODING_PCM_8BIT)
        PlaybackAudioMeter.reset()

        val input = ByteBuffer.allocateDirect(4).put(byteArrayOf(1, 2, 3, 4))
        input.flip()
        val output = feed(processor, input)

        assertEquals(4, output.remaining())
        assertEquals(0f, PlaybackAudioMeter.levels.value.peak, 0.0001f)
    }
}
