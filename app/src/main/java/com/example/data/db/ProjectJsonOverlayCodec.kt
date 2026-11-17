package com.example.data.db

import com.example.engine.AudioTiming
import com.example.model.AudioClip
import com.example.model.EffectType
import com.example.model.StickerOverlay
import com.example.model.StrokeOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.TimelineMarker
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * The overlay codecs: audio clips, text overlays, stickers, timeline
 * effects and markers.

 * Every field has a safe fallback so older rows keep working.
 */
internal fun audioToJson(audio: AudioClip): JSONObject = JSONObject().apply {
    put("id", audio.id)
    put("title", audio.title)
    put("artist", audio.artist)
    put("uriString", audio.uriString ?: JSONObject.NULL)
    put("startTimelineMs", audio.startTimelineMs)
    put("durationMs", audio.durationMs)
    put("originalDurationMs", audio.originalDurationMs)
    put("trimStartMs", audio.trimStartMs)
    put("trimEndMs", audio.trimEndMs)
    put("speed", audio.speed.toDouble())
    put("volume", audio.volume.toDouble())
    put("isVoiceover", audio.isVoiceover)
    put("isMuted", audio.isMuted)
    put("fadeInMs", audio.fadeInMs)
    put("fadeOutMs", audio.fadeOutMs)
    put("waveform", JSONArray(audio.waveform))
    put("audioEffects", audioEffectsToJson(audio.audioEffects))
}

internal fun audioFromJson(json: JSONObject): AudioClip {
    val raw = AudioClip(
        id = json.optString("id"),
        title = json.optString("title", "Audio"),
        artist = json.optString("artist", ""),
        uriString = json.optStringOrNull("uriString"),
        startTimelineMs = json.optLong("startTimelineMs"),
        durationMs = json.optLong("durationMs", 6000L),
        originalDurationMs = json.optLong("originalDurationMs", 0L),
        trimStartMs = json.optLong("trimStartMs"),
        trimEndMs = json.optLong("trimEndMs"),
        speed = json.optDouble("speed", 1.0).toFloat(),
        volume = json.optDouble("volume", 1.0).toFloat(),
        isVoiceover = json.optBoolean("isVoiceover", false),
        isMuted = json.optBoolean("isMuted", false),
        fadeInMs = json.optLong("fadeInMs"),
        fadeOutMs = json.optLong("fadeOutMs"),
        waveform = json.optJSONArray("waveform").toFloatList(),
        audioEffects = audioEffectsFromJson(json.optJSONObject("audioEffects"))
    )
    return AudioTiming.normalize(raw)
}

internal fun textToJson(text: TextOverlay): JSONObject = JSONObject().apply {
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
    put("isBold", text.isBold)
    put("isItalic", text.isItalic)
    put("letterSpacingEm", text.letterSpacingEm.toDouble())
    put("lineSpacingMultiplier", text.lineSpacingMultiplier.toDouble())
    put("keyframes", JSONArray().also { array -> text.keyframes.forEach { array.put(keyframeToJson(it)) } })
}

internal fun textFromJson(json: JSONObject): TextOverlay = TextOverlay(
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
    isBold = json.optBoolean("isBold", false),
    isItalic = json.optBoolean("isItalic", false),
    letterSpacingEm = json.optDouble("letterSpacingEm", 0.0).toFloat(),
    lineSpacingMultiplier = json.optDouble("lineSpacingMultiplier", 1.0).toFloat(),
    keyframes = json.optJSONArray("keyframes").mapObjects { keyframeFromJson(it) }
)

internal fun stickerToJson(sticker: StickerOverlay): JSONObject = JSONObject().apply {
    put("id", sticker.id)
    put("symbol", sticker.symbol)
    put("startTimelineMs", sticker.startTimelineMs)
    put("durationMs", sticker.durationMs)
    put("posX", sticker.posX.toDouble())
    put("posY", sticker.posY.toDouble())
    put("scale", sticker.scale.toDouble())
    put("keyframes", JSONArray().also { array -> sticker.keyframes.forEach { array.put(keyframeToJson(it)) } })
}

