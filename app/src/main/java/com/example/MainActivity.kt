package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LocaleUtils
import com.example.model.AspectRatioType
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SystemLogsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.paperNotebookBackground
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.EditorViewModelFactory

enum class AppScreen {
    HOME,
    EDITOR,
    SETTINGS_LOGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels {
        EditorViewModelFactory((application as CatByteApplication).container)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            val defaultResolution by viewModel.defaultResolution.collectAsStateWithLifecycle()
            val defaultFps by viewModel.defaultFps.collectAsStateWithLifecycle()
            val showPaperGrid by viewModel.showPaperGrid.collectAsStateWithLifecycle()
            val watermarkEnabled by viewModel.watermarkEnabled.collectAsStateWithLifecycle()
            val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Stop the preview when the app goes to the background.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        viewModel.pausePlayback()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Recreate once when the language preference differs from the applied locale.
            LaunchedEffect(appLanguage) {
                if (LocaleUtils.readLanguage(this@MainActivity) != appLanguage) {
                    LocaleUtils.persistLanguage(this@MainActivity, appLanguage)
                    recreate()
                }
            }

            MyApplicationTheme(themeMode = appThemeMode) {
                val colors = PencilTheme.colors
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .paperNotebookBackground(
                            showGrid = showPaperGrid,
                            gridColor = if (colors.isLight) Color(0x182C2B28) else Color(0x18F6F6F9),
                            marginLineColor = if (colors.isLight) Color(0x24D90429) else Color(0x2238BDF8)
                        ),
                    color = colors.background
                ) {
                    CapCutApp(
                        viewModel = viewModel,
                        onOpenSettings = { viewModel.openSettings() }
                    )
                }
            }
        }
    }
}

@Composable
fun CapCutApp(
    viewModel: EditorViewModel,
    onOpenSettings: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    // Settings is shown as a modal popup over the current screen.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Remember where to return to after closing the System Logs page.
    var logsReturnScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }

    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val defaultResolution by viewModel.defaultResolution.collectAsStateWithLifecycle()
    val defaultFps by viewModel.defaultFps.collectAsStateWithLifecycle()
    val showPaperGrid by viewModel.showPaperGrid.collectAsStateWithLifecycle()
    val watermarkEnabled by viewModel.watermarkEnabled.collectAsStateWithLifecycle()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val navigateToSettings: () -> Unit = {
        showSettings = true
        onOpenSettings()
    }

    val closeSettings: () -> Unit = {
        showSettings = false
        viewModel.closeSettings()
    }

    // The logs page is the only full-screen destination opened from Settings.
    val navigateToSystemLogs: () -> Unit = {
        logsReturnScreen = currentScreen
        showSettings = false
        currentScreen = AppScreen.SETTINGS_LOGS
    }

    val closeSystemLogs: () -> Unit = {
        currentScreen = logsReturnScreen
        showSettings = true
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.HOME -> {
                HomeScreen(
                    projects = projects,
                    onSelectProject = { proj ->
                        viewModel.openProject(proj)
                        currentScreen = AppScreen.EDITOR
                    },
                    onCreateNewProject = {
                        viewModel.createNewProject()
                        currentScreen = AppScreen.EDITOR
                    },
                    onCreateCustomProject = { title, ratio, paperStyle ->
                        viewModel.createNewProjectWithParams(title, ratio, paperStyle)
                        currentScreen = AppScreen.EDITOR
                    },
                    onToggleFavorite = { id ->
                        viewModel.toggleProjectFavorite(id)
                    },
                    onAddMediaClips = { uris ->
                        viewModel.createNewProject()
                        uris.forEachIndexed { idx, uri ->
                            viewModel.addClipFromUri(uri.toString(), "Clip ${idx + 1}")
                        }
                        currentScreen = AppScreen.EDITOR
                    },
                    onDuplicateProject = { id ->
                        viewModel.duplicateProject(id)
                    },
                    onDeleteProject = { id ->
                        viewModel.deleteProject(id)
                    },
                    onRestoreProject = { project ->
                        viewModel.restoreProject(project)
                    },
                    onOpenSettings = { navigateToSettings() }
                )
            }
            AppScreen.EDITOR -> {
                EditorScreen(
                    viewModel = viewModel,
                    onBackToHome = {
                        currentScreen = AppScreen.HOME
                    },
                    onOpenSettings = { navigateToSettings() }
                )
            }
            AppScreen.SETTINGS_LOGS -> {
                // Back returns to the Settings popup that launched this page.
                BackHandler { closeSystemLogs() }

                SystemLogsScreen(
                    onBack = { closeSystemLogs() }
                )
            }
        }
    }

    if (showSettings) {
        Dialog(
            onDismissRequest = { closeSettings() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    SettingsScreen(
                        currentThemeMode = appThemeMode,
                        defaultResolution = defaultResolution,
                        defaultFps = defaultFps,
                        showPaperGrid = showPaperGrid,
                        watermarkEnabled = watermarkEnabled,
                        cacheSizeMb = cacheSizeMb,
                        appLanguage = appLanguage,
                        onSelectTheme = { viewModel.setAppThemeMode(it) },
                        onSelectResolution = { viewModel.setDefaultResolution(it) },
                        onSelectFps = { viewModel.setDefaultFps(it) },
                        onTogglePaperGrid = { viewModel.togglePaperGrid(it) },
                        onToggleWatermark = { viewModel.toggleWatermark(it) },
                        onSelectLanguage = { viewModel.setAppLanguage(it) },
                        onClearCache = { viewModel.clearCache() },
                        onClearAllProjects = { viewModel.clearAllProjects() },
                        onOpenSystemLogs = { navigateToSystemLogs() },
                        isPopup = true,
                        onBack = { closeSettings() }
                    )
                }
            }
        }
    }
}
