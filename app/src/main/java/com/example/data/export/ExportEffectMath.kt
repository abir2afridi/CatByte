package com.example.data.export

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Small helpers used by the export pipeline. */
internal fun Float.safeScale(): Float = if (abs(this) < 0.0001f) 0.0001f else this

internal fun clampRatio(value: Float): Float = min(1f, max(0f, value))
