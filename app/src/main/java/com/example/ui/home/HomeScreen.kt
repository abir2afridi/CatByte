package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.AspectRatioType
import com.example.model.VideoProject
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
    onQuickTool: (QuickTool) -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    var selectedBottomNav by rememberSaveable { mutableIntStateOf(0) }

    var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterChoice by rememberSaveable { mutableIntStateOf(0) } // 0: All, 1: Pinned, 2: 9:16 Shorts, 3: 16:9 YouTube

    // State for delete confirmation with undo
    var deletedProject by rememberSaveable { mutableStateOf<VideoProject?>(null) }
    var showUndoBar by remember { mutableStateOf(false) }

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
                        onRequestDeleteProject = { project ->
                            deletedProject = project
                            onDeleteProject(project.id)
                            showUndoBar = true
                        }
                    )
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        HomeTopBar(onOpenSettings = onOpenSettings)
                        InboxTabContent(projects)
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        HomeTopBar(onOpenSettings = onOpenSettings)
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
