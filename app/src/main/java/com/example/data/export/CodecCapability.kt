package com.example.data.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import com.example.model.AspectRatioType

/**
 * Queries the device encoders so exports never request a codec or frame size the
 * hardware cannot encode. Implements the fallback ladder from the plan:
 *
 * - Codec:    HEVC (H.265) -> H.264 (AVC)
 * - Size:     requested -> 1440p -> 1080p -> 720p -> 480p
 * - Frame rate: 60 -> 30 -> 24 fps
 *
 * The query uses the typed [MediaCodecInfo.VideoCapabilities] API (available on
 * every API level >= 21) instead of reflection, so it works on release builds.
 */
object CodecCapabilityChecker {

    /** Ordered codec ladder (best first): better compression first. */
    private val codecLadder = listOf(
        MediaFormat.MIMETYPE_VIDEO_HEVC,
        MediaFormat.MIMETYPE_VIDEO_AVC
    )

    /** Absolute output heights the export may degrade to, best first. */
    private val heightLadder = listOf(2160, 1440, 1080, 720, 480)

    /** A device encoder that can handle the requested frame size and rate. */
    data class SupportedCodec(
        val mime: String,
        val isHardware: Boolean
    )

    /** The target the device can genuinely encode for an export request. */
    data class ResolvedTarget(
        /** Output codec MIME, or `null` to let Media3 choose its default. */
        val mime: String?,
        val width: Int,
        val height: Int,
        val fps: Int,
        val resolutionLabel: String,
        val requestedResolution: String,
        /** True when the requested resolution exceeded the encoders. */
        val downgraded: Boolean,
        /** True when the requested fps (e.g. 60) had to drop to 30. */
        val fpsDowngraded: Boolean
    )

    /**
     * Returns every encoder that can encode [width] x [height] at [fps],
     * codecs ordered by the ladder (HEVC before AVC). Empty when nothing
     * on the device supports the combination.
     */
    fun querySupportedCodecs(width: Int, height: Int, fps: Int): List<SupportedCodec> {
        if (width <= 0 || height <= 0 || fps <= 0) return emptyList()
        val found = mutableListOf<SupportedCodec>()
        val seenMimes = mutableSetOf<String>()
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        // codecInfos is null on the JVM unit test environment (android stubs
        // return defaults); treat that as "no encoders" instead of crashing.
        val codecInfos = runCatching { codecList.codecInfos }.getOrNull() ?: return emptyList()
        for (info in codecInfos) {
            for (mime in codecLadder) {
                if (mime in seenMimes) continue
                val codec = runCatching { checkEncoder(info, mime, width, height, fps) }
                    .onFailure { cause ->
                        com.example.data.AppLog.warn(
                            "CodecCapabilityChecker",
                            "Skipping encoder ${info.name}: $cause"
                        )
                    }
                    .getOrNull()
                if (codec != null) {
                    found += codec
                    seenMimes += mime
                }
            }
            // One match per codec keeps the list short and ordered by the ladder.
            if (found.size == codecLadder.size) break
        }
        return found
    }

    /** The encoder entry for [mime], or null when it cannot handle the frame. */
    private fun checkEncoder(
        info: MediaCodecInfo,
        mime: String,
        width: Int,
        height: Int,
        fps: Int
    ): SupportedCodec? {
        if (!info.isEncoder) return null
        if (!info.supportedTypes.contains(mime)) return null
        val caps = info.getCapabilitiesForType(mime).videoCapabilities ?: return null
        if (!caps.areSizeAndRateSupported(width, height, fps.toDouble())) return null
        return SupportedCodec(
            mime = mime,
            // isHardwareAccelerated is API 29+; older devices report hardware
            // support conservatively (true) so the ladder stays stable.
            isHardware = Build.VERSION.SDK_INT >= 29 && info.isHardwareAccelerated
        )
    }

    /**
     * Picks the best (codec, output size) pair the device can encode for an
     * export that asked for [requestedResolution] at [fps] with [aspectRatio].
     *
     * Falls back down the size ladder when the requested resolution is beyond
     * the encoder, and down the codec ladder within each size. When nothing
     * matches, returns a target with `mime = null` and the requested size so
     * Media3's default encoder path is used (graceful degradation).
     */
    fun resolve(
        aspectRatio: AspectRatioType,
        requestedResolution: String,
        fps: Int
    ): ResolvedTarget {
        val requestedHeight = ExportPresets.heightFor(requestedResolution)
        // Never propose something LARGER than the user asked for, only smaller.
        val heights = (listOf(requestedHeight) + heightLadder.filter { it < requestedHeight })
            .distinct()
        // Frame-rate ladder from the plan: 60 -> 30 fps.
        val fpsCandidates = (listOf(fps) + listOf(30, 24).filter { it < fps }).distinct()

        for (height in heights) {
            val width = evenWidth(aspectRatio, height)
            for (candidateFps in fpsCandidates) {
                val codec = querySupportedCodecs(width, height, candidateFps).firstOrNull()
                if (codec != null) {
                    return ResolvedTarget(
                        mime = codec.mime,
                        width = width,
                        height = height,
                        fps = candidateFps,
                        resolutionLabel = labelFor(height),
                        requestedResolution = requestedResolution,
                        downgraded = height != requestedHeight,
                        fpsDowngraded = candidateFps != fps
                    )
                }
            }
        }

        // Nothing matched: keep the requested size and let Media3 pick.
        return ResolvedTarget(
            mime = null,
            width = evenWidth(aspectRatio, requestedHeight),
            height = requestedHeight,
            fps = fps,
            resolutionLabel = requestedResolution,
            requestedResolution = requestedResolution,
            downgraded = false,
            fpsDowngraded = false
        )
    }

    /** Maps an output height back to a human readable resolution label. */
    fun labelFor(height: Int): String = when (height) {
        2160 -> "4K"
        1440 -> "1440P"
        1080 -> "1080P"
        720 -> "720P"
        480 -> "480P"
        360 -> "360P"
        else -> "1080P"
    }

    /** Frame width for an aspect ratio at the given height, rounded to even. */
    private fun evenWidth(ratio: AspectRatioType, height: Int): Int {
        val width = (height * ratio.ratioWidth / ratio.ratioHeight).toInt()
        return (width - (width % 2)).coerceAtLeast(2)
    }
}
