package com.example.data

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates the built-in audio library as real mono PCM WAV files.
 *
 * The app ships no copyrighted music payloads. Instead, every library entry
 * is synthesised locally from deterministic tones/noise, written to app
 * storage and then treated like any other imported audio file. This keeps the
 * library previewable and exportable without pretending that a null URI is a
 * playable track.
 */
object SampleAudioGenerator {

    const val SAMPLE_RATE = 44_100
    private const val BITS_PER_SAMPLE = 16
    private const val CHANNEL_COUNT = 1
    private const val MIN_GENERATED_DURATION_MS = 100L
    private const val MAX_GENERATED_DURATION_MS = 60_000L

    data class GeneratedAudio(
        val samples: ShortArray,
        val waveform: List<Float>,
        val durationMs: Long
    )

    /** Renders one library sample. */
    fun generate(sampleId: String, durationMs: Long): GeneratedAudio {
        val duration = durationMs.coerceIn(
            MIN_GENERATED_DURATION_MS,
            MAX_GENERATED_DURATION_MS
        )
        val sampleCount = (duration * SAMPLE_RATE / 1_000L)
            .coerceAtLeast(1L)
            .toInt()
        val raw = FloatArray(sampleCount)
        val random = Random(sampleId.hashCode())

        when {
            sampleId.startsWith("aud_") -> renderMusic(sampleId, raw, random)
            sampleId == "sfx_whoosh" -> renderWhoosh(raw, random)
            sampleId == "sfx_glitch" -> renderGlitch(raw, random)
            sampleId == "sfx_camera" -> renderCamera(raw, random)
            sampleId == "sfx_pop" -> renderPop(raw)
            sampleId == "sfx_ding" -> renderDing(raw)
            sampleId.startsWith("sfx_") -> renderFoley(sampleId, raw, random)
            else -> renderTone(raw)
        }

        var peak = 0f
        raw.forEach { peak = max(peak, abs(it)) }
        val scale = if (peak > 0f) 0.88f / peak else 1f
        val samples = ShortArray(raw.size) { index ->
            (raw[index] * scale * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return GeneratedAudio(
            samples = samples,
            waveform = waveform(samples, buckets = 48),
            durationMs = samples.size.toLong() * 1_000L / SAMPLE_RATE
        )
    }

    /** Writes 16-bit mono PCM WAV data. */
    fun writeWav(file: File, samples: ShortArray) {
        file.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        val dataSize = samples.size * 2
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { output ->
            output.writeBytes("RIFF")
            writeIntLittleEndian(output, 36 + dataSize)
            output.writeBytes("WAVE")
            output.writeBytes("fmt ")
            writeIntLittleEndian(output, 16)
            writeShortLittleEndian(output, 1) // PCM
            writeShortLittleEndian(output, CHANNEL_COUNT)
            writeIntLittleEndian(output, SAMPLE_RATE)
            writeIntLittleEndian(output, SAMPLE_RATE * CHANNEL_COUNT * BITS_PER_SAMPLE / 8)
            writeShortLittleEndian(output, CHANNEL_COUNT * BITS_PER_SAMPLE / 8)
            writeShortLittleEndian(output, BITS_PER_SAMPLE)
            output.writeBytes("data")
            writeIntLittleEndian(output, dataSize)
            samples.forEach { sample -> writeShortLittleEndian(output, sample.toInt()) }
        }
    }

    private fun renderMusic(sampleId: String, output: FloatArray, random: Random) {
        val roots = floatArrayOf(82.41f, 98.00f, 110.00f, 73.42f, 92.50f)
        val hashIndex = Math.floorMod(sampleId.hashCode(), roots.size)
        val root = roots[hashIndex]
        val scale = floatArrayOf(1f, 1.122f, 1.26f, 1.335f, 1.498f, 1.682f)
        val beatMs = 375L
        var previous = 0f
        output.forEachIndexed { index, _ ->
            val timeMs = index * 1_000L / SAMPLE_RATE
            val beat = timeMs / beatMs
            val phase = (timeMs % beatMs).toFloat() / beatMs.toFloat()
            val note = scale[
                (Math.floorMod(beat.toInt(), scale.size) + hashIndex) % scale.size
            ]
            val frequency = root * note
            val envelope = exp(-phase * 4.8f)
            val lead = sin(2.0 * PI * frequency * timeMs / 1_000.0).toFloat()
            val harmonic = 0.28f * sin(2.0 * PI * frequency * 2.0 * timeMs / 1_000.0).toFloat()
            val bass = 0.42f * sin(2.0 * PI * root * 0.5 * timeMs / 1_000.0).toFloat()
            val kickPhase = (timeMs % (beatMs * 2L)).toFloat() / (beatMs * 2L).toFloat()
            val kick = 0.55f * sin(2.0 * PI * (48.0 - 28.0 * kickPhase) * timeMs / 1_000.0).toFloat() *
                exp(-kickPhase * 10f)
            val filteredNoise = 0.035f * (random.nextFloat() * 2f - 1f)
            previous = previous * 0.82f + filteredNoise * 0.18f
            output[index] = (lead * envelope * 0.42f + harmonic * envelope + bass * 0.28f + kick + previous) * 0.45f
        }
    }

    private fun renderWhoosh(output: FloatArray, random: Random) {
        var filtered = 0f
        output.forEachIndexed { index, _ ->
            val progress = index.toFloat() / output.size.coerceAtLeast(1)
            val noise = random.nextFloat() * 2f - 1f
            filtered = filtered * 0.72f + noise * 0.28f
            val envelope = sin(PI * progress).toFloat()
            output[index] = filtered * envelope * 0.8f
        }
    }

    private fun renderGlitch(output: FloatArray, random: Random) {
        var previous = 0f
        output.forEachIndexed { index, _ ->
            val progress = index.toFloat() / output.size.coerceAtLeast(1)
            val gate = if ((index / 97) % 3 == 0) 1f else 0.18f
            val square = if ((index / 43) % 2 == 0) 1f else -1f
            val noise = random.nextFloat() * 2f - 1f
            previous = previous * 0.45f + noise * 0.55f
            output[index] = (square * 0.18f + previous * 0.55f) * gate * (1f - progress * 0.45f)
        }
    }

    private fun renderCamera(output: FloatArray, random: Random) {
        val firstClick = (SAMPLE_RATE * 0.035f).toInt()
        val secondClick = (SAMPLE_RATE * 0.20f).toInt()
        output.forEachIndexed { index, _ ->
            val first = if (index < firstClick) exp(-index / 95f) else 0f
            val secondDistance = index - secondClick
            val second = if (secondDistance in 0 until (SAMPLE_RATE * 0.08f).toInt()) {
                exp(-secondDistance / 140f)
            } else {
                0f
            }
            val noise = random.nextFloat() * 2f - 1f
            output[index] = noise * max(first, second) * 0.9f
        }
    }

    private fun renderPop(output: FloatArray) {
        output.forEachIndexed { index, _ ->
            val progress = index.toFloat() / output.size.coerceAtLeast(1)
            val frequency = 220f + 1_180f * progress
            val envelope = exp(-progress * 7f)
            output[index] = sin(2.0 * PI * frequency * index.toDouble() / SAMPLE_RATE).toFloat() * envelope * 0.8f
        }
    }

    private fun renderDing(output: FloatArray) {
        output.forEachIndexed { index, _ ->
            val progress = index.toFloat() / output.size.coerceAtLeast(1)
            val envelope = exp(-progress * 5.5f)
            val tone = sin(2.0 * PI * 880.0 * index.toDouble() / SAMPLE_RATE).toFloat()
            val overtone = 0.42f * sin(2.0 * PI * 1_320.0 * index.toDouble() / SAMPLE_RATE).toFloat()
            output[index] = (tone + overtone) * envelope * 0.7f
        }
    }

    private fun renderFoley(sampleId: String, output: FloatArray, random: Random) {
        var filtered = 0f
        output.forEachIndexed { index, _ ->
            val progress = index.toFloat() / output.size.coerceAtLeast(1)
            val noise = random.nextFloat() * 2f - 1f
            filtered = filtered * 0.86f + noise * 0.14f
            val pattern = when (sampleId) {
                "sfx_page_turn" -> {
                    val sweep = sin(PI * progress).toFloat()
                    val gate = if ((index / 180) % 2 == 0) 1f else 0.45f
                    sweep * gate
                }
                "sfx_scissors" -> {
                    val gate = if (index % (SAMPLE_RATE / 9) < SAMPLE_RATE / 28) 1f else 0.08f
                    gate * (1f - progress * 0.35f)
                }
                "sfx_tape_tear" -> {
                    val gate = if ((index / 110) % 4 < 2) 1f else 0.2f
                    gate * (1f - progress * 0.4f)
                }
                "sfx_ruler" -> {
                    val gate = if (progress < 0.72f) 1f else 0.08f
                    gate * exp(-progress * 2.5f)
                }
                else -> sin(PI * progress).toFloat() * (0.55f + 0.45f * sin(progress * 38f))
            }
            output[index] = filtered * pattern * 0.9f
        }
    }

    private fun renderTone(output: FloatArray) {
        output.forEachIndexed { index, _ ->
            val progress = index.toFloat() / output.size.coerceAtLeast(1)
            val envelope = min(1f, progress * 12f) * (1f - progress * 0.55f)
            output[index] = sin(2.0 * PI * 440.0 * index.toDouble() / SAMPLE_RATE).toFloat() * envelope * 0.65f
        }
    }

    private fun waveform(samples: ShortArray, buckets: Int): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val bucketCount = buckets.coerceAtLeast(1)
        return List(bucketCount) { bucket ->
            val start = bucket * samples.size / bucketCount
            val end = max(start + 1, (bucket + 1) * samples.size / bucketCount)
            var peak = 0
            for (index in start until min(end, samples.size)) {
                peak = max(peak, abs(samples[index].toInt()))
            }
            peak.toFloat() / Short.MAX_VALUE.toFloat()
        }
    }

    private fun writeIntLittleEndian(output: DataOutputStream, value: Int) {
        output.writeByte(value and 0xFF)
        output.writeByte(value shr 8 and 0xFF)
        output.writeByte(value shr 16 and 0xFF)
        output.writeByte(value shr 24 and 0xFF)
    }

    private fun writeShortLittleEndian(output: DataOutputStream, value: Int) {
        output.writeByte(value and 0xFF)
        output.writeByte(value shr 8 and 0xFF)
    }
}
