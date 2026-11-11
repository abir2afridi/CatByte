package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CapCutMeadowCanvas

/**
 * CapCut-style Fullscreen Exporting Page (Screenshot 2).
 * Displays the live render progress percentage over the video preview card.
 * The data comes from `ExportProgress` (ExportManager) via ExportUiState.
 */
@Composable
fun ExportingScreen(
    progress: Float,
    stage: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .testTag("exporting_fullscreen_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with left back arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title & Subtitle
            Text(
                text = "Exporting...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please don't close CatByte or lock your screen.",
                color = Color(0xFFA5A8B2),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(50.dp))

            // Center Video Preview Card with Live Percentage
            Box(
                modifier = Modifier
                    .width(310.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181A20))
                    .border(1.dp, Color(0xFF2C2E38), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

                // Dark subtle overlay for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                // Top thin white progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }

                // Center percentage text
                val percentageText = "${String.format(java.util.Locale.US, "%.1f", progress * 100f)}%"
                Text(
                    text = percentageText,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
