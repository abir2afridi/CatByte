package com.example.data

import com.example.engine.CaptionBridge
import com.example.engine.CaptionTimeline
import com.example.engine.SubtitleCue
import com.example.model.TextOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline auto-caption generator.
 *
 * There is no speech-recognition model in the APK yet, so the phrases below are
 * placeholders — but the *timing* path is the real one: each phrase is split into
 * short readable cues by [CaptionTimeline] and converted with [CaptionBridge],
 * which is exactly what a recogniser's word timestamps will feed later. Swapping
 * in a model only replaces [phrasesFor].
 */
object AutoCaptionManager {

    const val MAX_CHARS_PER_CUE = 32
    const val MAX_WORDS_PER_CUE = 5
    const val LEAD_IN_MS = 500L
    const val PHRASE_GAP_MS = 400L
    const val MIN_PHRASE_MS = 300L

    /** Reading speed a caption is stretched towards, characters per second. */
    const val TARGET_CPS = 17f

    suspend fun generateCaptions(
        durationMs: Long,
        language: String = "English",
    ): List<TextOverlay> = withContext(Dispatchers.Default) {
        if (durationMs <= 0L) return@withContext emptyList()

        val cues = ArrayList<CaptionTimeline.Cue>()
        var cursor = LEAD_IN_MS
        for ((phrase, phraseDuration) in phrasesFor(language)) {
            if (cursor >= durationMs) break
            val span = minOf(phraseDuration, durationMs - cursor)
            if (span < MIN_PHRASE_MS) break
            cues += cuesFor(phrase, cursor, span)
            cursor += phraseDuration + PHRASE_GAP_MS
        }

        val readable = CaptionTimeline.extendForReadability(cues, TARGET_CPS, gapMs = 60L)
            .map { cue -> SubtitleCue(cue.startMs, minOf(cue.endMs, durationMs), cue.text) }
        CaptionBridge.overlaysFromCues(readable)
    }

    /**
     * Every word gets an equal slice of the phrase, then whole words are regrouped
     * into cues so a long phrase becomes several subtitle cards instead of one
     * wall of text.
     */
    private fun cuesFor(phrase: String, startMs: Long, spanMs: Long): List<CaptionTimeline.Cue> {
        val words = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val step = (spanMs / words.size).coerceAtLeast(120L)
        val timed = words.mapIndexed { index, word ->
            val wordStart = startMs + index * step
            CaptionTimeline.Word(word, wordStart, minOf(startMs + spanMs, wordStart + step))
        }
        return CaptionTimeline.cues(timed, maxChars = MAX_CHARS_PER_CUE, maxWords = MAX_WORDS_PER_CUE)
    }

    private fun phrasesFor(language: String): List<Pair<String, Long>> =
        if (language.contains("Bengali", ignoreCase = true)) {
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
}
