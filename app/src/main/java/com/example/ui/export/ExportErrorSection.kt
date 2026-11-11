package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
 * The "Export failed" block: what broke and why, with a Close button.
 * Shown instead of silently closing the sheet when the render dies.
 */
@Composable
internal fun ExportErrorSection(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4D4D).copy(alpha = 0.15f))
                .border(1.5.dp, Color(0xFFFF4D4D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = "Export failed",
                tint = Color(0xFFFF4D4D),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Export failed",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            error,
            color = Color(0xFFA5A8B2),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
