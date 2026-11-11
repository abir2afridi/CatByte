package com.example.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The "Optical flow" row: frame interpolation for smoother playback.
 *
 * Like the AI ultra HD card this needs an interpolation pipeline that the
 * offline build does not ship, so the switch is disabled (see
 * FEATURE_MATRIX.md "AI UHD + Optical flow").
 */
@Composable
internal fun ExportOpticalFlowRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Optical flow",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Make video playback smoother.",
                    color = Color(0xFF26E5D7),
                    fontSize = 10.sp
                )
                Text("| Example", color = Color(0xFFA5A8B2), fontSize = 10.sp)
            }
        }
        Switch(checked = false, onCheckedChange = null, enabled = false)
    }
}
