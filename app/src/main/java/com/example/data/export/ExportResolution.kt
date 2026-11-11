package com.example.data.export

import com.example.model.AspectRatioType

/**
 * Export RESOLUTION mapping: the "480P / 720P / 1080P / 2K / 4K" labels shown
 * in the AI UHD dropdown become real output pixels here.
 *
 * Previously this object was called `ExportPresets`, which clashed with
 * `com.example.engine.ExportPresets` (the bitrate/preset ladder) - two
 * same-named objects made debugging confusing. The rename keeps one name per
 * concern: resolution lives here, bitrate lives in the engine presets.
 */
object ExportResolution {

    /** Target height (in pixels) for the exported video. */
    fun heightFor(resolution: String): Int = when (resolution.uppercase().replace(" ", "")) {
        "360P" -> 360
        "480P" -> 480
        "540P" -> 540
        "720P" -> 720
        "1080P" -> 1080
        "2K", "1440P" -> 1440
        "4K", "2160P" -> 2160
        else -> 1080
    }

    /** Output frame size for the given aspect ratio and resolution. */
    fun outputSize(ratio: AspectRatioType, resolution: String): Pair<Int, Int> =
        outputSizeForHeight(ratio, heightFor(resolution))

    /** Output frame size for an explicit height, rounded down to even dimensions. */
    fun outputSizeForHeight(ratio: AspectRatioType, height: Int): Pair<Int, Int> {
        val safeHeight = height.coerceAtLeast(2)
        val width = (safeHeight * ratio.ratioWidth / ratio.ratioHeight).toInt()
        return Pair(
            (width - (width % 2)).coerceAtLeast(2),
            (safeHeight - (safeHeight % 2)).coerceAtLeast(2)
        )
    }
}
