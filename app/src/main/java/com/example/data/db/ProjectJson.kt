package com.example.data.db

import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.StickerOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts the editing model to and from JSON so projects can be stored in the
 * Room database. Every field has a safe fallback so older rows keep working.
 */
object ProjectJson {

    fun toJson(project: VideoProject): String = projectToJson(project).toString()

    fun fromJson(raw: String): VideoProject? = runCatching {
        projectFromJson(JSONObject(raw))
    }.getOrNull()

    // -----------------------------------------------------------------
    // Project
    // -----------------------------------------------------------------

    private fun projectToJson(project: VideoProject): JSONObject = JSONObject().apply {
        put("id", project.id)
        put("title", project.title)
        put("aspectRatio", project.aspectRatio.name)
        put("resolution", project.resolution)
        put("fps", project.fps)
        put("paperStyle", project.paperStyle)
        put("isFavorite", project.isFavorite)
        put("lastModified", project.lastModified)
        put("thumbnailResId", project.thumbnailResId ?: JSONObject.NULL)
        put("clips", JSONArray().also { array -> project.clips.forEach { array.put(clipToJson(it)) } })
        put(
            "overlayTracks",
            JSONArray().also { array ->
                project.overlayTracks.forEach { track ->
                    array.put(
                        JSONObject().apply {
                            put("id", track.id)
                            put("name", track.name)
                            put("isMainTrack", track.isMainTrack)
                            put(
                                "clips",
                                JSONArray().also { clips -> track.clips.forEach { clips.put(clipToJson(it)) } }
                            )
                        }
                    )
                }
            }
        )
        put(
            "audioTracks",
            JSONArray().also { array -> project.audioTracks.forEach { array.put(audioToJson(it)) } }
        )
        put(
            "textOverlays",
            JSONArray().also { array -> project.textOverlays.forEach { array.put(textToJson(it)) } }
        )
        put(
            "stickers",
            JSONArray().also { array -> project.stickers.forEach { array.put(stickerToJson(it)) } }
        )
        put(
            "effects",
            JSONArray().also { array -> project.effects.forEach { array.put(effectToJson(it)) } }
        )
    }

    private fun projectFromJson(json: JSONObject): VideoProject = VideoProject(
        id = json.optString("id"),
        title = json.optString("title", "Untitled"),
        aspectRatio = parseEnum(json.optString("aspectRatio"), AspectRatioType.RATIO_9_16),
        clips = json.optJSONArray("clips").mapObjects { clipFromJson(it) },
        overlayTracks = json.optJSONArray("overlayTracks").mapObjects { trackJson ->
            VideoTrack(
                id = trackJson.optString("id"),
                name = trackJson.optString("name", "Track"),
                isMainTrack = trackJson.optBoolean("isMainTrack", false),
                clips = trackJson.optJSONArray("clips").mapObjects { clipFromJson(it) }
            )
        },
        audioTracks = json.optJSONArray("audioTracks").mapObjects { audioFromJson(it) },
        textOverlays = json.optJSONArray("textOverlays").mapObjects { textFromJson(it) },
        stickers = json.optJSONArray("stickers").mapObjects { stickerFromJson(it) },
        effects = json.optJSONArray("effects").mapObjects { effectFromJson(it) },
        thumbnailResId = if (json.isNull("thumbnailResId")) null else json.optInt("thumbnailResId"),
        lastModified = json.optLong("lastModified", System.currentTimeMillis()),
        resolution = json.optString("resolution", "1080P"),
        fps = json.optInt("fps", 30),
        isFavorite = json.optBoolean("isFavorite", false),
        paperStyle = json.optString("paperStyle", "Drafting Grid")
    )

    // -----------------------------------------------------------------
    // Clips
    // -----------------------------------------------------------------

    private fun clipToJson(clip: MediaClip): JSONObject = JSONObject().apply {
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
        put("volume", clip.volume.toDouble())
        put("filter", clip.filter.name)
        put("transition", clip.transition.name)
        put("transitionDurationMs", clip.transitionDurationMs)
        put("animationIn", clip.animationIn.name)
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
    }

