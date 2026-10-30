package com.example.data

import com.example.model.AudioClip
import com.example.model.MediaClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object AudioExtractionManager {

    /**
     * Extracts an [AudioClip] track from a [MediaClip] video clip.
     */
    suspend fun extractAudioFromClip(clip: MediaClip): AudioClip = withContext(Dispatchers.Default) {
        AudioClip(
            id = "ext_audio_${UUID.randomUUID().toString().take(8)}",
            title = "Audio - ${clip.title}",
            artist = "Extracted Track",
            startTimelineMs = clip.startTimelineMs,
            durationMs = clip.durationMs,
            volume = clip.volume,
            waveform = clip.waveform
        )
    }
}
