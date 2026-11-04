package com.example.engine

/**
 * Bangla-first text helpers.
 *
 * Bangla is the reason our editor can win locally, so the small typography
 * details (native digits, no broken conjuncts at a line break, correct line
 * budget) live in one tested place instead of being scattered through Compose
 * modifiers.
 */
object BanglaNumerals {

    private const val BANGLA_DIGITS = "\u09E6\u09E7\u09E8\u09E9\u09EA\u09EB\u09EC\u09ED\u09EE\u09EF"
    private const val LATIN_DIGITS = "0123456789"

    fun toBangla(input: String): String = buildString(input.length) {
        input.forEach { char ->
            val index = LATIN_DIGITS.indexOf(char)
            append(if (index >= 0) BANGLA_DIGITS[index] else char)
        }
    }

    fun toLatin(input: String): String = buildString(input.length) {
        input.forEach { char ->
            val index = BANGLA_DIGITS.indexOf(char)
            append(if (index >= 0) LATIN_DIGITS[index] else char)
        }
    }

    fun isBanglaDigit(char: Char): Boolean = BANGLA_DIGITS.indexOf(char) >= 0

    /** Timecode label using native digits when the app language is Bangla. */
    fun timecodeLabel(ms: Long, bangla: Boolean): String {
        val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
        val label = "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
        return if (bangla) toBangla(label) else label
    }
}

/**
 * Line breaking that respects Bangla orthography.
 *
 * A naive `take(maxChars)` split can sever a consonant from its vowel sign or
 * hasant, which renders a broken conjunct (the classic "বাংলা ভাঙা" bug).
 */
object BanglaLineBreaker {

    private val NO_BREAK_BEFORE: Set<Char> = buildSet {
        // Nukta, avagraha, vowel signs, virama, khanda-ta, additional signs.
        for (code in 0x09BC..0x09CD) add(code.toChar())
        add('\u09D7')
        add('\u09E2')
        add('\u09E3')
        add('\u200C') // ZWNJ
        add('\u200D') // ZWJ
    }

    fun canBreakBefore(char: Char): Boolean = char !in NO_BREAK_BEFORE

    fun canBreakBetween(previous: Char, next: Char): Boolean {
        if (previous.isWhitespace() || next.isWhitespace()) return true
        return canBreakBefore(next)
    }

    /**
     * Wraps [text] into at most [maxLines] lines of at most [maxCharsPerLine]
     * characters, falling back to a mark-safe character split for unspaced text.
     */
    fun wrap(text: String, maxCharsPerLine: Int, maxLines: Int = 2): List<String> {
        if (maxCharsPerLine <= 0 || maxLines <= 0) return emptyList()
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        val current = StringBuilder()

        fun pushCurrent() {
            if (current.isNotEmpty()) {
                lines += current.toString()
                current.clear()
            }
        }

        words.forEach { word ->
            if (word.length > maxCharsPerLine) {
                pushCurrent()
                lines += splitLongWord(word, maxCharsPerLine)
            } else {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (candidate.length > maxCharsPerLine) {
                    pushCurrent()
                    current.append(word)
                } else {
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(word)
                }
            }
        }
        pushCurrent()
        return lines.take(maxLines)
    }

    /** Character-level split that never separates a base letter from a mark. */
    fun splitLongWord(word: String, maxCharsPerLine: Int): List<String> {
        val chunks = mutableListOf<String>()
        val builder = StringBuilder()
        var index = 0
        while (index < word.length) {
            val char = word[index]
            if (builder.length >= maxCharsPerLine && canBreakBefore(char)) {
                chunks += builder.toString()
                builder.clear()
            }
            builder.append(char)
            index++
        }
        if (builder.isNotEmpty()) chunks += builder.toString()
        return chunks
    }

    /** True when every mark in the string still follows a base character. */
    fun isShapingSafe(text: String): Boolean {
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (!canBreakBefore(char)) {
                if (index == 0) return false
                val previous = text[index - 1]
                if (previous.isWhitespace()) return false
            }
            index++
        }
        return true
    }
}

/** Font size that fits a caption block into a safe area. */
object CaptionLayout {

    data class Fit(val fontSizeSp: Float, val lines: List<String>, val lineCount: Int)

    fun fit(
        text: String,
        canvasWidthPx: Int,
        safeWidthFraction: Float = 0.86f,
        preferredSizeSp: Float = 18f,
        minSizeSp: Float = 11f,
        maxLines: Int = 2
    ): Fit {
        val usablePx = canvasWidthPx * safeWidthFraction.coerceIn(0.3f, 1f)
        var size = preferredSizeSp
        var lines: List<String> = emptyList()
        while (size >= minSizeSp) {
            val charsPerLine = (usablePx / (size * 0.58f)).toInt().coerceAtLeast(8)
            lines = BanglaLineBreaker.wrap(text, charsPerLine, maxLines)
            if (lines.size <= maxLines) break
            size -= 1f
        }
        if (lines.size > maxLines) {
            lines = BanglaLineBreaker.wrap(text, 12, maxLines)
        }
        return Fit(size.coerceAtLeast(minSizeSp), lines, lines.size)
    }
}
