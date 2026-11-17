package com.example.engine

/**
 * Turns recognised words into caption overlays: the last pure step between a
 * speech recogniser and the timeline.
 *
 * The recogniser only decides WHAT was said and WHEN; every layout decision
 * (cues per line, reading speed, bottom-centred placement) stays in
 * [CaptionTimeline] and [CaptionBridge], exactly as it does for SRT imports.
 */
object CaptionAssembler {

    /**
     * @param words recognised word timings, in speaking order.
     * @param totalDurationMs length of the video; captions never extend past it.
     * @param maxChars / [maxWords] cue packing budgets, forwarded to
     *   [CaptionTimeline.cues].
     */
    fun cuesFromWords(
        words: List<WordTiming>,
        totalDurationMs: Long,
        maxChars: Int = AutoCaptionDefaults.MAX_CHARS_PER_CUE,
        maxWords: Int = AutoCaptionDefaults.MAX_WORDS_PER_CUE
    ): List<CaptionTimeline.Cue> {
        val timelineWords = words
            .asSequence()
            .filter { it.isSane }
            .map { word ->
                CaptionTimeline.Word(
                    text = word.text.trim(),
                    startMs = word.startMs.coerceAtLeast(0L),
                    endMs = word.endMs
                )
            }
            .filter { it.startMs < totalDurationMs }
            .toList()
        if (timelineWords.isEmpty()) return emptyList()

        val clamped = timelineWords.map { word ->
            word.copy(endMs = word.endMs.coerceAtMost(totalDurationMs))
        }
        return CaptionTimeline.cues(clamped, maxChars = maxChars, maxWords = maxWords)
    }

    /**
     * Words in, ready-to-place overlays out. This is the single entry the
     * ViewModel calls after transcription.
     */
    fun overlaysFromWords(
        words: List<WordTiming>,
        totalDurationMs: Long,
        idFactory: () -> String = { java.util.UUID.randomUUID().toString() }
    ): List<com.example.model.TextOverlay> {
        val cues = cuesFromWords(words, totalDurationMs)
        val readable = CaptionTimeline.extendForReadability(
            cues,
            AutoCaptionDefaults.TARGET_CPS,
            gapMs = 60L
        ).map { cue ->
            SubtitleCue(cue.startMs, cue.endMs.coerceAtMost(totalDurationMs), cue.text)
        }
        return CaptionBridge.overlaysFromCues(readable, idFactory)
    }
}

/** The tuning knobs shared by the assembler and its callers. */
object AutoCaptionDefaults {
    const val MAX_CHARS_PER_CUE = 32
    const val MAX_WORDS_PER_CUE = 5
    const val TARGET_CPS = 17f
}
