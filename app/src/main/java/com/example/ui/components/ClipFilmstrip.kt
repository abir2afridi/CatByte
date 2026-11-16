package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import com.example.engine.ClipFrames
import com.example.model.MediaClip
/**
 * Tiled filmstrip frames for one clip.
 *
 * Own composable on purpose: a trim drag changes only the clip's width, and
 * with strong skipping this whole row of Coil images is skipped unless the
 * tile count actually changes — one of the reasons the trim handle stays
 * smooth instead of stuttering.
 */
@Composable
internal fun ClipFilmstrip(
    clip: MediaClip,
    frameCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(modifier = modifier) {
        val frameWidthDp = 50.dp
        // Every tile shows a DIFFERENT frame, sampled evenly across the clip's
        // selected source window (in-point .. out-point). The sampling rule
        // (centre of each tile's span, snapped to a 500ms cache grid) lives in
        // engine/ClipFrames.kt so zooming reuses cached frames.
        for (f in 0 until frameCount) {
            if (clip.uriString != null) {
                val model = ImageRequest.Builder(context)
                    .data(clip.uriString)
                    .videoFrameMicros(ClipFrames.sampleSourceMs(clip, f, frameCount) * 1000L)
                    .size(160)
                    .build()
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(frameWidthDp)
                        .fillMaxHeight()
                        .border(0.4.dp, Color.Black.copy(alpha = 0.25f))
                )
            } else if (clip.drawableResId != null) {
                Image(
                    painter = painterResource(clip.drawableResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(frameWidthDp)
                        .fillMaxHeight()
                        .border(0.4.dp, Color.Black.copy(alpha = 0.25f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(frameWidthDp)
                        .fillMaxHeight()
                        .border(0.4.dp, Color.Black.copy(alpha = 0.25f))
                ) {
                    CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
