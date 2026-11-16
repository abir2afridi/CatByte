package com.example.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.PencilTheme

/** Debug section: entry point to the system log viewer. */
@Composable
internal fun SettingsSystemLogsSection(
    onOpenSystemLogs: () -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.section_system_logs),
        icon = Icons.Outlined.BugReport,
        iconTint = colors.accentPink
    ) {
        SettingsActionRow(
            title = stringResource(R.string.view_system_logs),
            description = stringResource(R.string.system_logs_description),
            icon = Icons.Outlined.Terminal,
            onClick = onOpenSystemLogs
        )
    }
}
