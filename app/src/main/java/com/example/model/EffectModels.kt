package com.example.model

enum class VideoFilter(val displayName: String, val description: String) {
    NONE("Original", "Natural colors"),
    SKETCH_GRAPHITE("2B Graphite", "Hand-drawn pencil linework & hatching"),
    BLUEPRINT("Blueprint", "Cyanotype architectural draft"),
    SEPIA_NOTEBOOK("Sepia Paper", "Aged vintage manuscript"),
    CHARCOAL_SMUDGE("Charcoal", "Smudged dramatic charcoal shading"),
    HALFTONE_MANGA("Manga Dots", "Classic comic dot screen tone"),
    CYBERPUNK("Cyberpunk", "Electric neon & deep blues"),
    VINTAGE("Vintage", "Warm nostalgic retro 90s"),
    GOLDEN_HOUR("Golden", "Warm sunset glow"),
    NOIR("Noir", "High-contrast monochrome"),
    TEAL_ORANGE("Blockbuster", "Cinematic Hollywood look"),
    MOODY("Moody", "Desaturated dark tones"),
    GLITCH_ART("Glitch Art", "RGB split & scanlines")
}

enum class TransitionType(val displayName: String) {
    NONE("None"),
    PAGE_FLIP("Page Turn"),
    PAPER_TEAR("Paper Tear"),
    PENCIL_WIPE("Pencil Wipe"),
    INK_BLEED("Ink Bleed"),
    FADE_BLACK("Fade Black"),
    WHIP_PAN("Whip Pan"),
    ZOOM_IN("Zoom In"),
    GLITCH("Glitch"),
    FLASH_WHITE("Flash White"),
    DISSOLVE("Dissolve")
}

enum class ClipAnimation(val displayName: String) {
    NONE("None"),
    ZOOM_IN("Zoom In"),
    SWIPE_RIGHT("Swipe Right"),
    BOUNCE("Bounce"),
    SPIN("Spin 360"),
    FADE_IN("Fade In")
}

enum class EffectType(val displayName: String, val iconName: String) {
    RGB_SPLIT("RGB Split", "split"),
    SHAKE("Beat Shake", "vibrate"),
    VHS_GRAIN("VHS Grain", "film"),
    NEON_HALO("Neon Halo", "glow"),
    LIGHTNING("Lightning", "bolt"),
    ZOOM_BLUR("Zoom Blur", "zoom")
}

// Phase 1 Enums
enum class BlendMode(val displayName: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    COLOR_DODGE("Color Dodge")
}