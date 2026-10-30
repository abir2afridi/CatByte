package com.example.model

import java.util.UUID

enum class AspectRatioType(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    RATIO_9_16("9:16", 9f, 16f),
    RATIO_16_9("16:9", 16f, 9f),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_4_5("4:5", 4f, 5f)
}

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

enum class TextAnimationType(val displayName: String) {
    NONE("None"),
    TYPEWRITER("Typewriter"),
    POP_UP("Pop Up"),
    WAVE("Wave Bounce"),
    NEON_PULSE("Neon Pulse"),
    FADE_IN("Fade In")
}

enum class TextFontStyle(val displayName: String) {
    CAPCUT_BOLD("CapCut Bold"),
    CLEAN_SANS("Clean Sans"),
    CINEMATIC_SERIF("Cinematic Serif"),
    NEON_GLOW("Neon Glow"),
    SUBTITLE("Clean Subtitle")
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

enum class KeyframeProperty(val displayName: String) {
    POSITION_X("Position X"),
    POSITION_Y("Position Y"),
    SCALE("Scale"),
    ROTATION("Rotation"),
    OPACITY("Opacity"),
    VOLUME("Volume")
}

enum class EasingCurve(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In-Out"),
    BOUNCE("Bounce")
}

enum class MaskType(val displayName: String) {
    NONE("None"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    SPLIT("Split Line"),
    MIRROR("Mirror"),
    FILMSTRIP("Filmstrip")
}

// Phase 2 Enums & Models
enum class SpeedCurvePreset(val displayName: String) {
    STANDARD("Standard (Constant)"),
    MONTAGE("Montage Wave"),
    HERO_MOMENT("Hero Moment"),
    BULLET_TIME("Bullet Time"),
    JUMP_CUT("Flash Fast"),
    CUSTOM("Custom Bezier")
}

data class BezierPoint(
    val x: Float, // 0.0f to 1.0f (Timeline Ratio)
    val speedMultiplier: Float // 0.1f to 10.0f
)

data class SpeedCurve(
    val preset: SpeedCurvePreset = SpeedCurvePreset.STANDARD,
    val points: List<BezierPoint> = listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
)

enum class HslChannel(val displayName: String, val colorHex: Long) {
    RED("Red", 0xFFFF0000),
    ORANGE("Orange", 0xFFFFA500),
    YELLOW("Yellow", 0xFFFFFF00),
    GREEN("Green", 0xFF00FF00),
    CYAN("Cyan", 0xFF00FFFF),
    BLUE("Blue", 0xFF0000FF),
    PURPLE("Purple", 0xFF800080),
    MAGENTA("Magenta", 0xFFFF00FF)
}

// Phase 1 Data Models
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val property: KeyframeProperty,
    val value: Float,
    val easing: EasingCurve = EasingCurve.EASE_IN_OUT
)

data class MaskData(
    val type: MaskType = MaskType.NONE,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val width: Float = 0.8f,
    val height: Float = 0.8f,
    val rotation: Float = 0f,
    val feather: Float = 0.1f,
    val isInverted: Boolean = false
)

data class HslAdjustment(
    val channel: HslChannel,
    val hue: Float = 0f,        // -180f to +180f
    val saturation: Float = 0f, // -100f to +100f
    val luminance: Float = 0f   // -100f to +100f
)

data class MediaClip(
    val id: String,
    val title: String,
    val uriString: String? = null,
    val drawableResId: Int? = null,
    val gradientColors: List<Long> = listOf(0xFF1E2640, 0xFF0D1B2A),
    val startTimelineMs: Long = 0L,
    /** Length of this clip on the timeline (after trim and speed). */
    val durationMs: Long = 3000L,
    /** Full length of the source media in milliseconds. */
    val originalDurationMs: Long = 3000L,
    /** Start position inside the source media (milliseconds). */
    val trimStartMs: Long = 0L,
    /** End position inside the source media (milliseconds). 0 means "until the end". */
    val trimEndMs: Long = 0L,
    val speed: Float = 1.0f,
    val speedCurve: SpeedCurve = SpeedCurve(),
    val volume: Float = 1.0f,
    val filter: VideoFilter = VideoFilter.NONE,
    val transition: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 400L,
    val animationIn: ClipAnimation = ClipAnimation.NONE,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val vignette: Float = 0f,
    val rotation: Float = 0f,
    val isMirrored: Boolean = false,
    val waveform: List<Float> = listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 0.6f, 0.4f, 0.9f, 0.3f, 0.7f),

    // Phase 1 Additions
    val blendMode: BlendMode = BlendMode.NORMAL,
    val keyframes: List<Keyframe> = emptyList(),
    val mask: MaskData = MaskData(),
    val hslAdjustments: List<HslAdjustment> = emptyList(),
    val scale: Float = 1.0f,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val opacity: Float = 1.0f
)

data class VideoTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Main Track",
    val isMainTrack: Boolean = true,
    val clips: List<MediaClip> = emptyList()
)

data class AudioClip(
    val id: String,
    val title: String,
    val artist: String,
    /** Source audio file. Null for the built-in library entries. */
    val uriString: String? = null,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 6000L,
    val volume: Float = 1.0f,
    val isVoiceover: Boolean = false,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val waveform: List<Float> = listOf(0.2f, 0.5f, 0.8f, 0.9f, 0.6f, 0.4f, 0.7f, 0.9f, 0.8f, 0.5f, 0.3f, 0.6f)
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
    val keyframes: List<Keyframe> = emptyList()
)

data class StickerOverlay(
    val id: String,
    val symbol: String,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 3000L,
    val posX: Float = 0.5f,
    val posY: Float = 0.3f,
    val scale: Float = 1.0f,
    val keyframes: List<Keyframe> = emptyList()
)

data class TimelineEffect(
    val id: String,
    val name: String,
    val effectType: EffectType,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 2000L
)

data class VideoProject(
    val id: String,
    val title: String,
    val aspectRatio: AspectRatioType = AspectRatioType.RATIO_9_16,
    val clips: List<MediaClip> = emptyList(),
    val overlayTracks: List<VideoTrack> = emptyList(),
    val audioTracks: List<AudioClip> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val stickers: List<StickerOverlay> = emptyList(),
    val effects: List<TimelineEffect> = emptyList(),
    val thumbnailResId: Int? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val resolution: String = "1080P",
    val fps: Int = 30,
    val isFavorite: Boolean = false,
    val paperStyle: String = "Drafting Grid"
) {
    val allVideoClips: List<MediaClip>
        get() = clips + overlayTracks.flatMap { it.clips }

    val totalDurationMs: Long
        get() {
            val maxMainClipEnd = clips.sumOf { it.durationMs }
            val maxOverlayClipEnd = overlayTracks.flatMap { it.clips }.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val maxAudioEnd = audioTracks.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val maxTextEnd = textOverlays.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val maxEffectEnd = effects.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            return maxOf(maxMainClipEnd, maxOverlayClipEnd, maxAudioEnd, maxTextEnd, maxEffectEnd, 1000L)
        }
}
