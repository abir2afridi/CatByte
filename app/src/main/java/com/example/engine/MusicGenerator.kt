package com.example.engine

import com.example.data.SampleAudioGenerator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * A small procedural music composer ("Audio → AI music").
 *
 * Everything is synthesised locally from deterministic oscillators - no model,
 * no network, no copyrighted payload - and the result is a real PCM render the
 * editor treats like any other imported track (preview, mixer, export all
 * work). Three moods cover the common short-video needs:
 *
 * - CHILL: slow pads with a gentle arpeggio (Lo-fi / vlog bed)
 * - UPBEAT: plucks, bass and a simple beat (transitions / intros)
 * - CINEMATIC: a minor drone with swelling fifths (trailers / reveals)
 */
object MusicGenerator {

    enum class Mood(val displayName: String, val description: String) {
        CHILL("Chill", "Soft pads and a slow arpeggio"),
        UPBEAT("Upbeat", "Plucks, bass and a light beat"),
        CINEMATIC("Cinematic", "Minor drone with swelling fifths")
    }

    private const val MIN_DURATION_MS = 1_000L
    private const val MAX_DURATION_MS = 60_000L

    /** Twelve-tone equal temperament: midi note -> Hz. */
    private fun midiHz(midi: Int): Float = 440f * 2.0.pow((midi - 69) / 12.0).toFloat()

    /** A closed minor/major chord in midi notes around the given root. */
    private fun chord(rootMidi: Int, minor: Boolean): List<Int> =
        listOf(rootMidi, rootMidi + (if (minor) 3 else 4), rootMidi + 7)

    // Progressions: (root midi, minor?) per bar. One bar = 4 beats.
    private val CHILL_PROGRESSION = listOf(
        Triple(57, true, "A minor"),   // Am
        Triple(53, true, "F major"),   // F
        Triple(60, false, "C major"),  // C
        Triple(55, false, "G major")   // G
    )
    private val UPBEAT_PROGRESSION = listOf(
        Triple(60, false, "C major"),  // C
        Triple(55, false, "G major"),  // G
        Triple(57, true, "A minor"),   // Am
        Triple(53, true, "F major")    // F
    )
    private val CINEMATIC_PROGRESSION = listOf(
        Triple(45, true, "A minor"),   // Am (low)
        Triple(48, true, "C major"),   // C (low)
        Triple(50, true, "D minor"),   // Dm (low)
        Triple(52, true, "E minor")    // Em (low)
    )

    /** Renders [mood] for [durationMs] as normalised PCM mono 44.1 kHz. */
    fun render(
        mood: Mood,
        durationMs: Long,
        seed: Long = 0L
    ): SampleAudioGenerator.GeneratedAudio {
        val duration = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        val sampleCount = (duration * SampleAudioGenerator.SAMPLE_RATE / 1_000L)
            .coerceAtLeast(1L)
            .toInt()
        val raw = FloatArray(sampleCount)
        val random = Random(seed)

        val bpm = when (mood) {
            Mood.CHILL -> 76
            Mood.UPBEAT -> 118
            Mood.CINEMATIC -> 60
        }
        val beatSec = 60f / bpm
        val barSec = beatSec * 4f
        val progression = when (mood) {
            Mood.CHILL -> CHILL_PROGRESSION
            Mood.UPBEAT -> UPBEAT_PROGRESSION
            Mood.CINEMATIC -> CINEMATIC_PROGRESSION
        }

        for (index in 0 until sampleCount) {
            val t = index.toFloat() / SampleAudioGenerator.SAMPLE_RATE
            val barIndex = (t / barSec).toInt()
            val (rootMidi, minor, _) = progression[Math.floorMod(barIndex, progression.size)]
            val chordNotes = chord(rootMidi, minor)
            val barT = t - barIndex * barSec
            // One-second fade in/out so the loop joins the timeline smoothly.
            val envelope = fadeInOut(t, duration / 1000f, 0.8f)
            if (envelope <= 0f) continue

            var sample = 0f
            when (mood) {
                Mood.CHILL -> {
                    sample += pad(chordNotes, t, barT, 0.30f)
                    sample += arp(chordNotes, t, beatSec / 2f, 0.18f, random.nextFloat() * 0.02f)
                    sample += bass(chordNotes[0], t, 0.22f)
                }
                Mood.UPBEAT -> {
                    sample += pluck(chordNotes, t, beatSec / 2f, 0.30f)
                    sample += bass(chordNotes[0] - 12, t, 0.26f)
                    sample += kick(t, beatSec, 0.5f)
                    sample += hat(t, beatSec, 0.12f, random)
                }
                Mood.CINEMATIC -> {
                    sample += drone(chordNotes, t, barT, barSec, 0.34f)
                    sample += swell(chordNotes[2], t, barT, barSec, 0.20f)
                    sample += bass(chordNotes[0] - 12, t, 0.20f)
                }
            }
            raw[index] = sample * envelope
        }

        // The second argument of normalize() is the waveform bucket count, not
        // the sample rate; passing 44100 overflowed the bucket offsets and made
        // waveform extraction read negative indices.
        return SampleAudioGenerator.normalize(raw)
    }

