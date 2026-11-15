package com.example.ui.components

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


/**
 * Realistic CapCut meadow and blue sky canvas for default sample clip rendering,
 * matching the user's reference image (blue sky, fluffy white clouds, glowing sun, green grass).
 */
@Composable
fun CapCutMeadowCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Blue sky gradient
        val skyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1976D2), // Deep sky blue top
                Color(0xFF42A5F5), // Bright blue sky mid
                Color(0xFF90CAF9), // Horizon sky light blue
                Color(0xFFE3F2FD)  // Atmospheric haze
            ),
            startY = 0f,
            endY = h * 0.62f
        )
        drawRect(brush = skyBrush, size = androidx.compose.ui.geometry.Size(w, h * 0.62f))

        // 2. Bright Sun with glowing halo
        val sunCenter = Offset(w * 0.28f, h * 0.18f)
        drawCircle(
            color = Color.White.copy(alpha = 0.40f),
            radius = w * 0.18f,
            center = sunCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.70f),
            radius = w * 0.11f,
            center = sunCenter
        )
        drawCircle(
            color = Color(0xFFFFFDE7),
            radius = w * 0.065f,
            center = sunCenter
        )

        // 3. Fluffy white clouds
        val cloudColor = Color.White.copy(alpha = 0.90f)

        // Left cloud group
        drawCircle(color = cloudColor, radius = w * 0.12f, center = Offset(w * 0.14f, h * 0.22f))
        drawCircle(color = cloudColor, radius = w * 0.14f, center = Offset(w * 0.26f, h * 0.20f))
        drawCircle(color = cloudColor, radius = w * 0.10f, center = Offset(w * 0.36f, h * 0.24f))

        // Right cloud group
        drawCircle(color = cloudColor, radius = w * 0.11f, center = Offset(w * 0.58f, h * 0.16f))
        drawCircle(color = cloudColor, radius = w * 0.15f, center = Offset(w * 0.74f, h * 0.13f))
        drawCircle(color = cloudColor, radius = w * 0.12f, center = Offset(w * 0.88f, h * 0.18f))

        // 4. Distant green hills
        val hillPath = Path().apply {
            moveTo(0f, h * 0.56f)
            cubicTo(w * 0.25f, h * 0.50f, w * 0.45f, h * 0.54f, w * 0.65f, h * 0.48f)
            cubicTo(w * 0.80f, h * 0.44f, w * 0.92f, h * 0.50f, w, h * 0.52f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = hillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF558B2F), Color(0xFF33691E)),
                startY = h * 0.46f,
                endY = h
            )
        )

        // 5. Foreground green grass meadow (lush blades & field)
        val meadowPath = Path().apply {
            moveTo(0f, h * 0.50f)
            cubicTo(w * 0.20f, h * 0.44f, w * 0.50f, h * 0.52f, w * 0.75f, h * 0.45f)
            cubicTo(w * 0.88f, h * 0.41f, w * 0.95f, h * 0.46f, w, h * 0.48f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = meadowPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF689F38), Color(0xFF2E7D32), Color(0xFF1B5E20)),
                startY = h * 0.41f,
                endY = h
            )
        )
    }
}