    private fun clipFromJson(json: JSONObject): MediaClip = MediaClip(
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
        volume = json.optDouble("volume", 1.0).toFloat(),
        filter = parseEnum(json.optString("filter"), VideoFilter.NONE),
        transition = parseEnum(json.optString("transition"), TransitionType.NONE),
        transitionDurationMs = json.optLong("transitionDurationMs", 400L),
        animationIn = parseEnum(json.optString("animationIn"), ClipAnimation.NONE),
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
        opacity = json.optDouble("opacity", 1.0).toFloat()
    )

    // -----------------------------------------------------------------
    // Audio / text / stickers / effects
    // -----------------------------------------------------------------

    private fun audioToJson(audio: AudioClip): JSONObject = JSONObject().apply {
        put("id", audio.id)
        put("title", audio.title)
        put("artist", audio.artist)
        put("uriString", audio.uriString ?: JSONObject.NULL)
        put("startTimelineMs", audio.startTimelineMs)
        put("durationMs", audio.durationMs)
        put("volume", audio.volume.toDouble())
        put("isVoiceover", audio.isVoiceover)
        put("isMuted", audio.isMuted)
        put("fadeInMs", audio.fadeInMs)
        put("fadeOutMs", audio.fadeOutMs)
        put("waveform", JSONArray(audio.waveform))
    }

    private fun audioFromJson(json: JSONObject): AudioClip = AudioClip(
        id = json.optString("id"),
        title = json.optString("title", "Audio"),
        artist = json.optString("artist", ""),
        uriString = json.optStringOrNull("uriString"),
        startTimelineMs = json.optLong("startTimelineMs"),
        durationMs = json.optLong("durationMs", 6000L),
        volume = json.optDouble("volume", 1.0).toFloat(),
        isVoiceover = json.optBoolean("isVoiceover", false),
        isMuted = json.optBoolean("isMuted", false),
        fadeInMs = json.optLong("fadeInMs"),
        fadeOutMs = json.optLong("fadeOutMs"),
        waveform = json.optJSONArray("waveform").toFloatList()
    )

    private fun textToJson(text: TextOverlay): JSONObject = JSONObject().apply {
        put("id", text.id)
        put("text", text.text)
        put("startTimelineMs", text.startTimelineMs)
        put("durationMs", text.durationMs)
        put("fontStyle", text.fontStyle.name)
        put("color", text.color)
        put("backgroundColor", text.backgroundColor ?: JSONObject.NULL)
        put("animation", text.animation.name)
        put("posX", text.posX.toDouble())
        put("posY", text.posY.toDouble())
        put("scale", text.scale.toDouble())
        put("keyframes", JSONArray().also { array -> text.keyframes.forEach { array.put(keyframeToJson(it)) } })
    }

    private fun textFromJson(json: JSONObject): TextOverlay = TextOverlay(
        id = json.optString("id"),
        text = json.optString("text", ""),
        startTimelineMs = json.optLong("startTimelineMs"),
        durationMs = json.optLong("durationMs", 3000L),
        fontStyle = parseEnum(json.optString("fontStyle"), TextFontStyle.CAPCUT_BOLD),
        color = json.optLong("color", 0xFFFFFFFF),
        backgroundColor = if (json.isNull("backgroundColor")) null else json.optLong("backgroundColor"),
        animation = parseEnum(json.optString("animation"), TextAnimationType.POP_UP),
        posX = json.optDouble("posX", 0.5).toFloat(),
        posY = json.optDouble("posY", 0.5).toFloat(),
        scale = json.optDouble("scale", 1.0).toFloat(),
        keyframes = json.optJSONArray("keyframes").mapObjects { keyframeFromJson(it) }
    )

