package com.example.engine

import com.example.model.EffectFamily
import com.example.model.EffectSpec
import com.example.model.EffectType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * One primitive draw instruction for a video effect. All geometry is in
 * absolute pixels for the current frame; both renderers (the Compose preview
 * and the exported-file canvas) consume the exact same list, so preview and
 * output always match.
 */
data class EffectOp(
    val kind: EffectOpKind,
    /** Rect for RECT/GRADIENT_H/RADIAL ops (pixels). */
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    /** Centre for OVAL_FILLED/LINE/CIRCLE_STROKE ops (pixels). */
    val cx: Float = 0f,
    val cy: Float = 0f,
    /** Radius or line end offset (pixels). */
    val radius: Float = 0f,
    val colorA: Int = 0,
    /** Second gradient colour for GRADIENT_H/RADIAL ops (argb). */
    val colorB: Int = 0,
    /** Stroke width for CIRCLE_STROKE/LINE ops (pixels). */
    val strokeWidth: Float = 0f
)

enum class EffectOpKind {
    /** Filled rect from (left,top) to (right,bottom). */
    RECT,
    /** Horizontal linear gradient rect, [colorA] -> transparent-ish [colorB]. */
    GRADIENT_H,
    /** Radial gradient centred in the rect, [colorA] core -> [colorB] edge. */
    RADIAL,
    /** Filled circle at (cx,cy) with [radius]. */
    OVAL_FILLED,
    /** Stroke circle at (cx,cy). */
    CIRCLE_STROKE,
    /** Stroke rect border from (left,top) to (right,bottom). */
    RECT_STROKE,
    /** Line from (cx,cy) to (left,top). */
    LINE
}

/**
 * Turns an [EffectType] plus its [EffectSpec] into the primitive draw ops for
 * one frame. Pure Kotlin: JVM-testable and shared by preview and export.
 *
 * Every family is a real visual - particles fall, lights pulse, scanlines
 * scroll - parameterised by colour/intensity/speed so each preset looks and
 * behaves differently.
 */
object EffectOps {

    /** Frame step for noise so speckles/grain shimmer at a visible rate. */
    private const val NOISE_FRAME_MS = 40L

    fun compute(
        type: EffectType,
        spec: EffectSpec,
        localMs: Long,
        width: Float,
        height: Float
    ): List<EffectOp> {
        if (width <= 0f || height <= 0f) return emptyList()
        val rng = Random(type.name.hashCode() * 31L + localMs / NOISE_FRAME_MS)
        return when (spec.family) {
            EffectFamily.LIGHT_LEAK -> lightLeak(spec, localMs, width, height)
            EffectFamily.STROBE -> strobe(spec, localMs, width, height)
            EffectFamily.SCANLINES -> scanlines(spec, localMs, rng, width, height)
            EffectFamily.GRAIN -> grain(spec, localMs, rng, width, height)
            EffectFamily.VIGNETTE -> vignette(spec, localMs, width, height)
            EffectFamily.RINGS -> rings(spec, localMs, width, height)
            EffectFamily.BEAMS -> beams(spec, localMs, rng, width, height)
            EffectFamily.PARTICLES -> particles(spec, localMs, rng, width, height)
            EffectFamily.BOKEH -> bokeh(spec, localMs, rng, width, height)
            EffectFamily.EDGE_GLOW -> edgeGlow(spec, localMs, width, height)
            EffectFamily.CINEMA_BARS -> cinemaBars(spec, localMs, width, height)
            EffectFamily.WAVE -> wave(spec, localMs, width, height)
            EffectFamily.PIXEL_BLOCKS -> pixelBlocks(spec, localMs, rng, width, height)
            EffectFamily.SPOTLIGHT -> spotlight(spec, localMs, width, height)
            EffectFamily.FILM_BURN -> filmBurn(spec, localMs, rng, width, height)
            EffectFamily.GRID -> grid(spec, localMs, width, height)
            EffectFamily.CONFETTI -> confetti(spec, localMs, rng, width, height)
            EffectFamily.PLASMA -> plasma(spec, localMs, width, height)
            EffectFamily.STARFIELD -> starfield(spec, localMs, rng, width, height)
            EffectFamily.RIPPLE -> ripple(spec, localMs, width, height)
        }
    }

    // ------------------------------------------------------------------
    // Families
    // ------------------------------------------------------------------

