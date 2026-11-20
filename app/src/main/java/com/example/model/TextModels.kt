package com.example.model

enum class TextAnimationType(val displayName: String) {
    NONE("None"),
    TYPEWRITER("Typewriter"),
    POP_UP("Pop Up"),
    WAVE("Wave Bounce"),
    NEON_PULSE("Neon Pulse"),
    FADE_IN("Fade In"),
    FADE_OUT("Fade Out"),
    GLITCH("Glitch Text"),
    BOUNCE_IN("Bounce In")
}

enum class TextFontStyle(val displayName: String) {
    CAPCUT_BOLD("CapCut Bold"),
    CLEAN_SANS("Clean Sans"),
    CINEMATIC_SERIF("Cinematic Serif"),
    NEON_GLOW("Neon Glow"),
    SUBTITLE("Clean Subtitle"),
    BANGLA_NOTO_SANS("Noto Sans বাংলা"),
    BANGLA_HIND_SILIGURI("Hind Siliguri বাংলা"),
    BANGLA_TIRO("Tiro Serif বাংলা"),
    BANGLA_SOLAIMAN_LIPI("SolaimanLipi বাংলা"),
    BANGLA_KALPURUSH("Kalpurush বাংলা")
}

/** One timed word inside a caption, for the karaoke highlight. */
data class KaraokeWord(
    val text: String,
    /** Start offset in milliseconds from the caption's own start. */
    val startMs: Long,
    /** End offset in milliseconds from the caption's own start. */
    val endMs: Long
)

data class TextOverlay(
    val id: String,
    val text: String,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 3000L,
    val fontStyle: TextFontStyle = TextFontStyle.CAPCUT_BOLD,
    val color: Long = 0xFFFFFFFF,
    val backgroundColor: Long? = 0x88000000,
    val animation: TextAnimationType = TextAnimationType.POP_UP,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scale: Float = 1.0f,
    /** Extra weight on top of the font style's own weight. */
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    /** Letter spacing in em (0 = normal, 0.1 = wide). */
    val letterSpacingEm: Float = 0f,
    /** Line height multiplier (1 = normal, 1.5 = airy). */
    val lineSpacingMultiplier: Float = 1f,
    /**
     * Word timings for the karaoke highlight, in speaking order. Times are
     * relative to this overlay's start; empty for plain (non-caption) text.
     */
    val karaokeWords: List<KaraokeWord> = emptyList(),
    /** When true, the word being spoken is highlighted while the caption shows. */
    val karaokeEnabled: Boolean = false,
    val keyframes: List<Keyframe> = emptyList()
) {
    /**
     * The karaoke word active at [localMs] (overlay-relative), or null when the
     * highlight is off, the words are missing, or nothing is being spoken.
     */
    fun activeKaraokeIndex(localMs: Long): Int? {
        if (!karaokeEnabled || karaokeWords.isEmpty()) return null
        return karaokeWords.indexOfFirst { localMs >= it.startMs && localMs < it.endMs }
            .takeIf { it >= 0 }
    }

    /** Start offsets of each karaoke word inside [text]; nulls when mismatched. */
    fun karaokeOffsetsInText(): List<Int>? {
        if (karaokeWords.isEmpty()) return null
        var rebuilt = ""
        val offsets = karaokeWords.map { word ->
            if (rebuilt.isNotEmpty()) rebuilt += " "
            val start = rebuilt.length
            rebuilt += word.text
            start
        }
        // Words joined by single spaces must reproduce the caption text.
        val expected = karaokeWords.joinToString(" ") { it.text }
        return if (expected == text.trim()) offsets else null
    }
}