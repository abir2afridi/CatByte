package com.example.ui.screens

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

enum class QuickTool {
    CAPTIONS,
    CANVAS,
    VELOCITY,
    AUDIO,
    EXTRACT_AUDIO,
    KEYFRAMES
}

@Composable
fun HomeScreen(
    projects: List<VideoProject>,
    onSelectProject: (VideoProject) -> Unit,
    onCreateNewProject: () -> Unit,
    onCreateCustomProject: (title: String, ratio: AspectRatioType, paperStyle: String) -> Unit = { _, _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onAddMediaClips: (List<Uri>) -> Unit,
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
                                        onQuickCamera = { showNewProjectDialog = true }
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

// ==========================================
// Top Bar
// ==========================================

@Composable
private fun HomeTopBar(
    onOpenSettings: () -> Unit,
    isOverlay: Boolean = false
) {
    val colors = PencilTheme.colors
    val textColor = if (isOverlay) Color.White else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Theme Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CatByte",
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            // Theme Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isOverlay) Color.Black.copy(alpha = 0.4f) else colors.surfaceElevated)
                    .border(0.8.dp, if (isOverlay) Color.White.copy(alpha = 0.3f) else colors.borderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (colors.isLight) "LIGHT" else "DARK",
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isOverlay) Color.Black.copy(alpha = 0.4f) else colors.surfaceElevated)
                    .border(0.8.dp, if (isOverlay) Color.White.copy(alpha = 0.3f) else colors.borderSubtle, CircleShape)
                    .testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textColor,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

// ==========================================
// Cover Video Header Section (catbyte.mp4)
// ==========================================

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun CoverVideoHeaderSection(
    scrollOffsetProvider: () -> Float = { 0f },
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current
    val density = LocalDensity.current
    val maxScrollPx = with(density) { 180.dp.toPx() }

    val coverPlayer = remember(context) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            volume = 0f
            val assetUri = Uri.parse("asset:///cover/catbyte.mp4")
            val localFile = java.io.File("D:/GitHub Project/CatByte/asset/cover/catbyte.mp4")
            val targetUri = if (localFile.exists()) {
                Uri.fromFile(localFile)
            } else {
                assetUri
            }
            setMediaItem(androidx.media3.common.MediaItem.fromUri(targetUri))
            prepare()
            playWhenReady = true
        }
    }

    androidx.compose.runtime.DisposableEffect(coverPlayer) {
        onDispose {
            coverPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer {
                val offset = scrollOffsetProvider()
                val fraction = (offset / maxScrollPx).coerceIn(0f, 1f)
                translationY = -offset * 0.40f
                alpha = (1f - fraction * 1.1f).coerceIn(0f, 1f)
            }
            .background(Color.Black)
            .testTag("edit_page_cover_video")
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = coverPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle top gradient shadow for top bar readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Transparent Top Header Overlay (behind status bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            HomeTopBar(
                onOpenSettings = onOpenSettings,
                isOverlay = true
            )
        }
    }
}

// ==========================================
// Hero New Project Section
// ==========================================

@Composable
private fun HeroNewProjectSection(
    onNewProjectClick: () -> Unit,
    onQuickCamera: () -> Unit
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main "New Project" Card
        Box(
            modifier = Modifier
                .weight(1.8f)
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(
                    width = 1.dp,
                    color = colors.borderSubtle,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onNewProjectClick() }
                .testTag("hero_new_project_button")
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Project",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "4K • 60FPS",
                            color = colors.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column {
                    Text(
                        text = "New Project",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.import_clips_sketchpad),
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Side Camera Card
        Box(
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .clickable { 
                    Toast.makeText(context, context.getString(R.string.camera_prompter_not_implemented), Toast.LENGTH_SHORT).show()
                }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(0.8.dp, colors.borderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Camera",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.camera_prompter_not_implemented),
                    color = colors.textTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ==========================================
// Quick Tools
// ==========================================

private data class QuickToolItem(
    val icon: ImageVector,
    val label: String,
    val tool: QuickTool
)

@Composable
private fun QuickToolsRow(onQuickTool: (QuickTool) -> Unit) {
    val colors = PencilTheme.colors
    val tools = listOf(
        QuickToolItem(Icons.Default.Subtitles, "Captions", QuickTool.CAPTIONS),
        QuickToolItem(Icons.Default.AutoAwesome, "Canvas", QuickTool.CANVAS),
        QuickToolItem(Icons.Default.Speed, "Velocity", QuickTool.VELOCITY),
        QuickToolItem(Icons.Default.GraphicEq, "Audio", QuickTool.AUDIO),
        QuickToolItem(Icons.Default.VolumeUp, "Extract", QuickTool.EXTRACT_AUDIO),
        QuickToolItem(Icons.Default.VideoLibrary, "Keyframes", QuickTool.KEYFRAMES)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(tools) { (icon, name, tool) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onQuickTool(tool) }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = name,
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// Project Item
// ==========================================

@Composable
private fun ProjectCardItem(
    project: VideoProject,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = PencilTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                .clickable { onSelect() }
                .padding(10.dp)
                .testTag("project_item_${project.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {

        // Thumbnail with subtle border
        Box(
            modifier = Modifier
                .size(width = 84.dp, height = 76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
        ) {
            if (project.thumbnailResId != null) {
                Image(
                    painter = painterResource(id = project.thumbnailResId),
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Project",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Aspect ratio tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(topEnd = 6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = project.aspectRatio.label,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (project.isFavorite) {
                    Text("📌", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = project.title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val durationSec = project.totalDurationMs / 1000f
            Text(
                text = "${String.format("%.1fs", durationSec)} • ${project.clips.size} clips",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (project.isFavorite) colors.accentRed else colors.accentCyan)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (project.isFavorite) stringResource(R.string.pinned_to_top) else stringResource(R.string.pencil_draft_saved),
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Pin Button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = if (project.isFavorite) "Unpin" else "Pin",
                tint = if (project.isFavorite) colors.accentRed else colors.textTertiary.copy(alpha = 0.45f),
                modifier = Modifier.size(19.dp)
            )
        }

        // More Menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colors.textTertiary
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(colors.surface)
                    .border(1.2.dp, colors.border)
            ) {
                DropdownMenuItem(
                    text = { Text("Duplicate", color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.textPrimary) },
                    onClick = {
                        menuExpanded = false
                        onDuplicate()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = colors.accentRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.accentRed) },
                    onClick = {
                        menuExpanded = false
                        showDeleteConfirm = true
                    }
                )
            }
        }
    }

    // Metal Paperclip for pinned projects
    if (project.isFavorite) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 18.dp, y = (-10).dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Pinned",
                tint = colors.accentRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete project?", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("This project will be removed. You can undo within a few seconds.", color = colors.textSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = colors.accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(colors.surface)
                .border(2.dp, colors.border, RoundedCornerShape(16.dp))
        )
    }
}
}

// ==========================================
// Empty State
// ==========================================

@Composable
private fun EmptyProjectsPlaceholder(
    onStartCreating: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(1.5.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.empty_state_title),
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.empty_state_subtitle),
            color = colors.textSecondary,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStartCreating,
            colors = ButtonDefaults.buttonColors(containerColor = colors.border),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Start Editing", color = if (colors.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// Inbox & Profile Secondary Tab Placeholders
// ==========================================

@Composable
private fun InboxTabContent(projects: List<VideoProject>) {
    val colors = PencilTheme.colors
    val draftCount = projects.size
    val favoriteCount = projects.count { it.isFavorite }
    val totalClips = projects.sumOf { it.clips.size }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.inbox_title), color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.inbox_subtitle), color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊 Your Project Stats", color = colors.accentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$draftCount", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Projects", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$favoriteCount", color = colors.accentRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Pinned", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalClips", color = colors.accentGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Clips", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    projects: List<VideoProject>,
    onOpenSettings: () -> Unit
) {
    val colors = PencilTheme.colors
    val draftCount = projects.size
    val favoriteCount = projects.count { it.isFavorite }
    val totalClips = projects.sumOf { it.clips.size }
    val totalDurationSec = projects.sumOf { it.totalDurationMs } / 1000f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(2.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(42.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.profile_title), color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.profile_id), color = colors.accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$draftCount", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Projects", color = colors.textSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${String.format("%.1fs", totalDurationSec)}", color = colors.accentCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Total Time", color = colors.textSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$totalClips", color = colors.accentYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Clips", color = colors.textSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, colors.border, RoundedCornerShape(10.dp))
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = colors.textPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Settings", color = colors.textPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// Bottom Navigation Bar
// ==========================================

@Composable
private fun CapCutBottomNavigationBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(colors.surface)
            .border(width = 1.dp, color = colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(0, "Edit", Icons.Outlined.ContentCut),
                Triple(1, "Inbox", Icons.Outlined.Notifications),
                Triple(2, "Profile", Icons.Outlined.Person)
            )

            items.forEach { (index, label, icon) ->
                val isSelected = selectedIndex == index
                val itemColor = if (isSelected) colors.accentCyan else colors.textTertiary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = itemColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==========================================
// Search & Filter Bar
// ==========================================

@Composable
private fun ProjectSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    filterChoice: Int,
    onSelectFilter: (Int) -> Unit,
    totalDraftsCount: Int,
    pinnedCount: Int
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = "Search projects by title...",
                    color = colors.textTertiary,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.border,
                unfocusedBorderColor = colors.borderSubtle,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("home_search_input")
        )

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                "All ($totalDraftsCount)",
                "📌 Pinned ($pinnedCount)",
                "9:16 Shorts",
                "16:9 YouTube"
            )

            chips.forEachIndexed { index, label ->
                val isSelected = filterChoice == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) colors.surfaceElevated else colors.surface)
                        .border(
                            width = 0.8.dp,
                            color = if (isSelected) colors.accentCyan else colors.borderSubtle,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectFilter(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.accentCyan else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==========================================
// New Project Setup Dialog
// ==========================================

@Composable
private fun NewProjectBlueprintDialog(
    onDismiss: () -> Unit,
    onCreateCustom: (title: String, ratio: AspectRatioType, paperStyle: String) -> Unit,
    onPickMedia: () -> Unit
) {
    val colors = PencilTheme.colors
    var projectTitle by remember { mutableStateOf("New Project #${System.currentTimeMillis() % 1000}") }
    var selectedRatio by remember { mutableStateOf(AspectRatioType.RATIO_9_16) }
    var selectedPaperStyle by remember { mutableStateOf("Standard") }

    val ratios = listOf(
        Triple(AspectRatioType.RATIO_9_16, "9:16", "Reels / Shorts / TikTok"),
        Triple(AspectRatioType.RATIO_16_9, "16:9", "YouTube / Cinema"),
        Triple(AspectRatioType.RATIO_1_1, "1:1", "Instagram Square"),
        Triple(AspectRatioType.RATIO_4_5, "4:5", stringResource(R.string.portrait_feed_post))
    )

    val paperStyles = listOf(
        "Standard",
        "Grid",
        "Clean",
        "Dark"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(2.dp, colors.border, RoundedCornerShape(18.dp))
                .padding(20.dp)
                .testTag("new_project_blueprint_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📐", fontSize = 18.sp)
                        Text(
                            text = "New Project",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                // Project Title Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Project Name",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated,
                            focusedBorderColor = colors.border,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Canvas Aspect Ratio
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Canvas Aspect Ratio",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ratios.forEach { (ratio, label, desc) ->
                            val isSelected = selectedRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.surfaceElevated else colors.surface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.accentCyan else colors.borderSubtle,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedRatio = ratio }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) colors.accentCyan else colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (ratio) {
                                            AspectRatioType.RATIO_9_16 -> "Reels"
                                            AspectRatioType.RATIO_16_9 -> "Cinema"
                                            AspectRatioType.RATIO_1_1 -> "Square"
                                            AspectRatioType.RATIO_4_5 -> "Feed"
                                        },
                                        color = colors.textTertiary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Background Style
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Background Style",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paperStyles.forEach { style ->
                            val isSelected = selectedPaperStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accentYellow.copy(alpha = 0.25f) else colors.surfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.6.dp else 1.dp,
                                        color = if (isSelected) colors.accentYellow else colors.borderSubtle,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPaperStyle = style }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style,
                                    color = colors.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pick Media
                    Button(
                        onClick = onPickMedia,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Import Media",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Create Custom Project
                    Button(
                        onClick = {
                            onCreateCustom(projectTitle, selectedRatio, selectedPaperStyle)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(
                            text = "Create Project",
                            color = if (colors.isLight) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}