    private fun stickerToJson(sticker: StickerOverlay): JSONObject = JSONObject().apply {
        put("id", sticker.id)
        put("symbol", sticker.symbol)
        put("startTimelineMs", sticker.startTimelineMs)
        put("durationMs", sticker.durationMs)
        put("posX", sticker.posX.toDouble())
        put("posY", sticker.posY.toDouble())
        put("scale", sticker.scale.toDouble())
        put("keyframes", JSONArray().also { array -> sticker.keyframes.forEach { array.put(keyframeToJson(it)) } })
    }

    private fun stickerFromJson(json: JSONObject): StickerOverlay = StickerOverlay(
        id = json.optString("id"),
        symbol = json.optString("symbol", "â­"),
        startTimelineMs = json.optLong("startTimelineMs"),
        durationMs = json.optLong("durationMs", 3000L),
        posX = json.optDouble("posX", 0.5).toFloat(),
        posY = json.optDouble("posY", 0.3).toFloat(),
        scale = json.optDouble("scale", 1.0).toFloat(),
        keyframes = json.optJSONArray("keyframes").mapObjects { keyframeFromJson(it) }
    )

    private fun effectToJson(effect: TimelineEffect): JSONObject = JSONObject().apply {
        put("id", effect.id)
        put("name", effect.name)
        put("effectType", effect.effectType.name)
        put("startTimelineMs", effect.startTimelineMs)
        put("durationMs", effect.durationMs)
    }

    private fun effectFromJson(json: JSONObject): TimelineEffect = TimelineEffect(
        id = json.optString("id"),
        name = json.optString("name", "Effect"),
        effectType = parseEnum(json.optString("effectType"), EffectType.RGB_SPLIT),
        startTimelineMs = json.optLong("startTimelineMs"),
        durationMs = json.optLong("durationMs", 2000L)
    )

    private fun keyframeToJson(keyframe: Keyframe): JSONObject = JSONObject().apply {
        put("id", keyframe.id)
        put("timestampMs", keyframe.timestampMs)
        put("property", keyframe.property.name)
        put("value", keyframe.value.toDouble())
        put("easing", keyframe.easing.name)
    }

    private fun keyframeFromJson(json: JSONObject): Keyframe = Keyframe(
        id = json.optString("id"),
        timestampMs = json.optLong("timestampMs"),
        property = parseEnum(json.optString("property"), KeyframeProperty.POSITION_X),
        value = json.optDouble("value", 0.0).toFloat(),
        easing = parseEnum(json.optString("easing"), EasingCurve.EASE_IN_OUT)
    )

    private fun maskToJson(mask: MaskData): JSONObject = JSONObject().apply {
        put("type", mask.type.name)
        put("posX", mask.posX.toDouble())
        put("posY", mask.posY.toDouble())
        put("width", mask.width.toDouble())
        put("height", mask.height.toDouble())
        put("rotation", mask.rotation.toDouble())
        put("feather", mask.feather.toDouble())
        put("isInverted", mask.isInverted)
    }

    private fun maskFromJson(json: JSONObject): MaskData = MaskData(
        type = parseEnum(json.optString("type"), MaskType.NONE),
        posX = json.optDouble("posX", 0.5).toFloat(),
        posY = json.optDouble("posY", 0.5).toFloat(),
        width = json.optDouble("width", 0.8).toFloat(),
        height = json.optDouble("height", 0.8).toFloat(),
        rotation = json.optDouble("rotation", 0.0).toFloat(),
        feather = json.optDouble("feather", 0.1).toFloat(),
        isInverted = json.optBoolean("isInverted", false)
    )

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return value.takeIf { it.isNotBlank() }
    }

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            optJSONObject(index)?.let { result += transform(it) }
        }
        return result
    }

    private fun JSONArray?.toFloatList(): List<Float> {
        if (this == null) return emptyList()
        val result = mutableListOf<Float>()
        for (index in 0 until length()) {
            result += optDouble(index, 0.0).toFloat()
        }
        return result
    }

    private fun JSONArray?.toLongList(): List<Long> {
        if (this == null) return emptyList()
        val result = mutableListOf<Long>()
        for (index in 0 until length()) {
            result += optLong(index, 0L)
        }
        return result
    }
}
