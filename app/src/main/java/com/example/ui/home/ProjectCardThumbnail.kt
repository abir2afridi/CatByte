package com.example.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoProject
import com.example.data.ProjectThumbnailLoader
import com.example.ui.theme.PencilTheme

/** Project thumbnail with its aspect-ratio badge. */
@Composable
internal fun ProjectCardThumbnail(
    project: VideoProject
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current
    // Real cover: a frame of the project video (or the user's cover image).
    val cover by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        project.id,
        project.coverFrameMs,
        project.coverImageUri,
        project.clips.firstOrNull()?.uriString
    ) {
        value = ProjectThumbnailLoader.load(context, project)
    }

    // Thumbnail with subtle border
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
    ) {
        if (project.thumbnailResId != null) {
            Image(
                painter = painterResource(id = project.thumbnailResId),
                contentDescription = project.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (cover != null) {
            Image(
                bitmap = cover!!.asImageBitmap(),
                contentDescription = project.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Project",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Aspect ratio tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(topEnd = 6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = project.aspectRatio.label,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
