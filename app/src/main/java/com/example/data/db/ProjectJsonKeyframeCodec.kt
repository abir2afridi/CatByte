package com.example.data.db

import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import org.json.JSONObject

/**
 * The keyframe and mask codecs.

 * Every field has a safe fallback so older rows keep working.
 */
internal fun keyframeToJson(keyframe: Keyframe): JSONObject = JSONObject().apply {
put("id", keyframe.id)
put("timestampMs", keyframe.timestampMs)
put("property", keyframe.property.name)
put("value", keyframe.value.toDouble())
put("easing", keyframe.easing.name)
}

internal fun keyframeFromJson(json: JSONObject): Keyframe = Keyframe(
id = json.optString("id"),
timestampMs = json.optLong("timestampMs"),
property = parseEnum(json.optString("property"), KeyframeProperty.POSITION_X),
value = json.optDouble("value", 0.0).toFloat(),
easing = parseEnum(json.optString("easing"), EasingCurve.EASE_IN_OUT)
)

internal fun maskToJson(mask: MaskData): JSONObject = JSONObject().apply {
put("type", mask.type.name)
put("posX", mask.posX.toDouble())
put("posY", mask.posY.toDouble())
put("width", mask.width.toDouble())
put("height", mask.height.toDouble())
put("rotation", mask.rotation.toDouble())
put("feather", mask.feather.toDouble())
put("isInverted", mask.isInverted)
}

internal fun maskFromJson(json: JSONObject): MaskData = MaskData(
type = parseEnum(json.optString("type"), MaskType.NONE),
posX = json.optDouble("posX", 0.5).toFloat(),
posY = json.optDouble("posY", 0.5).toFloat(),
width = json.optDouble("width", 0.8).toFloat(),
height = json.optDouble("height", 0.8).toFloat(),
rotation = json.optDouble("rotation", 0.0).toFloat(),
feather = json.optDouble("feather", 0.1).toFloat(),
isInverted = json.optBoolean("isInverted", false)
)
