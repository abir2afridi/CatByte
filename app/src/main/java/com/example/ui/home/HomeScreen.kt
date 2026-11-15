package com.example.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.example.BuildConfig
import com.example.R
import com.example.model.AspectRatioType
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.pencilHatching
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

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            // Top Cover Video Banner (catbyte.mp4 at y = 0)
                            item {
                                CoverVideoHeaderSection(
                                    scrollOffsetProvider = scrollOffsetProvider,
                                    onOpenSettings = onOpenSettings
                                )
                            }

                            // Hero "New Project" Card overlapping cover video bottom edge
                            item {
                                Box(modifier = Modifier.offset(y = (-20).dp)) {
                                    HeroNewProjectSection(
                                        onNewProjectClick = { showNewProjectDialog = true },
                                        onQuickCamera = onOpenCamera
                                    )
                                }
                            }

                        // Quick Tools Tiles
                        item {
                            Box(modifier = Modifier.offset(y = (-10).dp)) {
                                QuickToolsRow(onQuickTool = onQuickTool)
                            }
                        }

                        // Search & Filter Bar
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            ProjectSearchAndFilterBar(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { searchQuery = it },
                                filterChoice = filterChoice,
                                onSelectFilter = { filterChoice = it },
                                totalDraftsCount = projects.size,
                                pinnedCount = projects.count { it.isFavorite }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Projects list
                        if (filteredProjects.isEmpty()) {
                            item {
                                if (searchQuery.isNotBlank() || filterChoice != 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_drafts_match),
                                            color = colors.textSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    EmptyProjectsPlaceholder(onStartCreating = { showNewProjectDialog = true })
                                }
                            }
                        } else {
                            items(filteredProjects, key = { it.id }) { project ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    ProjectCardItem(
                                        project = project,
                                        onSelect = { onSelectProject(project) },
                                        onToggleFavorite = { onToggleFavorite(project.id) },
                                        onDuplicate = { onDuplicateProject(project.id) },
                                        onDelete = {
                                            deletedProject = project
                                            onDeleteProject(project.id)
                                            showUndoBar = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
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
            AnimatedVisibility(
                visible = showUndoBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"${deletedProject!!.title}\" deleted",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                onRestoreProject(deletedProject!!)
                                showUndoBar = false
                                deletedProject = null
                            }
                        ) {
                            Text("UNDO", color = colors.accentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