internal fun stickerFromJson(json: JSONObject): StickerOverlay = StickerOverlay(
    id = json.optString("id"),
    symbol = json.optString("symbol", "â­"),
    startTimelineMs = json.optLong("startTimelineMs"),
    durationMs = json.optLong("durationMs", 3000L),
    posX = json.optDouble("posX", 0.5).toFloat(),
    posY = json.optDouble("posY", 0.3).toFloat(),
    scale = json.optDouble("scale", 1.0).toFloat(),
    keyframes = json.optJSONArray("keyframes").mapObjects { keyframeFromJson(it) }
)

internal fun strokeToJson(stroke: StrokeOverlay): JSONObject = JSONObject().apply {
    put("id", stroke.id)
    put("points", JSONArray().also { array -> stroke.points.forEach { array.put(it.toDouble()) } })
    put("colorArgb", stroke.colorArgb)
    put("widthFraction", stroke.widthFraction.toDouble())
    put("startTimelineMs", stroke.startTimelineMs)
    put("durationMs", stroke.durationMs)
}

internal fun strokeFromJson(json: JSONObject): StrokeOverlay = StrokeOverlay(
    id = json.optString("id"),
    points = json.optJSONArray("points")?.let { array ->
        (0 until array.length()).map { array.optDouble(it, 0.0).toFloat() }
    }.orEmpty(),
    colorArgb = json.optLong("colorArgb", 0xFFFF3B30),
    widthFraction = json.optDouble("widthFraction", 0.010).toFloat(),
    startTimelineMs = json.optLong("startTimelineMs"),
    durationMs = json.optLong("durationMs", 3000L)
)

internal fun effectToJson(effect: TimelineEffect): JSONObject = JSONObject().apply {
    put("id", effect.id)
    put("name", effect.name)
    put("effectType", effect.effectType.name)
    put("startTimelineMs", effect.startTimelineMs)
    put("durationMs", effect.durationMs)
}

internal fun effectFromJson(json: JSONObject): TimelineEffect = TimelineEffect(
    id = json.optString("id"),
    name = json.optString("name", "Effect"),
    effectType = parseEnum(json.optString("effectType"), EffectType.RGB_SPLIT),
    startTimelineMs = json.optLong("startTimelineMs"),
    durationMs = json.optLong("durationMs", 2000L)
)

internal fun markerToJson(marker: TimelineMarker): JSONObject = JSONObject().apply {
    put("id", marker.id)
    put("atMs", marker.atMs)
    put("label", marker.label)
    put("colorArgb", marker.colorArgb.toString())
}

internal fun markerFromJson(json: JSONObject): TimelineMarker = TimelineMarker(
    id = json.optString("id"),
    atMs = json.optLong("atMs"),
    label = json.optString("label", ""),
    colorArgb = json.optString("colorArgb", "4276944634").toLongOrNull() ?: 0xFFFFC94A
)

/**
 * Decodes markers into a safe, unique list. Duplicate or blank ids would
 * crash Compose's keyed lazy list and make jump/rename actions ambiguous, so
 * malformed ids are replaced while valid ids remain stable across reloads.
 */
internal fun markersFromJson(array: JSONArray?): List<TimelineMarker> {
    if (array == null) return emptyList()
    val usedIds = mutableSetOf<String>()
    return buildList {
        for (index in 0 until array.length()) {
            val marker = array.optJSONObject(index)?.let(::markerFromJson) ?: continue
            val requestedId = marker.id.trim()
            var safeId = requestedId.takeIf { it.isNotEmpty() && it !in usedIds }
            if (safeId == null) {
                do {
                    safeId = "marker_${UUID.randomUUID()}"
                } while (safeId in usedIds)
            }
            usedIds += safeId
            add(
                marker.copy(
                    id = safeId,
                    atMs = marker.atMs.coerceAtLeast(0L),
                    label = marker.label.trim().take(24),
                    colorArgb = marker.colorArgb and 0xFFFFFFFFL
                )
            )
        }
    }
}
