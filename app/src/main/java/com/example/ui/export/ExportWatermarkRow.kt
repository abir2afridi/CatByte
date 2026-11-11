package com.example.ui.export

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The "Watermark   <name> >" row shared by the video and GIF tabs. Opens the
 * watermark sub-panel ([ExportWatermarkPanel]).
 */
@Composable
internal fun ExportWatermarkRow(
    watermarkEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Watermark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (watermarkEnabled) "CapCut" else "None",
                color = Color(0xFFA5A8B2),
                fontSize = 12.sp
            )
            Text(">", color = Color(0xFFA5A8B2), fontSize = 12.sp)
        }
    }
}
