package com.example.model

import java.util.UUID

data class VideoTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Main Track",
    val isMainTrack: Boolean = true,
    val clips: List<MediaClip> = emptyList(),
    /** Track-level audio: silencing a track drops gain for every clip on it. */
    val isMuted: Boolean = false,
    /** Locked tracks refuse moves, trims and deletes (editing safety). */
    val isLocked: Boolean = false,
    /** Solo mutes every other video track's audio, like an audio desk. */
    val isSolo: Boolean = false
)

data class AudioClip(
    val id: String,
    val title: String,
    val artist: String,
    /** Source audio file. Null only for built-in library metadata before generation. */
    val uriString: String? = null,
    val startTimelineMs: Long = 0L,
    /** Duration on the output timeline after trim and speed. */
    val durationMs: Long = 6000L,
    /** Full untrimmed source duration. Zero means "infer from legacy data". */
    val originalDurationMs: Long = 0L,
    /** Absolute source in-point. */
    val trimStartMs: Long = 0L,
    /** Absolute source out-point. Zero means "until the source ends". */
    val trimEndMs: Long = 0L,
    /** Playback speed multiplier for the source audio. */
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isVoiceover: Boolean = false,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    /**
     * Decoded peak envelope of the source file (0..1 per bar). Empty until the
     * real file has been analysed - the UI renders "no data" instead of a
     * decorative fake pattern.
     */
    val waveform: List<Float> = emptyList()
)

/** Stable ids for the two tracks that exist without a `VideoTrack` object. */
object TimelineTrackIds {
    const val MAIN = "__main__"
}