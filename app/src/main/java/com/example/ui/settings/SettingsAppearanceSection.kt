package com.example.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PencilTheme

/** Appearance & theme picker: the three theme cards. */
@Composable
internal fun SettingsAppearanceSection(
    currentThemeMode: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.appearance_theme),
        icon = Icons.Outlined.Brush,
        iconTint = colors.accentPink
    ) {
        ThemeSelectionCard(
            title = "CapCut Dark Studio",
            description = "Pro pitch dark video studio layout (Default)",
            isSelected = currentThemeMode == AppThemeMode.DARK,
            icon = Icons.Outlined.DarkMode,
            previewColor = Color(0xFF0A0A0E),
            borderColor = Color(0xFF00F2FE),
            onClick = { onSelectTheme(AppThemeMode.DARK) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ThemeSelectionCard(
            title = "Pencil & Paper Sketch",
            description = "Warm cream sketchbook paper theme (Optional)",
            isSelected = currentThemeMode == AppThemeMode.LIGHT,
            icon = Icons.Outlined.LightMode,
            previewColor = Color(0xFFF7F3E8),
            borderColor = Color(0xFF2C2B28),
            onClick = { onSelectTheme(AppThemeMode.LIGHT) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ThemeSelectionCard(
            title = stringResource(R.string.follow_system),
            description = stringResource(R.string.match_phone_setting),
            isSelected = currentThemeMode == AppThemeMode.SYSTEM,
            icon = Icons.Outlined.Brush,
            previewColor = Color.Gray,
            borderColor = colors.border,
            onClick = { onSelectTheme(AppThemeMode.SYSTEM) }
        )
    }
}
