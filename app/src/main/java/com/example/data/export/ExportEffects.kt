package com.example.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import com.example.model.AspectRatioType
import com.example.model.TextFontStyle
import com.example.model.TransitionType
import com.example.model.VideoFilter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Helpers that translate the CatByte editing model into real Media3 rendering
 * effects so that what the user sees in the preview is actually burned into the
 * exported MP4 file.
 */
object ExportPresets {

    /** Target height (in pixels) for the exported video. */
    fun heightFor(resolution: String): Int = when (resolution.uppercase().replace(" ", "")) {
        "360P" -> 360
        "720P" -> 720
        "4K", "2160P" -> 2160
        else -> 1080
    }

    /** Output frame size for the given aspect ratio and resolution. */
    fun outputSize(ratio: AspectRatioType, resolution: String): Pair<Int, Int> {
        val height = heightFor(resolution)
        val width = (height * ratio.ratioWidth / ratio.ratioHeight).toInt()
        return Pair(width - (width % 2), height - (height % 2))
    }
}

/**
 * A colour grading effect that can be evaluated for every frame. Supports
 * brightness, contrast, saturation and per-channel gains (used by the filters).
 *
 * The matrix layout follows Media3's 4x4 column major convention:
 * ```
 * m[0]  m[1]  m[2]  m[3]    -> red
 * m[4]  m[5]  m[6]  m[7]    -> green
 * m[8]  m[9]  m[10] m[11]   -> blue
 * m[12] m[13] m[14] m[15]   -> alpha
 * ```
 */
class ColorGradeEffect(
    private val brightness: Float = 0f,
    private val contrast: Float = 0f,
    private val saturation: Float = 1f,
    private val redGain: Float = 1f,
    private val greenGain: Float = 1f,
    private val blueGain: Float = 1f
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useLinearToneMapping: Boolean): FloatArray {
        val matrix = GlUtil.create4x4IdentityMatrix()

        val b = brightness.coerceIn(-1f, 1f)
        val c = contrast.coerceIn(-1f, 1f)
        val s = saturation.coerceIn(0f, 2f)

        // Contrast around the 0.5 mid point.
        val contrastScale = (1f + c) / 1.0001f
        val offset = -0.5f * contrastScale + 0.5f + b

        // Rec.709 style saturation matrix (identity when s == 1).
        val sr = 0.213f + 0.787f * s
        val sg = 0.715f - 0.715f * s
        val sb = 0.072f - 0.072f * s
        val gr = 0.213f - 0.213f * s
        val gg = 0.715f + 0.285f * s
        val gb = 0.072f - 0.072f * s
        val br = 0.213f - 0.213f * s
        val bg = 0.715f - 0.715f * s
        val bb = 0.072f + 0.928f * s

        matrix[0] = sr * contrastScale * redGain
        matrix[1] = sg * contrastScale * redGain
        matrix[2] = sb * contrastScale * redGain
        matrix[3] = offset * redGain

        matrix[4] = gr * contrastScale * greenGain
        matrix[5] = gg * contrastScale * greenGain
        matrix[6] = gb * contrastScale * greenGain
        matrix[7] = offset * greenGain

        matrix[8] = br * contrastScale * blueGain
        matrix[9] = bg * contrastScale * blueGain
        matrix[10] = bb * contrastScale * blueGain
        matrix[11] = offset * blueGain

        matrix[15] = 1f
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return brightness == 0f && contrast == 0f && saturation == 1f &&
            redGain == 1f && greenGain == 1f && blueGain == 1f
    }
}

/**
 * A transition effect that changes the RGB gain over time. Used to burn the
 * fade / flash transitions into the exported video.
 */
class TimeRgbGainEffect(
    private val startUs: Long,
    private val endUs: Long,
    private val gainAt: (Long) -> FloatArray
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useLinearToneMapping: Boolean): FloatArray {
        val matrix = GlUtil.create4x4IdentityMatrix()
        val gain = gainAt(presentationTimeUs)
        matrix[0] = gain[0]
        matrix[5] = gain[1]
        matrix[10] = gain[2]
        matrix[15] = 1f
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

/**
 * A transformation that can change over time - used for the zoom / slide
 * transitions.
 */
class TimeTransformEffect(
    private val matrixAt: (Long) -> android.graphics.Matrix
) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): android.graphics.Matrix = matrixAt(presentationTimeUs)

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

