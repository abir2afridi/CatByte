package com.example.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings → Backup: writes the open project (optionally with its media) to a
 * `.catbyte.zip` and restores such backups back into the library.
 *
 * [onBackup] / [onRestore] are fire-and-forget: the caller launches them in
 * its own scope and reports the outcome through [onNotify].
 */
@Composable
fun SettingsBackupSection(
    /** Title of the currently open project; empty = nothing to back up. */
    currentProjectTitle: String,
    currentProjectId: String?,
    onBackup: (destination: android.net.Uri, includeMedia: Boolean, onDone: (String) -> Unit) -> Unit,
    onRestore: (source: android.net.Uri, onDone: (String) -> Unit) -> Unit,
    onNotify: (String) -> Unit
) {
    val colors = PencilTheme.colors
    var includeMedia by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destination ->
        if (destination != null && currentProjectId != null && !busy) {
            busy = true
            onNotify("Backing up...")
            onBackup(destination, includeMedia) { message ->
                busy = false
                onNotify(message)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { source ->
        if (source != null && !busy) {
            busy = true
            onNotify("Restoring backup...")
            onRestore(source) { message ->
                busy = false
                onNotify(message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Backup & restore",
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (currentProjectTitle.isBlank()) {
                "Open a project to back it up. Restoring works any time."
            } else {
                "Back up \"$currentProjectTitle\" as a .catbyte.zip you can share or keep."
            },
            color = colors.textSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include media files", color = colors.textPrimary, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = includeMedia,
                onCheckedChange = { includeMedia = it },
                enabled = !busy
            )
        }
        Text(
            "Off = settings and timeline only (small file). On = videos, audio and images too (large).",
            color = colors.textSecondary,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            TextButton(
                onClick = {
                    if (currentProjectId == null) {
                        onNotify("Open a project first - then come back to back it up")
                    } else {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        val safeTitle = currentProjectTitle
                            .ifBlank { "project" }
                            .replace(Regex("[^A-Za-z0-9_-]"), "_")
                            .take(24)
                        backupLauncher.launch("CatByte_${safeTitle}_$stamp.catbyte.zip")
                    }
                },
                enabled = !busy,
                modifier = Modifier.testTag("settings_backup_project")
            ) {
                Text("Back up project", color = colors.accentCyan, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                },
                enabled = !busy,
                modifier = Modifier.testTag("settings_restore_backup")
            ) {
                Text("Restore backup", color = colors.textPrimary)
            }
        }
    }
}
