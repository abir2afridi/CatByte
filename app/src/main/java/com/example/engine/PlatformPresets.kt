package com.example.engine

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Share-target presets. Each platform gets its ideal frame, but the target is
 * never larger than the source's short edge, so picking "4K" on a 720p clip
 * downscales instead of inventing pixels.
 */
object PlatformPresets {

    enum class Platform(val id: String) {
        TIKTOK("tiktok"),
        REELS("reels"),
        SHORTS("shorts"),
        YOUTUBE("youtube"),
        YOUTUBE_4K("youtube_4k"),
        WHATSAPP("whatsapp"),
        SQUARE("square"),
        PORTRAIT("portrait"),
        ORIGINAL("original"),
    }

    data class Target(
        val platform: Platform,
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrateKbps: Int,
    ) {
        val megapixels: Float get() = width.toFloat() * height / 1_000_000f
    }

    fun target(
        platform: Platform,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceFps: Int = 30,
    ): Target {
        require(sourceWidth > 0 && sourceHeight > 0) {
            "source must be positive (was ${sourceWidth}x$sourceHeight)"
        }
        if (platform == Platform.ORIGINAL) {
            val w = even(sourceWidth)
            val h = even(sourceHeight)
            val fps = sourceFps.coerceIn(24, 60)
            return Target(platform, w, h, fps, bitrateFor(w, h, fps))
        }

        val (idealWidth, idealHeight, idealFps) = when (platform) {
            Platform.TIKTOK, Platform.REELS, Platform.SHORTS -> Triple(1080, 1920, 30)
            Platform.YOUTUBE -> Triple(1920, 1080, 30)
            Platform.YOUTUBE_4K -> Triple(3840, 2160, 30)
            Platform.WHATSAPP -> Triple(1280, 720, 30)
            Platform.SQUARE -> Triple(1080, 1080, 30)
            Platform.PORTRAIT -> Triple(1080, 1350, 30)
            Platform.ORIGINAL -> Triple(sourceWidth, sourceHeight, sourceFps)
        }

        val shortIdeal = min(idealWidth, idealHeight).toFloat()
        val shortSource = min(sourceWidth, sourceHeight).toFloat()
        val scale = min(1f, shortSource / shortIdeal)
        val width = even((idealWidth * scale).roundToInt())
        val height = even((idealHeight * scale).roundToInt())
        val fps = min(idealFps, sourceFps)
        return Target(platform, width, height, fps, bitrateFor(width, height, fps))
    }

    private fun even(value: Int): Int = ((value.coerceAtLeast(2)) / 2) * 2

    /** ~0.10 bits per pixel, clamped to a sane encoder window. */
    private fun bitrateFor(width: Int, height: Int, fps: Int): Int {
        val bitsPerSecond = width.toDouble() * height * fps * 0.10
        return (bitsPerSecond / 1000.0).roundToInt().coerceIn(1_500, 45_000)
    }

    fun all(): List<Target> = Platform.entries
        .filter { it != Platform.ORIGINAL }
        .map { target(it, 1920, 1080, 30) }
}
