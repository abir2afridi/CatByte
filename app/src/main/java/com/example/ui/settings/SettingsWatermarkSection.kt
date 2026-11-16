package com.example.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.PencilTheme

/** Watermark options: enable the sketch logo overlay. */
@Composable
internal fun SettingsWatermarkSection(
    watermarkEnabled: Boolean,
    onToggleWatermark: (Boolean) -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.watermark),
        icon = Icons.Outlined.Brush,
        iconTint = colors.accentYellow
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.add_watermark),
            description = stringResource(R.string.places_sketch_logo),
            checked = watermarkEnabled,
            onCheckedChange = onToggleWatermark
        )
    }
}
