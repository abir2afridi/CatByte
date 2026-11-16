package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PencilTheme

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
            SettingsAppearanceSection(
                currentThemeMode = currentThemeMode,
                onSelectTheme = onSelectTheme
            )

            SettingsCanvasSection(
                showPaperGrid = showPaperGrid,
                onTogglePaperGrid = onTogglePaperGrid
            )

            SettingsExportQualitySection(
                defaultResolution = defaultResolution,
                defaultFps = defaultFps,
                onSelectResolution = onSelectResolution,
                onSelectFps = onSelectFps
            )

            SettingsLanguageSection(
                appLanguage = appLanguage,
                onSelectLanguage = onSelectLanguage
            )

            SettingsWatermarkSection(
                watermarkEnabled = watermarkEnabled,
                onToggleWatermark = onToggleWatermark
            )

            SettingsStorageSection(
                cacheSizeMb = cacheSizeMb,
                onClearCache = onClearCache,
                onRequestClearAllProjects = { showClearAllConfirm = true }
            )

            SettingsSystemLogsSection(onOpenSystemLogs = onOpenSystemLogs)

            SettingsAboutFooter()
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
            SettingsClearAllDialog(
                onDismiss = { showClearAllConfirm = false },
                onConfirm = {
                    onClearAllProjects()
                    showClearAllConfirm = false
                }
            )
        }
    }
}
