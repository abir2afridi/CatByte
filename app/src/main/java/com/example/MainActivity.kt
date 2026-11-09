package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LocaleUtils
import com.example.model.AspectRatioType
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuickTool
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SystemLogsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.paperNotebookBackground
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.EditorViewModelFactory

enum class AppScreen {
    SPLASH,
    HOME,
    EDITOR,
    SETTINGS,
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
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.SPLASH) }
    // Remember where to return to after closing Settings or System Logs page.
    var settingsReturnScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
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
        settingsReturnScreen = currentScreen
        currentScreen = AppScreen.SETTINGS
        onOpenSettings()
    }

    val closeSettings: () -> Unit = {
        currentScreen = settingsReturnScreen
        viewModel.closeSettings()
    }

    val navigateToSystemLogs: () -> Unit = {
        logsReturnScreen = currentScreen
        currentScreen = AppScreen.SETTINGS_LOGS
    }

    val closeSystemLogs: () -> Unit = {
        currentScreen = logsReturnScreen
    }

    val appContext = androidx.compose.ui.platform.LocalContext.current

    // Quick Tools are real shortcuts: creation tools open a fresh project with
    // the relevant sheet, while clip tools open the most recent project that
    // actually contains video (or explain what is missing).
    val handleQuickTool: (QuickTool) -> Unit = { tool ->
        when (tool) {
            QuickTool.CAPTIONS -> {
                viewModel.createNewProject()
                viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
                currentScreen = AppScreen.EDITOR
            }
            QuickTool.CANVAS -> {
                viewModel.createNewProject()
                viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO)
                currentScreen = AppScreen.EDITOR
            }
            QuickTool.AUDIO -> {
                viewModel.createNewProject()
                viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY)
                currentScreen = AppScreen.EDITOR
            }
            QuickTool.VELOCITY, QuickTool.KEYFRAMES, QuickTool.EXTRACT_AUDIO -> {
                val target = viewModel.currentProject.value
                    ?.takeIf { it.allVideoClips.isNotEmpty() }
                    ?: projects.firstOrNull { it.allVideoClips.isNotEmpty() }
                if (target == null) {
                    Toast.makeText(
                        appContext,
                        "Add a video clip before using this tool",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.openProject(target)
                    currentScreen = AppScreen.EDITOR
                    when (tool) {
                        QuickTool.VELOCITY ->
                            viewModel.setActiveSheet(ActiveToolSheet.SPEED)
                        QuickTool.KEYFRAMES ->
                            viewModel.setActiveSheet(ActiveToolSheet.KEYFRAME)
                        QuickTool.EXTRACT_AUDIO -> {
                            viewModel.extractAudioFromSelectedClip()
                            viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }
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
                        val uriStrings = uris.map { it.toString() }
                        val titles = uriStrings.indices.map { "Clip ${it + 1}" }
                        viewModel.importMultipleUris(uriStrings, titles) {
                            currentScreen = AppScreen.EDITOR
                        }
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
                    onQuickTool = handleQuickTool,
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
            AppScreen.SETTINGS -> {
                BackHandler { closeSettings() }

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
                    isPopup = false,
                    onBack = { closeSettings() }
                )
            }
            AppScreen.SETTINGS_LOGS -> {
                BackHandler { closeSystemLogs() }

                SystemLogsScreen(
                    onBack = { closeSystemLogs() }
                )
            }
        }
    }
}
