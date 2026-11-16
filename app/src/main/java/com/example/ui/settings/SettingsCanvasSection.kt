package com.example.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.PencilTheme

/** Sketchbook canvas options: the notebook grid toggle. */
@Composable
internal fun SettingsCanvasSection(
    showPaperGrid: Boolean,
    onTogglePaperGrid: (Boolean) -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.sketchbook_canvas),
        icon = Icons.Outlined.GridOn,
        iconTint = colors.accentGreen
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.notebook_grid_lines),
            description = stringResource(R.string.subtle_pencil_grid),
            checked = showPaperGrid,
            onCheckedChange = onTogglePaperGrid
        )
    }
}
