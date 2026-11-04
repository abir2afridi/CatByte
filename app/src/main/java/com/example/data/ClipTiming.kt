package com.example.data

import androidx.media3.common.C
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import com.example.model.BezierPoint
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import kotlin.math.max
import kotlin.math.min

/**
 * Single source of truth for clip timing.
 *
 * The editor stores the final timeline length in [MediaClip.durationMs]. The
 * render graph normalises every speed provider so that the number of output
 * microseconds produced by Media3 matches that value exactly. This is what
 * keeps the preview playhead and the exported MP4 in sync even when a clip is
 * trimmed, sped up, or uses a velocity curve.
 */
object ClipTiming {

    const val MIN_CLIP_MS = 300L
    private const val CURVE_STEP_US = 40_000L
    private const val MIN_SPEED = 0.05f
    private const val MAX_SPEED = 32f

    /** Length of the source media selected by the trim handles, in milliseconds. */
    fun sourceLengthMs(clip: MediaClip): Long {
        val original = clip.originalDurationMs.coerceAtLeast(1L)
        val start = clip.trimStartMs.coerceIn(0L, (original - 1L).coerceAtLeast(0L))
        val end = if (clip.trimEndMs > start) {
            clip.trimEndMs.coerceIn(start + 1L, original)
        } else {
            original
        }
        return (end - start).coerceAtLeast(1L)
    }

    /** True when the clip should be evaluated using its velocity curve. */
    fun hasSpeedCurve(clip: MediaClip): Boolean = hasSpeedCurve(clip.speedCurve)

    fun hasSpeedCurve(curve: SpeedCurve): Boolean {
        if (curve.preset == SpeedCurvePreset.STANDARD) return false
        val points = curve.points.sortedBy { it.x }
        if (points.size < 2) return false
        return points.any { kotlin.math.abs(it.speedMultiplier - 1f) > 0.001f }
    }

    /** Timeline length a clip would have with its current trim/speed settings. */
    fun presentationDurationMs(clip: MediaClip): Long {
        val sourceMs = sourceLengthMs(clip)
        if (hasSpeedCurve(clip)) {
            return (curveOutputUs(sourceMs * 1000L, clip.speedCurve) / 1000L)
                .coerceAtLeast(MIN_CLIP_MS)
        }
        val speed = clip.speed.coerceIn(MIN_SPEED, MAX_SPEED)
        return (sourceMs / speed).toLong().coerceAtLeast(MIN_CLIP_MS)
    }

    /**
     * Output duration of a velocity curve in microseconds. The integration uses
     * the same 40 ms segments as [ClipSpeedProvider].
     */
    fun curveOutputUs(sourceDurationUs: Long, curve: SpeedCurve): Long {
        if (sourceDurationUs <= 0L) return 0L
        val points = normalisedPoints(curve)
        if (points.size < 2) return sourceDurationUs
        var timeUs = 0L
        var output = 0.0
        while (timeUs < sourceDurationUs) {
            val next = min(timeUs + CURVE_STEP_US, sourceDurationUs)
            val progress = timeUs.toDouble() / sourceDurationUs.toDouble()
            val speed = interpolateSpeed(points, progress).coerceIn(MIN_SPEED, MAX_SPEED)
            output += (next - timeUs) / speed.toDouble()
            timeUs = next
        }
        return output.toLong().coerceAtLeast(1L)
    }

    /** Speed multiplier of the curve at [progress] (0..1). */
    fun speedAt(curve: SpeedCurve, progress: Float): Float =
        interpolateSpeed(normalisedPoints(curve), progress.toDouble().coerceIn(0.0, 1.0))

    private fun normalisedPoints(curve: SpeedCurve): List<BezierPoint> {
        val points = curve.points
            .map { BezierPoint(it.x.coerceIn(0f, 1f), it.speedMultiplier.coerceIn(MIN_SPEED, MAX_SPEED)) }
            .sortedBy { it.x }
            .distinctBy { it.x }
        if (points.isEmpty()) return listOf(BezierPoint(0f, 1f), BezierPoint(1f, 1f))
        if (points.size == 1) return listOf(points.first(), points.first().copy(x = 1f))
        return points
    }

    private fun interpolateSpeed(points: List<BezierPoint>, progress: Double): Float {
        if (points.size < 2) return points.firstOrNull()?.speedMultiplier ?: 1f
        if (progress <= points.first().x) return points.first().speedMultiplier
        if (progress >= points.last().x) return points.last().speedMultiplier
        val nextIndex = points.indexOfFirst { it.x >= progress }.let { if (it <= 0) 1 else it }
        val previous = points[nextIndex - 1]
        val next = points[nextIndex]
        val span = (next.x - previous.x).coerceAtLeast(0.00001f)
        val fraction = ((progress - previous.x) / span).coerceIn(0.0, 1.0).toFloat()
        return previous.speedMultiplier + (next.speedMultiplier - previous.speedMultiplier) * fraction
    }
}

/**
 * Media3 speed provider for one timeline clip.
 *
 * [sourceDurationUs] is the *clipped* duration, i.e. what the speed effect sees
 * after the clipping configuration has been applied. The provider is normalised
 * against [MediaClip.durationMs] so rounding in the editor cannot accumulate
 * into a preview/export drift.
 */
@UnstableApi
class ClipSpeedProvider(
    private val clip: MediaClip,
    private val sourceDurationUs: Long
) : SpeedProvider {

    private val curve = clip.speedCurve
    private val curveActive = ClipTiming.hasSpeedCurve(curve)
    private val targetOutputUs = (clip.durationMs.coerceAtLeast(1L) * 1000L)
    private val baseOutputUs = if (curveActive) {
        ClipTiming.curveOutputUs(sourceDurationUs, curve)
    } else {
        (sourceDurationUs / clip.speed.coerceIn(0.05f, 32f)).toLong().coerceAtLeast(1L)
    }
    private val normalisation = baseOutputUs.toDouble() / targetOutputUs.toDouble()

    override fun getSpeed(presentationTimeUs: Long): Float {
        val rawSpeed = if (curveActive && sourceDurationUs > 0L) {
            val progress = presentationTimeUs.toDouble() / sourceDurationUs.toDouble()
            ClipTiming.speedAt(curve, progress.toFloat())
        } else {
            clip.speed
        }
        return (rawSpeed * normalisation)
            .toFloat()
            .coerceIn(0.05f, 64f)
    }

    override fun getNextSpeedChangeTimeUs(presentationTimeUs: Long): Long {
        if (!curveActive || sourceDurationUs <= 0L) return C.TIME_UNSET
        val next = ((presentationTimeUs / CURVE_STEP_US) + 1L) * CURVE_STEP_US
        return if (next < sourceDurationUs) next else C.TIME_UNSET
    }

    private companion object {
        const val CURVE_STEP_US = 40_000L
    }
}

/** Small duration helpers shared by imports and timeline edits. */
object DurationMath {
    fun trimLengthMs(originalDurationMs: Long, trimStartMs: Long, trimEndMs: Long): Long {
        val original = originalDurationMs.coerceAtLeast(1L)
        val start = trimStartMs.coerceIn(0L, max(0L, original - 1L))
        val end = if (trimEndMs > start) trimEndMs.coerceIn(start + 1L, original) else original
        return (end - start).coerceAtLeast(1L)
    }
}
