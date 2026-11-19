package com.example.data.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The mixer pan processor balances real PCM. The buffers are fed straight into
 * the processor, so the maths is verified without a device or a player.
 */
class PanAudioProcessorTest {

    private fun configure(
        processor: PanAudioProcessor,
        channels: Int = 2,
        encoding: Int = C.ENCODING_PCM_16BIT
    ): PanAudioProcessor {
        processor.configure(AudioProcessor.AudioFormat(44_100, channels, encoding))
        // Media3 stores the configured format and activates it on the flush that
        // precedes the first buffer, exactly like the audio sink does.
        processor.flush()
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

    /** Queues one buffer and drains the output like the audio sink does. */
    private fun feed(processor: PanAudioProcessor, buffer: ByteBuffer): ShortArray {
        processor.queueInput(buffer)
        val output = processor.output
        val shorts = ShortArray(output.remaining() / 2)
        output.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts)
        output.position(output.limit())
        return shorts
    }

    @Test
    fun `a stereo track is balanced to the right`() {
        val processor = configure(PanAudioProcessor(0.5f))

        val out = feed(processor, pcm16(10_000, 10_000, 10_000, 10_000))

        assertEquals(4, out.size)
        assertEquals(5_000, out[0].toInt())
        assertEquals(10_000, out[1].toInt())
        assertEquals(5_000, out[2].toInt())
        assertEquals(10_000, out[3].toInt())
    }

    @Test
    fun `a stereo track is balanced to the left`() {
        val processor = configure(PanAudioProcessor(-0.5f))

        val out = feed(processor, pcm16(10_000, 10_000))

        assertEquals(10_000, out[0].toInt())
        assertEquals(5_000, out[1].toInt())
    }

    @Test
    fun `a centred track is left untouched`() {
        val processor = configure(PanAudioProcessor(0f))

        val out = feed(processor, pcm16(12_345, -6_789))

        assertEquals(12_345, out[0].toInt())
        assertEquals(-6_789, out[1].toInt())
    }

    @Test
    fun `a mono track is spread over both speakers`() {
        val processor = configure(PanAudioProcessor(-1f), channels = 1)

        val out = feed(processor, pcm16(8_000, 4_000))

        // Two output channels per input frame.
        assertEquals(4, out.size)
        assertEquals(8_000, out[0].toInt())
        assertEquals(0, out[1].toInt())
        assertEquals(4_000, out[2].toInt())
        assertEquals(0, out[3].toInt())
    }

    @Test
    fun `float samples are balanced too`() {
        val processor = configure(PanAudioProcessor(0.25f), encoding = C.ENCODING_PCM_FLOAT)

        processor.queueInput(pcmFloat(1f, 1f))
        val output = processor.output
        val floats = FloatArray(output.remaining() / 4)
        output.order(ByteOrder.nativeOrder()).asFloatBuffer().get(floats)

        assertEquals(0.75f, floats[0], 0.0001f)
        assertEquals(1f, floats[1], 0.0001f)
    }

    @Test
    fun `an unreadable encoding is rejected instead of mangling the audio`() {
        val processor = PanAudioProcessor(0.5f)

        val failed = runCatching {
            processor.configure(AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_8BIT))
        }.isFailure

        assertTrue(failed)
    }

    @Test
    fun `the balance never clips the samples`() {
        val processor = configure(PanAudioProcessor(-1f))

        val out = feed(processor, pcm16(Short.MIN_VALUE, Short.MAX_VALUE))

        assertTrue(out.all { it.toInt() in Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt() })
        assertEquals(Short.MIN_VALUE.toInt(), out[0].toInt())
        assertEquals(0, out[1].toInt())
    }
}