/**
 * A texture overlay that is only visible between [startUs] and [endUs].
 * Outside of that window a fully transparent texture is used so the GL
 * pipeline always has a valid texture bound.
 */
class TimedBitmapOverlay(
    private val bitmap: Bitmap,
    private val startUs: Long,
    private val endUs: Long,
    private val overlaySettings: StaticOverlaySettings = StaticOverlaySettings.Builder().build()
) : TextureOverlay() {

    private var contentTextureId: Int = 0
    private var blankTextureId: Int = 0
    private var released = false

    override fun getTextureId(presentationTimeUs: Long): Int {
        if (released) return 0
        val active = presentationTimeUs >= startUs && presentationTimeUs <= endUs
        if (active) {
            if (contentTextureId == 0) {
                contentTextureId = runCatching { GlUtil.createTexture(bitmap) }.getOrDefault(0)
            }
            return contentTextureId
        }
        if (blankTextureId == 0) {
            blankTextureId = runCatching {
                val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                blank.setPixel(0, 0, Color.TRANSPARENT)
                GlUtil.createTexture(blank)
            }.getOrDefault(0)
        }
        return blankTextureId
    }

    override fun getTextureSize(presentationTimeUs: Long): Size =
        Size(bitmap.width, bitmap.height)

    override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings = overlaySettings

    override fun release() {
        released = true
        if (contentTextureId != 0) {
            runCatching { GlUtil.deleteTexture(contentTextureId) }
            contentTextureId = 0
        }
        if (blankTextureId != 0) {
            runCatching { GlUtil.deleteTexture(blankTextureId) }
            blankTextureId = 0
        }
    }
}

/** Maps the CatByte filters onto real colour grading values. */
object FilterMapper {

    data class Grade(
        val redGain: Float,
        val greenGain: Float,
        val blueGain: Float,
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    )

    fun gradeFor(filter: VideoFilter): Grade = when (filter) {
        VideoFilter.NONE -> Grade(1f, 1f, 1f, 0f, 0f, 1f)
        VideoFilter.SKETCH_GRAPHITE -> Grade(0.94f, 0.96f, 1f, 0.06f, 0.22f, 0.15f)
        VideoFilter.BLUEPRINT -> Grade(0.42f, 0.78f, 1.25f, -0.04f, 0.18f, 0.85f)
        VideoFilter.SEPIA_NOTEBOOK -> Grade(1.22f, 1.02f, 0.72f, 0.05f, 0.05f, 0.7f)
        VideoFilter.CHARCOAL_SMUDGE -> Grade(1f, 1f, 1f, -0.1f, 0.42f, 0f)
        VideoFilter.HALFTONE_MANGA -> Grade(1f, 1f, 1f, 0.08f, 0.3f, 0.25f)
        VideoFilter.CYBERPUNK -> Grade(0.88f, 0.95f, 1.3f, -0.04f, 0.26f, 1.35f)
        VideoFilter.VINTAGE -> Grade(1.14f, 1.02f, 0.86f, 0.04f, -0.06f, 0.8f)
        VideoFilter.GOLDEN_HOUR -> Grade(1.2f, 1.04f, 0.78f, 0.08f, 0.08f, 1.15f)
        VideoFilter.NOIR -> Grade(1f, 1f, 1f, 0f, 0.45f, 0f)
        VideoFilter.TEAL_ORANGE -> Grade(0.92f, 1.03f, 1.12f, 0.04f, 0.18f, 1.2f)
        VideoFilter.MOODY -> Grade(0.92f, 0.97f, 1.06f, -0.08f, 0.12f, 0.55f)
        VideoFilter.GLITCH_ART -> Grade(1.12f, 0.94f, 1.18f, 0.02f, 0.32f, 1.45f)
    }
}

/** Builds the transition effects that are burned into the exported video. */
object TransitionMapper {

