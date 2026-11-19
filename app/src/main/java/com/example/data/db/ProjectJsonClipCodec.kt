package com.example.data.db

import com.example.engine.ClipAnimationMath
import com.example.model.AudioCompressor
import com.example.model.AudioEffectSettings
import com.example.model.AudioEqualizer
import com.example.model.BezierPoint
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.MaskData
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VoiceEffectType
import org.json.JSONArray
import org.json.JSONObject

/**
 * The clip codec: MediaClip <-> JSON, including the speed curve.

 * Every field has a safe fallback so older rows keep working.
 */
internal fun clipToJson(clip: MediaClip): JSONObject = JSONObject().apply {
    put("id", clip.id)
    put("title", clip.title)
    put("uriString", clip.uriString ?: JSONObject.NULL)
    put("drawableResId", clip.drawableResId ?: JSONObject.NULL)
    put("gradientColors", JSONArray(clip.gradientColors))
    put("startTimelineMs", clip.startTimelineMs)
    put("durationMs", clip.durationMs)
    put("originalDurationMs", clip.originalDurationMs)
    put("trimStartMs", clip.trimStartMs)
    put("trimEndMs", clip.trimEndMs)
    put("speed", clip.speed.toDouble())
    put("speedCurve", speedCurveToJson(clip.speedCurve))
    put("maintainPitch", clip.maintainPitch)
    put("volume", clip.volume.toDouble())
    put("isMuted", clip.isMuted)
    put("filter", clip.filter.name)
    put("transition", clip.transition.name)
    put("transitionDurationMs", clip.transitionDurationMs)
    put("animationIn", clip.animationIn.name)
    put("animationDurationMs", clip.animationDurationMs)
    put("brightness", clip.brightness.toDouble())
    put("contrast", clip.contrast.toDouble())
    put("saturation", clip.saturation.toDouble())
    put("vignette", clip.vignette.toDouble())
    put("rotation", clip.rotation.toDouble())
    put("isMirrored", clip.isMirrored)
    put("waveform", JSONArray(clip.waveform))
    put("blendMode", clip.blendMode.name)
    put("keyframes", JSONArray().also { array -> clip.keyframes.forEach { array.put(keyframeToJson(it)) } })
    put("mask", maskToJson(clip.mask))
    put(
        "hslAdjustments",
        JSONArray().also { array ->
            clip.hslAdjustments.forEach {
                array.put(
                    JSONObject().apply {
                        put("channel", it.channel.name)
                        put("hue", it.hue.toDouble())
                        put("saturation", it.saturation.toDouble())
                        put("luminance", it.luminance.toDouble())
                    }
                )
            }
        }
    )
    put("scale", clip.scale.toDouble())
    put("posX", clip.posX.toDouble())
    put("posY", clip.posY.toDouble())
    put("opacity", clip.opacity.toDouble())
    put("motionBlur", clip.motionBlur)
    put("isReversed", clip.isReversed)
    put("sourceWidth", clip.sourceWidth)
    put("sourceHeight", clip.sourceHeight)
    put("isLoudnessAdjusted", clip.isLoudnessAdjusted)
    put("audioEffects", audioEffectsToJson(clip.audioEffects))
    put("audioFadeInMs", clip.audioFadeInMs)
    put("audioFadeOutMs", clip.audioFadeOutMs)
    put("chromaKey", JSONObject().apply {
        put("isEnabled", clip.chromaKey.isEnabled)
        put("keyColorHex", clip.chromaKey.keyColorHex)
        put("tolerance", clip.chromaKey.tolerance.toDouble())
        put("softness", clip.chromaKey.softness.toDouble())
        put("spill", clip.chromaKey.spill.toDouble())
    })
}