    /** A pulsing diagonal light wash with a bright streak along one edge. */
    private fun lightLeak(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val t = localMs / 1000f
        val pulse = 0.45f + 0.55f * sin((t * (0.6f + spec.speed * 2.4f) * 2f * PI).toFloat())
        val alpha = (spec.intensity * 110 * pulse).toInt().coerceIn(0, 255)
        return listOf(
            EffectOp(
                kind = EffectOpKind.GRADIENT_H,
                left = 0f, top = 0f, right = w, bottom = h,
                colorA = withAlpha(spec.colorA, alpha),
                colorB = withAlpha(spec.colorB, (alpha * 0.25f).toInt())
            ),
            EffectOp(
                kind = EffectOpKind.GRADIENT_H,
                left = 0f, top = h * 0.35f, right = w * 0.35f, bottom = h,
                colorA = withAlpha(spec.colorA, (alpha * 0.9f).toInt()),
                colorB = withAlpha(spec.colorB, 0)
            )
        )
    }

    /** Full-frame flash at a fixed period, [spec.speed] sets the tempo. */
    private fun strobe(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val periodMs = (1400f - spec.speed * 1150f).coerceAtLeast(120f)
        val phase = (localMs % periodMs.toLong()) / periodMs
        val on = phase < 0.18f
        val alpha = if (on) (spec.intensity * 165).toInt() else 0
        if (alpha <= 0) return emptyList()
        return listOf(
            EffectOp(EffectOpKind.RECT, 0f, 0f, w, h, colorA = withAlpha(spec.colorA, alpha)),
            EffectOp(
                EffectOpKind.RECT, 0f, 0f, w, h,
                colorA = withAlpha(0xFFFFFFFF.toInt(), (alpha * 0.35f).toInt())
            )
        )
    }

    /** CRT scanlines scrolling down plus a faint colour cast. */
    private fun scanlines(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val spacing = max(6f, h / (8f + spec.intensity * 26f))
        val scroll = (localMs / 1000f) * (8f + spec.speed * 46f)
        val alpha = (30 + spec.intensity * 60).toInt().coerceIn(0, 120)
        var y = (scroll % spacing) - spacing
        while (y < h) {
            ops += EffectOp(
                EffectOpKind.RECT, 0f, y, w, y + max(1.2f, spacing * 0.22f),
                colorA = withAlpha(spec.colorA, alpha)
            )
            y += spacing
        }
        ops += EffectOp(
            EffectOpKind.RECT, 0f, 0f, w, h,
            colorA = withAlpha(spec.colorB, (18 + spec.intensity * 20).toInt())
        )
        return ops
    }

