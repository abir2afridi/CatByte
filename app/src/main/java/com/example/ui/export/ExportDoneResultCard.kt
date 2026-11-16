package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CapCutIconPlayTriangle
import com.example.ui.components.CapCutMeadowCanvas

/**
 * Result card: video preview, the TikTok boost option, Share to TikTok,
 * Create template and the social share row.
 */
@Composable
internal fun ExportDoneResultCard(
    boostOnTikTok: Boolean,
    onToggleBoost: () -> Unit,
    onSharePlatform: (String) -> Unit,
    onDone: () -> Unit
) {
    // Main Slate Card Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF334356))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Video Preview Card
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2632)),
                contentAlignment = Alignment.Center
            ) {
                CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

                // Play triangle button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CapCutIconPlayTriangle(tint = Color.White, size = 18.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Boost on TikTok radio option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleBoost() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = boostOnTikTok,
                    onClick = { onToggleBoost() },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF26E5D7),
                        unselectedColor = Color(0xFFA0ADB8)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Boost your video on TikTok Γôÿ",
                    color = Color(0xFFA0ADB8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Share to TikTok white button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { onSharePlatform("TikTok") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "TikTok",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share to TikTok",
                    color = Color(0xFF141416),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Create template slate button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF42556C))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create template",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            ExportDoneSocialShareRow(onSharePlatform = onSharePlatform)
        }
    }
}
