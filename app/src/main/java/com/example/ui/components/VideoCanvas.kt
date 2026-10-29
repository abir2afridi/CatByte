package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.model.AspectRatioType
import com.example.model.EffectType
import com.example.model.MediaClip
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.ui.theme.DraftingCropCorners
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.WashiTapeCorner
import com.example.ui.theme.sketchPencilBorder

@Composable
fun VideoCanvas(
    project: VideoProject,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    watermarkEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val ratio = project.aspectRatio
    val targetAspect = ratio.ratioWidth / ratio.ratioHeight

    // Active clip resolution
    var accumulatedMs = 0L
    var activeClip: MediaClip? = null
    var clipLocalTimeMs = 0L

    for (clip in project.clips) {
        val clipEnd = accumulatedMs + clip.durationMs
        if (currentPositionMs in accumulatedMs..clipEnd) {
            activeClip = clip
            clipLocalTimeMs = currentPositionMs - accumulatedMs
            break
        }
        accumulatedMs = clipEnd
    }
    if (activeClip == null && project.clips.isNotEmpty()) {
        activeClip = project.clips.lastOrNull()
        clipLocalTimeMs = activeClip?.durationMs ?: 0L
    }

    // Active overlays
    val activeTexts = project.textOverlays.filter {
        currentPositionMs >= it.startTimelineMs && currentPositionMs <= (it.startTimelineMs + it.durationMs)
    }
    val activeStickers = project.stickers.filter {
        currentPositionMs >= it.startTimelineMs && currentPositionMs <= (it.startTimelineMs + it.durationMs)
    }
    val activeEffects = project.effects.filter {
        currentPositionMs >= it.startTimelineMs && currentPositionMs <= (it.startTimelineMs + it.durationMs)
    }

    // Pulse animation - only runs when playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = if (isPlaying) infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ) else infiniteRepeatable(
            animation = tween(0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Outer Canvas Frame with sketched border and drafting marks
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .aspectRatio(targetAspect)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 14.dp)
                .clickable { onTogglePlay() }
                .testTag("video_canvas_preview")
                .width(720.dp),
            contentAlignment = Alignment.Center
        ) {
            // Drafting Crop Marks in 4 corners
            DraftingCropCorners(
                modifier = Modifier
                    .matchParentSize()
                    .padding(6.dp),
                strokeColor = colors.accentCyan.copy(alpha = 0.7f),
                lineLength = 14.dp,
                strokeWidth = 1.6.dp
            )

            // Drafting Washi Tape mounting strips on top corners
            WashiTapeCorner(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-2).dp),
                rotationDegrees = -15f,
                widthDp = 44.dp,
                heightDp = 14.dp,
                tapeLabel = "PIN"
            )
            WashiTapeCorner(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp),
                rotationDegrees = 15f,
                widthDp = 44.dp,
                heightDp = 14.dp,
                tapeLabel = "DRAFT"
            )
            if (activeClip != null) {
                // Visual Rendering
                RenderClipVisual(
                    clip = activeClip,
                    activeEffects = activeEffects,
                    pulseAlpha = pulseAlpha
                )

                // Transition effect
                if (activeClip.transition != TransitionType.NONE && clipLocalTimeMs < activeClip.transitionDurationMs) {
                    RenderTransitionEffect(
                        transition = activeClip.transition,
                        progress = clipLocalTimeMs.toFloat() / activeClip.transitionDurationMs
                    )
                }
            } else {
                // Empty state inside canvas
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "No media clips",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap '+' on timeline to add footage",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }
            }

            // Text Overlays
            activeTexts.forEach { textOverlay ->
                val displayText = if (textOverlay.animation == TextAnimationType.TYPEWRITER) {
                    val progress = ((currentPositionMs - textOverlay.startTimelineMs).toFloat() / textOverlay.durationMs).coerceIn(0f, 1f)
                    val charCount = (textOverlay.text.length * progress * 2).toInt().coerceIn(1, textOverlay.text.length)
                    textOverlay.text.take(charCount)
                } else {
                    textOverlay.text
                }

                val textScale = if (textOverlay.animation == TextAnimationType.POP_UP) {
                    val p = ((currentPositionMs - textOverlay.startTimelineMs).toFloat() / 300f).coerceIn(0f, 1f)
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
                            TextFontStyle.CINEMATIC_SERIF -> 20.sp
                            TextFontStyle.SUBTITLE -> 16.sp
                            TextFontStyle.CLEAN_SANS -> 17.sp
                        },
                        fontWeight = when (textOverlay.fontStyle) {
                            TextFontStyle.CAPCUT_BOLD -> FontWeight.Black
                            TextFontStyle.CINEMATIC_SERIF -> FontWeight.SemiBold
                            else -> FontWeight.Bold
                        },
                        fontFamily = when (textOverlay.fontStyle) {
                            TextFontStyle.CINEMATIC_SERIF -> FontFamily.Serif
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

            // Sticker Overlays
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

            // Watermark overlay
            if (watermarkEnabled) {
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

            // Centered Play/Pause HUD when paused - Sketched Paper & Graphite Button
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.90f))
                        .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 29.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .size(34.dp)
                            .offset(x = 2.dp)
                    )
                }
            }

            // Top Status Overlay (Quality & Active Filter)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aspect Ratio indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.aspectRatio.label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Filter Pill
                if (activeClip != null && activeClip.filter != VideoFilter.NONE) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accentCyan)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeClip.filter.displayName,
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Bottom Timecode Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                val currentSec = currentPositionMs / 1000f
                val totalSec = project.totalDurationMs / 1000f
                Text(
                    text = String.format("%02d:%04.1f / %02d:%04.1f", (currentSec / 60).toInt(), currentSec % 60, (totalSec / 60).toInt(), totalSec % 60),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RenderClipVisual(
    clip: MediaClip,
    activeEffects: List<com.example.model.TimelineEffect>,
    pulseAlpha: Float
) {
    val colorFilter = when (clip.filter) {
        VideoFilter.SKETCH_GRAPHITE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                0.33f, 0.33f, 0.33f, 0f, 15f,
                0.33f, 0.33f, 0.33f, 0f, 12f,
                0.33f, 0.33f, 0.33f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.BLUEPRINT -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                0.1f, 0.2f, 0.4f, 0f, -30f,
                0.2f, 0.5f, 0.8f, 0f, 10f,
                0.4f, 0.7f, 1.4f, 0f, 50f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.SEPIA_NOTEBOOK -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 25f,
                0.349f, 0.686f, 0.168f, 0f, 15f,
                0.272f, 0.534f, 0.131f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.CHARCOAL_SMUDGE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.3f, 1.3f, 1.3f, 0f, -50f,
                1.3f, 1.3f, 1.3f, 0f, -50f,
                1.3f, 1.3f, 1.3f, 0f, -50f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.HALFTONE_MANGA -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.4f, 0f, 0f, 0f, -20f,
                0f, 1.4f, 0f, 0f, -20f,
                0f, 0f, 1.4f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.CYBERPUNK -> ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(1.4f)
        })
        VideoFilter.VINTAGE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.2f, 0.1f, 0.1f, 0f, 15f,
                0.1f, 1.0f, 0.1f, 0f, 10f,
                0.1f, 0.1f, 0.8f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.GOLDEN_HOUR -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.3f, 0.2f, 0f, 0f, 20f,
                0.1f, 1.1f, 0f, 0f, 10f,
                0f, 0f, 0.7f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.NOIR -> ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(0f)
        })
        VideoFilter.TEAL_ORANGE -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.25f, 0f, 0f, 0f, 10f,
                0f, 1.15f, 0.1f, 0f, 0f,
                0f, 0.1f, 1.35f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.MOODY -> ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(0.6f)
        })
        VideoFilter.GLITCH_ART -> ColorFilter.colorMatrix(ColorMatrix(
            floatArrayOf(
                1.5f, 0f, 0f, 0f, 30f,
                0f, 0.8f, 0f, 0f, -10f,
                0f, 0f, 1.5f, 0f, 25f,
                0f, 0f, 0f, 1f, 0f
            )
        ))
        VideoFilter.NONE -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotate(clip.rotation)
            .scale(if (clip.isMirrored) -1f else 1f, 1f)
    ) {
        if (clip.uriString != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(clip.uriString)
                    .crossfade(true)
                    .build(),
                contentDescription = clip.title,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else if (clip.drawableResId != null) {
            Image(
                painter = painterResource(id = clip.drawableResId),
                contentDescription = clip.title,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val startColor = Color(clip.gradientColors.firstOrNull() ?: 0xFF1E2640)
            val endColor = Color(clip.gradientColors.lastOrNull() ?: 0xFF0D1B2A)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(startColor, endColor)
                        )
                    )
            )
        }

        // Active visual effects
        activeEffects.forEach { effect ->
            when (effect.effectType) {
                EffectType.RGB_SPLIT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color.Cyan.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                }
                EffectType.SHAKE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((pulseAlpha * 6).toInt(), 0) }
                    )
                }
                EffectType.VHS_GRAIN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.07f))
                    )
                }
                EffectType.NEON_HALO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(5.dp, Color(0xFF38BDF8).copy(alpha = pulseAlpha * 0.7f))
                    )
                }
                EffectType.LIGHTNING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = if (pulseAlpha > 0.95f) 0.35f else 0f))
                    )
                }
                EffectType.ZOOM_BLUR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.04f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderTransitionEffect(
    transition: TransitionType,
    progress: Float
) {
    val alpha = (1f - (progress * 2 - 1f) * (progress * 2 - 1f)).coerceIn(0f, 1f)
    when (transition) {
        TransitionType.FADE_BLACK -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
            )
        }
        TransitionType.FLASH_WHITE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = alpha))
            )
        }
        TransitionType.WHIP_PAN -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = alpha * 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.GLITCH -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Cyan.copy(alpha = alpha * 0.5f),
                                Color.Transparent,
                                Color.Magenta.copy(alpha = alpha * 0.5f)
                            )
                        )
                    )
            )
        }
        TransitionType.PAGE_FLIP -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF2B231D).copy(alpha = alpha * 0.7f),
                                Color(0xFFFAF6EE).copy(alpha = alpha * 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.PAPER_TEAR -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFAF6EE).copy(alpha = alpha * 0.9f),
                                Color(0xFF332922).copy(alpha = alpha * 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.PENCIL_WIPE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2C241E).copy(alpha = alpha * 0.85f),
                                Color(0xFF4A3E36).copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.INK_BLEED -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF1E2838).copy(alpha = alpha * 0.85f),
                                Color(0xFF1E2838).copy(alpha = alpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TransitionType.DISSOLVE, TransitionType.ZOOM_IN, TransitionType.NONE -> {}
    }
}
