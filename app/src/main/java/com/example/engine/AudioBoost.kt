package com.example.engine

import com.example.model.VideoProject
import kotlin.math.abs

/**
 * The export-time loudness boost.
 *
 * "Boost your video" (CapCut shows it as "Boost your video on TikTok") raises
 * the whole mix before the file is encoded. CatByte does the same with real
 * maths: every audio source of the project is scaled by a fixed gain, so the
 * exported file really is louder - the preview keeps the original level, the
 * file gets the boost.
 *
 * Muted sources stay muted, and no source is scaled above [MAX_GAIN], so a
 * hot mix cannot run away.
 */
object AudioBoost {

    /** +3 dB - the usual "make it louder for social" step. */
    const val SOCIAL_GAIN = 1.41f

    /** No source is scaled above this, so the mix cannot explode. */
    const val MAX_GAIN = 2f

    /** Floor for loudness-normalisation cuts (never silence a source). */
    const val MIN_GAIN = 0.25f

    /** The gain that belongs to a boost toggle. */
    fun gainFor(boost: Boolean): Float = if (boost) SOCIAL_GAIN else 1f

    /**
     * A copy of [project] with every audio source scaled by [gain].
     *
     * The copy is throw-away: the user's own mix keeps its levels, only the
     * rendered file is louder.
     */
    fun boosted(project: VideoProject, gain: Float): VideoProject {
        // Loudness normalisation may CUT as well as boost, so gains below 1
        // are real work now - only a no-op gain is skipped.
        if (!gain.isFinite() || abs(gain - 1f) <= 0.001f) return project
        val factor = gain.coerceIn(MIN_GAIN, MAX_GAIN)
        return project.copy(
            clips = project.clips.map { clip ->
                clip.copy(volume = (clip.volume * factor).coerceIn(0f, MAX_GAIN))
            },
            overlayTracks = project.overlayTracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        clip.copy(volume = (clip.volume * factor).coerceAtMost(MAX_GAIN))
                    }
                )
            },
            audioTracks = project.audioTracks.map { audio ->
                audio.copy(volume = (audio.volume * factor).coerceAtMost(MAX_GAIN))
            }
        )
    }
}
