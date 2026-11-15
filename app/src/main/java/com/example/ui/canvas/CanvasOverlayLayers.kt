package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = when {
                textOverlay.posY < 0.4f -> Alignment.TopCenter
                textOverlay.posY > 0.6f -> Alignment.BottomCenter
                else -> Alignment.Center
            }
        ) {
            Text(
                text = displayText,
                color = Color(textOverlay.color),
                fontSize = when (textOverlay.fontStyle) {
                    TextFontStyle.CAPCUT_BOLD -> 23.sp
                    TextFontStyle.NEON_GLOW -> 21.sp
                    TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> 20.sp
                    TextFontStyle.SUBTITLE -> 16.sp
                    else -> 17.sp
                },
                fontWeight = when (textOverlay.fontStyle) {
                    TextFontStyle.CAPCUT_BOLD -> FontWeight.Black
                    TextFontStyle.CINEMATIC_SERIF -> FontWeight.SemiBold
                    else -> FontWeight.Bold
                },
                fontFamily = when (textOverlay.fontStyle) {
                    TextFontStyle.CINEMATIC_SERIF, TextFontStyle.BANGLA_TIRO -> FontFamily.Serif
                    else -> FontFamily.SansSerif
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(textScale * textOverlay.scale)
                    .background(
                        color = if (textOverlay.backgroundColor != null) Color(textOverlay.backgroundColor) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/** Sticker overlays (the fallback renderer draws the symbol, not an image). */
@Composable
internal fun CanvasStickerOverlayLayer(activeStickers: List<StickerOverlay>) {
    activeStickers.forEach { sticker ->
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
