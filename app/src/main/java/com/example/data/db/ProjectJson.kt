package com.example.data.db

import com.example.model.AspectRatioType
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts the editing model to and from JSON so projects can be stored in the
 * Room database. Every field has a safe fallback so older rows keep working.

 * This file owns the project itself; the parts live in their own files:
 *  - `ProjectJsonClipCodec.kt`: MediaClip and the speed curve.
 *  - `ProjectJsonOverlayCodec.kt`: audio, text, stickers, effects, markers.
 *  - `ProjectJsonKeyframeCodec.kt`: keyframes and masks.
 *  - `JsonExtensions.kt`: the shared JSON helpers.
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
        put("backgroundImageUri", project.backgroundImageUri)
        put("isFavorite", project.isFavorite)
        put("lastModified", project.lastModified)
        put("isMainTrackMuted", project.isMainTrackMuted)
        put("isMainTrackLocked", project.isMainTrackLocked)
        put("isMainTrackSolo", project.isMainTrackSolo)
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
                            put("isMuted", track.isMuted)
                            put("isLocked", track.isLocked)
                            put("isSolo", track.isSolo)
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
            "markers",
            JSONArray().also { array -> project.markers.forEach { array.put(markerToJson(it)) } }
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
                isMuted = trackJson.optBoolean("isMuted", false),
                isLocked = trackJson.optBoolean("isLocked", false),
                isSolo = trackJson.optBoolean("isSolo", false),
                clips = trackJson.optJSONArray("clips").mapObjects { clipFromJson(it) }
            )
        },
        audioTracks = json.optJSONArray("audioTracks").mapObjects { audioFromJson(it) },
        textOverlays = json.optJSONArray("textOverlays").mapObjects { textFromJson(it) },
        markers = json.optJSONArray("markers").mapObjects { markerFromJson(it) },
        stickers = json.optJSONArray("stickers").mapObjects { stickerFromJson(it) },
        effects = json.optJSONArray("effects").mapObjects { effectFromJson(it) },
        thumbnailResId = if (json.isNull("thumbnailResId")) null else json.optInt("thumbnailResId"),
        lastModified = json.optLong("lastModified", System.currentTimeMillis()),
        resolution = json.optString("resolution", "1080P"),
        fps = json.optInt("fps", 30),
        isFavorite = json.optBoolean("isFavorite", false),
        paperStyle = json.optString("paperStyle", "Drafting Grid"),
        backgroundImageUri = json.optString("backgroundImageUri", ""),
        isMainTrackMuted = json.optBoolean("isMainTrackMuted", false),
        isMainTrackLocked = json.optBoolean("isMainTrackLocked", false),
        isMainTrackSolo = json.optBoolean("isMainTrackSolo", false)
    )
}
