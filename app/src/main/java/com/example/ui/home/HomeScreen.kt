package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.ai.AiConfig
import com.example.model.AspectRatioType
import com.example.model.VideoProject
import com.example.ui.ailab.AiLabRun
import com.example.ui.ailab.AiLabScreen
import com.example.ui.theme.PencilTheme
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    projects: List<VideoProject>,
    onSelectProject: (VideoProject) -> Unit,
    onCreateNewProject: () -> Unit,
    onCreateCustomProject: (title: String, ratio: AspectRatioType, paperStyle: String) -> Unit = { _, _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onAddMediaClips: (List<Uri>) -> Unit,
    onOpenCamera: () -> Unit = {},
    onRenameProject: (id: String, title: String) -> Unit = { _, _ -> },
    onDuplicateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onRestoreProject: (VideoProject) -> Unit = {}, // NEW: undo delete
    onBackupProject: (projectId: String, destination: Uri, includeMedia: Boolean) -> Unit = { _, _, _ -> },
    onRestoreBackup: (Uri) -> Unit = {},
    onQuickTool: (QuickTool) -> Unit = {},
    onOpenSettings: () -> Unit,
    aiConfig: com.example.data.ai.AiConfig,
    aiLabHistory: List<com.example.ui.ailab.AiLabRun>
) {
    val colors = PencilTheme.colors
    var selectedBottomNav by rememberSaveable { mutableIntStateOf(0) }

    var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterChoice by rememberSaveable { mutableIntStateOf(0) } // 0: All, 1: Pinned, 2: 9:16 Shorts, 3: 16:9 YouTube

    // State for delete confirmation with undo
    var deletedProject by rememberSaveable { mutableStateOf<VideoProject?>(null) }
    var showUndoBar by remember { mutableStateOf(false) }
    // Project id + media choice while the SAF create-document picker is open.
    // Saved so a rotation while the picker is up does not lose the backup.
    var pendingBackupId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBackupIncludeMedia by rememberSaveable { mutableStateOf(true) }
    var backupChoiceProject by remember { mutableStateOf<VideoProject?>(null) }

    val filteredProjects = remember(projects, searchQuery, filterChoice) {
        projects.filter { proj ->
            val matchesSearch = searchQuery.isBlank() || proj.title.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterChoice) {
                1 -> proj.isFavorite
                2 -> proj.aspectRatio == AspectRatioType.RATIO_9_16
                3 -> proj.aspectRatio == AspectRatioType.RATIO_16_9
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    // Hide the Undo bar automatically after a short grace period.
    LaunchedEffect(showUndoBar, deletedProject?.id) {
        if (showUndoBar) {
            delay(5_000)
            showUndoBar = false
            deletedProject = null
        }
    }

    // Android Zero-Permission Photo Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddMediaClips(uris)
        } else {
            onCreateNewProject()
        }
    }

    val lazyListState = rememberLazyListState()

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destination ->
        val projectId = pendingBackupId
        pendingBackupId = null
        if (destination != null && projectId != null) {
            onBackupProject(projectId, destination, pendingBackupIncludeMedia)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { source ->
        if (source != null) onRestoreBackup(source)
    }

    val startBackup: (VideoProject, Boolean) -> Unit = { project, includeMedia ->
        pendingBackupId = project.id
        pendingBackupIncludeMedia = includeMedia
        backupLauncher.launch(backupFileName(project.title))
    }

    val pickRestoreBackup: () -> Unit = {
        restoreLauncher.launch(
            arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
        )
    }
    val scrollOffsetProvider = remember(lazyListState) {
        {
            if (lazyListState.firstVisibleItemIndex > 0) 1000f
            else lazyListState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedBottomNav) {
                0 -> {
                    // "Edit" Tab: Parallax Cover Video Header + Hero New Project + Quick Tools + Projects
                    HomeEditTab(
                        projects = projects,
                        filteredProjects = filteredProjects,
                        searchQuery = searchQuery,
                        filterChoice = filterChoice,
                        lazyListState = lazyListState,
                        scrollOffsetProvider = scrollOffsetProvider,
                        onOpenSettings = onOpenSettings,
                        onNewProjectClick = { showNewProjectDialog = true },
                        onQuickCamera = onOpenCamera,
                        onQuickTool = onQuickTool,
                        onSearchQueryChanged = { searchQuery = it },
                        onSelectFilter = { filterChoice = it },
                        onSelectProject = onSelectProject,
                        onToggleFavorite = onToggleFavorite,
                        onRenameProject = onRenameProject,
                        onDuplicateProject = onDuplicateProject,
                        onBackupProject = { project -> backupChoiceProject = project },
                        onRestoreBackup = pickRestoreBackup,
                        onRequestDeleteProject = { project ->
                            deletedProject = project
                            onDeleteProject(project.id)
                            showUndoBar = true
                        }
                    )
                }
                1 -> {
                    // AI Lab Tab
                    AiLabScreen(
                        aiConfig = aiConfig,
                        history = aiLabHistory,
                        onOpenSettings = onOpenSettings,
                        onClearHistory = { /* TODO: wire to repository */ },
                        onCreateProjectWithScript = { title, lines ->
                            onCreateNewProject()
                            // TODO: open editor with script
                        },
                        onRunRecorded = { _, _, _ -> }
                    )
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        HomeTopBar(onOpenSettings = onOpenSettings, onRestoreBackup = pickRestoreBackup)
                        InboxTabContent(projects)
                    }
                }
                3 -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        HomeTopBar(onOpenSettings = onOpenSettings, onRestoreBackup = pickRestoreBackup)
                        ProfileTabContent(projects, onOpenSettings = onOpenSettings)
                    }
                }
            }
        }

        // Undo Bar (appears after delete, auto-dismisses)
        if (showUndoBar && deletedProject != null) {
            HomeUndoBar(
                deletedProject = deletedProject!!,
                onUndo = {
                    onRestoreProject(deletedProject!!)
                    showUndoBar = false
                    deletedProject = null
                }
            )
        }

        // Bottom Navigation Bar
        CapCutBottomNavigationBar(
            selectedIndex = selectedBottomNav,
            onSelect = { selectedBottomNav = it }
        )

        // Backup options: media makes a portable archive, project-only stays small.
        backupChoiceProject?.let { project ->
            AlertDialog(
                onDismissRequest = { backupChoiceProject = null },
                title = { Text("Back up \"${project.title}\"", color = colors.textPrimary) },
                text = {
                    Text(
                        "Include the original media files so this backup opens on another device? " +
                            "A project-only backup is much smaller and can relink media later.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupChoiceProject = null
                            startBackup(project, true)
                        }
                    ) {
                        Text("Include media")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                backupChoiceProject = null
                                startBackup(project, false)
                            }
                        ) {
                            Text("Project only")
                        }
                        TextButton(onClick = { backupChoiceProject = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // New Project Setup Dialog
        if (showNewProjectDialog) {
            NewProjectBlueprintDialog(
                onDismiss = { showNewProjectDialog = false },
                onCreateCustom = { title, ratio, paperStyle ->
                    showNewProjectDialog = false
                    onCreateCustomProject(title, ratio, paperStyle)
                },
                onPickMedia = {
                    showNewProjectDialog = false
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            )
        }
    }
}

/** SAF-safe file name for a project backup (`<title>.catbyte.zip`). */
private fun backupFileName(title: String): String {
    val cleaned = title.trim()
        .map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }
        .joinToString("")
        .trim('_')
        .take(48)
        .ifEmpty { "CatByte_project" }
    return "$cleaned.catbyte.zip"
}