    /**
     * Returns a list of effects for a clip that plays between [clipStartUs] and
     * [clipEndUs] and uses [transition].
     */
    fun effectsFor(
        transition: TransitionType,
        clipStartUs: Long,
        clipEndUs: Long,
        transitionDurationMs: Long
    ): List<androidx.media3.effect.GlEffect> {
        if (transition == TransitionType.NONE) return emptyList()
        val durationUs = transitionDurationMs.coerceIn(80L, 2000L) * 1000L
        val span = (clipEndUs - clipStartUs).coerceAtLeast(1L)

        return when (transition) {
            TransitionType.FADE_BLACK, TransitionType.DISSOLVE, TransitionType.PAGE_FLIP,
            TransitionType.PAPER_TEAR, TransitionType.PENCIL_WIPE, TransitionType.INK_BLEED ->
                listOf(fadeEffect(clipStartUs, clipEndUs, durationUs, span, flashToWhite = false))

            TransitionType.FLASH_WHITE ->
                listOf(fadeEffect(clipStartUs, clipEndUs, durationUs, span, flashToWhite = true))

            TransitionType.WHIP_PAN -> listOf(
                fadeEffect(clipStartUs, clipEndUs, durationUs, span, flashToWhite = false),
                slideEffect(clipStartUs, clipEndUs, durationUs, span, horizontal = true)
            )

            TransitionType.ZOOM_IN -> listOf(
                fadeEffect(clipStartUs, clipEndUs, durationUs, span, flashToWhite = false),
                zoomEffect(clipStartUs, clipEndUs, durationUs, span)
            )

            TransitionType.GLITCH -> listOf(
                TimeRgbGainEffect(clipStartUs, clipEndUs) { time ->
                    val phase = ((time - clipStartUs) % 120_000L) / 120_000L
                    val flicker = if (phase < 0.5f) 1.35f else 0.82f
                    floatArrayOf(flicker, 1f, 1.18f)
                }
            )

            TransitionType.NONE -> emptyList()
        }
    }

    /** Fade the clip towards black at the start and back at the end. */
    private fun fadeEffect(
        startUs: Long,
        endUs: Long,
        durationUs: Long,
        span: Long,
        flashToWhite: Boolean
    ): TimeRgbGainEffect = TimeRgbGainEffect(startUs, endUs) { time ->
        val local = time - startUs
        val remaining = endUs - time
        var gain = 1f
        if (local < durationUs) {
            val progress = local.toFloat() / durationUs
            gain = if (flashToWhite) {
                // Brighten towards white instead of fading to black.
                1f + 2.4f * progress
            } else {
                progress
            }
        } else if (remaining < durationUs) {
            val progress = remaining.toFloat() / durationUs
            gain = if (flashToWhite) {
                1f + 2.4f * progress
            } else {
                progress
            }
        }
        val clamped = gain.coerceIn(0f, 3.5f)
        floatArrayOf(clamped, clamped, clamped)
    }.also { _ -> /* span kept for readability */ }

    /** Slide the clip in from the side. */
    private fun slideEffect(
        startUs: Long,
        endUs: Long,
        durationUs: Long,
        span: Long,
        horizontal: Boolean
    ): TimeTransformEffect = TimeTransformEffect { time ->
        val matrix = android.graphics.Matrix()
        val local = time - startUs
        val remaining = endUs - time
        val progress = when {
            local < durationUs -> 1f - (local.toFloat() / durationUs)
            remaining < durationUs -> remaining.toFloat() / durationUs
            else -> 0f
        }
        val offset = progress * 0.35f
        if (horizontal) matrix.setTranslate(offset, 0f) else matrix.setTranslate(0f, offset)
        matrix
    }

    /** Zoom the clip in slightly at the beginning. */
    private fun zoomEffect(
        startUs: Long,
        endUs: Long,
        durationUs: Long,
        span: Long
    ): TimeTransformEffect = TimeTransformEffect { time ->
        val matrix = android.graphics.Matrix()
        val local = time - startUs
        val progress = if (local < durationUs) {
            1f - (local.toFloat() / durationUs) * 0.25f
        } else {
            1f
        }
        matrix.setScale(progress, progress, 0.5f, 0.5f)
        matrix
    }
}

