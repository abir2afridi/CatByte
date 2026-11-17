package com.example.data.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.example.engine.AudioDsp
import com.example.model.AudioEffectSettings
import java.nio.ByteBuffer

/**
 * Media3 audio processors for [AudioEffectSettings].
 *
 * The DSP itself lives in [AudioDsp] (pure Kotlin, JVM-testable); these
 * adapters only move bytes between Media3 ByteBuffers and the kernels, so the
 * exact same maths runs in a unit test, in the CompositionPlayer preview and
 * in the exported file.
 */
@UnstableApi
object AudioEffectProcessors {

    /**
     * The full offline chain for one clip, in a fixed, sensible order:
     * noise gate → voice EQ → creative kernel → pitch. Returns an empty list
     * for a no-op so callers never pay for inactive processors.
     */
    fun forSettings(settings: AudioEffectSettings): List<AudioProcessor> {
        if (settings.isNoOp) return emptyList()
        val processors = mutableListOf<AudioProcessor>()
        if (settings.reduceNoise) {
            processors += KernelAudioProcessor { sampleRate, _ ->
                AudioDsp.NoiseGate(sampleRate)
            }
        }
        if (settings.isolateVoice) {
            processors += KernelAudioProcessor { sampleRate, _ ->
                AudioDsp.IsolateVoice(sampleRate)
            }
        }
        if (settings.enhanceVoice) {
            processors += KernelAudioProcessor { sampleRate, _ ->
                AudioDsp.VoiceEnhance(sampleRate)
            }
        }
        when (settings.type) {
            com.example.model.VoiceEffectType.ECHO -> processors +=
                KernelAudioProcessor { sampleRate, channels ->
                    AudioDsp.Echo(sampleRate, channels, intensity = settings.intensity)
                }
            com.example.model.VoiceEffectType.REVERB -> processors +=
                KernelAudioProcessor { sampleRate, channels ->
                    AudioDsp.Reverb(sampleRate, channels, room = settings.intensity)
                }
            com.example.model.VoiceEffectType.ROBOT -> processors +=
                KernelAudioProcessor { sampleRate, _ ->
                    AudioDsp.Robot(sampleRate, intensity = settings.intensity)
                }
            else -> Unit
        }
        if (settings.type.pitchFactor != 1f) {
            processors += SonicAudioProcessor().apply { setPitch(settings.type.pitchFactor) }
        }
        return processors
    }

    /** Adapts one [AudioDsp.SampleKernel] to the Media3 processor contract. */
    class KernelAudioProcessor(
        private val creator: (sampleRate: Int, channels: Int) -> AudioDsp.SampleKernel
    ) : BaseAudioProcessor() {

        private var kernel: AudioDsp.SampleKernel? = null
        private var scratch = ShortArray(0)

        override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
                throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }
            kernel = creator(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return
            val channels = inputAudioFormat.channelCount
            val frames = remaining / (2 * channels)
            if (frames == 0) {
                // A trailing partial frame: pass it through untouched.
                val buffer = replaceOutputBuffer(remaining)
                buffer.put(inputBuffer)
                buffer.flip()
                return
            }
            val bytes = frames * 2 * channels
            if (scratch.size < frames * channels) {
                scratch = ShortArray(frames * channels)
            }
            // Media3 hands out (and expects back) native-order buffers.
            inputBuffer.asShortBuffer().get(scratch, 0, frames * channels)
            kernel?.process(scratch, frames, channels)
            val buffer = replaceOutputBuffer(bytes)
            buffer.asShortBuffer().put(scratch, 0, frames * channels)
            buffer.position(buffer.position() + bytes)
            buffer.flip()
            inputBuffer.position(inputBuffer.position() + bytes)
        }
    }
}
