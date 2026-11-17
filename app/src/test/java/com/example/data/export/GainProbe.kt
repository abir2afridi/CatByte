package com.example.data.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.GainProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Renders full-scale PCM through a gain provider so a test can read the
 * resulting gain curve instead of trusting the provider's internals.
 *
 * Media3's [GainProcessor.gainProvider] is private, so the only way to prove
 * that a fade or a duck lands at the right moment is to push samples through
 * the real processor and measure the output.
 */
internal object GainProbe {

    const val SAMPLE_RATE = 48_000

    /** How long a single render may take before the test calls it a hang. */
    private const val RENDER_TIMEOUT_MS = 10_000L

    /** Renders through a fresh processor around [provider]. */
    fun render(
        provider: GainProcessor.GainProvider,
        seconds: Int,
        sampleRate: Int = SAMPLE_RATE
    ): FloatArray = render(GainProcessor(provider), seconds, sampleRate)

    /**
     * Renders through an existing processor (for example one from a plan).
     *
     * The render runs on its own thread: Media3 copies unity samples in bulk
     * and asks the provider where the unity region ends, so a provider that
     * reports the current position would loop forever. The watchdog turns
     * that into a failing test instead of a hanging build.
     */
    fun render(
        processor: GainProcessor,
        seconds: Int,
        sampleRate: Int = SAMPLE_RATE
    ): FloatArray {
        var values: FloatArray? = null
        var failure: Throwable? = null
        val worker = Thread {
            try {
                values = renderBlocking(processor, seconds, sampleRate)
            } catch (error: Throwable) {
                failure = error
            }
        }
        worker.isDaemon = true
        worker.start()
        worker.join(RENDER_TIMEOUT_MS)
        if (worker.isAlive) {
            throw AssertionError("The gain processor never finished rendering (endless unity region?)")
        }
        failure?.let { throw it }
        return values ?: FloatArray(0)
    }

    private fun renderBlocking(
        processor: GainProcessor,
        seconds: Int,
        sampleRate: Int
    ): FloatArray {
        processor.configure(
            AudioProcessor.AudioFormat(
                sampleRate,
                /* channelCount = */ 1,
                C.ENCODING_PCM_FLOAT
            )
        )
        // The processor only accepts samples after it has been flushed with
        // the configured format.
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        val frameCount = sampleRate * seconds
        val input = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        repeat(frameCount) { input.putFloat(1f) }
        input.flip()
        processor.queueInput(input)
        val output = processor.getOutput() ?: return FloatArray(0)
        val floats = output.order(ByteOrder.nativeOrder()).asFloatBuffer()
        val values = FloatArray(floats.remaining())
        floats.get(values)
        return values
    }

    /** The sample index of [seconds] into a rendered buffer. */
    fun sampleAt(seconds: Float, sampleRate: Int = SAMPLE_RATE): Int =
        (sampleRate * seconds).toInt()
}
