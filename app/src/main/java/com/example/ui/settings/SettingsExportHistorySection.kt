package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLog
import com.example.data.db.CatByteDatabase
import com.example.data.db.ExportJobEntity
import com.example.data.ExportServiceScheduler
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * The export queue and history: every queued, running, done and failed
 * background export, with retry (re-enqueue verbatim), open and delete.
 * Backed by the export_jobs Room table the foreground service drains.
 */
@Composable
internal fun SettingsExportHistorySection() {
    val colors = PencilTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val jobs by CatByteDatabase.getInstance(context).exportJobDao()
        .history().collectAsState(initial = emptyList())

    SettingsSection(
        title = "Export history & queue",
        icon = Icons.Default.Refresh,
        iconTint = colors.accentCyan
    ) {
        if (jobs.isEmpty()) {
            Text(
                text = "No background exports yet. \"Keep editing — export in background\" " +
                    "on the export sheet queues a job here and continues the work outside the app.",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            return@SettingsSection
        }

        jobs.take(12).forEach { job ->
            ExportJobRow(
                job = job,
                onRetry = {
                    scope.launch(Dispatchers.IO) {
                        CatByteDatabase.getInstance(context).exportJobDao().requeue(job.id)
                        ExportServiceScheduler.startDrain(context)
                    }
                },
                onOpen = { openExport(context, job) },
                onDelete = {
                    scope.launch(Dispatchers.IO) {
                        CatByteDatabase.getInstance(context).exportJobDao().delete(job.id)
                    }
                }
            )
        }

        TextButton(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    CatByteDatabase.getInstance(context).exportJobDao().clearFinished()
                }
            },
            modifier = Modifier.testTag("settings_exports_clear")
        ) {
            Text("Clear finished", color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExportJobRow(
    job: ExportJobEntity,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = PencilTheme.colors
    val stateColor = when (job.state) {
        ExportJobEntity.STATE_DONE -> colors.accentGreen
        ExportJobEntity.STATE_FAILED -> Color(0xFFFF6B6B)
        ExportJobEntity.STATE_CANCELLED -> Color(0xFFA5A8B2)
        ExportJobEntity.STATE_RUNNING -> colors.accentCyan
        else -> colors.accentYellow
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                job.title.ifBlank { "Export" },
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = when (job.state) {
                ExportJobEntity.STATE_RUNNING ->
                    "Exporting… ${(job.progress * 100).toInt()}%"
                ExportJobEntity.STATE_QUEUED -> "Queued"
                ExportJobEntity.STATE_DONE -> "Done${if (job.outputPath != null) "" else " (file moved)"}"
                else -> job.error?.takeIf { it.isNotBlank() } ?: job.state.lowercase()
                    .replaceFirstChar { it.uppercase() }
            }
            Text(detail, color = stateColor, fontSize = 10.sp)
        }

        if (job.state == ExportJobEntity.STATE_DONE && job.outputPath != null) {
            Text(
                "Open",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpen() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        if (job.state == ExportJobEntity.STATE_FAILED || job.state == ExportJobEntity.STATE_CANCELLED) {
            IconButton(onClick = onRetry, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Retry export",
                    tint = colors.accentCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete export record",
                tint = colors.textTertiary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/** Opens a finished export through the same VIEW intent the notification uses. */
private fun openExport(context: android.content.Context, job: ExportJobEntity) {
    val path = job.outputPath ?: return
    val file = File(path)
    if (!file.exists()) return
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure { AppLog.warn("SettingsExports", "Could not open export: ${it.message}") }
}
