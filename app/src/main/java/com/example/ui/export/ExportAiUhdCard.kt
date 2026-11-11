package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
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
 * The "AI ultra HD" card at the top of the video export settings.
 *
 * Layout parity with CapCut: the card renders, but the enhancement itself
 * needs an online AI model that this offline build does not ship, so the
 * switch is disabled (see FEATURE_MATRIX.md "AI UHD + Optical flow").
 */
@Composable
internal fun ExportAiUhdCard(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2026))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("AI ultra HD", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("| Example", color = Color(0xFFA5A8B2), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Make your video clearer and smoother with AI enhancement and frame interpolation.",
                color = Color(0xFFA5A8B2),
                fontSize = 10.sp
            )
        }
        Switch(checked = false, onCheckedChange = null, enabled = false)
    }
}
