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
    var autoRefresh by remember { mutableStateOf(false) }

    // Auto-refresh logs every 2 seconds when enabled
    LaunchedEffect(autoRefresh) {
        if (autoRefresh) {
            while (autoRefresh) {
                kotlinx.coroutines.delay(2000)
                if (autoRefresh) {
                    entries = AppLog.snapshot()
                }
            }
        }
    }

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
                    onClear = { showClearDialog = true },
                    autoRefresh = autoRefresh,
                    onAutoRefreshChange = { autoRefresh = it }
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
