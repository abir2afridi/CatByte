package com.example.ui.systemlogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppLog
import com.example.data.AppLogEntry
import com.example.data.AppLogLevel
import com.example.ui.theme.AppColors
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.WashiTapeSticker
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun OverviewCard(
    entries: List<AppLogEntry>,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit
) {
    val colors = PencilTheme.colors
    val errors = entries.count { it.level == AppLogLevel.ERROR }
    val warnings = entries.count { it.level == AppLogLevel.WARN }
    val crashes = entries.count { it.tag == "Crash" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .pencilHatching(color = colors.border.copy(alpha = 0.04f), spacing = 9.dp)
            .sketchPencilBorder(
                strokeWidth = 1.6.dp,
                color = colors.border,
                cornerRadius = 16.dp
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CRASH & ERROR REPORTS",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("logs_refresh")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh logs",
                    tint = colors.accentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onClear,
                enabled = entries.isNotEmpty(),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("logs_clear")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear logs",
                    tint = if (entries.isNotEmpty()) colors.accentRed else colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogStatPill(
                label = "Total",
                value = entries.size.toString(),
                tint = colors.accentCyan,
                modifier = Modifier.weight(1f)
            )
            LogStatPill(
                label = "Errors",
                value = errors.toString(),
                tint = colors.accentRed,
                modifier = Modifier.weight(1f)
            )
            LogStatPill(
                label = "Crashes",
                value = crashes.toString(),
                tint = colors.accentPink,
                modifier = Modifier.weight(1f)
            )
            LogStatPill(
                label = "Warnings",
                value = warnings.toString(),
                tint = colors.accentYellow,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Logs are stored on this device only. Copy or download them and share the file with the developer to debug issues faster.",
            color = colors.textSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
internal fun LogStatPill(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = tint,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