internal fun clipFromJson(json: JSONObject): MediaClip = MediaClip(
    id = json.optString("id"),
    title = json.optString("title", "Clip"),
    uriString = json.optStringOrNull("uriString"),
    drawableResId = if (json.isNull("drawableResId")) null else json.optInt("drawableResId"),
    gradientColors = json.optJSONArray("gradientColors").toLongList()
        .ifEmpty { listOf(0xFF1E2640, 0xFF0D1B2A) },
    startTimelineMs = json.optLong("startTimelineMs"),
    durationMs = json.optLong("durationMs", 3000L),
    originalDurationMs = json.optLong("originalDurationMs", 3000L),
    trimStartMs = json.optLong("trimStartMs"),
    trimEndMs = json.optLong("trimEndMs"),
    speed = json.optDouble("speed", 1.0).toFloat(),
    speedCurve = json.optJSONObject("speedCurve")?.let { speedCurveFromJson(it) }
        ?: SpeedCurve(),
    maintainPitch = json.optBoolean("maintainPitch", true),
    volume = json.optDouble("volume", 1.0).toFloat(),
    isMuted = json.optBoolean("isMuted", false),
    filter = parseEnum(json.optString("filter"), VideoFilter.NONE),
    transition = parseEnum(json.optString("transition"), TransitionType.NONE),
    transitionDurationMs = json.optLong("transitionDurationMs", 400L),
    animationIn = parseEnum(json.optString("animationIn"), ClipAnimation.NONE),
    animationDurationMs = ClipAnimationMath.sanitizeDuration(
        json.optLong("animationDurationMs", ClipAnimationMath.DURATION_MS)
    ),
    brightness = json.optDouble("brightness", 0.0).toFloat(),
    contrast = json.optDouble("contrast", 1.0).toFloat(),
    saturation = json.optDouble("saturation", 1.0).toFloat(),
    vignette = json.optDouble("vignette", 0.0).toFloat(),
    rotation = json.optDouble("rotation", 0.0).toFloat(),
    isMirrored = json.optBoolean("isMirrored", false),
    waveform = json.optJSONArray("waveform").toFloatList(),
    blendMode = parseEnum(json.optString("blendMode"), BlendMode.NORMAL),
    keyframes = json.optJSONArray("keyframes").mapObjects { keyframeFromJson(it) },
    mask = json.optJSONObject("mask")?.let { maskFromJson(it) } ?: MaskData(),
    hslAdjustments = json.optJSONArray("hslAdjustments").mapObjects {
        HslAdjustment(
            channel = parseEnum(it.optString("channel"), HslChannel.RED),
            hue = it.optDouble("hue", 0.0).toFloat(),
            saturation = it.optDouble("saturation", 0.0).toFloat(),
            luminance = it.optDouble("luminance", 0.0).toFloat()
        )
    },
    scale = json.optDouble("scale", 1.0).toFloat(),
    posX = json.optDouble("posX", 0.5).toFloat(),
    posY = json.optDouble("posY", 0.5).toFloat(),
    opacity = json.optDouble("opacity", 1.0).toFloat(),
    motionBlur = json.optBoolean("motionBlur", false),
    isReversed = json.optBoolean("isReversed", false),
    sourceWidth = json.optInt("sourceWidth", 0),
    sourceHeight = json.optInt("sourceHeight", 0),
    isLoudnessAdjusted = json.optBoolean("isLoudnessAdjusted", false),
    audioEffects = audioEffectsFromJson(json.optJSONObject("audioEffects")),
    audioFadeInMs = json.optLong("audioFadeInMs", 0L),
    audioFadeOutMs = json.optLong("audioFadeOutMs", 0L),
    chromaKey = json.optJSONObject("chromaKey")?.let { cj ->
        com.example.model.ChromaKeyData(
            isEnabled = cj.optBoolean("isEnabled", false),
            keyColorHex = cj.optLong("keyColorHex", 0xFF00FF00),
            tolerance = cj.optDouble("tolerance", 0.25).toFloat(),
            softness = cj.optDouble("softness", 0.20).toFloat(),
            spill = cj.optDouble("spill", 0.30).toFloat()
        )
    } ?: com.example.model.ChromaKeyData()
)

/**
 * The audio-effects codec: [AudioEffectSettings] <-> JSON.
 *
 * Unknown preset names and malformed numbers fall back to safe defaults so a
 * project written by a newer build still opens on an older one.
 */
internal fun audioEffectsToJson(settings: AudioEffectSettings): JSONObject = JSONObject().apply {
    put("type", settings.type.name)
    put("intensity", settings.intensity.toDouble())
    put("enhanceVoice", settings.enhanceVoice)
    put("isolateVoice", settings.isolateVoice)
    put("reduceNoise", settings.reduceNoise)
}

