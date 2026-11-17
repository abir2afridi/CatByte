package com.example.model

/**
 * Clip-level audio processing ("Audio effects", "Enhance voice", "Reduce
 * noise" in the editor). Everything here runs offline on the device - no
 * neural models, no cloud; the honest presets are DSP that really runs.
 */
data class AudioEffectSettings(
    /** Creative voice effect. Pitch presets are applied via Sonic. */
    val type: VoiceEffectType = VoiceEffectType.NONE,
    /** 0..1 depth of the creative effect (wet mix, feedback, ring depth). */
    val intensity: Float = 0.6f,
    /** Speech-clarity EQ (90 Hz rumble cut + 3 kHz presence shelf). */
    val enhanceVoice: Boolean = false,
    /** Basic peak-envelope noise gate. */
    val reduceNoise: Boolean = false
) {
    val isNoOp: Boolean
        get() = type == VoiceEffectType.NONE && !enhanceVoice && !reduceNoise
}

/** The creative effects of the "Audio effects" sheet. */
enum class VoiceEffectType(val displayName: String) {
    NONE("None"),
    ECHO("Echo"),
    REVERB("Reverb"),
    ROBOT("Robot"),
    /** Lower pitch, same duration (Sonic pitch 0.7). */
    DEEP("Deep"),
    /** Higher pitch, same duration (Sonic pitch 1.5). */
    CHIPMUNK("Chipmunk");

    /** Sonic pitch factor for this preset, or 1 when pitch is unchanged. */
    val pitchFactor: Float
        get() = when (this) {
            DEEP -> 0.7f
            CHIPMUNK -> 1.5f
            else -> 1f
        }

    val usesKernel: Boolean
        get() = this == ECHO || this == REVERB || this == ROBOT
}
