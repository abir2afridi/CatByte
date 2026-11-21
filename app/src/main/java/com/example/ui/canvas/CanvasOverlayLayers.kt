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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KeyframeInterpolator
import com.example.engine.TextFx
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
        // [WYSIWYG] All animation curves come from engine/TextFx - the exact
        // same maths the burned-in export uses. Do not fork the values here.
        val localMs = (positionMs - textOverlay.startTimelineMs).coerceAtLeast(0L)
        val progress = (localMs.toFloat() / textOverlay.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
        val displayText = if (textOverlay.animation == TextAnimationType.TYPEWRITER) {
            textOverlay.text.take(TextFx.typewriterChars(textOverlay.text, progress))
        } else {
            textOverlay.text
        }

        val textScale = when (textOverlay.animation) {
            TextAnimationType.POP_UP -> TextFx.popUpScale(localMs)
            TextAnimationType.NEON_PULSE -> TextFx.neonPulseAlpha(localMs)
            else -> 1.0f
        }
        val fadeAlpha = when (textOverlay.animation) {
            TextAnimationType.FADE_IN -> TextFx.fadeInAlpha(localMs)
            else -> 1.0f
        }
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

        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val previewHeightPx = constraints.maxHeight.toFloat()
            val waveOffsetPx = when (textOverlay.animation) {
                TextAnimationType.WAVE -> TextFx.waveOffsetY(localMs, previewHeightPx)
                else -> 0f
            }
            // [WYSIWYG] Font size = canvas width * TextFx.TITLE_WIDTH_FRACTION,
            // the same fraction the export renderer uses - not per-style sp
            // guesses.
            val baseSize = with(density) {
                (constraints.maxWidth.toFloat() * TextFx.TITLE_WIDTH_FRACTION).toSp()
            }
            val baseSizePx = with(density) { baseSize.toPx() }
            // Compose 1.7 Shadow.blurRadius is Float PIXELS — the px value
            // the export renderer computes (width * TextFx fraction) maps 1:1.
            val neonShadowBlurPx = constraints.maxWidth.toFloat() * TextFx.NEON_SHADOW_WIDTH_FRACTION
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
                // NEON_GLOW gets the export's soft glow shadow.
                style = if (textOverlay.fontStyle == TextFontStyle.NEON_GLOW) {
                    // Compose 1.7 Shadow.blurRadius is Float PIXELS - the
                    // same px value the export renderer computes.
                    TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(textOverlay.color).copy(alpha = 0.5f),
                            blurRadius = neonShadowBlurPx
                        )
                    )
                } else {
                    TextStyle.Default
                },
                // User overrides from the text sheet: bold / italic / spacing.
                fontWeight = if (textOverlay.isBold) FontWeight.Black else baseWeight,
                fontStyle = if (textOverlay.isItalic) FontStyle.Italic else FontStyle.Normal,
                // Letter-spacing is an EM value: scale it by the font size
                // like the export's Paint.letterSpacing does.
                letterSpacing = with(density) {
                    TextFx.letterSpacingPx(baseSizePx, textOverlay.letterSpacingEm).toSp()
                },
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
                        alpha = (keyOpacity * fadeAlpha).coerceIn(0f, 1f)
                        rotationZ = keyRotation
                        translationY = waveOffsetPx * density.density / density.density
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
