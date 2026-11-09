package com.example.engine

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Exact frame rate as a fraction (e.g. 30000/1001 for 29.97 fps).
 *
 * The editor used integer milliseconds before; milliseconds cannot represent
 * 1001-based frame rates, so long timelines drift against the exported file by
 * more than a frame. Every new timing calculation goes through [TimeUs].
 */
data class Rational(val numerator: Int, val denominator: Int = 1) {

    init {
        require(denominator != 0) { "denominator must not be zero" }
        require(numerator >= 0) { "numerator must not be negative" }
    }

    /** Decimal value, only for display and threshold comparisons. */
    val asDouble: Double get() = numerator.toDouble() / denominator.toDouble()

    /** Nominal whole frame rate (29.97 -> 30), used for timecode labels. */
    val nominal: Int get() = asDouble.roundToInt().coerceAtLeast(1)

    fun reduced(): Rational {
        val divisor = gcd(numerator, denominator)
        return Rational(numerator / divisor, denominator / divisor)
    }

    fun invert(): Rational = Rational(denominator, numerator.coerceAtLeast(1))

    /** Duration of exactly one frame. */
    fun frameDurationUs(): TimeUs = TimeUs((1_000_000L * denominator) / numerator)

    operator fun times(frames: Long): TimeUs = TimeUs.fromFrames(frames, this)

    companion object {
        val FPS_24 = Rational(24)
        val FPS_25 = Rational(25)
        val FPS_30 = Rational(30)
        val FPS_2997 = Rational(30_000, 1_001)
        val FPS_50 = Rational(50)
        val FPS_5994 = Rational(60_000, 1_001)
        val FPS_60 = Rational(60)

        /** Normalised, non-negative constructor. */
        fun of(numerator: Int, denominator: Int = 1): Rational {
            require(denominator != 0) { "denominator must not be zero" }
            val divisor = gcd(abs(numerator), abs(denominator))
            return Rational(
                abs(numerator) / divisor,
                abs(denominator) / divisor
            )
        }

        /** Bridges the project's legacy `fps: Int` field. */
        fun fromLegacy(fps: Int): Rational = Rational(fps.coerceIn(1, 480))
    }
}

/** Rounding direction used when a time does not land on a frame boundary. */
enum class FrameRounding { DOWN, UP, NEAREST }

/**
 * Microsecond timestamp on the project timeline.
 *
 * Immutable and exact: conversions are integer-only so the same value is
 * produced on preview and export.
 */
@JvmInline
value class TimeUs(val value: Long) : Comparable<TimeUs> {

    operator fun plus(other: TimeUs): TimeUs = TimeUs(value + other.value)

    operator fun minus(other: TimeUs): TimeUs = TimeUs(value - other.value)

    operator fun times(scale: Double): TimeUs = TimeUs((value * scale).roundToLong())

    override fun compareTo(other: TimeUs): Int = value.compareTo(other.value)

    fun coerceIn(minimum: TimeUs, maximum: TimeUs): TimeUs =
        TimeUs(value.coerceIn(minimum.value, maximum.value))

    fun toMs(): Long = value / 1_000L

    fun toMsRounded(): Long = (value / 1_000.0).roundToLong()

    /**
     * Nearest frame index.
     *
     * Rounding (not truncating) is required because [fromFrames] itself
     * truncates: one frame at 29.97 fps is 33366 us, and truncating that again
     * would report frame 0 for frame 1.
     */
    fun toFrame(fps: Rational): Long {
        val denominator = 1_000_000L * fps.denominator
        return (value * fps.numerator + denominator / 2L) / denominator
    }

    /** Frame index whose start is at or before this time (playback sampling). */
    fun toFrameFloor(fps: Rational): Long =
        (value * fps.numerator) / (1_000_000L * fps.denominator)

    /** Frame containing this time, snapped with [rounding]. */
    fun snapToFrame(fps: Rational, rounding: FrameRounding = FrameRounding.NEAREST): TimeUs {
        val frameUs = (1_000_000L * fps.denominator) / fps.numerator
        if (frameUs <= 0L) return this
        val floorFrame = toFrameFloor(fps)
        val base = TimeUs.fromFrames(floorFrame, fps)
        if (base.value == value) return this
        return when (rounding) {
            FrameRounding.DOWN -> base
            FrameRounding.UP -> TimeUs.fromFrames(floorFrame + 1, fps)
            FrameRounding.NEAREST -> {
                val remainder = value - base.value
                if (remainder * 2L < frameUs) base else TimeUs.fromFrames(floorFrame + 1, fps)
            }
        }
    }

    companion object {
        val ZERO = TimeUs(0L)

        fun fromMs(ms: Long): TimeUs = TimeUs(ms * 1_000L)

        fun fromFrames(frame: Long, fps: Rational): TimeUs =
            TimeUs((frame * 1_000_000L * fps.denominator) / fps.numerator)

        fun max(a: TimeUs, b: TimeUs): TimeUs = if (a.value >= b.value) a else b

        fun min(a: TimeUs, b: TimeUs): TimeUs = if (a.value <= b.value) a else b
    }
}

/** Formatting helpers shared by the ruler, transport bar and export metadata. */
object Timecode {

    /** `HH:MM:SS:FF` (non drop-frame). */
    fun format(time: TimeUs, fps: Rational): String {
        val fpsLabel = fps.nominal
        val totalFrames = time.toFrame(fps).coerceAtLeast(0L)
        val frames = totalFrames % fpsLabel
        val totalSeconds = totalFrames / fpsLabel
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L
        return "%02d:%02d:%02d:%02d".format(hours, minutes, seconds, frames)
    }

    /** `MM:SS` label for compact chips. */
    fun short(time: TimeUs, fps: Rational): String {
        val totalSeconds = time.toFrame(fps).coerceAtLeast(0L) / fps.nominal
        return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    fun parse(raw: String, fps: Rational): TimeUs? {
        val parts = raw.trim().split(':')
        if (parts.size !in 2..4) return null
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        var seconds = 0L
        var frames = 0L
        when (numbers.size) {
            4 -> {
                seconds = numbers[0] * 3600L + numbers[1] * 60L + numbers[2]
                frames = numbers[3]
            }
            3 -> {
                seconds = numbers[0] * 60L + numbers[1]
                frames = numbers[2]
            }
            else -> {
                seconds = numbers[0]
                frames = numbers[1]
            }
        }
        if (seconds < 0L || frames < 0L || frames >= fps.nominal) return null
        return TimeUs.fromMs(seconds * 1_000L) + TimeUs.fromFrames(frames, fps)
    }
}

private fun gcd(a: Int, b: Int): Int {
    var x = abs(a)
    var y = abs(b)
    while (y != 0) {
        val temp = x % y
        x = y
        y = temp
    }
    return if (x == 0) 1 else x
}

/** Frame ladder helpers: the export path clamps the project rate to the device. */
object FrameRateLadder {

    private val DESCENDING = listOf(
        Rational.FPS_60,
        Rational.FPS_50,
        Rational.FPS_30,
        Rational.FPS_25,
        Rational.FPS_24
    )

    /** Highest rate the encoder supports that does not exceed the request. */
    fun resolve(requested: Rational, supported: Set<Int>): Rational {
        val maxFps = supported.maxOrNull() ?: return requested
        if (requested.asDouble <= maxFps + 0.001) return requested
        return DESCENDING.firstOrNull { it.asDouble <= maxFps + 0.001 } ?: Rational.FPS_24
    }
}
