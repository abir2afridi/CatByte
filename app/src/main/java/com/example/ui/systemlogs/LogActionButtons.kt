package com.example.ui.systemlogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** Copy-all and download actions for the filtered log entries. */
@Composable
internal fun LogActionButtons(
    enabled: Boolean,
    onCopyAll: () -> Unit,
    onDownload: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onCopyAll,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentGreen,
                disabledContainerColor = colors.surfaceElevated,
                disabledContentColor = colors.textTertiary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("logs_copy")
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy all", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onDownload,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentCyan,
                disabledContainerColor = colors.surfaceElevated,
                disabledContentColor = colors.textTertiary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("logs_download")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
