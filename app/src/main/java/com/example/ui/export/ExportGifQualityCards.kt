package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * GIF export quality cards (240P / 320P / 640P) for the GIF tab.
 *
 * The selected quality maps to a video-pipeline resolution in
 * EditorViewModel.startSelectedExport() until a dedicated GIF encoder lands.
 */
internal object ExportGifQualityOptions {
    val values = listOf("240P", "320P", "640P")
    val labels = mapOf(
        "240P" to "Standard",
        "320P" to "High",
        "640P" to "Ultra high"
    )
}

@Composable
internal fun ExportGifQualityCards(
    selectedQuality: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ExportGifQualityOptions.values.forEach { quality ->
            val selected = selectedQuality == quality
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2026))
                    .border(
                        width = if (selected) 1.5.dp else 0.8.dp,
                        color = if (selected) Color.White else Color(0xFF2D303B),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(quality) }
                    .padding(8.dp)
            ) {
                Text(quality, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF2D303B))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    ExportGifQualityOptions.labels[quality].orEmpty(),
                    color = Color(0xFFA5A8B2),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Footer note under the GIF cards. */
@Composable
internal fun ExportGifSizeNote(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Size of exported GIF is about 90MB",
            color = Color(0xFFA5A8B2),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
