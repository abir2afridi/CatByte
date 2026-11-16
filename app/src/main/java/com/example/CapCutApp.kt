package com.example

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.camera.CameraCaptureScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.QuickTool
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.systemlogs.SystemLogsScreen
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

enum class AppScreen {
    SPLASH,
    HOME,
    CAMERA,
    EDITOR,
    SETTINGS,
    SETTINGS_LOGS
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
    val showSafeAreaGuides by viewModel.showSafeAreaGuides.collectAsStateWithLifecycle()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val aiConfig by viewModel.aiConfig.collectAsStateWithLifecycle()

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
                    onRenameProject = { id, title ->
                        viewModel.renameProject(id, title)
                    },
                    onDeleteProject = { id ->
                        viewModel.deleteProject(id)
                    },
                    onRestoreProject = { project ->
                        viewModel.restoreProject(project)
                    },
                    onQuickTool = handleQuickTool,
                    onOpenCamera = {
                        com.example.data.AppLog.info("Camera", "Home Camera tapped -> opening capture screen")
                        currentScreen = AppScreen.CAMERA
                    },
                    onOpenSettings = { navigateToSettings() }
                )
            }
            AppScreen.CAMERA -> {
                CameraCaptureScreen(
                    onBack = { currentScreen = AppScreen.HOME },
                    onMediaCaptured = { uri ->
                        // Shoot & Edit: start a fresh project, import the real
                        // photo/video and jump straight into the editor.
                        com.example.data.AppLog.info("Camera", "Importing captured media: $uri")
                        viewModel.createNewProject()
                        viewModel.importMultipleUris(
                            listOf(uri.toString()),
                            listOf("Camera clip")
                        ) {
                            currentScreen = AppScreen.EDITOR
                        }
                    }
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
                    showSafeAreaGuides = showSafeAreaGuides,
                    cacheSizeMb = cacheSizeMb,
                    appLanguage = appLanguage,
                    aiBaseUrl = aiConfig.baseUrl,
                    aiApiKey = aiConfig.apiKey,
                    aiModel = aiConfig.model,
                    aiImageModel = aiConfig.imageModel,
                    aiSpeechModel = aiConfig.speechModel,
                    onSelectTheme = { viewModel.setAppThemeMode(it) },
                    onSelectResolution = { viewModel.setDefaultResolution(it) },
                    onSelectFps = { viewModel.setDefaultFps(it) },
                    onTogglePaperGrid = { viewModel.togglePaperGrid(it) },
                    onToggleWatermark = { viewModel.toggleWatermark(it) },
                    onToggleSafeAreaGuides = { viewModel.toggleSafeAreaGuides(it) },
                    onSelectLanguage = { viewModel.setAppLanguage(it) },
                    onSaveAiConfig = { url, key, model, imageModel, speechModel ->
                        viewModel.saveAiConfig(url, key, model, imageModel, speechModel)
                    },
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
