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
internal fun RenderTransitionEffect(
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
