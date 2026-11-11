package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CapCut-style Export Options Bottom Sheet Drawer (Screenshot 1).
 * Displays Save to TikTok, Save to Instagram, Save to Device cards, and
 * watermark choices.
 */
@Composable
fun ExportOptionsBottomSheet(
    watermarkEnabled: Boolean,
    onToggleWatermark: (Boolean) -> Unit,
    onSaveToDevice: () -> Unit,
    onShareTikTok: () -> Unit,
    onShareInstagram: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var boostOnTikTok by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .padding(16.dp)
            .testTag("export_options_bottom_sheet")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(28.dp))
                Text(
                    text = "Export",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA5A8B2),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. TikTok Card (White background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { onShareTikTok() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "TikTok",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Save and share to TikTok",
                    color = Color(0xFF141416),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Instagram Card (Dark background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2026))
                    .clickable { onShareInstagram() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Instagram",
                    tint = Color(0xFFE1306C),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Save and share to Instagram",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Save to device Card (Dark background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2026))
                    .clickable { onSaveToDevice() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Save to device",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Save to device",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Radio options at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleWatermark(!watermarkEnabled) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(
                    selected = !watermarkEnabled,
                    onClick = { onToggleWatermark(!watermarkEnabled) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF26E5D7),
                        unselectedColor = Color(0xFFA5A8B2)
                    )
                )
                Text(
                    text = "Remove watermark",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { boostOnTikTok = !boostOnTikTok }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(
                    selected = boostOnTikTok,
                    onClick = { boostOnTikTok = !boostOnTikTok },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF26E5D7),
                        unselectedColor = Color(0xFFA5A8B2)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Boost your video on TikTok",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("ⓘ", color = Color(0xFFA5A8B2), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
