package com.example.model

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
    /** Keep the original pitch when the clip is retimed (CapCut's default). */
    val maintainPitch: Boolean = true,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
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
    val opacity: Float = 1.0f,
    /** Real GPU directional motion blur applied to this clip (Edit → Motion blur). */
    val motionBlur: Boolean = false,
    val isReversed: Boolean = false,
    val chromaKey: ChromaKeyData = ChromaKeyData(),

    /** Source pixel dimensions, 0 when unknown (colour clips, unprobed media). */
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    /** Set when the loudness-adjustment switch normalised this clip's gain. */
    val isLoudnessAdjusted: Boolean = false,
    /** Per-clip audio processing (voice presets, enhance voice, noise gate). */
    val audioEffects: AudioEffectSettings = AudioEffectSettings(),
    /** Fade-in of the clip's own audio, in output-timeline milliseconds. */
    val audioFadeInMs: Long = 0L,
    /** Fade-out of the clip's own audio, in output-timeline milliseconds. */
    val audioFadeOutMs: Long = 0L
) {
    /**
     * True when this clip has no real media behind it, e.g. the legacy
     * "Meadow & Sky" demo placeholder. Such clips render as gaps and can be
     * discarded when the user imports real footage.
     */
    val isPlaceholderClip: Boolean
        get() = uriString.isNullOrBlank() && drawableResId == null
}