    /** Speckle noise shimmering per noise-frame. */
    private fun grain(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val count = (30 + spec.intensity * 170).toInt()
        val alpha = (26 + spec.intensity * 66).toInt().coerceIn(0, 140)
        val ops = mutableListOf<EffectOp>()
        repeat(count) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val size = 1.5f + rng.nextFloat() * 4.5f
            val bright = rng.nextFloat() > 0.45f
            ops += EffectOp(
                EffectOpKind.RECT, x, y,
                min(w, x + size), min(h, y + size),
                colorA = withAlpha(if (bright) spec.colorA else spec.colorB, alpha)
            )
        }
        return ops
    }

    /** Pulsing dark vignette pulling the eye to the centre. */
    private fun vignette(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val t = localMs / 1000f
        val pulse = 0.6f + 0.4f * sin((t * (0.5f + spec.speed * 2f) * 2f * PI).toFloat())
        val alpha = (spec.intensity * 190 * pulse).toInt().coerceIn(0, 255)
        return listOf(
            EffectOp(
                EffectOpKind.RADIAL,
                left = 0f, top = 0f, right = w, bottom = h,
                colorA = withAlpha(0x00000000, 0),
                colorB = withAlpha(spec.colorA, alpha)
            )
        )
    }

    /** Expanding concentric rings from the centre. */
    private fun rings(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val period = 1600f - spec.speed * 1100f
        val ops = mutableListOf<EffectOp>()
        val count = 4
        for (i in 0 until count) {
            val phase = (((localMs / period.toLong()) + i.toFloat() / count) % 1f)
            val radius = (phase * max(w, h) * 0.75f).coerceAtLeast(2f)
            val alpha = ((1f - phase) * spec.intensity * 150).toInt().coerceIn(0, 255)
            ops += EffectOp(
                EffectOpKind.CIRCLE_STROKE,
                cx = w / 2f, cy = h / 2f, radius = radius,
                colorA = withAlpha(spec.colorA, alpha),
                strokeWidth = max(1.5f, w * 0.004f)
            )
        }
        return ops
    }

    /** Rotating club beams from the top centre. */
    private fun beams(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val count = 5
        val spin = (localMs / 1000f) * (0.35f + spec.speed * 1.6f)
        for (i in 0 until count) {
            val angle = spin + i * (2f * PI.toFloat() / count)
            val length = max(w, h)
            val endX = w / 2f + cos(angle) * length
            val endY = h / 2f + sin(angle) * length
            val alpha = (spec.intensity * 90 * (0.55f + 0.45f * sin(angle * 3f))).toInt()
                .coerceIn(0, 255)
            ops += EffectOp(
                EffectOpKind.LINE,
                cx = w / 2f, cy = h / 2f, left = endX, top = endY,
                colorA = withAlpha(spec.colorA, alpha),
                strokeWidth = max(2f, w * (0.006f + 0.004f * rng.nextFloat()))
            )
        }
        return ops
    }

    /** Falling particles (snow / rain / dust by parameters). */
    private fun particles(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val count = (14 + spec.intensity * 60).toInt()
        val fall = (h * 0.35f) * (0.3f + spec.speed * 2.2f)
        repeat(count) {
            val seedX = rng.nextFloat()
            val x = seedX * w + sin(localMs / 900f + it * 1.7f) * w * 0.02f
            val y = ((rng.nextFloat() * h + localMs / 1000f * fall) % (h + 30f)) - 15f
            val size = (2f + rng.nextFloat() * 6f) * (0.7f + spec.intensity * 0.7f)
            val alpha = (60 + spec.intensity * 120).toInt().coerceIn(0, 255)
            ops += EffectOp(
                EffectOpKind.OVAL_FILLED,
                cx = x, cy = y, radius = size,
                colorA = withAlpha(spec.colorA, alpha)
            )
        }
        return ops
    }

    /** Large soft translucent circles drifting upward. */
    private fun bokeh(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val count = (6 + (spec.intensity * 14).toInt())
        repeat(count) {
            val x = rng.nextFloat() * w
            val drift = (localMs / 1000f) * (10f + spec.speed * 60f)
            val y = ((rng.nextFloat() * h - drift) % (h + 120f))
            val radius = (18f + rng.nextFloat() * 60f) * (0.6f + spec.intensity * 0.8f)
            val alpha = (26 + spec.intensity * 44).toInt().coerceIn(0, 120)
            ops += EffectOp(
                EffectOpKind.OVAL_FILLED,
                cx = x, cy = if (y < 0f) y + h + 120f else y, radius = radius,
                colorA = withAlpha(if (it % 2 == 0) spec.colorA else spec.colorB, alpha)
            )
        }
        return ops
    }

    /** A pulsing coloured border (neon frame). */
    private fun edgeGlow(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val t = localMs / 1000f
        val pulse = 0.5f + 0.5f * sin((t * (0.8f + spec.speed * 3f) * 2f * PI).toFloat())
        val alpha = (60 + spec.intensity * 150 * pulse).toInt().coerceIn(0, 255)
        val stroke = max(2f, w * (0.008f + spec.intensity * 0.010f))
        return listOf(
            EffectOp(
                EffectOpKind.RECT_STROKE,
                left = 2f, top = 2f, right = w - 2f, bottom = h - 2f,
                colorA = withAlpha(spec.colorA, alpha),
                strokeWidth = stroke
            ),
            EffectOp(
                EffectOpKind.RECT_STROKE,
                left = 6f, top = 6f, right = w - 6f, bottom = h - 6f,
                colorA = withAlpha(spec.colorB, (alpha * 0.7f).toInt()),
                strokeWidth = stroke * 0.5f
            )
        )
    }

    /** Soft cinematic bars sliding slightly with a warm tint wash. */
    private fun cinemaBars(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val breathe = sin((localMs / 1000f) * (0.4f + spec.speed * 1.4f) * 2f * PI.toFloat()).toFloat()
        val barH = h * (0.10f + spec.intensity * 0.07f) * (1f + breathe * 0.06f)
        return listOf(
            EffectOp(EffectOpKind.RECT, 0f, 0f, w, barH, colorA = spec.colorA),
            EffectOp(EffectOpKind.RECT, 0f, h - barH, w, h, colorA = spec.colorA),
            EffectOp(
                EffectOpKind.RECT, 0f, 0f, w, h,
                colorA = withAlpha(spec.colorB, (22 + spec.intensity * 22).toInt())
            )
        )
    }

    /** Translucent sine bands sliding sideways. */
    private fun wave(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val bands = (4 + (spec.intensity * 8).toInt())
        val speed = 0.5f + spec.speed * 2.5f
        for (i in 0 until bands) {
            val phase = sin((localMs / 1000f) * speed * 2f * PI + i * 1.1f).toFloat()
            val bandH = h * 0.05f * (1f + 0.4f * phase)
            val y = h * (i + 0.5f) / bands + phase * h * 0.04f
            val alpha = (20 + spec.intensity * 55 * (0.6f + 0.4f * phase)).toInt().coerceIn(0, 160)
            ops += EffectOp(
                EffectOpKind.RECT, 0f, y - bandH / 2f, w, y + bandH / 2f,
                colorA = withAlpha(if (i % 2 == 0) spec.colorA else spec.colorB, alpha)
            )
        }
        return ops
    }

    /** Translucent mosaic squares sliding across the frame. */
    private fun pixelBlocks(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val cell = max(14f, w / (10f + spec.intensity * 22f))
        val cols = (w / cell).toInt() + 1
        val rows = (h / cell).toInt() + 1
        val shift = ((localMs / 1000f) * (12f + spec.speed * 90f)) % cell
        repeat((cols * rows * 0.16f).toInt().coerceAtLeast(6)) {
            val col = rng.nextInt(cols)
            val row = rng.nextInt(rows)
            val alpha = (24 + spec.intensity * 70).toInt().coerceIn(0, 150)
            ops += EffectOp(
                EffectOpKind.RECT,
                left = col * cell + shift * (if (row % 2 == 0) 1f else -1f),
                top = row * cell,
                right = col * cell + shift * (if (row % 2 == 0) 1f else -1f) + cell * 0.92f,
                bottom = row * cell + cell * 0.92f,
                colorA = withAlpha(if (rng.nextBoolean()) spec.colorA else spec.colorB, alpha)
            )
        }
        return ops
    }

    /** A soft light spot wandering across the frame. */
    private fun spotlight(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val t = (localMs / 1000f) * (0.3f + spec.speed * 1.5f)
        val x = w / 2f + sin(t * 2f * PI.toFloat() * 0.7f) * w * 0.3f
        val y = h / 2f + cos(t * 2f * PI.toFloat() * 0.5f) * h * 0.3f
        val radius = min(w, h) * (0.24f + spec.intensity * 0.18f)
        return listOf(
            EffectOp(
                EffectOpKind.RADIAL,
                left = x - radius * 2f, top = y - radius * 2f,
                right = x + radius * 2f, bottom = y + radius * 2f,
                colorA = withAlpha(spec.colorA, (70 + spec.intensity * 110).toInt()),
                colorB = withAlpha(spec.colorB, 0)
            )
        )
    }

    /** Warm blotchy burn patches drifting with a flicker. */
    private fun filmBurn(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val flicker = 0.75f + 0.25f * sin(localMs / 120.0).toFloat()
        repeat((5 + (spec.intensity * 9).toInt())) {
            val drift = (localMs / 1000f) * (14f + spec.speed * 70f)
            val x = rng.nextFloat() * w
            val y = ((rng.nextFloat() * h + drift) % (h + 80f)) - 40f
            val radius = (24f + rng.nextFloat() * 90f) * (0.6f + spec.intensity * 0.7f)
            val alpha = (34 * flicker * (0.7f + spec.intensity * 0.6f)).toInt().coerceIn(0, 150)
            ops += EffectOp(
                EffectOpKind.RADIAL,
                left = x - radius, top = y - radius,
                right = x + radius, bottom = y + radius,
                colorA = withAlpha(spec.colorA, alpha),
                colorB = withAlpha(spec.colorB, 0)
            )
        }
        return ops
    }

    /** A pulsing perspective-style grid. */
    private fun grid(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val cell = max(16f, w / (8f + spec.intensity * 16f))
        val pulse = 0.6f + 0.4f * sin((localMs / 1000f) * (0.7f + spec.speed * 2.6f) * 2f * PI).toFloat()
        val alpha = (34 + spec.intensity * 80 * pulse).toInt().coerceIn(0, 170)
        val stroke = max(1f, w * 0.0018f)
        var x = 0f
        while (x <= w) {
            ops += EffectOp(
                EffectOpKind.LINE, cx = x, cy = 0f, left = x, top = h,
                colorA = withAlpha(spec.colorA, alpha), strokeWidth = stroke
            )
            x += cell
        }
        var y = ((localMs / 1000f) * (10f + spec.speed * 50f)) % cell
        while (y <= h) {
            ops += EffectOp(
                EffectOpKind.LINE, cx = 0f, cy = y, left = w, top = y,
                colorA = withAlpha(spec.colorA, alpha), strokeWidth = stroke
            )
            y += cell
        }
        return ops
    }

    /** Rotating confetti squares falling. */
    private fun confetti(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val count = (16 + spec.intensity * 50).toInt()
        repeat(count) {
            val x = rng.nextFloat() * w
            val fall = (h * 0.5f) * (0.4f + spec.speed * 2.4f)
            val y = ((rng.nextFloat() * h + localMs / 1000f * fall) % (h + 40f)) - 20f
            val size = (4f + rng.nextFloat() * 9f) * (0.7f + spec.intensity * 0.6f)
            val alpha = (90 + spec.intensity * 120).toInt().coerceIn(0, 255)
            val color = when (it % 3) {
                0 -> spec.colorA
                1 -> spec.colorB
                else -> 0xFFFFFFFF.toInt()
            }
            ops += EffectOp(
                EffectOpKind.RECT,
                left = x, top = y, right = x + size, bottom = y + size,
                colorA = withAlpha(color, alpha)
            )
        }
        return ops
    }

    /** Slow drifting colour blobs. */
    private fun plasma(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val t = (localMs / 1000f) * (0.2f + spec.speed * 1.1f)
        repeat(3) { i ->
            val x = w * (0.5f + 0.36f * sin(t * 2f * PI.toFloat() + i * 2.1f))
            val y = h * (0.5f + 0.36f * cos(t * 2f * PI.toFloat() + i * 1.3f))
            val radius = min(w, h) * (0.42f + spec.intensity * 0.2f)
            val alpha = (44 + spec.intensity * 50).toInt().coerceIn(0, 140)
            ops += EffectOp(
                EffectOpKind.RADIAL,
                left = x - radius, top = y - radius,
                right = x + radius, bottom = y + radius,
                colorA = withAlpha(if (i % 2 == 0) spec.colorA else spec.colorB, alpha),
                colorB = withAlpha(if (i % 2 == 0) spec.colorB else spec.colorA, 0)
            )
        }
        return ops
    }

    /** Twinkling star dots. */
    private fun starfield(
        spec: EffectSpec,
        localMs: Long,
        rng: Random,
        w: Float,
        h: Float
    ): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val count = (24 + spec.intensity * 80).toInt()
        repeat(count) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val twinkle = 0.4f + 0.6f * abs(sin(localMs / 260f + it * 2.4f))
            val radius = (1f + rng.nextFloat() * 2.6f) * (0.7f + spec.intensity * 0.6f)
            val alpha = (70 + spec.intensity * 130 * twinkle).toInt().coerceIn(0, 255)
            ops += EffectOp(
                EffectOpKind.OVAL_FILLED,
                cx = x, cy = y, radius = radius,
                colorA = withAlpha(spec.colorA, alpha)
            )
        }
        return ops
    }

    /** Concentric ripples expanding from the bottom centre. */
    private fun ripple(spec: EffectSpec, localMs: Long, w: Float, h: Float): List<EffectOp> {
        val ops = mutableListOf<EffectOp>()
        val period = 1500f - spec.speed * 1000f
        repeat(3) { i ->
            val phase = ((localMs / period.toLong()) + i.toFloat() / 3f) % 1f
            val radius = phase * w * 0.6f
            val alpha = ((1f - phase) * spec.intensity * 120).toInt().coerceIn(0, 255)
            ops += EffectOp(
                EffectOpKind.CIRCLE_STROKE,
                cx = w / 2f, cy = h * 0.86f, radius = radius.coerceAtLeast(3f),
                colorA = withAlpha(spec.colorA, alpha),
                strokeWidth = max(1.5f, w * 0.005f)
            )
        }
        return ops
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Replaces only the alpha channel of an argb colour. */
    fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
}
