package com.example.data

import com.example.data.ai.TranscriptSegment
import com.example.engine.CaptionBridge
import com.example.engine.CaptionTimeline
import com.example.engine.SubtitleCue
import com.example.engine.WordTiming
import com.example.model.TextOverlay

/**
 * Turns a speech-to-text result into timed, readable subtitle overlays.
 *
 * The AI endpoint returns phrase-level segments; each phrase is split into
 * short cards here (max characters/words) so a long sentence becomes several
 * readable subtitles, then shifted from *source* time into *timeline* time
 * using the clip's start, trim and speed. Everything is pure so the mapping is
 * unit-tested instead of discovered on device.
 */
object AutoCaptionManager {

    const val MAX_CHARS_PER_CUE = 32
    const val MAX_WORDS_PER_CUE = 5
    const val MIN_CUE_MS = 120L

    /**
     * @param segments transcript segments, relative to the uploaded audio file
     * @param sourceOffsetMs absolute source position of the uploaded file's first sample
     * @param clipStartMs clip position on the timeline
     * @param trimStartMs clip's source trim start
     * @param speed clip playback speed (1 = real time)
     * @param clipDurationMs timeline length of the clip
     */
    fun overlaysFromTranscript(
        segments: List<TranscriptSegment>,
        sourceOffsetMs: Long,
        clipStartMs: Long,
        trimStartMs: Long,
        speed: Float,
        clipDurationMs: Long
    ): List<TextOverlay> {
        if (segments.isEmpty() || clipDurationMs <= 0L) return emptyList()
        val safeSpeed = if (speed.isFinite() && speed > 0.01f) speed else 1f
        val cues = mutableListOf<SubtitleCue>()

        segments.forEach { segment ->
            val localStart = ((sourceOffsetMs + segment.startMs - trimStartMs) / safeSpeed)
                .toLong()
                .coerceIn(0L, clipDurationMs)
            val localEnd = ((sourceOffsetMs + segment.endMs - trimStartMs) / safeSpeed)
                .toLong()
                .coerceIn(localStart + 1L, clipDurationMs)
            if (localStart >= clipDurationMs) return@forEach
            val span = (localEnd - localStart).coerceAtLeast(MIN_CUE_MS)
            // cuesFor() returns word-timed CaptionTimeline cues; the rest of
            // this class speaks SubtitleCue, so flatten them here.
            cues.addAll(
                cuesFor(segment.text, clipStartMs + localStart, span).map {
                    SubtitleCue(it.startMs, it.endMs, it.text)
                }
            )
        }

        if (cues.isEmpty()) return emptyList()
        return CaptionBridge.overlaysFromCues(cues)
    }

    /**
     * Groups recognised word timings (offline Vosk path) into phrase-level
     * segments with REAL start/end times: a break on a speech pause or a word
     * budget. Feeding these into [overlaysFromTranscript] keeps every caption
     * on the timeline at the moment it was actually spoken.
     */
    fun segmentsFromWords(
        words: List<WordTiming>,
        pauseMs: Long = 650L,
        maxWords: Int = MAX_WORDS_PER_CUE + 3
    ): List<TranscriptSegment> {
        val sane = words.filter { it.isSane }.sortedBy { it.startMs }
        if (sane.isEmpty()) return emptyList()
        val out = mutableListOf<TranscriptSegment>()
        val current = mutableListOf<WordTiming>()
        var lastEnd = sane.first().startMs

        fun flush() {
            if (current.isEmpty()) return
            out.add(
                TranscriptSegment(
                    startMs = current.first().startMs,
                    endMs = current.last().endMs,
                    text = current.joinToString(" ") { it.text }
                )
            )
            current.clear()
        }
        for (word in sane) {
            if (current.isNotEmpty() &&
                (word.startMs - lastEnd > pauseMs || current.size >= maxWords)
            ) {
                flush()
            }
            current.add(word)
            lastEnd = word.endMs
        }
        flush()
        return out
    }

    /**
     * Every word gets an equal slice of the phrase, then whole words are regrouped
     * into cues so a long phrase becomes several subtitle cards instead of one
     * wall of text.
     */
    internal fun cuesFor(phrase: String, startMs: Long, spanMs: Long): List<CaptionTimeline.Cue> {
        val words = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val step = (spanMs / words.size).coerceAtLeast(60L)
        val timed = words.mapIndexed { index, word ->
            val wordStart = startMs + index * step
            CaptionTimeline.Word(word, wordStart, minOf(startMs + spanMs, wordStart + step))
        }
        return CaptionTimeline.cues(timed, maxChars = MAX_CHARS_PER_CUE, maxWords = MAX_WORDS_PER_CUE)
    }
}
