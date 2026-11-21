package com.example.data.export

import android.graphics.Bitmap
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
import com.example.engine.TextFx
import com.example.engine.BackgroundMath
import com.example.engine.backgroundArgbFor
import com.example.model.EffectType
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
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
    private val watermarkEnabled: Boolean,
    /** Decoded canvas background image, already downscaled (null = colour only). */
    private val backgroundImage: Bitmap? = null,
    /**
     * True when the composition itself supplies the background (blurred video
     * filling the canvas). The bars are already painted by that input, so the
     * overlay must not draw the colour/image background on top of it.
     */
    private val backgroundSuppliedByComposition: Boolean = false
) : CanvasOverlay(true) {

    private val overlaySettings: OverlaySettings = StaticOverlaySettings.Builder()
        .setAlphaScale(1f)
        .build()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundImagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val effectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = overlaySettings

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        // CanvasOverlay reuses its bitmap between frames, so start every frame
        // from a genuinely transparent surface.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val timeMs = (presentationTimeUs / 1000L).coerceAtLeast(0L)
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        if (width <= 0f || height <= 0f) return

        if (!backgroundSuppliedByComposition) {
            drawCanvasBackground(canvas, timeMs, width, height)
        }
        drawVignette(canvas, timeMs, width, height)
        drawTimelineEffects(canvas, timeMs, width, height)
        project.textOverlays.forEach { drawText(canvas, it, timeMs, width, height) }
        project.stickers.forEach { drawSticker(canvas, it, timeMs, width, height) }
        project.strokes.forEach { drawStroke(canvas, it, timeMs, width, height) }
        if (watermarkEnabled) drawWatermark(canvas, width, height)
    }

    // ------------------------------------------------------------------
    // Canvas background (the bars around a video that does not fill the canvas)
    // ------------------------------------------------------------------

    /**
     * Fills the letterbox/pillarbox bars with the project's background colour
     * and (when set) the background image. The video itself is never covered:
     * the fitted rectangle comes from [BackgroundMath] and matches the contain
     * layout Media3 uses for the clip.
     */
    private fun drawCanvasBackground(canvas: Canvas, timeMs: Long, width: Float, height: Float) {
        val active = mainClipAndLocal(timeMs) ?: return
        val clip = active.first
        val localMs = active.second
        val sourceAspect = sourceAspectOf(clip) ?: return
        // During a transition two clips share the frame; filling the outgoing
        // clip's bars could paint over the incoming picture.
        if (clip.transition != com.example.model.TransitionType.NONE &&
            localMs < clip.transitionDurationMs
        ) {
            return
        }
        // A zoomed/cropped clip (scale > 1) already fills the frame; painting
        // anything would cover real pixels.
        if (clip.scale > 1.0001f) return

        val rect = BackgroundMath.fittedVideoRect(sourceAspect, width / height)
        val left = rect.left * width
        val top = rect.top * height
        val right = rect.right * width
        val bottom = rect.bottom * height
        val fillsCanvas = rect.width >= 0.999f && rect.height >= 0.999f
        if (fillsCanvas) return

        val bars = arrayOf(
            RectF(0f, 0f, width, top),
            RectF(0f, bottom, width, height),
            RectF(0f, top, left, bottom),
            RectF(right, top, width, bottom)
        )

        backgroundPaint.apply {
            shader = null
            style = Paint.Style.FILL
            alpha = 255
            color = backgroundArgbFor(project.paperStyle).toInt()
        }
        bars.forEach { bar ->
            if (bar.width() > 0f && bar.height() > 0f) canvas.drawRect(bar, backgroundPaint)
        }

        val bitmap = backgroundImage ?: return
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return
        val destination = coverRect(bitmap, width, height)
        bars.forEach { bar ->
            if (bar.width() <= 0f || bar.height() <= 0f) return@forEach
            canvas.save()
            canvas.clipRect(bar)
            canvas.drawBitmap(bitmap, null, destination, backgroundImagePaint)
            canvas.restore()
        }
    }

    /** Centre-cropped destination of [bitmap] covering the whole canvas (CSS `cover`). */
    private fun coverRect(bitmap: Bitmap, width: Float, height: Float): RectF {
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val canvasAspect = width / height
        return if (imageAspect > canvasAspect) {
            val drawnWidth = height * imageAspect
            val left = (width - drawnWidth) / 2f
            RectF(left, 0f, left + drawnWidth, height)
        } else {
            val drawnHeight = width / imageAspect
            val top = (height - drawnHeight) / 2f
            RectF(0f, top, width, top + drawnHeight)
        }
    }

    /** Active main clip plus its clip-local time (main clips are cumulative). */
    private fun mainClipAndLocal(timeMs: Long): Pair<MediaClip, Long>? {
        var cursor = 0L
        for (clip in project.clips) {
            val end = cursor + clip.durationMs
            if (timeMs < end) return clip to (timeMs - cursor).coerceAtLeast(0L)
            cursor = end
        }
        val last = project.clips.lastOrNull() ?: return null
        return last to last.durationMs
    }

    /** Display aspect of the clip's source, swapping for 90°/270° rotation. */
    private fun sourceAspectOf(clip: MediaClip): Float? {
        val width = clip.sourceWidth
        val height = clip.sourceHeight
        if (width <= 0 || height <= 0) return null
        val rotation = ((clip.rotation % 180f) + 180f) % 180f
        val swapped = rotation > 45f && rotation < 135f
        return if (swapped) height.toFloat() / width else width.toFloat() / height
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
            TextAnimationType.TYPEWRITER -> overlay.text.take(
                TextFx.typewriterChars(overlay.text, progress)
            )
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
            TextAnimationType.POP_UP -> animationScale = TextFx.popUpScale(localMs)
            TextAnimationType.FADE_IN -> animationAlpha = TextFx.fadeInAlpha(localMs)
            TextAnimationType.NEON_PULSE -> animationAlpha = TextFx.neonPulseAlpha(localMs)
            TextAnimationType.WAVE -> yOffset = TextFx.waveOffsetY(localMs, height)
            TextAnimationType.TYPEWRITER, TextAnimationType.NONE -> Unit
            else -> Unit
        }

        val alpha = (opacity * animationAlpha * 255f).toInt().coerceIn(0, 255)
        textPaint.apply {
            color = withAlpha(overlay.color.toInt(), alpha)
            textSize = TextFx.titleTextSizePx(width) * keyScale * animationScale
            // Font style + the user's bold/italic/letter-spacing overrides.
            val textStyle = when {
                overlay.isBold && overlay.isItalic -> Typeface.BOLD_ITALIC
                overlay.isBold -> Typeface.BOLD
                overlay.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            typeface = Typeface.create(typefaceFor(overlay.fontStyle), textStyle)
            letterSpacing = overlay.letterSpacingEm
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            clearShadowLayer()
            if (overlay.fontStyle == TextFontStyle.NEON_GLOW) {
                setShadowLayer(
                    max(2f, TextFx.NEON_SHADOW_WIDTH_FRACTION * width),
                    0f, 0f,
                    withAlpha(overlay.color.toInt(), alpha / 2)
                )
            }
        }

        val maxTextWidth = max(2, (width * 0.88f).toInt())
        val layout = StaticLayout.Builder
            .obtain(displayText, 0, displayText.length, textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, overlay.lineSpacingMultiplier.coerceIn(0.7f, 2.5f))
            .build()

        // The karaoke highlight: a warm box behind the word being spoken, in
        // layout-local coordinates (the same space layout.draw uses below).
        val karaokeHighlight = karaokeHighlightRect(overlay, displayText, localMs, layout, width)

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
        karaokeHighlight?.let { highlight ->
            highlightPaint.color = withAlpha(karaokeHighlightArgb, alpha)
            canvas.drawRoundRect(highlight.rect, width * 0.008f, width * 0.008f, highlightPaint)
        }
        layout.draw(canvas)
        // Re-ink the active word on top of the box so it stays readable.
        karaokeHighlight?.let { highlight ->
            val dark = textPaint.color
            textPaint.color = withAlpha(0xFF141414.toInt(), alpha)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(highlight.word, highlight.left, highlight.baseline, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = dark
        }
        canvas.restore()
    }

    private data class KaraokeHighlight(
        val rect: RectF,
        val word: String,
        val left: Float,
        val baseline: Float
    )

    /**
     * The bounding box of the karaoke word active at [localMs], in the
     * coordinate space of [layout]. Null when the highlight is off or the
     * words no longer match the displayed text.
     */
    private fun karaokeHighlightRect(
        overlay: com.example.model.TextOverlay,
        displayText: String,
        localMs: Long,
        layout: StaticLayout,
        width: Float
    ): KaraokeHighlight? {
        val activeIndex = overlay.activeKaraokeIndex(localMs) ?: return null
        val offsets = overlay.karaokeOffsetsInText() ?: return null
        val wordStart = offsets.getOrNull(activeIndex) ?: return null
        val word = overlay.karaokeWords[activeIndex]
        if (wordStart + word.text.length > displayText.length) return null

        val line = layout.getLineForOffset(wordStart)
        val lineStart = layout.getLineStart(line)
        val prefix = displayText.substring(lineStart, wordStart)
        val pad = width * 0.006f
        val left = layout.getLineLeft(line) + textPaint.measureText(prefix) - pad
        val right = left + textPaint.measureText(word.text) + pad * 2f
        return KaraokeHighlight(
            rect = RectF(
                left,
                layout.getLineTop(line) - pad * 0.5f,
                max(left + pad, right),
                layout.getLineBottom(line) + pad * 0.5f
            ),
            word = word.text,
            left = left + pad,
            baseline = layout.getLineBaseline(line).toFloat()
        )
    }

    // ------------------------------------------------------------------
    // Stickers
    // ------------------------------------------------------------------

    /** Freehand drawing strokes (Text → Draw), normalised to the canvas. */
    private fun drawStroke(
        canvas: Canvas,
        stroke: com.example.model.StrokeOverlay,
        timeMs: Long,
        width: Float,
        height: Float
    ) {
        if (stroke.points.size < 4) return
        if (timeMs < stroke.startTimelineMs) return
        if (timeMs > stroke.startTimelineMs + stroke.durationMs) return

        val path = android.graphics.Path()
        path.moveTo(stroke.points[0] * width, stroke.points[1] * height)
        var index = 2
        while (index + 1 < stroke.points.size) {
            path.lineTo(stroke.points[index] * width, stroke.points[index + 1] * height)
            index += 2
        }
        strokePaint.apply {
            color = stroke.colorArgb.toInt()
            style = Paint.Style.STROKE
            strokeWidth = (stroke.widthFraction * width).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        canvas.drawPath(path, strokePaint)
    }

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
            // Exclusive end: the effect's final impact frame is start+duration-1,
            // matching the preview. A progress == 1.0 draw (full-peak white of a
            // burn/flash family) can never linger on the last exported frame.
            if (timeMs >= effect.startTimelineMs + effect.durationMs) return@forEach
            // Interactive families blend inside the GPU (EffectBlendShader),
            // so the overlay must NOT draw them a second time on top.
            if (effect.effectType.spec.family.isShaderFamily) return@forEach
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
            EffectType.RGB_SPLIT ->
                com.example.data.export.legacy.drawRgbSplit(canvas, effectPaint, localMs, width, height)
            EffectType.SHAKE ->
                com.example.data.export.legacy.drawBeatShake(canvas, effectPaint, localMs, width, height)
            EffectType.VHS_GRAIN ->
                com.example.data.export.legacy.drawVhsGrain(canvas, effectPaint, localMs, width, height)
            EffectType.NEON_HALO ->
                com.example.data.export.legacy.drawNeonHalo(canvas, effectPaint, localMs, width, height)
            EffectType.LIGHTNING ->
                com.example.data.export.legacy.drawLightning(canvas, effectPaint, localMs, width, height)
            EffectType.ZOOM_BLUR ->
                com.example.data.export.legacy.drawZoomBlur(canvas, effectPaint, localMs, width, height)
            else -> {
                // Every library preset: one shared op computation feeds both
                // this export renderer and the Compose preview.
                val spec = effect.effectType.spec.let {
                    it.copy(intensity = (it.intensity * effect.intensityScale).coerceIn(0.05f, 1.6f))
                }
                EffectOpsRenderer.draw(
                    canvas,
                    com.example.engine.EffectOps.compute(effect.effectType, spec, localMs, width, height)
                )
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

    /** Warm yellow of the karaoke word highlight (opaque ARGB). */
    private val karaokeHighlightArgb: Int get() = 0xFFFFC531.toInt()

    private fun typefaceFor(style: TextFontStyle): Typeface {
        // BANGLA_* styles resolve to the bundled OFL Bangla families first
        // (stock Android often ships no Bengali font - a system fallback
        // renders tofu). DEFAULT here means "not a Bangla style" or "the
        // bundled font is missing", in which case the legacy mapping below
        // still applies.
        val bangla = com.example.data.fonts.BanglaFontManager.forTextFontStyle(style)
        if (bangla != Typeface.DEFAULT) return bangla
        return when (style) {
            TextFontStyle.CAPCUT_BOLD -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            TextFontStyle.CLEAN_SANS, TextFontStyle.BANGLA_NOTO_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            TextFontStyle.NEON_GLOW -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
            TextFontStyle.SUBTITLE, TextFontStyle.BANGLA_HIND_SILIGURI -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
    }

    // Kept here so the unused-import check does not flag the shared timing logic.
    @Suppress("unused")
    private fun activeClipDurationMs(): Long =
        project.clips.sumOf { it.durationMs }.coerceAtLeast(1L)

    @Suppress("unused")
    private fun approximatelyEqual(a: Float, b: Float): Boolean = abs(a - b) < 0.0001f
}
