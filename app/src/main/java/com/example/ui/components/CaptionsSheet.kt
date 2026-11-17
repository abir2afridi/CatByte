package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CaptionsSheet(
    onAutoCaptions: () -> Unit,
    onImportCaptions: () -> Unit,
    onExportSrt: () -> Unit,
    onExportVtt: () -> Unit,
    onAddText: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color(0xFF262832), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("captions_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5A5B64))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.ClosedCaption,
                    contentDescription = "Captions",
                    tint = Color(0xFF26E5D7),
                    modifier = Modifier.size(20.dp)
                )
                Text("Captions & Subtitles", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22242B))
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color(0xFF26E5D7), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CaptionTile(Icons.Default.ClosedCaption, "Auto captions", "Auto") { onAutoCaptions() }
            CaptionTile(Icons.Default.Upload, "Import SRT/VTT", "Import") { onImportCaptions() }
            CaptionTile(Icons.Default.Download, "Export SRT", "Srt") { onExportSrt() }
            CaptionTile(Icons.Default.Download, "Export VTT", "Vtt") { onExportVtt() }
            CaptionTile(Icons.Default.PostAdd, "Add text", "Text") { onAddText() }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Auto captions transcribe the selected clip with the AI endpoint from Settings → AI. Import accepts .srt and .vtt files.",
            color = Color(0xFFA5A8B2),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun CaptionTile(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF22242B))
            .border(1.dp, Color(0xFF33353E), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .testTag("caption_action_$tag")
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF26E5D7), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