internal fun audioEffectsFromJson(json: JSONObject?): AudioEffectSettings {
    if (json == null) return AudioEffectSettings()
    val type = json.optString("type")
        .let { name -> VoiceEffectType.entries.firstOrNull { it.name == name } }
        ?: VoiceEffectType.NONE
    val intensity = json.optDouble("intensity", AudioEffectSettings().intensity.toDouble())
        .toFloat()
    return AudioEffectSettings(
        type = type,
        intensity = if (intensity.isFinite()) intensity.coerceIn(0f, 1f) else 0.6f,
        enhanceVoice = json.optBoolean("enhanceVoice", false),
        isolateVoice = json.optBoolean("isolateVoice", false),
        reduceNoise = json.optBoolean("reduceNoise", false)
    )
}

/**
 * The mixer EQ codec: [AudioEqualizer] <-> JSON.
 *
 * Missing bands fall back to flat and out-of-range or non-numeric gains are
 * clamped by [AudioEqualizer.sanitized], so a malformed project cannot place
 * a 400 dB shelf in the render chain.
 */
internal fun equalizerToJson(equalizer: AudioEqualizer): JSONObject = JSONObject().apply {
    put("lowGainDb", equalizer.lowGainDb.toDouble())
    put("midGainDb", equalizer.midGainDb.toDouble())
    put("highGainDb", equalizer.highGainDb.toDouble())
}

internal fun equalizerFromJson(json: JSONObject?): AudioEqualizer {
    if (json == null) return AudioEqualizer()
    return AudioEqualizer(
        lowGainDb = json.optDouble("lowGainDb", 0.0).toFloat(),
        midGainDb = json.optDouble("midGainDb", 0.0).toFloat(),
        highGainDb = json.optDouble("highGainDb", 0.0).toFloat()
    ).sanitized()
}

/**
 * The mixer compressor codec: [AudioCompressor] <-> JSON.
 *
 * Missing settings fall back to the defaults and out-of-range or
 * non-numeric values are clamped by [AudioCompressor.sanitized], so a
 * malformed project cannot put a 1000:1 ratio or a negative release time
 * into the render chain.
 */
internal fun compressorToJson(compressor: AudioCompressor): JSONObject = JSONObject().apply {
    put("enabled", compressor.enabled)
    put("thresholdDb", compressor.thresholdDb.toDouble())
    put("ratio", compressor.ratio.toDouble())
    put("makeupGainDb", compressor.makeupGainDb.toDouble())
    put("attackMs", compressor.attackMs.toDouble())
    put("releaseMs", compressor.releaseMs.toDouble())
}

internal fun compressorFromJson(json: JSONObject?): AudioCompressor {
    if (json == null) return AudioCompressor()
    return AudioCompressor(
        enabled = json.optBoolean("enabled", false),
        thresholdDb = json.optDouble("thresholdDb", AudioCompressor.DEFAULT_THRESHOLD_DB.toDouble()).toFloat(),
        ratio = json.optDouble("ratio", AudioCompressor.DEFAULT_RATIO.toDouble()).toFloat(),
        makeupGainDb = json.optDouble("makeupGainDb", 0.0).toFloat(),
        attackMs = json.optDouble("attackMs", AudioCompressor.DEFAULT_ATTACK_MS.toDouble()).toFloat(),
        releaseMs = json.optDouble("releaseMs", AudioCompressor.DEFAULT_RELEASE_MS.toDouble()).toFloat()
    ).sanitized()
}

internal fun speedCurveToJson(curve: SpeedCurve): JSONObject = JSONObject().apply {
    put("preset", curve.preset.name)
    put(
        "points",
        JSONArray().also { array ->
            curve.points.forEach { point ->
                array.put(
                    JSONObject().apply {
                        put("x", point.x.toDouble().coerceIn(0.0, 1.0))
                        put("speedMultiplier", point.speedMultiplier.toDouble())
                    }
                )
            }
        }
    )
}

internal fun speedCurveFromJson(json: JSONObject): SpeedCurve {
    val points = json.optJSONArray("points").mapObjects { point ->
        BezierPoint(
            x = point.optDouble("x", 0.0).toFloat().coerceIn(0f, 1f),
            speedMultiplier = point.optDouble("speedMultiplier", 1.0)
                .toFloat()
                .coerceIn(0.05f, 32f)
        )
    }.sortedBy { it.x }
    return SpeedCurve(
        preset = parseEnum(json.optString("preset"), SpeedCurvePreset.STANDARD),
        points = points.ifEmpty {
            listOf(BezierPoint(0f, 1f), BezierPoint(1f, 1f))
        }
    )
}
