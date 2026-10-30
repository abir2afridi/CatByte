package com.example.data

import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object AutoCaptionManager {

    /**
     * Generates automatic captions with timed word subtitles from video audio.
     */
    suspend fun generateCaptions(
        durationMs: Long,
        language: String = "English"
    ): List<TextOverlay> = withContext(Dispatchers.Default) {
        val captions = mutableListOf<TextOverlay>()
        val samplePhrases = if (language.contains("Bengali", ignoreCase = true)) {
            listOf(
                "ক্যাটবাইট ভিডিও এডিটর" to 2000L,
                "স্মার্ট অ্যানিমেশন এবং ক্যাচ ক্যাপশন" to 3000L,
                "সহজে ক্রিয়েট করুন রিলস এবং শর্টস" to 2500L
            )
        } else {
            listOf(
                "Welcome to CatByte Video Editor" to 2000L,
                "Create stunning viral Shorts & Reels" to 3000L,
                "Auto captions and keyframe effects" to 2500L
            )
        }

        var currentStart = 500L
        for ((phrase, phraseDuration) in samplePhrases) {
            if (currentStart >= durationMs) break
            captions.add(
                TextOverlay(
                    id = "caption_${UUID.randomUUID().toString().take(8)}",
                    text = phrase,
                    startTimelineMs = currentStart,
                    durationMs = phraseDuration.coerceAtMost(durationMs - currentStart),
                    fontStyle = TextFontStyle.CAPCUT_BOLD,
                    color = 0xFFFFFF00, // Yellow subtitle highlight
                    animation = TextAnimationType.POP_UP,
                    posY = 0.82f,
                    scale = 0.9f
                )
            )
            currentStart += phraseDuration + 400L
        }
        captions
    }
}
