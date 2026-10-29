package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.BuildConfig
import com.example.data.AppLanguage
import com.example.ui.theme.WashiTapeSticker
import com.example.ui.theme.sketchPencilBorder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PencilTheme

@Composable
fun CapCutSettingsDialog(
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
    onDismiss: () -> Unit
) {
    val colors = PencilTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 16.dp)
                .padding(20.dp)
                .testTag("settings_dialog")
        ) {
            WashiTapeSticker(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-8).dp),
                text = "PENCIL CONFIG"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                        Column {
                            Text(
                                text = "Settings",
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pencil & Paper Edition",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Appearance & Light Theme Toggle
                Text(
                    text = "APPEARANCE & THEME",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Light Theme Option (Pencil on Paper)
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

                // Dark Theme Option (Charcoal Slate)
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

                // Follow System
                ThemeSelectionCard(
                    title = "Follow System",
                    description = "Automatically match your phone's dark/light setting",
                    isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                    icon = Icons.Default.Brush,
                    previewColor = Color.Gray,
                    borderColor = colors.border,
                    onClick = { onSelectTheme(AppThemeMode.SYSTEM) }
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Sketch Paper Grid Lines
                Text(
                    text = "SKETCHBOOK CANVAS",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                            text = "Notebook Grid Lines",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Display subtle pencil drafting grid on timeline",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = showPaperGrid,
                        onCheckedChange = onTogglePaperGrid,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.accentCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Video Export Defaults
                Text(
                    text = "DEFAULT EXPORT QUALITY",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Resolution Selector
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
                            .padding(vertical = 8.dp),
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

                Spacer(modifier = Modifier.height(10.dp))
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
                            .padding(vertical = 8.dp),
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

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Language
                Text(
                    text = "LANGUAGE",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                            .padding(vertical = 8.dp),
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

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 5: Watermark
                Text(
                    text = "WATERMARK",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                            text = "Add CatByte Watermark",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Places a subtle sketch logo on exported videos",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = watermarkEnabled,
                        onCheckedChange = onToggleWatermark,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.accentCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 6: Storage & Cache
                Text(
                    text = "STORAGE & DRAFTS",
                    color = colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                            text = "Draft Cache: ${cacheSizeMb} MB",
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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(if (cacheSizeMb > 0) "Clear" else "Clean", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // About section
                Column(
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
