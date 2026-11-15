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
internal fun FilterRow(
    selected: LogFilter,
    onSelect: (LogFilter) -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LogFilter.values().forEach { option ->
            val isSelected = option == selected
            val tint = option.tint(colors)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) tint else colors.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) tint else colors.borderSubtle,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = option.label,
                    color = if (isSelected) Color.White else colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

internal enum class LogFilter(val label: String, val level: AppLogLevel?) {
    ALL("All", null),
    ERROR("Errors", AppLogLevel.ERROR),
    WARN("Warnings", AppLogLevel.WARN),
    INFO("Info", AppLogLevel.INFO),
    DEBUG("Debug", AppLogLevel.DEBUG)
}

internal fun LogFilter.tint(colors: AppColors): Color = when (this) {
    LogFilter.ALL -> colors.border
    LogFilter.ERROR -> colors.accentRed
    LogFilter.WARN -> colors.accentYellow
    LogFilter.INFO -> colors.accentCyan
    LogFilter.DEBUG -> colors.accentPurple
}

internal fun AppLogLevel.tint(colors: AppColors): Color = when (this) {
    AppLogLevel.ERROR -> colors.accentRed
    AppLogLevel.WARN -> colors.accentYellow
    AppLogLevel.INFO -> colors.accentCyan
    AppLogLevel.DEBUG -> colors.accentPurple
}

