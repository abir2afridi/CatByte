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
    val keyframes: List<Keyframe> = emptyList()
)