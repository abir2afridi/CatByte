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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.WashiTapeSticker
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder

/**
 * Full-screen Settings page.
 *
 * Replaces the old popup dialog: it is a dedicated destination with its own
 * top bar, back navigation and scrollable content.
 */
@Composable
fun SettingsScreen(
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
    onBack: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                title = "APPEARANCE & THEME",
                icon = Icons.Default.Brush,
                iconTint = colors.accentPink
            ) {
                ThemeSelectionCard(
                    title = "Pencil on Paper (Light Theme)",
                    description = "Warm cream sketchbook paper with 2B graphite strokes",
                    isSelected = currentThemeMode == AppThemeMode.PENCIL_LIGHT,
                    icon = Icons.Default.LightMode,
                    previewColor = Color(0xFFF7F3E8),
                    borderColor = Color(0xFF2C2B28),
                    onClick = { onSelectTheme(AppThemeMode.PENCIL_LIGHT) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeSelectionCard(
                    title = "Charcoal Slate (Dark Theme)",
                    description = "Dark chalkboard with crisp chalk sketch lines",
                    isSelected = currentThemeMode == AppThemeMode.CHARCOAL_DARK,
                    icon = Icons.Default.DarkMode,
                    previewColor = Color(0xFF141519),
                    borderColor = Color(0xFF4C4E61),
                    onClick = { onSelectTheme(AppThemeMode.CHARCOAL_DARK) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeSelectionCard(
                    title = "Follow System",
                    description = "Automatically match your phone's dark/light setting",
                    isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                    icon = Icons.Default.Brush,
                    previewColor = Color.Gray,
                    borderColor = colors.border,
                    onClick = { onSelectTheme(AppThemeMode.SYSTEM) }
                )
            }

            // ---------------- Sketchbook Canvas ----------------
            SettingsSection(
                title = "SKETCHBOOK CANVAS",
                icon = Icons.Default.GridOn,
                iconTint = colors.accentGreen
            ) {
                SettingsToggleRow(
                    title = "Notebook Grid Lines",
                    description = "Display subtle pencil drafting grid on timeline",
                    checked = showPaperGrid,
                    onCheckedChange = onTogglePaperGrid
                )
            }

            // ---------------- Default Export Quality ----------------
            SettingsSection(
                title = "DEFAULT EXPORT QUALITY",
                icon = Icons.Default.Videocam,
                iconTint = colors.accentCyan
            ) {
                Text("Default Resolution", color = colors.textSecondary, fontSize = 11.sp)
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
                Text("Default Frame Rate", color = colors.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24 to "24 fps", 30 to "30 fps", 60 to "60 fps").forEach { (fps, label) ->
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
                title = "LANGUAGE",
                icon = Icons.Default.Language,
                iconTint = colors.accentPurple
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AppLanguage.SYSTEM to "System Default",
                        AppLanguage.ENGLISH to "English",
                        AppLanguage.BENGALI to "বাংলা"
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
                title = "WATERMARK",
                icon = Icons.Default.Brush,
                iconTint = colors.accentYellow
            ) {
                SettingsToggleRow(
                    title = "Add CatByte Watermark",
                    description = "Places a subtle sketch logo on exported videos",
                    checked = watermarkEnabled,
                    onCheckedChange = onToggleWatermark
                )
            }

            // ---------------- Storage & Cache ----------------
            SettingsSection(
                title = "STORAGE & DRAFTS",
                icon = Icons.Default.DeleteSweep,
                iconTint = colors.accentRed
            ) {
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
                            text = "Draft Cache: $cacheSizeMb MB",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (cacheSizeMb > 0) "Free up space on your device" else "Cache is completely clear!",
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
                        Text(if (cacheSizeMb > 0) "Clear" else "Clean", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
    }
}

// ==========================================
// Top Bar
// ==========================================

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .sketchPencilBorder(strokeWidth = 1.4.dp, color = colors.border, cornerRadius = 20.dp)
                    .clickable { onBack() }
                    .testTag("settings_back"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = colors.accentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pencil & Paper Edition",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }

        WashiTapeSticker(
            modifier = Modifier.align(Alignment.TopEnd),
            text = "PENCIL CONFIG"
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
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .pencilHatching(color = colors.border.copy(alpha = 0.04f), spacing = 9.dp)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 16.dp)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        content()
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
                width = if (isSelected) 2.dp else 1.dp,
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
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = borderColor,
                modifier = Modifier.size(20.dp)
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
