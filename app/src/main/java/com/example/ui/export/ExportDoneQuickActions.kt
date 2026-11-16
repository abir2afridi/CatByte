package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Quick actions: new project, export options, quality, watermark, edit photo. */
@Composable
internal fun ExportDoneQuickActions(
    onDone: () -> Unit
) {
    // Quick actions Section
    Text(
        text = "Quick actions",
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("New project", Icons.Default.Add, "new_project"),
            Triple("Export options", Icons.Default.FileUpload, "export_options"),
            Triple("Video quality", Icons.Default.Hd, "video_quality"),
            Triple("Remove watermark", Icons.Default.VisibilityOff, "watermark"),
            Triple("Edit photo", Icons.Default.PhotoLibrary, "edit_photo")
        ).forEach { (label, icon, tag) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF334356))
                    .clickable { onDone() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    color = Color(0xFFA0ADB8),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
