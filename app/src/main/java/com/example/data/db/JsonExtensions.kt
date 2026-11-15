package com.example.data.db

import org.json.JSONArray
import org.json.JSONObject

/**
 * Small JSON helpers shared by the project codecs.

 * They all tolerate missing or null fields so an older row never crashes
 * the loader.
 */
internal inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)

internal fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return value.takeIf { it.isNotBlank() }
    }

internal fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            optJSONObject(index)?.let { result += transform(it) }
        }
        return result
    }

internal fun JSONArray?.toFloatList(): List<Float> {
        if (this == null) return emptyList()
        val result = mutableListOf<Float>()
        for (index in 0 until length()) {
            result += optDouble(index, 0.0).toFloat()
        }
        return result
    }

internal fun JSONArray?.toLongList(): List<Long> {
        if (this == null) return emptyList()
        val result = mutableListOf<Long>()
        for (index in 0 until length()) {
            result += optLong(index, 0L)
        }
        return result
    }
