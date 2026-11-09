package com.example.engine

import com.example.model.EasingCurve
import com.example.model.TransitionType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Renderer-level transition kinds (two frames blended in one shader pass). */
enum class TransitionKind {
    CUT,
    CROSS_DISSOLVE,
    DIP_TO_BLACK,
    FLASH_WHITE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    PUSH_LEFT,
    WIPE_LINEAR,
    WIPE_RADIAL,
    ZOOM,
    SPIN,
    BLUR,
    GLITCH,
    FLIP,
    ORGANIC_WIPE,
    INK_BLEED
}

data class TransitionSpec(
    val kind: TransitionKind,
    val durationUs: Long
)

/**
 * One composited transition frame: what both layers contribute and what fills
 * the canvas behind them (dip-to-black/white need a visible background).
 */
data class TransitionFrame(
    val outgoingAlpha: Float,
    val incomingAlpha: Float,
    val outgoingOffsetX: Float = 0f,
    val outgoingOffsetY: Float = 0f,
    val incomingOffsetX: Float = 0f,
    val incomingOffsetY: Float = 0f,
    val outgoingScale: Float = 1f,
    val incomingScale: Float = 1f,
    val outgoingRotationDeg: Float = 0f,
    val incomingRotationDeg: Float = 0f,
    val fill: Float = 0f,
    val progress: Float = 0f
)

/**
 * Two-frame transition maths.
 *
 * Media3 can only hand one frame at a time to an effect, which is why the
 * legacy transitions looked like opacity fades. The compositor samples *both*
 * neighbours into one pass; this object decides the weights and offsets so the
 * shader stays dumb and testable.
 */
object TransitionMath {

    fun fromLegacy(type: TransitionType): TransitionKind = when (type) {
        TransitionType.NONE -> TransitionKind.CUT
        TransitionType.PAGE_FLIP -> TransitionKind.FLIP
        TransitionType.PAPER_TEAR -> TransitionKind.ORGANIC_WIPE
        TransitionType.PENCIL_WIPE -> TransitionKind.WIPE_LINEAR
        TransitionType.INK_BLEED -> TransitionKind.INK_BLEED
        TransitionType.FADE_BLACK -> TransitionKind.DIP_TO_BLACK
        TransitionType.WHIP_PAN -> TransitionKind.PUSH_LEFT
        TransitionType.ZOOM_IN -> TransitionKind.ZOOM
        TransitionType.GLITCH -> TransitionKind.GLITCH
        TransitionType.FLASH_WHITE -> TransitionKind.FLASH_WHITE
        TransitionType.DISSOLVE -> TransitionKind.CROSS_DISSOLVE
    }

    /** A transition may never consume more than half of either neighbour. */
    fun clampDurationUs(requestedUs: Long, previousDurationUs: Long, currentDurationUs: Long): Long {
        if (requestedUs <= 0L) return 0L
        val limit = max(0L, min(previousDurationUs, currentDurationUs) / 2L)
        return min(requestedUs, limit)
    }

