package com.example.ui.canvas

import android.view.SurfaceView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.R
import com.example.data.KeyframeInterpolator
import com.example.data.ScrubPreviewFrame
import com.example.engine.backgroundArgbFor
import com.example.model.AspectRatioType
import com.example.model.BlendMode
import com.example.model.EffectType
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.ui.components.CapCutMeadowCanvas
import com.example.ui.components.LocalVideoPlayer
import com.example.model.TextAnimationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.model.TextFontStyle
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.ui.theme.DraftingCropCorners
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.WashiTapeCorner
import com.example.ui.theme.sketchPencilBorder


@Composable
internal fun RenderClipVisual(
    clip: MediaClip,
    activeEffects: List<com.example.model.TimelineEffect>,
    pulseAlpha: Float,
    currentClipLocalMs: Long = 0L
) {
    val keyScale = KeyframeInterpolator.getScale(clip, currentClipLocalMs)
    val keyRotation = KeyframeInterpolator.getRotation(clip, currentClipLocalMs)
    val keyOpacity = KeyframeInterpolator.getOpacity(clip, currentClipLocalMs)
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
            .scale(keyScale)
            .rotate(keyRotation)
            .graphicsLayer { alpha = keyOpacity }
            .scale(if (clip.isMirrored) -1f else 1f, 1f)
    ) {
        if (clip.uriString != null) {
            // Real video frame via ExoPlayer when available, else thumbnail via Coil
            var showVideo by remember(clip.uriString) { mutableStateOf(true) }
            val player = LocalVideoPlayer.current
            if (showVideo && player != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(clip.uriString)
                        .crossfade(true)
                        .build(),
                    contentDescription = clip.title,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                    onError = { showVideo = false }
                )
            }
        } else if (clip.drawableResId != null) {
            Image(
                painter = painterResource(id = clip.drawableResId),
                contentDescription = clip.title,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
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

        // Render Mask preview overlay
        when (clip.mask.type) {
            MaskType.RECTANGLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .border(2.dp, Color.Cyan, RoundedCornerShape(8.dp))
                )
            }
            MaskType.CIRCLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Cyan, CircleShape)
                )
            }
            MaskType.SPLIT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )
            }
            MaskType.MIRROR, MaskType.FILMSTRIP -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Magenta.copy(alpha = 0.7f))
                )
            }
            MaskType.NONE -> {}
        }

        if (clip.blendMode != BlendMode.NORMAL) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue.copy(alpha = 0.1f))
            )
        }
    }
}
