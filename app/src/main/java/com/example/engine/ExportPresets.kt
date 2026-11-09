package com.example.engine

import kotlin.math.min
import kotlin.math.roundToInt

enum class ExportContainer { MP4_H264, MP4_HEVC, AUDIO_M4A, GIF, WEBP }

/** Bits per pixel per frame: the only sane way to scale bitrate with canvas. */
enum class ExportQuality(val bitsPerPixel: Float, val label: String) {
    LOW(0.06f, "Data saver"),
    MEDIUM(0.10f, "Balanced"),
    HIGH(0.15f, "High"),
    MAX(0.22f, "Maximum")
}

data class ExportPreset(
    val id: String,
    val displayName: String,
    val container: ExportContainer,
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrateKbps: Int,
    val audioBitrateKbps: Int,
    val platform: String
) {
    val isAudioOnly: Boolean get() = container == ExportContainer.AUDIO_M4A
    val aspectLabel: String get() = if (height >= width) "Portrait" else "Landscape"
}

/**
 * Platform export presets plus the bitrate ladder.
 *
 * CapCut's most appreciated export detail is that the default "just works" for
 * TikTok/Reels/Shorts. That means correct canvas, fps cap and a bitrate that is
 * not so high the phone heats up, nor so low the upload looks mushy.
 */
object ExportPresets {

    fun videoBitrateKbps(width: Int, height: Int, fps: Int, quality: ExportQuality): Int {
        val pixels = width.toDouble() * height.toDouble() * fps.coerceAtLeast(1)
        return ((pixels * quality.bitsPerPixel) / 1_000.0).roundToInt().coerceAtLeast(600)
    }

    fun feed(height: Int, quality: ExportQuality = ExportQuality.HIGH): ExportPreset {
        val h = height.coerceIn(480, 2_160)
        val w = if (h * 16 / 9 % 2 == 0) h * 16 / 9 else h * 16 / 9 + 1
        val fps = if (h >= 1_080) 30 else 30
        return ExportPreset(
            id = "feed_$h",
            displayName = "${h}P Feed",
            container = ExportContainer.MP4_H264,
            width = w,
            height = h,
            fps = fps,
            videoBitrateKbps = videoBitrateKbps(w, h, fps, quality),
            audioBitrateKbps = 128,
            platform = "Feed"
        )
    }

    fun portrait(
        width: Int = 1_080,
        height: Int = 1_920,
        quality: ExportQuality = ExportQuality.HIGH,
        fps: Int = 30,
        platform: String = "TikTok/Reels/Shorts"
    ): ExportPreset = ExportPreset(
        id = "portrait_${width}x${height}",
        displayName = "${height}P Shorts",
        container = ExportContainer.MP4_H264,
        width = width,
        height = height,
        fps = fps,
        videoBitrateKbps = videoBitrateKbps(width, height, fps, quality),
        audioBitrateKbps = 128,
        platform = platform
    )

    fun landscape(
        width: Int = 1_920,
        height: Int = 1_080,
        quality: ExportQuality = ExportQuality.HIGH,
        fps: Int = 30
    ): ExportPreset = ExportPreset(
        id = "landscape_${width}x${height}",
        displayName = "${height}P YouTube",
        container = ExportContainer.MP4_H264,
        width = width,
        height = height,
        fps = fps,
        videoBitrateKbps = videoBitrateKbps(width, height, fps, quality),
        audioBitrateKbps = 192,
        platform = "YouTube"
    )

    fun audioOnly(bitrateKbps: Int = 192): ExportPreset = ExportPreset(
        id = "audio_m4a",
        displayName = "Audio only (.m4a)",
        container = ExportContainer.AUDIO_M4A,
        width = 0,
        height = 0,
        fps = 0,
        videoBitrateKbps = 0,
        audioBitrateKbps = bitrateKbps,
        platform = "Audio"
    )

    fun gif(width: Int = 720, height: Int = 1_280, fps: Int = 15): ExportPreset = ExportPreset(
        id = "gif_${width}x$height",
        displayName = "GIF",
        container = ExportContainer.GIF,
        width = width,
        height = height,
        fps = fps,
        videoBitrateKbps = videoBitrateKbps(width, height, fps, ExportQuality.LOW),
        audioBitrateKbps = 0,
        platform = "GIF"
    )

    /** Exact size estimate so the storage pre-check does not lie. */
    fun estimateBytes(preset: ExportPreset, durationMs: Long): Long {
        val seconds = durationMs.coerceAtLeast(0L) / 1_000.0
        val kbps = preset.videoBitrateKbps + preset.audioBitrateKbps
        return ((kbps * seconds) / 8.0 * 1_024.0).toLong().coerceAtLeast(1L)
    }

    fun estimateMb(preset: ExportPreset, durationMs: Long): Double =
        estimateBytes(preset, durationMs) / (1_024.0 * 1_024.0)

    /** Downgrades a preset to what the device's encoder can actually do. */
    fun clampToDevice(preset: ExportPreset, maxHeight: Int, maxFps: Int): ExportPreset {
        if (preset.isAudioOnly) return preset
        val targetHeight = min(preset.height, maxHeight.coerceAtLeast(480))
        val scale = targetHeight.toDouble() / preset.height.coerceAtLeast(1).toDouble()
        val targetWidth = ((preset.width * scale).roundToInt()).coerceAtLeast(2)
        val evenWidth = if (targetWidth % 2 == 0) targetWidth else targetWidth + 1
        val evenHeight = if (targetHeight % 2 == 0) targetHeight else targetHeight + 1
        val fps = min(preset.fps.coerceAtLeast(1), maxFps.coerceAtLeast(24))
        return preset.copy(
            width = evenWidth,
            height = evenHeight,
            fps = fps,
            videoBitrateKbps = videoBitrateKbps(evenWidth, evenHeight, fps, qualityOf(preset))
        )
    }

    /** Infers the quality bucket from an existing preset's bitrate. */
    fun qualityOf(preset: ExportPreset): ExportQuality {
        if (preset.isAudioOnly) return ExportQuality.MEDIUM
        val pixels = preset.width.toDouble() * preset.height * preset.fps.coerceAtLeast(1)
        val bpp = (preset.videoBitrateKbps * 1_000.0) / pixels.coerceAtLeast(1.0)
        return ExportQuality.entries.minByOrNull { kotlin.math.abs(it.bitsPerPixel - bpp) } ?: ExportQuality.HIGH
    }

    /** All presets the export sheet shows for a given orientation. */
    fun forOrientation(portrait: Boolean, quality: ExportQuality = ExportQuality.HIGH): List<ExportPreset> =
        if (portrait) {
            listOf(
                portrait(quality = quality),
                portrait(width = 720, height = 1_280, quality = quality),
                portrait(width = 1_080, height = 1_920, quality = ExportQuality.MAX, fps = 60),
                audioOnly(),
                gif()
            )
        } else {
            listOf(
                landscape(quality = quality),
                landscape(width = 1_280, height = 720, quality = quality),
                landscape(width = 3_840, height = 2_160, quality = ExportQuality.MAX, fps = 30),
                audioOnly(),
                gif(width = 1_280, height = 720)
            )
        }
}