    // ---------------- voices ----------------

    /** Warm chord pad: detuned sine stack per note with a slow bar attack. */
    private fun pad(notes: List<Int>, t: Float, barT: Float, gain: Float): Float {
        val attack = min(1f, barT / 0.6f)
        val release = min(1f, max(0f, (barT - 3.2f) / 0.8f).let { 1f - it })
        var sum = 0f
        notes.forEachIndexed { i, note ->
            val hz = midiHz(note + 12)
            sum += sin(2f * PI.toFloat() * hz * t) * 0.6f
            sum += sin(2f * PI.toFloat() * hz * 1.003f * t) * 0.4f
            sum += sin(2f * PI.toFloat() * hz * 2f * t) * 0.15f
            sum *= 1f - i * 0.08f
        }
        return sum * gain * attack * release / notes.size
    }

    /** Soft arpeggio: one chord tone per step with an exponential decay. */
    private fun arp(notes: List<Int>, t: Float, stepSec: Float, gain: Float, wobble: Float): Float {
        val step = (t / stepSec).toInt()
        val note = notes[Math.floorMod(step, notes.size)] + 12
        val stepT = t - step * stepSec
        val decay = exp(-stepT * 6f)
        val hz = midiHz(note) * (1f + wobble)
        return sin(2f * PI.toFloat() * hz * t) * gain * decay * 0.6f
    }

    /** Plucked eighth notes cycling the chord (brighter, faster decay). */
    private fun pluck(notes: List<Int>, t: Float, stepSec: Float, gain: Float): Float {
        val step = (t / stepSec).toInt()
        val note = notes[Math.floorMod(step, notes.size)] + 12
        val stepT = t - step * stepSec
        val decay = exp(-stepT * 14f)
        val hz = midiHz(note)
        return (sin(2f * PI.toFloat() * hz * t) * 0.7f +
            sin(2f * PI.toFloat() * hz * 2f * t) * 0.3f) * gain * decay
    }

    /** Root-note bass, retriggered every bar. */
    private fun bass(rootMidi: Int, t: Float, gain: Float): Float {
        val hz = midiHz(rootMidi)
        val sine = sin(2f * PI.toFloat() * hz * t)
        // A little saturation for weight.
        val driven = sine - 0.18f * sine * sine * sine
        return driven * gain
    }

    /** Kick drum on every beat: a fast sine drop with click. */
    private fun kick(t: Float, beatSec: Float, gain: Float): Float {
        val beatT = t - (t / beatSec).toInt() * beatSec
        if (beatT > 0.18f) return 0f
        val sweep = 120f * exp(-beatT * 22f) + 48f
        return sin(2f * PI.toFloat() * sweep * beatT) * gain * exp(-beatT * 16f)
    }

    /** Closed hat on the off-beats: short high-passed noise burst. */
    private fun hat(t: Float, beatSec: Float, gain: Float, random: Random): Float {
        val half = beatSec / 2f
        val stepT = t - (t / half).toInt() * half
        if ((t / half).toInt() % 2 == 0) return 0f // off-beats only
        if (stepT > 0.045f) return 0f
        val noise = random.nextFloat() * 2f - 1f
        // Cheap high-pass: subtract a slow average of the burst.
        return noise * exp(-stepT * 90f) * gain
    }

    /** Cinematic drone: chord tones with a slow vibrato, held for the bar. */
    private fun drone(notes: List<Int>, t: Float, barT: Float, barSec: Float, gain: Float): Float {
        val attack = min(1f, barT / (barSec * 0.35f))
        val release = min(1f, (barSec - barT) / (barSec * 0.35f))
        val vib = 1f + 0.004f * sin(2f * PI.toFloat() * 0.8f * t)
        var sum = 0f
        notes.forEach { note ->
            val hz = midiHz(note) * vib
            sum += sin(2f * PI.toFloat() * hz * t)
        }
        return sum / notes.size * gain * attack * release
    }

    /** Swelling fifth above the chord, loudest at the end of each bar. */
    private fun swell(fifthMidi: Int, t: Float, barT: Float, barSec: Float, gain: Float): Float {
        val shape = (barT / barSec).let { it * it } // ease-in swell
        val hz = midiHz(fifthMidi + 12)
        return sin(2f * PI.toFloat() * hz * t) * gain * shape
    }

    /** Smooth 0 -> 1 -> 0 envelope with linear ramps. */
    private fun fadeInOut(t: Float, totalSec: Float, fadeSec: Float): Float {
        val fadeIn = min(1f, t / fadeSec)
        val fadeOut = min(1f, (totalSec - t) / fadeSec)
        return min(fadeIn, fadeOut).coerceAtLeast(0f)
    }
}
