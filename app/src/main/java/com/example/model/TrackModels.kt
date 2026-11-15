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
    /** Source audio file. Null for the built-in library entries. */
    val uriString: String? = null,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 6000L,
    /** Source trim points. Both zero means "play the whole source". */
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    /** Playback speed multiplier for the source audio. */
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isVoiceover: Boolean = false,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val waveform: List<Float> = listOf(0.2f, 0.5f, 0.8f, 0.9f, 0.6f, 0.4f, 0.7f, 0.9f, 0.8f, 0.5f, 0.3f, 0.6f)
)

/** Stable ids for the two tracks that exist without a `VideoTrack` object. */
object TimelineTrackIds {
    const val MAIN = "__main__"
}