    fun progress(elapsedUs: Long, durationUs: Long): Float {
        if (durationUs <= 0L) return 1f
        return (elapsedUs.toDouble() / durationUs.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }

    /** Easing curves shared with keyframe animation. */
    fun ease(curve: EasingCurve, t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return when (curve) {
            EasingCurve.LINEAR -> x
            EasingCurve.EASE_IN -> x * x
            EasingCurve.EASE_OUT -> 1f - (1f - x) * (1f - x)
            EasingCurve.EASE_IN_OUT ->
                if (x < 0.5f) 2f * x * x else 1f - 2f * (1f - x) * (1f - x)
            EasingCurve.BOUNCE -> bounceOut(x)
        }
    }

    private fun bounceOut(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                val t = x - 1.5f / d1
                n1 * t * t + 0.75f
            }
            x < 2.5f / d1 -> {
                val t = x - 2.25f / d1
                n1 * t * t + 0.9375f
            }
            else -> {
                val t = x - 2.625f / d1
                n1 * t * t + 0.984375f
            }
        }
    }

    /**
     * Contribution of each side for [progress] (0 = fully outgoing, 1 = fully
     * incoming). Alphas of dip/flash transitions intentionally do not sum to 1:
     * the gap is the visible fill colour.
     */
    fun frame(kind: TransitionKind, progress: Float): TransitionFrame {
        val p = progress.coerceIn(0f, 1f)
        return when (kind) {
            TransitionKind.CUT ->
                if (p < 0.5f) TransitionFrame(1f, 0f, progress = p)
                else TransitionFrame(0f, 1f, progress = p)

            TransitionKind.CROSS_DISSOLVE,
            TransitionKind.WIPE_LINEAR,
            TransitionKind.WIPE_RADIAL,
            TransitionKind.ORGANIC_WIPE,
            TransitionKind.INK_BLEED ->
                TransitionFrame(1f - p, p, progress = p)

            TransitionKind.DIP_TO_BLACK,
            TransitionKind.FLASH_WHITE -> {
                val outgoing = max(0f, 1f - 2f * p)
                val incoming = max(0f, 2f * p - 1f)
                TransitionFrame(
                    outgoingAlpha = outgoing,
                    incomingAlpha = incoming,
                    fill = (1f - outgoing - incoming).coerceIn(0f, 1f),
                    progress = p
                )
            }

            TransitionKind.SLIDE_LEFT ->
                TransitionFrame(1f, 1f, incomingOffsetX = 1f - p, progress = p)

            TransitionKind.SLIDE_RIGHT ->
                TransitionFrame(1f, 1f, incomingOffsetX = -(1f - p), progress = p)

            TransitionKind.SLIDE_UP ->
                TransitionFrame(1f, 1f, incomingOffsetY = 1f - p, progress = p)

            TransitionKind.PUSH_LEFT ->
                TransitionFrame(1f, 1f, outgoingOffsetX = -p, incomingOffsetX = 1f - p, progress = p)

            TransitionKind.ZOOM -> TransitionFrame(
                outgoingAlpha = 1f - p,
                incomingAlpha = p,
                outgoingScale = 1f + 0.35f * p,
                incomingScale = 0.65f + 0.35f * p,
                progress = p
            )

            TransitionKind.SPIN -> TransitionFrame(
                outgoingAlpha = 1f - p,
                incomingAlpha = p,
                outgoingRotationDeg = 12f * p,
                incomingRotationDeg = -12f * (1f - p),
                progress = p
            )

            TransitionKind.BLUR -> TransitionFrame(
                outgoingAlpha = 1f - p,
                incomingAlpha = p,
                outgoingScale = 1f + 0.08f * p,
                incomingScale = 0.92f + 0.08f * p,
                progress = p
            )

            TransitionKind.GLITCH -> {
                // Two hard cuts with a shake in between: cheap, very CapCut.
                val shake = sin(p * 24f) * 0.01f
                TransitionFrame(
                    outgoingAlpha = if (p < 0.5f) 1f else 0f,
                    incomingAlpha = if (p < 0.5f) 0f else 1f,
                    outgoingOffsetX = shake,
                    incomingOffsetX = -shake,
                    progress = p
                )
            }

            TransitionKind.FLIP -> {
                // Cosine scale on X fakes a card flip without a 3D matrix.
                val scaleX = cos(p * Math.PI).toFloat()
                if (p < 0.5f) {
                    TransitionFrame(1f, 0f, outgoingScale = max(0.02f, kotlin.math.abs(scaleX)), progress = p)
                } else {
                    TransitionFrame(0f, 1f, incomingScale = max(0.02f, kotlin.math.abs(scaleX)), progress = p)
                }
            }
        }
    }

    /** Fragment shader blending `uTexA` (outgoing) into `uTexB` (incoming). */
    fun shaderFor(kind: TransitionKind): String = when (kind) {
        TransitionKind.CUT -> TRANSITION_TEMPLATE.replace("/*BODY*/", "float m = step(0.5, uProgress);")
            .replace("vec4 outColor = mix(a, b, m);", "vec4 outColor = mix(a, b, m);")
        TransitionKind.CROSS_DISSOLVE,
        TransitionKind.ORGANIC_WIPE,
        TransitionKind.INK_BLEED -> TRANSITION_TEMPLATE.replace(
            "/*BODY*/",
            "float m = smoothstep(0.0, 1.0, uProgress);"
        )
        TransitionKind.WIPE_LINEAR -> TRANSITION_TEMPLATE.replace(
            "/*BODY*/",
            "float m = smoothstep(uProgress - 0.08, uProgress + 0.08, uv.x);"
        )
        TransitionKind.WIPE_RADIAL -> TRANSITION_TEMPLATE.replace(
            "/*BODY*/",
            "float d = distance(uv, vec2(0.5));\n  float m = smoothstep(uProgress - 0.08, uProgress + 0.08, d * 1.42);"
        )
        TransitionKind.DIP_TO_BLACK,
        TransitionKind.FLASH_WHITE -> TRANSITION_TEMPLATE.replace(
            "/*BODY*/",
            "float m = step(0.5, uProgress);\n  float fill = 1.0 - abs(uProgress * 2.0 - 1.0);\n  outColor = mix(outColor, uFillColor, fill);"
        )
        TransitionKind.SLIDE_LEFT,
        TransitionKind.SLIDE_RIGHT,
        TransitionKind.SLIDE_UP,
        TransitionKind.PUSH_LEFT -> TRANSITION_TEMPLATE.replace(
            "/*BODY*/",
            "vec2 shift = vec2(1.0 - uProgress, 0.0);\n  vec4 slideA = texture(uTexA, uv - shift);\n  vec4 slideB = texture(uTexB, uv + shift - vec2(1.0, 0.0));\n  outColor = mix(slideA, slideB, step(0.5, uv.x + shift.x));"
        )
        TransitionKind.ZOOM,
        TransitionKind.SPIN,
        TransitionKind.BLUR,
        TransitionKind.FLIP,
        TransitionKind.GLITCH -> TRANSITION_TEMPLATE.replace(
            "/*BODY*/",
            "float m = smoothstep(0.0, 1.0, uProgress);\n  outColor = mix(a, b, m);"
        )
    }

    private const val TRANSITION_TEMPLATE = """
#version 300 es
precision mediump float;
uniform sampler2D uTexA;
uniform sampler2D uTexB;
uniform float uProgress;
uniform vec4 uFillColor;
in vec2 uv;
out vec4 fragColor;
void main() {
  vec4 a = texture(uTexA, uv);
  vec4 b = texture(uTexB, uv);
  vec4 outColor = mix(a, b, uProgress);
  /*BODY*/
  fragColor = outColor;
}
"""
}

