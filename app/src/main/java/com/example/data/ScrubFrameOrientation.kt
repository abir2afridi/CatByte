package com.example.data

import kotlin.math.abs

/**
 * Keeps a decoded scrub frame in the SAME orientation the preview player shows.
 *
 * Media3 (and therefore the CompositionPlayer preview) applies the source's
 * rotation metadata, so a portrait phone video (1920x1080 encoded + 90°
 * rotation) is rendered upright. `MediaMetadataRetriever` hands back the RAW
 * encoded frame, so without this correction the preview flips from portrait to
 * landscape the moment the scrub overlay appears — the "aspect ratio changes
 * while scrolling" report.
 *
 * Pure decision logic on purpose: whether a frame still needs rotating depends
 * on the metadata and the decoded bitmap's size, so it is unit-tested here
 * instead of inside the Android-only loader.
 */
object ScrubFrameOrientation {

    /** Rounded to a quarter turn; anything else cannot be applied safely. */
    fun normalizedRotation(rotationDegrees: Int): Int {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        // Only exact quarter turns can be applied: a 45 degree source would
        // need a different (and lossy) transform, so it is left alone.
        if (normalized % 90 != 0) return 0
        return normalized
    }

    /**
     * True when [frameWidth] x [frameHeight] still has to be rotated by
     * [rotationDegrees] to match the source's display orientation.
     *
     * @param rawWidth raw (encoded) width of the source, 0 when unknown.
     * @param rawHeight raw (encoded) height of the source, 0 when unknown.
     */
    fun needsRotation(
        rotationDegrees: Int,
        frameWidth: Int,
        frameHeight: Int,
        rawWidth: Int,
        rawHeight: Int
    ): Boolean {
        val rotation = normalizedRotation(rotationDegrees)
        if (rotation == 0) return false
        if (frameWidth <= 0 || frameHeight <= 0) return false
        // A quarter turn swaps the axes, so the decoded size tells us whether
        // the retriever already applied the rotation.
        if (rotation == 90 || rotation == 270) {
            if (rawWidth > 0 && rawHeight > 0) {
                if (frameWidth == rawWidth && frameHeight == rawHeight) return true
                if (frameWidth == rawHeight && frameHeight == rawWidth) return false
                // Sizes do not match exactly (the frame is scaled for the
                // preview): compare the aspect ratios instead. "<=" keeps the
                // ambiguous case (square source) on the "not rotated yet" side.
                val frameAspect = frameWidth.toFloat() / frameHeight.toFloat()
                val rawAspect = rawWidth.toFloat() / rawHeight.toFloat()
                return abs(frameAspect - rawAspect) <= abs(frameAspect - 1f / rawAspect)
            }
            // Unknown source size: assume the raw orientation.
            return true
        }
        // 180 degrees keeps the size, so it cannot be detected — the platform
        // retriever returns the encoded orientation, so it is applied.
        return true
    }
}
