package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.example.data.db.CatByteDatabase
import com.example.data.db.ProjectVersionEntity
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat

import java.util.Date
import java.util.Locale

/**
 * The project time machine for the currently open project: save a named
 * snapshot, browse the automatic and manual ones, restore any of them.
 */
@Composable
internal fun SettingsProjectVersionsSection(
    currentProjectId: String?,
    currentProjectTitle: String,
    onSaveVersion: () -> Unit,
    onRestoreVersion: (String) -> Unit
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current

    val versions by produceState(initialValue = emptyList<ProjectVersionEntity>(), currentProjectId) {
        value = if (currentProjectId.isNullOrBlank()) {
            emptyList()
        } else {
            CatByteDatabase.getInstance(context).projectVersionDao()
                .versionsFor(currentProjectId!!)
                .first()
        }
    }

    SettingsSection(
        title = "Project versions (time machine)",
        icon = Icons.Default.History,
        iconTint = colors.accentYellow
    ) {
        if (currentProjectId.isNullOrBlank()) {
            Text(
                text = "Open a project, then its snapshots appear here. Automatic " +
                    "snapshots land every 50 edits; save one manually before a big change.",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            return@SettingsSection
        }

        Button(
            onClick = onSaveVersion,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accentYellow),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_versions_save")
        ) {
            Text("Save version of \"$currentProjectTitle\"", color = Color(0xFF141416), fontWeight = FontWeight.Bold)
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(vertical = 6.dp))

        val list = versions ?: emptyList()
        if (list.isEmpty()) {
            Text(
                text = "No versions yet. Automatic snapshots appear every 50 edits.",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
        list.take(10).forEach { version ->
            VersionRow(version = version, onRestore = { onRestoreVersion(version.id) })
        }
        if (list.size > 10) {
            Text(
                text = "…and ${list.size - 10} older versions",
                color = colors.textTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun VersionRow(version: ProjectVersionEntity, onRestore: () -> Unit) {
    val colors = PencilTheme.colors
    val time = remember(version.createdAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(version.createdAt))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .clickable { onRestore() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("settings_version_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                version.label,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$time · ${version.clipCount} clips",
                color = colors.textTertiary,
                fontSize = 10.sp
            )
        }
        Icon(
            Icons.Default.Restore,
            contentDescription = "Restore this version",
            tint = colors.accentCyan,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
