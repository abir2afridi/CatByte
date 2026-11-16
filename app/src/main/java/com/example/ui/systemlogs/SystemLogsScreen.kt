package com.example.ui.systemlogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppLog
import com.example.data.AppLogLevel
import com.example.ui.theme.PencilTheme

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
                LogSearchField(
                    query = query,
                    onQueryChange = { query = it }
                )
            }

            item {
                FilterRow(
                    selected = filter,
                    onSelect = { filter = it }
                )
            }

            item {
                LogActionButtons(
                    enabled = filteredEntries.isNotEmpty(),
                    onCopyAll = {
                        copyText(
                            text = buildLogReport(filteredEntries, filter),
                            toast = if (filteredEntries.isEmpty()) {
                                "No logs to copy"
                            } else {
                                "Copied ${filteredEntries.size} log entries"
                            }
                        )
                    },
                    onDownload = { saveLauncher.launch(logFileName()) }
                )
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
        LogClearDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                AppLog.clear()
                entries = emptyList()
                expandedKeys = emptySet()
                showClearDialog = false
                Toast.makeText(context, "System logs cleared", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
