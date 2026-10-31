package com.example.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

/**
 * Debug screen that shows real in-app logs and crash reports collected by
 * [AppLog]. Entries can be filtered, searched, copied, or exported as a .txt
 * file through the system file picker.
 */
@Composable
fun SystemLogsScreen(
    onBack: () -> Unit
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current

    var entries by remember { mutableStateOf(AppLog.snapshot()) }
    var filter by remember { mutableStateOf(LogFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var expandedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, filter, query) {
        entries.filter { entry ->
            val matchesLevel = filter.level == null || entry.level == filter.level
            val matchesQuery = query.isBlank() ||
                entry.tag.contains(query, ignoreCase = true) ||
                entry.message.contains(query, ignoreCase = true)
            matchesLevel && matchesQuery
        }
    }

    val latestFiltered by rememberUpdatedState(filteredEntries)
    val latestFilter by rememberUpdatedState(filter)

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val report = buildLogReport(latestFiltered, latestFilter)
        val saved = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(report.toByteArray(Charsets.UTF_8))
            } ?: error("The selected file could not be opened")
        }
        saved.onSuccess {
            Toast.makeText(context, "Log file saved", Toast.LENGTH_SHORT).show()
            AppLog.info("SystemLogs", "Exported ${latestFiltered.size} log entries")
        }.onFailure { throwable ->
            AppLog.error("SystemLogs", "Failed to export log file", throwable)
            Toast.makeText(context, "Could not save log file", Toast.LENGTH_LONG).show()
        }
    }

    fun copyText(text: String, toast: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CatByte system logs", text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    fun toggleExpanded(key: String) {
        expandedKeys = if (key in expandedKeys) expandedKeys - key else expandedKeys + key
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("system_logs_screen")
    ) {
        SystemLogsTopBar(
            entryCount = entries.size,
            errorCount = entries.count { it.level == AppLogLevel.ERROR },
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OverviewCard(
                    entries = entries,
                    onRefresh = {
                        entries = AppLog.snapshot()
                        expandedKeys = emptySet()
                    },
                    onClear = { showClearDialog = true }
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logs_search"),
                    placeholder = {
                        Text(
                            text = "Search tag or message...",
                            color = colors.textTertiary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        cursorColor = colors.accentCyan,
                        focusedBorderColor = colors.accentCyan,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedPlaceholderColor = colors.textTertiary,
                        unfocusedPlaceholderColor = colors.textTertiary
                    )
                )
            }

            item {
                FilterRow(
                    selected = filter,
                    onSelect = { filter = it }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            copyText(
                                text = buildLogReport(filteredEntries, filter),
                                toast = if (filteredEntries.isEmpty()) {
                                    "No logs to copy"
                                } else {
                                    "Copied ${filteredEntries.size} log entries"
                                }
                            )
                        },
                        enabled = filteredEntries.isNotEmpty(),
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
                        onClick = { saveLauncher.launch(logFileName()) },
                        enabled = filteredEntries.isNotEmpty(),
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

            if (filteredEntries.isEmpty()) {
                item {
                    EmptyLogsState(
                        hasAnyLogs = entries.isNotEmpty(),
                        onRefresh = { entries = AppLog.snapshot() }
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredEntries,
                    key = { index, entry -> "${entry.timestamp}_${entry.tag}_$index" }
                ) { _, entry ->
                    val entryKey = "${entry.timestamp}_${entry.tag}_${entry.message.hashCode()}"
                    LogEntryCard(
                        entry = entry,
                        expanded = entryKey in expandedKeys,
                        onToggleExpand = { toggleExpanded(entryKey) },
                        onCopy = {
                            copyText(
                                text = entry.fullText,
                                toast = "Log entry copied"
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Clear all logs?") },
            text = {
                Text(
                    "This removes every captured log and crash report, including " +
                        "entries saved from previous sessions. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppLog.clear()
                        entries = emptyList()
                        expandedKeys = emptySet()
                        showClearDialog = false
                        Toast.makeText(context, "System logs cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = colors.accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

// ==========================================
// Top bar
// ==========================================

@Composable
private fun SystemLogsTopBar(
    entryCount: Int,
    errorCount: Int,
    onBack: () -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .sketchPencilBorder(
                        strokeWidth = 1.4.dp,
                        color = colors.border,
                        cornerRadius = 20.dp
                    )
                    .clickable { onBack() }
                    .testTag("logs_back"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = colors.accentPink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Logs",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$entryCount entries • $errorCount errors",
                    color = if (errorCount > 0) colors.accentRed else colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }

        WashiTapeSticker(
            modifier = Modifier.align(Alignment.TopEnd),
            text = "DEBUG"
        )
    }
}

// ==========================================
// Overview / actions
// ==========================================

@Composable
private fun OverviewCard(
    entries: List<AppLogEntry>,
    onRefresh: () -> Unit,
    onClear: () -> Unit
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
private fun LogStatPill(
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

// ==========================================
// Filters
// ==========================================

@Composable
private fun FilterRow(
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

// ==========================================
// Log entry
// ==========================================

@Composable
private fun LogEntryCard(
    entry: AppLogEntry,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopy: () -> Unit
) {
    val colors = PencilTheme.colors
    val tint = entry.level.tint(colors)
    val hasStackTrace = !entry.stackTrace.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.07f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(enabled = hasStackTrace) { onToggleExpand() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tint)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = entry.level.label,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = entry.tag,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (entry.fromPreviousSession) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.textTertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SAVED",
                        color = colors.textSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = entry.timeLabel,
                color = colors.textTertiary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy log entry",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.message,
            color = colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis
        )

        if (hasStackTrace) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = entry.stackTrace.orEmpty().trimEnd(),
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (expanded) "Tap to collapse" else "Tap to view full stack trace",
                color = colors.textTertiary,
                fontSize = 9.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ==========================================
// Empty state
// ==========================================

@Composable
private fun EmptyLogsState(
    hasAnyLogs: Boolean,
    onRefresh: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(46.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasAnyLogs) "No matching logs" else "No logs captured yet",
            color = colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasAnyLogs) {
                "Try a different filter or search term."
            } else {
                "Crashes, errors and warnings will appear here automatically."
            },
            color = colors.textTertiary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRefresh) {
            Text("Refresh", color = colors.accentCyan, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// Helpers
// ==========================================

private enum class LogFilter(val label: String, val level: AppLogLevel?) {
    ALL("All", null),
    ERROR("Errors", AppLogLevel.ERROR),
    WARN("Warnings", AppLogLevel.WARN),
    INFO("Info", AppLogLevel.INFO),
    DEBUG("Debug", AppLogLevel.DEBUG)
}

private fun LogFilter.tint(colors: AppColors): Color = when (this) {
    LogFilter.ALL -> colors.border
    LogFilter.ERROR -> colors.accentRed
    LogFilter.WARN -> colors.accentYellow
    LogFilter.INFO -> colors.accentCyan
    LogFilter.DEBUG -> colors.accentPurple
}

private fun AppLogLevel.tint(colors: AppColors): Color = when (this) {
    AppLogLevel.ERROR -> colors.accentRed
    AppLogLevel.WARN -> colors.accentYellow
    AppLogLevel.INFO -> colors.accentCyan
    AppLogLevel.DEBUG -> colors.accentPurple
}

private fun logFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "catbyte-logs-$stamp.txt"
}

private fun buildLogReport(
    entries: List<AppLogEntry>,
    filter: LogFilter
): String {
    val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    val header = buildString {
        appendLine("CatByte System Logs")
        appendLine("===================")
        appendLine("Generated : $generatedAt")
        appendLine("App       : CatByte ${BuildConfig.VERSION_NAME} (${if (BuildConfig.DEBUG) "debug" else "release"})")
        appendLine("Package   : ${BuildConfig.APPLICATION_ID}")
        appendLine("Device    : ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android   : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Filter    : ${filter.label}")
        appendLine("Entries   : ${entries.size}")
        appendLine()
        appendLine("------------------- LOGS -------------------")
    }
    if (entries.isEmpty()) {
        return header + "\n(no log entries)"
    }
    return header + "\n" + entries.joinToString(separator = "\n\n") { it.fullText }
}
