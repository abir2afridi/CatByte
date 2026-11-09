package com.example.engine

import kotlin.math.max
import kotlin.math.min

/** One subtitle cue on the project timeline. */
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/** Word-level timing used by karaoke / word-by-word captions. */
data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class SubtitleStyle(
    val maxCharsPerLine: Int = 42,
    val maxLines: Int = 2,
    val maxCueDurationMs: Long = 7_000L,
    val minCueDurationMs: Long = 700L
)

/**
 * SRT/VTT read and write.
 *
 * Captions are the single most-used text feature in short-form editing, so they
 * have to round-trip exactly: import, edit on the timeline, export back without
 * shuffling timings by a frame.
 */
object SubtitleIO {

    private val TIMESTAMP = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )

    fun parse(raw: String): List<SubtitleCue> {
        val trimmed = raw.trimStart('\uFEFF', '\n', '\r', ' ')
        return if (trimmed.startsWith("WEBVTT", ignoreCase = true)) parseVtt(raw) else parseSrt(raw)
    }

    fun parseSrt(raw: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = raw.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
        blocks.forEach { block ->
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return@forEach
            val timingIndex = lines.indexOfFirst { TIMESTAMP.containsMatchIn(it) }
            if (timingIndex < 0) return@forEach
            val match = TIMESTAMP.find(lines[timingIndex]) ?: return@forEach
            val startMs = toMs(match.groupValues, 1)
            val endMs = toMs(match.groupValues, 5)
            val text = lines.drop(timingIndex + 1).joinToString("\n").trim()
            if (text.isEmpty() || endMs <= startMs) return@forEach
            cues += SubtitleCue(startMs, endMs, text)
        }
        return cues.sortedBy { it.startMs }
    }

    fun parseVtt(raw: String): List<SubtitleCue> {
        val body = raw.replace("\r\n", "\n")
            .substringAfter("WEBVTT", "")
            .replace(Regex("(?m)^(NOTE|STYLE|REGION).*$"), "")
        val cues = mutableListOf<SubtitleCue>()
        body.split(Regex("\n\\s*\n")).forEach { block ->
            val lines = block.lines().filter { it.isNotBlank() }
            val timingIndex = lines.indexOfFirst { TIMESTAMP.containsMatchIn(it) }
            if (timingIndex < 0) return@forEach
            val match = TIMESTAMP.find(lines[timingIndex]) ?: return@forEach
            val startMs = toMs(match.groupValues, 1)
            val endMs = toMs(match.groupValues, 5)
            val text = lines.drop(timingIndex + 1).joinToString("\n").trim()
            if (text.isEmpty() || endMs <= startMs) return@forEach
            cues += SubtitleCue(startMs, endMs, text)
        }
        return cues.sortedBy { it.startMs }
    }

    private fun toMs(groups: List<String>, offset: Int): Long {
        val hours = groups[offset].toLong()
        val minutes = groups[offset + 1].toLong()
        val seconds = groups[offset + 2].toLong()
        val fraction = groups[offset + 3].padEnd(3, '0').toLong()
        return ((hours * 3600L + minutes * 60L + seconds) * 1_000L) + fraction
    }

    fun toSrt(cues: List<SubtitleCue>): String = cues.sortedBy { it.startMs }
        .mapIndexed { index, cue ->
            "${index + 1}\n${formatSrt(cue.startMs)} --> ${formatSrt(cue.endMs)}\n${cue.text}"
        }
        .joinToString("\n\n", postfix = "\n")

    fun toVtt(cues: List<SubtitleCue>): String {
        val body = cues.sortedBy { it.startMs }
            .joinToString("\n\n") { cue ->
                "${formatVtt(cue.startMs)} --> ${formatVtt(cue.endMs)}\n${cue.text}"
            }
        return "WEBVTT\n\n$body\n"
    }

    private fun formatSrt(ms: Long): String = "%02d:%02d:%02d,%03d".format(
        ms / 3_600_000L,
        (ms / 60_000L) % 60L,
        (ms / 1_000L) % 60L,
        ms % 1_000L
    )

    private fun formatVtt(ms: Long): String = "%02d:%02d:%02d.%03d".format(
        ms / 3_600_000L,
        (ms / 60_000L) % 60L,
        (ms / 1_000L) % 60L,
        ms % 1_000L
    )

    /** Shifts every cue (positive = later) and clamps to zero. */
    fun retime(cues: List<SubtitleCue>, offsetMs: Long): List<SubtitleCue> = cues.map { cue ->
        val start = (cue.startMs + offsetMs).coerceAtLeast(0L)
        val end = (cue.endMs + offsetMs).coerceAtLeast(start + 100L)
        cue.copy(startMs = start, endMs = end)
    }

    /** Splits a cue into display lines without breaking words. */
    fun wrap(cue: SubtitleCue, style: SubtitleStyle = SubtitleStyle()): List<String> {
        val words = cue.text.replace("\n", " ").split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        val builder = StringBuilder()
        words.forEach { word ->
            val candidate = if (builder.isEmpty()) word else "$builder $word"
            if (candidate.length > style.maxCharsPerLine && builder.isNotEmpty()) {
                lines += builder.toString()
                builder.clear()
                builder.append(word)
            } else {
                if (builder.isNotEmpty()) builder.append(' ')
                builder.append(word)
            }
        }
        if (builder.isNotEmpty()) lines += builder.toString()
        return lines.take(style.maxLines)
    }

    /** Issues the editor should surface before burning captions in. */
    fun validate(cues: List<SubtitleCue>, projectDurationMs: Long): List<String> {
        val problems = mutableListOf<String>()
        val sorted = cues.sortedBy { it.startMs }
        sorted.forEachIndexed { index, cue ->
            if (cue.endMs <= cue.startMs) problems += "Cue ${index + 1} has no duration"
            if (cue.durationMs > 15_000L) problems += "Cue ${index + 1} is longer than 15s"
            if (cue.startMs > projectDurationMs) problems += "Cue ${index + 1} starts after the end"
            val next = sorted.getOrNull(index + 1)
            if (next != null && next.startMs < cue.endMs) {
                problems += "Cue ${index + 1} overlaps cue ${index + 2}"
            }
        }
        return problems
    }

    /**
     * Builds readable cues from word timings (Whisper-style output), breaking on
     * sentence punctuation, character budget and maximum duration.
     */
    fun cuesFromWords(
        words: List<TimedWord>,
        style: SubtitleStyle = SubtitleStyle()
    ): List<SubtitleCue> {
        if (words.isEmpty()) return emptyList()
        val cues = mutableListOf<SubtitleCue>()
        var current = mutableListOf<TimedWord>()

        fun flush() {
            if (current.isEmpty()) return
            val text = current.joinToString(" ") { it.text.trim() }.trim()
            val start = current.first().startMs
            val end = current.last().endMs.coerceAtLeast(start + style.minCueDurationMs)
            if (text.isNotEmpty()) cues += SubtitleCue(start, end, text)
            current = mutableListOf()
        }

        words.forEach { word ->
            current += word
            val textLength = current.sumOf { it.text.length + 1 }
            val duration = word.endMs - current.first().startMs
            val endsSentence = word.text.trim().lastOrNull() in setOf('.', '!', '?', '\u0964', '\u0965')
            if (textLength >= style.maxCharsPerLine || duration >= style.maxCueDurationMs || endsSentence) {
                flush()
            }
        }
        flush()
        return cues
    }

    /** Splits one cue into per-word cues for karaoke highlighting. */
    fun karaokeSplit(cue: SubtitleCue): List<TimedWord> {
        val words = cue.text.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
        var cursor = cue.startMs
        return words.map { word ->
            val share = max(1L, (cue.durationMs * word.length) / totalChars)
            val start = cursor
            val end = min(cue.endMs, start + share)
            cursor = end
            TimedWord(word, start, end)
        }
    }
}