/** Small helper for building a previewable list of transitions in the UI. */
object TransitionCatalog {

    private val twoFrame: Set<TransitionKind> = setOf(
        TransitionKind.CROSS_DISSOLVE,
        TransitionKind.WIPE_LINEAR,
        TransitionKind.WIPE_RADIAL,
        TransitionKind.ORGANIC_WIPE,
        TransitionKind.INK_BLEED,
        TransitionKind.ZOOM,
        TransitionKind.PUSH_LEFT,
        TransitionKind.GLITCH
    )

    fun requiresTwoFrames(kind: TransitionKind): Boolean = kind in twoFrame

    fun ordered(): List<TransitionKind> = TransitionKind.entries

    /** Frame the shader mix is evaluated at, snapped to the project timebase. */
    fun mixFrame(elapsedUs: Long, spec: TransitionSpec, fps: Rational): Long =
        TimeUs(elapsedUs).snapToFrame(fps, FrameRounding.NEAREST).toFrame(fps)

    fun supportsRotation(kind: TransitionKind): Boolean =
        kind == TransitionKind.SPIN || kind == TransitionKind.FLIP

    fun supportsFill(kind: TransitionKind): Boolean =
        kind == TransitionKind.DIP_TO_BLACK || kind == TransitionKind.FLASH_WHITE
}