/** Draws the text / sticker / watermark layers that are burned into the export. */
object OverlayRenderer {

    /**
     * Renders a transparent canvas the same aspect ratio as the output frame and
     * draws [text] at the requested position. The canvas is scaled down to keep
     * memory usage reasonable while still covering the whole frame.
     */
    fun renderTextLayer(
        frameWidth: Int,
        frameHeight: Int,
        text: String,
        color: Long,
        fontStyle: TextFontStyle,
        posX: Float,
        posY: Float,
        scale: Float,
        withShadow: Boolean = false
    ): Bitmap? {
        if (text.isBlank()) return null
        val factor = max(1f, (max(frameWidth, frameHeight) / 960f))
        val canvasWidth = max(2, (frameWidth / factor).toInt())
        val canvasHeight = max(2, (frameHeight / factor).toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toInt()
            textSize = canvasWidth * 0.085f * scale.coerceAtLeast(0.2f)
            typeface = typefaceFor(fontStyle)
            textAlign = Paint.Align.CENTER
            if (withShadow) {
                setShadowLayer(canvasWidth * 0.02f, 0f, 0f, Color.BLACK)
            }
        }
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(110, 0, 0, 0)
        }

        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = bounds.width() + canvasWidth / 20
        val textHeight = bounds.height() + canvasWidth / 24

        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = (posX.coerceIn(0f, 1f) * canvasWidth).toInt()
        val centerY = (posY.coerceIn(0f, 1f) * canvasHeight).toInt()

        val left = centerX - textWidth / 2
        val top = centerY - textHeight / 2
        canvas.drawRoundRect(
            left.toFloat(),
            top.toFloat(),
            (left + textWidth).toFloat(),
            (top + textHeight).toFloat(),
            canvasWidth * 0.02f,
            canvasWidth * 0.02f,
            backgroundPaint
        )
        canvas.drawText(text, centerX.toFloat(), (centerY + bounds.height() / 2f), paint)
        return bitmap
    }

    /** Renders a sticker (emoji) onto a transparent full-frame canvas. */
    fun renderStickerLayer(
        frameWidth: Int,
        frameHeight: Int,
        symbol: String,
        posX: Float,
        posY: Float,
        scale: Float
    ): Bitmap? {
        if (symbol.isBlank()) return null
        val factor = max(1f, (max(frameWidth, frameHeight) / 960f))
        val canvasWidth = max(2, (frameWidth / factor).toInt())
        val canvasHeight = max(2, (frameHeight / factor).toInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = canvasWidth * 0.22f * scale.coerceAtLeast(0.2f)
            textAlign = Paint.Align.CENTER
        }
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = (posX.coerceIn(0f, 1f) * canvasWidth).toInt()
        val centerY = (posY.coerceIn(0f, 1f) * canvasHeight).toInt()
        canvas.drawText(symbol, centerX.toFloat(), centerY.toFloat(), paint)
        return bitmap
    }

    /** Renders the small "CatByte" watermark in the bottom-right corner. */
    fun renderWatermarkLayer(frameWidth: Int, frameHeight: Int): Bitmap? {
        val factor = max(1f, (max(frameWidth, frameHeight) / 960f))
        val canvasWidth = max(2, (frameWidth / factor).toInt())
        val canvasHeight = max(2, (frameHeight / factor).toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = canvasWidth * 0.045f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(canvasWidth * 0.012f, 0f, 0f, Color.argb(160, 0, 0, 0))
        }
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText("CatByte", (canvasWidth * 0.97f).toInt().toFloat(), (canvasHeight * 0.96f).toInt().toFloat(), paint)
        return bitmap
    }

    private fun typefaceFor(style: TextFontStyle): Typeface = when (style) {
        TextFontStyle.CAPCUT_BOLD -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        TextFontStyle.CLEAN_SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        TextFontStyle.CINEMATIC_SERIF -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        TextFontStyle.NEON_GLOW -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
        TextFontStyle.SUBTITLE -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
}

/** Small helpers used by the export pipeline. */
internal fun Float.safeScale(): Float = if (abs(this) < 0.0001f) 0.0001f else this

internal fun clampRatio(value: Float): Float = min(1f, max(0f, value))
