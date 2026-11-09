package com.example.engine

/**
 * Caption layout maths: word timings in, readable cues out.
 *
 * [SubtitleIO] owns the SRT/VTT file format; this object owns the timing decisions
 * (where to break a line, how fast a cue is read, which word is highlighted at
 * time t). Keeping them apart means the same cue builder feeds burned-in export,
 * the karaoke highlight and the SRT writer.
 */
object CaptionTimeline {

    data class Word(val text: String, val startMs: Long, val endMs: Long) {
        val durationMs: Long get() = endMs - startMs
    }

    data class Cue(val startMs: Long, val endMs: Long, val words: List<Word>) {
        val durationMs: Long get() = endMs - startMs
        val text: String get() = words.joinToString(" ") { it.text }
    }

    data class KaraokeFrame(val startMs: Long, val endMs: Long, val activeIndex: Int)

    /**
     * Groups word timings into cues that fit on screen: a break happens on a
     * character budget, a word budget, a long pause, or a maximum on-screen time.
     */
    fun cues(
        words: List<Word>,
        maxChars: Int = 32,
        maxWords: Int = 7,
        maxDurationMs: Long = 5_000L,
        pauseMs: Long = 700L,
    ): List<Cue> {
        val sorted = words.sortedBy { it.startMs }
        if (sorted.isEmpty()) return emptyList()
        val out = ArrayList<Cue>()
        var current = ArrayList<Word>()

        fun flush() {
            if (current.isEmpty()) return
            out.add(Cue(current.first().startMs, current.last().endMs, current.toList()))
            current = ArrayList()
        }

        for (word in sorted) {
            if (current.isNotEmpty()) {
                val previous = current.last()
                val chars = current.joinToString(" ") { it.text }.length + 1 + word.text.length
                val gap = word.startMs - previous.endMs
                val span = word.endMs - current.first().startMs
                if (chars > maxChars || current.size >= maxWords || gap > pauseMs || span > maxDurationMs) {
                    flush()
                }
            }
            current.add(word)
        }
        flush()
        return out
    }

    /**
     * Per-word highlight windows. A word is active from its own start until the
     * next word starts, so the highlight never blinks off between words.
     */
    fun karaoke(cue: Cue): List<KaraokeFrame> {
        if (cue.words.isEmpty()) return emptyList()
        val frames = ArrayList<KaraokeFrame>(cue.words.size)
        for ((index, word) in cue.words.withIndex()) {
            val end = cue.words.getOrNull(index + 1)?.startMs ?: cue.endMs
            frames.add(KaraokeFrame(word.startMs, maxOf(end, word.startMs + 1L), index))
        }
        return frames
    }

    fun activeIndex(cue: Cue, atMs: Long): Int? =
        cue.words.indexOfLast { it.startMs <= atMs }.takeIf { it >= 0 && atMs <= cue.endMs }

    /** Characters per second; the readability knob captioners actually tune. */
    fun readingSpeed(cue: Cue): Float {
        if (cue.durationMs <= 0L) return Float.MAX_VALUE
        return cue.text.length * 1000f / cue.durationMs
    }

    /**
     * Extends cues that are too fast for their text, but never past the next cue
     * (minus [gapMs]) so two captions can never overlap.
     */
    fun extendForReadability(
        cues: List<Cue>,
        targetCps: Float = 17f,
        gapMs: Long = 80L,
    ): List<Cue> {
        if (cues.isEmpty()) return cues
        val sorted = cues.sortedBy { it.startMs }
        val out = ArrayList<Cue>(sorted.size)
        for ((index, cue) in sorted.withIndex()) {
            val needed = cue.words.first().startMs + (cue.text.length / targetCps * 1000f).toLong()
            val limit = sorted.getOrNull(index + 1)?.startMs?.minus(gapMs)
            var end = maxOf(cue.endMs, needed)
            if (limit != null) end = minOf(end, limit)
            end = maxOf(end, cue.words.last().endMs)
            out.add(cue.copy(endMs = end))
        }
        return out
    }
}
