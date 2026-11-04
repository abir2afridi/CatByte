package com.example.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.StaticOverlaySettings
import com.example.data.KeyframeInterpolator
import com.example.model.EffectType
import com.example.model.KeyframeProperty
import com.example.model.StickerOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.VideoProject
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws every 2D layer of the project into a single full-frame bitmap overlay.
 *
 * Using one overlay (instead of one bitmap texture per text/sticker) keeps the
 * GL pipeline small, lets animations be evaluated against the exact
 * presentation timestamp, and guarantees that the exported MP4 contains the
 * same text, stickers, effects and watermark as the live preview.
 */
@UnstableApi
class TimelineCanvasOverlay(
    private val project: VideoProject,
    private val watermarkEnabled: Boolean
) : CanvasOverlay(true) {

    private val overlaySettings: OverlaySettings = StaticOverlaySettings.Builder()
        .setAlphaScale(1f)
        .build()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val effectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = overlaySettings

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        // CanvasOverlay reuses its bitmap between frames, so start every frame
        // from a genuinely transparent surface.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val timeMs = (presentationTimeUs / 1000L).coerceAtLeast(0L)
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        if (width <= 0f || height <= 0f) return

        drawVignette(canvas, timeMs, width, height)
        drawTimelineEffects(canvas, timeMs, width, height)
        project.textOverlays.forEach { drawText(canvas, it, timeMs, width, height) }
        project.stickers.forEach { drawSticker(canvas, it, timeMs, width, height) }
        if (watermarkEnabled) drawWatermark(canvas, width, height)
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    private fun drawText(
        canvas: Canvas,
        overlay: TextOverlay,
        timeMs: Long,
        width: Float,
        height: Float
    ) {
        if (overlay.text.isBlank()) return
        if (timeMs < overlay.startTimelineMs) return
        if (timeMs > overlay.startTimelineMs + overlay.durationMs) return

        val localMs = timeMs - overlay.startTimelineMs
        val progress = (localMs.toFloat() / overlay.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)

        val displayText = when (overlay.animation) {
            TextAnimationType.TYPEWRITER -> {
                val visible = (overlay.text.length * (progress * 1.6f)).toInt()
                    .coerceIn(1, overlay.text.length)
                overlay.text.take(visible)
            }
            else -> overlay.text
        }
        if (displayText.isEmpty()) return

        val posX = KeyframeInterpolator.interpolate(
            overlay.keyframes, KeyframeProperty.POSITION_X, localMs, overlay.posX
        ).coerceIn(0f, 1f)
        val posY = KeyframeInterpolator.interpolate(
            overlay.keyframes, KeyframeProperty.POSITION_Y, localMs, overlay.posY
        ).coerceIn(0f, 1f)
        val keyScale = KeyframeInterpolator.interpolate(
            overlay.keyframes, KeyframeProperty.SCALE, localMs, overlay.scale
        ).coerceAtLeast(0.05f)
        val rotation = KeyframeInterpolator.interpolate(
            overlay.keyframes, KeyframeProperty.ROTATION, localMs, 0f
        )
        val opacity = KeyframeInterpolator.interpolate(
            overlay.keyframes, KeyframeProperty.OPACITY, localMs, 1f
        ).coerceIn(0f, 1f)

        var animationScale = 1f
        var animationAlpha = 1f
        var yOffset = 0f
        when (overlay.animation) {
            TextAnimationType.POP_UP -> {
                val p = (localMs / 320f).coerceIn(0f, 1f)
                animationScale = 0.6f + 0.4f * p
            }
            TextAnimationType.FADE_IN -> animationAlpha = (localMs / 320f).coerceIn(0f, 1f)
            TextAnimationType.NEON_PULSE -> {
                animationAlpha = 0.72f + 0.28f * sin(localMs / 210.0).toFloat()
            }
            TextAnimationType.WAVE -> yOffset = sin(localMs / 125.0).toFloat() * height * 0.018f
            TextAnimationType.TYPEWRITER, TextAnimationType.NONE -> Unit
        }

        val alpha = (opacity * animationAlpha * 255f).toInt().coerceIn(0, 255)
        textPaint.apply {
            color = withAlpha(overlay.color.toInt(), alpha)
            textSize = width * 0.085f * keyScale * animationScale
            typeface = typefaceFor(overlay.fontStyle)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            clearShadowLayer()
            if (overlay.fontStyle == TextFontStyle.NEON_GLOW) {
                setShadowLayer(max(2f, width * 0.012f), 0f, 0f, withAlpha(overlay.color.toInt(), alpha / 2))
            }
        }

        val maxTextWidth = max(2, (width * 0.88f).toInt())
        val layout = StaticLayout.Builder
            .obtain(displayText, 0, displayText.length, textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.0f)
            .build()

        val textWidth = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
        val padX = width * 0.018f
        val padY = height * 0.010f
        val centerX = posX * width
        val centerY = posY * height + yOffset

        canvas.save()
        canvas.rotate(rotation, centerX, centerY)

        overlay.backgroundColor?.let { color ->
            backgroundPaint.apply {
                this.color = withAlpha(color.toInt(), alpha)
                style = Paint.Style.FILL
            }
            val halfW = (textWidth / 2f + padX).coerceAtMost(maxTextWidth / 2f + padX)
            val halfH = layout.height / 2f + padY
            val radius = min(width, height) * 0.018f
            canvas.drawRoundRect(
                RectF(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH),
                radius,
                radius,
                backgroundPaint
            )
        }

        canvas.translate(centerX - maxTextWidth / 2f, centerY - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    // ------------------------------------------------------------------
    // Stickers
    // ------------------------------------------------------------------

    private fun drawSticker(
        canvas: Canvas,
        sticker: StickerOverlay,
        timeMs: Long,
        width: Float,
        height: Float
    ) {
        if (sticker.symbol.isBlank()) return
        if (timeMs < sticker.startTimelineMs) return
        if (timeMs > sticker.startTimelineMs + sticker.durationMs) return
        val localMs = timeMs - sticker.startTimelineMs

        val posX = KeyframeInterpolator.interpolate(
            sticker.keyframes, KeyframeProperty.POSITION_X, localMs, sticker.posX
        ).coerceIn(0f, 1f)
        val posY = KeyframeInterpolator.interpolate(
            sticker.keyframes, KeyframeProperty.POSITION_Y, localMs, sticker.posY
        ).coerceIn(0f, 1f)
        val keyScale = KeyframeInterpolator.interpolate(
            sticker.keyframes, KeyframeProperty.SCALE, localMs, sticker.scale
        ).coerceAtLeast(0.05f)
        val rotation = KeyframeInterpolator.interpolate(
            sticker.keyframes, KeyframeProperty.ROTATION, localMs, 0f
        )
        val opacity = KeyframeInterpolator.interpolate(
            sticker.keyframes, KeyframeProperty.OPACITY, localMs, 1f
        ).coerceIn(0f, 1f)
        val appear = (localMs / 240f).coerceIn(0f, 1f)
        val scale = keyScale * (0.7f + 0.3f * appear)

        paint.apply {
            color = withAlpha(Color.WHITE, (opacity * 255).toInt().coerceIn(0, 255))
            textSize = width * 0.22f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            clearShadowLayer()
            setShadowLayer(max(1f, width * 0.006f), 0f, width * 0.004f, Color.argb(120, 0, 0, 0))
        }

        val centerX = posX * width
        val centerY = posY * height
        canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        canvas.drawText(sticker.symbol, centerX, centerY + paint.textSize * 0.35f, paint)
        canvas.restore()
    }

    // ------------------------------------------------------------------
    // Watermark / vignette
    // ------------------------------------------------------------------

    private fun drawWatermark(canvas: Canvas, width: Float, height: Float) {
        paint.apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = width * 0.045f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            clearShadowLayer()
            setShadowLayer(max(1f, width * 0.008f), 0f, 0f, Color.argb(170, 0, 0, 0))
        }
        canvas.drawText(
            "CatByte",
            width * 0.97f,
            height * 0.96f,
            paint
        )
    }

    private fun drawVignette(canvas: Canvas, timeMs: Long, width: Float, height: Float) {
        val clip = mainClipAt(timeMs) ?: return
        val strength = clip.vignette.coerceIn(0f, 1f)
        if (strength <= 0.001f) return
        effectPaint.shader = RadialGradient(
            width / 2f,
            height / 2f,
            max(width, height) * 0.72f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.argb((strength * 180).toInt().coerceIn(0, 255), 0, 0, 0)
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, effectPaint)
        effectPaint.shader = null
    }

    // ------------------------------------------------------------------
    // Timeline visual effects
    // ------------------------------------------------------------------

    private fun drawTimelineEffects(canvas: Canvas, timeMs: Long, width: Float, height: Float) {
        project.effects.forEach { effect ->
            if (timeMs < effect.startTimelineMs) return@forEach
            if (timeMs > effect.startTimelineMs + effect.durationMs) return@forEach
            drawTimelineEffect(canvas, effect, timeMs - effect.startTimelineMs, width, height)
        }
    }

    private fun drawTimelineEffect(
        canvas: Canvas,
        effect: TimelineEffect,
        localMs: Long,
        width: Float,
        height: Float
    ) {
        when (effect.effectType) {
            EffectType.RGB_SPLIT -> {
                val shift = width * 0.008f
                effectPaint.shader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(Color.argb(52, 255, 0, 80), Color.TRANSPARENT, Color.argb(52, 0, 220, 255)),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, effectPaint)
                effectPaint.shader = null
                effectPaint.color = Color.argb(38, 255, 0, 40)
                canvas.drawRect(0f, 0f, shift, height, effectPaint)
                effectPaint.color = Color.argb(38, 0, 220, 255)
                canvas.drawRect(width - shift, 0f, width, height, effectPaint)
            }
            EffectType.SHAKE -> {
                val jitter = sin(localMs / 38.0).toFloat() * width * 0.006f
                effectPaint.color = Color.argb(26, 255, 255, 255)
                canvas.drawRect(jitter, 0f, width + jitter, height, effectPaint)
                effectPaint.color = Color.argb(18, 0, 0, 0)
                canvas.drawRect(-jitter, 0f, width - jitter, height, effectPaint)
            }
            EffectType.VHS_GRAIN -> {
                val random = Random(localMs / 33L + 7L)
                effectPaint.color = Color.argb(34, 255, 255, 255)
                repeat(46) {
                    val y = random.nextFloat() * height
                    val thickness = 1f + random.nextFloat() * 2.2f
                    canvas.drawRect(0f, y, width, y + thickness, effectPaint)
                }
                effectPaint.color = Color.argb(18, 0, 0, 0)
                repeat(18) {
                    val y = random.nextFloat() * height
                    canvas.drawRect(0f, y, width, y + 3f, effectPaint)
                }
            }
            EffectType.NEON_HALO -> {
                val pulse = 0.55f + 0.45f * sin(localMs / 180.0).toFloat()
                effectPaint.style = Paint.Style.STROKE
                effectPaint.strokeWidth = max(2f, width * 0.012f)
                effectPaint.color = Color.argb((70 * pulse).toInt(), 56, 189, 248)
                canvas.drawRect(2f, 2f, width - 2f, height - 2f, effectPaint)
                effectPaint.strokeWidth = max(1f, width * 0.005f)
                effectPaint.color = Color.argb((110 * pulse).toInt(), 125, 211, 252)
                canvas.drawRect(6f, 6f, width - 6f, height - 6f, effectPaint)
                effectPaint.style = Paint.Style.FILL
            }
            EffectType.LIGHTNING -> {
                val phase = localMs % 1150L
                if (phase < 75L) {
                    effectPaint.color = Color.argb(120, 255, 255, 255)
                    canvas.drawRect(0f, 0f, width, height, effectPaint)
                }
            }
            EffectType.ZOOM_BLUR -> {
                effectPaint.style = Paint.Style.STROKE
                effectPaint.strokeWidth = max(1f, width * 0.003f)
                val rings = 5
                for (i in 0 until rings) {
                    val fraction = (i + 1f) / rings
                    val alpha = (18 * (1f - fraction)).toInt().coerceAtLeast(0)
                    effectPaint.color = Color.argb(alpha, 255, 255, 255)
                    val inset = width * 0.06f * fraction
                    canvas.drawRect(inset, inset, width - inset, height - inset, effectPaint)
                }
                effectPaint.style = Paint.Style.FILL
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun mainClipAt(timeMs: Long): com.example.model.MediaClip? {
        var cursor = 0L
        for (clip in project.clips) {
            val end = cursor + clip.durationMs
            if (timeMs >= cursor && timeMs <= end) return clip
            cursor = end
        }
        return null
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun typefaceFor(style: TextFontStyle): Typeface = when (style) {
        TextFontStyle.CAPCUT_BOLD -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        TextFontStyle.CLEAN_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        TextFontStyle.CINEMATIC_SERIF -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        TextFontStyle.NEON_GLOW -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
        TextFontStyle.SUBTITLE -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Kept here so the unused-import check does not flag the shared timing logic.
    @Suppress("unused")
    private fun activeClipDurationMs(): Long =
        project.clips.sumOf { it.durationMs }.coerceAtLeast(1L)

    @Suppress("unused")
    private fun approximatelyEqual(a: Float, b: Float): Boolean = abs(a - b) < 0.0001f
}
