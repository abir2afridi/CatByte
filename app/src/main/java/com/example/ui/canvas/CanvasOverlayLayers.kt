package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KeyframeInterpolator
import com.example.model.KeyframeProperty
import com.example.model.StickerOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay

/**
 * The fallback 2D overlay layers (text, stickers, watermark).
 *
 * The composition preview burns these into the video, so they are only drawn
 * by the Compose fallback renderer - and during a scrub, where the extracted
 * frame is raw source media and therefore has no overlays at all.
 */

/** Text overlays, with the typewriter / pop-up / neon-pulse animations. */
@Composable
internal fun CanvasTextOverlayLayers(
    activeTexts: List<TextOverlay>,
    positionMs: Long,
    pulseAlpha: Float
) {
    activeTexts.forEach { textOverlay ->
        val displayText = if (textOverlay.animation == TextAnimationType.TYPEWRITER) {
            val progress = ((positionMs - textOverlay.startTimelineMs).toFloat() / textOverlay.durationMs).coerceIn(0f, 1f)
            val charCount = (textOverlay.text.length * progress * 2).toInt().coerceIn(1, textOverlay.text.length)
            textOverlay.text.take(charCount)
        } else {
            textOverlay.text
        }

        val textScale = if (textOverlay.animation == TextAnimationType.POP_UP) {
            val p = ((positionMs - textOverlay.startTimelineMs).toFloat() / 300f).coerceIn(0f, 1f)
            0.6f + (0.4f * p)
        } else if (textOverlay.animation == TextAnimationType.NEON_PULSE) {
            pulseAlpha
        } else {
            1.0f
        }

        // The export renderer evaluates text keyframes; the fallback preview
        // must do the same or a keyframed text would jump between scrub and play.
        val localMs = (positionMs - textOverlay.startTimelineMs).coerceAtLeast(0L)
        val keyPosX = KeyframeInterpolator.interpolate(
            textOverlay.keyframes, KeyframeProperty.POSITION_X, localMs, textOverlay.posX
        )
        val keyPosY = KeyframeInterpolator.interpolate(
            textOverlay.keyframes, KeyframeProperty.POSITION_Y, localMs, textOverlay.posY
        )
        val keyScale = KeyframeInterpolator.interpolate(
            textOverlay.keyframes, KeyframeProperty.SCALE, localMs, textOverlay.scale
        )
        val keyOpacity = KeyframeInterpolator.interpolate(
            textOverlay.keyframes, KeyframeProperty.OPACITY, localMs, 1f
        )
        val keyRotation = KeyframeInterpolator.interpolate(
            textOverlay.keyframes, KeyframeProperty.ROTATION, localMs, 0f
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val baseSize = when (textOverlay.fontStyle) {
                TextFontStyle.CAPCUT_BOLD -> 23.sp
                TextFontStyle.NEON_GLOW -> 21.sp
                TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> 20.sp
                TextFontStyle.SUBTITLE -> 16.sp
                else -> 17.sp
            }
            val baseWeight = when (textOverlay.fontStyle) {
                TextFontStyle.CAPCUT_BOLD -> FontWeight.Black
                TextFontStyle.CINEMATIC_SERIF -> FontWeight.SemiBold
                else -> FontWeight.Bold
            }
            // Karaoke: highlight the word being spoken, matching the
            // burned-in export render (warm box under the active word).
            val text = if (textOverlay.karaokeEnabled) {
                karaokeAnnotated(textOverlay, positionMs - textOverlay.startTimelineMs)
            } else {
                androidx.compose.ui.text.buildAnnotatedString { append(displayText) }
            }
            Text(
                text = text,
                color = Color(textOverlay.color),
                fontSize = baseSize,
                // User overrides from the text sheet: bold / italic / spacing.
                fontWeight = if (textOverlay.isBold) FontWeight.Black else baseWeight,
                fontStyle = if (textOverlay.isItalic) FontStyle.Italic else FontStyle.Normal,
                letterSpacing = textOverlay.letterSpacingEm.sp,
                lineHeight = (baseSize.value * textOverlay.lineSpacingMultiplier).sp,
                fontFamily = when (textOverlay.fontStyle) {
                    TextFontStyle.CINEMATIC_SERIF -> FontFamily.Serif
                    // BANGLA_* styles load the bundled OFL Bangla fonts, so
                    // the fallback preview matches the burned-in export.
                    TextFontStyle.BANGLA_NOTO_SANS,
                    TextFontStyle.BANGLA_HIND_SILIGURI,
                    TextFontStyle.BANGLA_TIRO,
                    TextFontStyle.BANGLA_SOLAIMAN_LIPI,
                    TextFontStyle.BANGLA_KALPURUSH -> FontFamily(
                        com.example.data.fonts.BanglaFontManager.forTextFontStyle(textOverlay.fontStyle)
                    )
                    else -> FontFamily.SansSerif
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    // Centred child + fractional offset = the same normalised
                    // (posX, posY) anchor the export renderer uses.
                    .offset(
                        x = maxWidth * (keyPosX.coerceIn(0f, 1f) - 0.5f),
                        y = maxHeight * (keyPosY.coerceIn(0f, 1f) - 0.5f)
                    )
                    .graphicsLayer {
                        scaleX = keyScale * textScale
                        scaleY = keyScale * textScale
                        alpha = keyOpacity.coerceIn(0f, 1f)
                        rotationZ = keyRotation
                    }
                    .background(
                        color = if (textOverlay.backgroundColor != null) Color(textOverlay.backgroundColor) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/** The warm karaoke highlight colour (matches the export renderer). */
private val KaraokeHighlightColor = Color(0xFFFFC531)

/**
 * The caption text with the word being spoken highlighted, as an annotated
 * string. Plain text when the karaoke words do not match the caption.
 */
private fun karaokeAnnotated(overlay: TextOverlay, localMs: Long): androidx.compose.ui.text.AnnotatedString {
    val offsets = overlay.karaokeOffsetsInText()
        ?: return androidx.compose.ui.text.buildAnnotatedString { append(overlay.text) }
    val active = overlay.activeKaraokeIndex(localMs)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(overlay.text)
        if (active != null) {
            val start = offsets.getOrNull(active) ?: return@buildAnnotatedString
            val end = (start + overlay.karaokeWords[active].text.length)
                .coerceAtMost(overlay.text.length)
            addStyle(
                androidx.compose.ui.text.SpanStyle(
                    background = KaraokeHighlightColor,
                    color = Color(0xFF141414)
                ),
                start,
                end
            )
        }
    }
}

/** Sticker overlays (the fallback renderer draws the symbol, not an image). */
@Composable
internal fun CanvasStickerOverlayLayer(activeStickers: List<StickerOverlay>) {    activeStickers.forEach { sticker ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sticker.symbol,
                fontSize = (44 * sticker.scale).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The watermark badge in the bottom-right corner. */
@Composable
internal fun BoxScope.CanvasWatermarkOverlay() {
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "✏️ CatByte",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Freehand drawing strokes ("Text → Draw"). Same normalised points and width
 * fraction as the exported file, so the drawing lands in the same place.
 */
@Composable
internal fun CanvasStrokeOverlayLayers(
    strokes: List<com.example.model.StrokeOverlay>,
    positionMs: Long
) {
    if (strokes.isEmpty()) return
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        strokes.forEach { stroke ->
            if (stroke.points.size < 4) return@forEach
            if (positionMs < stroke.startTimelineMs) return@forEach
            if (positionMs > stroke.startTimelineMs + stroke.durationMs) return@forEach

            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(stroke.points[0] * size.width, stroke.points[1] * size.height)
            var index = 2
            while (index + 1 < stroke.points.size) {
                path.lineTo(stroke.points[index] * size.width, stroke.points[index + 1] * size.height)
                index += 2
            }
            drawPath(
                path = path,
                color = Color(stroke.colorArgb),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = (stroke.widthFraction * size.width).coerceAtLeast(1.5f),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}
