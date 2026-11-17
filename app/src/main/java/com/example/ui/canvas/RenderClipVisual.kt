package com.example.ui.canvas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.KeyframeInterpolator
import com.example.model.BlendMode
import com.example.model.MediaClip
import com.example.model.TimelineEffect
import com.example.ui.components.CapCutMeadowCanvas
import com.example.ui.components.LocalVideoPlayer

/**
 * Renders one clip's media plus its preview overlays: keyframed
 * scale/rotation/opacity, the selected filter, active effects, the mask guide
 * and the blend-mode tint.
 */
@Composable
internal fun RenderClipVisual(
    clip: MediaClip,
    activeEffects: List<TimelineEffect>,
    pulseAlpha: Float,
    currentClipLocalMs: Long = 0L
) {
    val keyScale = KeyframeInterpolator.getScale(clip, currentClipLocalMs)
    val keyRotation = KeyframeInterpolator.getRotation(clip, currentClipLocalMs)
    val keyOpacity = KeyframeInterpolator.getOpacity(clip, currentClipLocalMs)
    val colorFilter = clipFilterColorMatrix(clip.filter)

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
        ClipEffectOverlays(
            activeEffects = activeEffects,
            pulseAlpha = pulseAlpha,
            localMs = currentClipLocalMs
        )

        // Render Mask preview overlay
        ClipMaskOverlay(maskType = clip.mask.type)

        if (clip.blendMode != BlendMode.NORMAL) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue.copy(alpha = 0.1f))
            )
        }
    }
}
