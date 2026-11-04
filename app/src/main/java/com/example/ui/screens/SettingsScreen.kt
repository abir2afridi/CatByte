package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.WashiTapeSticker
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder

/**
 * Settings content shown as a modal popup over the current screen.
 *
 * The same composable can also be hosted full-screen by passing
 * [isPopup] = false when a dedicated destination is needed.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentThemeMode: AppThemeMode,
    defaultResolution: String,
    defaultFps: Int,
    showPaperGrid: Boolean,
    watermarkEnabled: Boolean,
    cacheSizeMb: Int,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectResolution: (String) -> Unit,
    onSelectFps: (Int) -> Unit,
    onTogglePaperGrid: (Boolean) -> Unit,
    onToggleWatermark: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onClearCache: () -> Unit,
    onClearAllProjects: () -> Unit,
    onOpenSystemLogs: () -> Unit,
    isPopup: Boolean = false,
    onBack: () -> Unit
) {
    val colors = PencilTheme.colors

    var showClearAllConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .then(
                if (isPopup) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                }
            )
            .testTag("settings_screen")
    ) {
        SettingsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------------- Appearance & Theme ----------------
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

            // ---------------- Sketchbook Canvas ----------------
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

            // ---------------- Default Export Quality ----------------
            SettingsSection(
                title = stringResource(R.string.default_export_quality_caps),
                icon = Icons.Outlined.Videocam,
                iconTint = colors.accentCyan
            ) {
                Text(stringResource(R.string.default_resolution), color = colors.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("720P", "1080P", "4K").forEach { res ->
                        val isSel = defaultResolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                                .border(1.dp, if (isSel) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                                .clickable { onSelectResolution(res) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res,
                                color = if (isSel) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.default_frame_rate), color = colors.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fpsLabels = listOf(
                    24 to stringResource(R.string.fps_label, 24),
                    30 to stringResource(R.string.fps_label, 30),
                    60 to stringResource(R.string.fps_label, 60)
                )
                fpsLabels.forEach { (fps, label) ->
                        val isSel = defaultFps == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                                .border(1.dp, if (isSel) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                                .clickable { onSelectFps(fps) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ---------------- Language ----------------
            SettingsSection(
                title = stringResource(R.string.language),
                icon = Icons.Outlined.Language,
                iconTint = colors.accentPurple
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AppLanguage.SYSTEM to stringResource(R.string.system_default),
                        AppLanguage.ENGLISH to stringResource(R.string.english),
                        AppLanguage.BENGALI to stringResource(R.string.bengali)
                    ).forEach { (lang, label) ->
                        val isSel = appLanguage == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                                .border(1.dp, if (isSel) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                                .clickable { onSelectLanguage(lang) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ---------------- Watermark ----------------
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

            // ---------------- Storage & Cache ----------------
            SettingsSection(
                title = stringResource(R.string.section_storage_drafts),
                icon = Icons.Outlined.DeleteSweep,
                iconTint = colors.accentRed
            ) {
                // Clear Cache Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.draft_cache_mb, cacheSizeMb),
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (cacheSizeMb > 0) stringResource(R.string.free_up_space) else stringResource(R.string.cache_completely_clear),
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onClearCache,
                        enabled = cacheSizeMb > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentRed,
                            disabledContainerColor = colors.surfaceElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            if (cacheSizeMb > 0) stringResource(R.string.clear_cache_short) else stringResource(R.string.cache_clean_short),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clear All Projects Row (Danger Zone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.accentRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.clear_all_projects),
                            color = colors.accentRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.clear_all_projects_desc),
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { showClearAllConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            stringResource(R.string.clear_all_projects),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---------------- System Logs (Debug) ----------------
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

            // ---------------- About ----------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CatByte — Pencil & Paper Sketch Edition",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} • Powered by Jetpack Compose",
                    color = colors.textTertiary,
                    fontSize = 10.sp
                )
            }
        }

        // Bottom action bar
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = colors.border),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp)
                .testTag("settings_done")
        ) {
            Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // Confirmation Dialog for Clear All Projects
        if (showClearAllConfirm) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirm = false },
                title = { Text(stringResource(R.string.confirm_clear_all), color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.confirm_clear_all_desc), color = colors.textSecondary, fontSize = 13.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearAllProjects()
                            showClearAllConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.clear_all_projects), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearAllConfirm = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// ==========================================
// Top Bar
// ==========================================

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .testTag("settings_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Settings",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// Section container
// ==========================================

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        content()
    }
}

// ==========================================
// Action row (for navigation items)
// ==========================================

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accentCyan,
                modifier = Modifier.size(20.dp).padding(end = 12.dp)
            )
            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Navigate",
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ==========================================
// Toggle row
// ==========================================

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.accentCyan
            )
        )
    }
}

// ==========================================
// Theme selection card
// ==========================================

@Composable
private fun ThemeSelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    icon: ImageVector,
    previewColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.surfaceElevated else colors.surfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.accentCyan else colors.borderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color thumbnail swatch representing the theme
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewColor)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = borderColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.accentCyan,
                unselectedColor = colors.textTertiary
            )
        )
    }
}
