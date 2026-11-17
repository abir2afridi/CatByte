package com.example.data.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.example.model.AudioEffectSettings
import com.example.model.VoiceEffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * The Media3 adapter for [AudioEffectSettings]: a no-op must build no
 * processors at all, and the active chain must stay in the documented order
 * (gate -> voice EQ -> creative kernel -> pitch).
 */
@UnstableApi
class AudioEffectProcessorsTest {

    @Test
    fun `no-op settings build no processors`() {
        assertTrue(AudioEffectProcessors.forSettings(AudioEffectSettings()).isEmpty())
        // Intensity alone does nothing without a kernel preset.
        assertTrue(
            AudioEffectProcessors
                .forSettings(AudioEffectSettings(intensity = 1f))
                .isEmpty()
        )
        // Pitch-only presets build exactly one Sonic processor.
        val deep = AudioEffectProcessors.forSettings(AudioEffectSettings(type = VoiceEffectType.DEEP))
        assertEquals(1, deep.size)
    }

    @Test
    fun `active chain follows the documented order`() {
        val echoChain = AudioEffectSettings(
            type = VoiceEffectType.ECHO,
            intensity = 0.8f,
            enhanceVoice = true,
            reduceNoise = true
        )

        // Noise gate + voice enhance + echo kernel. Echo keeps the pitch, so
        // no Sonic processor joins the chain.
        val processors = AudioEffectProcessors.forSettings(echoChain)
        assertEquals(3, processors.size)
        assertTrue(processors[0] is AudioEffectProcessors.KernelAudioProcessor)
        assertTrue(processors[1] is AudioEffectProcessors.KernelAudioProcessor)
        assertTrue(processors[2] is AudioEffectProcessors.KernelAudioProcessor)

        // Chipmunk has no kernel of its own: gate + voice EQ, then Sonic
        // pitch appends last.
        val pitched = AudioEffectSettings(
            type = VoiceEffectType.CHIPMUNK,
            intensity = 0.8f,
            enhanceVoice = true,
            reduceNoise = true
        )
        val pitchedChain = AudioEffectProcessors.forSettings(pitched)
        assertEquals(3, pitchedChain.size)
        assertEquals("SonicAudioProcessor", pitchedChain[2]::class.java.simpleName)
    }

    @Test
    fun `kernel processor passes configured pcm 16 bit through`() {
        val processor = AudioEffectProcessors.KernelAudioProcessor { sampleRate, channels ->
            com.example.engine.AudioDsp.Echo(sampleRate, channels, intensity = 0.5f)
        }
        val format = AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
        assertEquals(format, processor.configure(format))

        // A silent frame must stay silent (echo adds no noise).
        val input = ShortArray(960) // 10 ms stereo
        val buffer = ByteBuffer.allocateDirect(input.size * 2)
            .order(java.nio.ByteOrder.nativeOrder())
        buffer.asShortBuffer().put(input)
        processor.queueInput(buffer)
        val out = processor.getOutput()
        val outBuffer = out.asShortBuffer()
        for (i in 0 until outBuffer.remaining()) {
            assertEquals(0f, outBuffer.get(i).toFloat(), 2f)
        }
    }
}
