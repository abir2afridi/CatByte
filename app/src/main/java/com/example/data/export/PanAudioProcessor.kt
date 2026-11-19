package com.example.data.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import com.example.engine.StereoPan
import java.nio.ByteBuffer

/**
 * Balances one audio track in the stereo field.
 *
 * A stereo source keeps its two channels and the side the track moves away
 * from fades out. A mono source is spread over both channels with the same
 * gains, so a voice-over can be moved to one side too. The gains never exceed
 * one, so the processor cannot make a track louder or clip the mix.
 *
 * The processor sits in the shared composition chain, which means the preview
 * and the exported file balance the track in exactly the same way.
 */
class PanAudioProcessor(pan: Float) : BaseAudioProcessor() {

    private val leftGain = StereoPan.gains(pan).left
    private val rightGain = StereoPan.gains(pan).right
    private var inputChannels = 2
    private var outputChannels = 2

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputChannels = inputAudioFormat.channelCount.coerceAtLeast(1)
        // A mono source becomes stereo so it can be placed in the mix.
        outputChannels = if (inputChannels == 1) 2 else inputChannels
        return AudioFormat(
            inputAudioFormat.sampleRate,
            outputChannels,
            inputAudioFormat.encoding
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> queueFloat(inputBuffer)
            else -> queue16(inputBuffer)
        }
    }

    private fun queue16(inputBuffer: ByteBuffer) {
        val bytesPerSample = 2
        val frames = inputBuffer.remaining() / (bytesPerSample * inputChannels)
        if (frames == 0) {
            // A trailing partial frame cannot be balanced; pass it through.
            val buffer = replaceOutputBuffer(inputBuffer.remaining())
            buffer.put(inputBuffer)
            buffer.flip()
            return
        }
        val inputBytes = frames * bytesPerSample * inputChannels
        val outputBytes = frames * bytesPerSample * outputChannels
        // duplicate() resets the order, so keep the order of the incoming data.
        val input = inputBuffer.duplicate().order(inputBuffer.order())
        val samples = input.asShortBuffer()
        val output = replaceOutputBuffer(outputBytes)
        val outSamples = output.asShortBuffer()
        for (frame in 0 until frames) {
            if (inputChannels == 1) {
                val mono = samples.get()
                outSamples.put(scale(mono, leftGain))
                outSamples.put(scale(mono, rightGain))
            } else if (inputChannels == 2) {
                val left = samples.get()
                val right = samples.get()
                outSamples.put(scale(left, leftGain))
                outSamples.put(scale(right, rightGain))
            } else {
                for (channel in 0 until inputChannels) {
                    val value = samples.get()
                    val gain = when (channel) {
                        0 -> leftGain
                        1 -> rightGain
                        else -> 1f
                    }
                    outSamples.put(scale(value, gain))
                }
            }
        }
        output.position(outputBytes)
        output.flip()
        inputBuffer.position(inputBuffer.position() + inputBytes)
    }

    private fun queueFloat(inputBuffer: ByteBuffer) {
        val bytesPerSample = 4
        val frames = inputBuffer.remaining() / (bytesPerSample * inputChannels)
        if (frames == 0) {
            val buffer = replaceOutputBuffer(inputBuffer.remaining())
            buffer.put(inputBuffer)
            buffer.flip()
            return
        }
        val inputBytes = frames * bytesPerSample * inputChannels
        val outputBytes = frames * bytesPerSample * outputChannels
        val input = inputBuffer.duplicate().order(inputBuffer.order())
        val samples = input.asFloatBuffer()
        val output = replaceOutputBuffer(outputBytes)
        val outSamples = output.asFloatBuffer()
        for (frame in 0 until frames) {
            if (inputChannels == 1) {
                val mono = samples.get()
                outSamples.put(mono * leftGain)
                outSamples.put(mono * rightGain)
            } else if (inputChannels == 2) {
                val left = samples.get()
                val right = samples.get()
                outSamples.put(left * leftGain)
                outSamples.put(right * rightGain)
            } else {
                for (channel in 0 until inputChannels) {
                    val value = samples.get()
                    val gain = when (channel) {
                        0 -> leftGain
                        1 -> rightGain
                        else -> 1f
                    }
                    outSamples.put(value * gain)
                }
            }
        }
        output.position(outputBytes)
        output.flip()
        inputBuffer.position(inputBuffer.position() + inputBytes)
    }

    private fun scale(sample: Short, gain: Float): Short {
        val value = (sample.toInt() * gain).toInt()
        return value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}
