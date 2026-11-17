package com.example.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.VideoProject

/**
 * The "Edit" tab: parallax cover header, hero new-project card, quick tools,
 * the search/filter bar and the project list.
 */
@Composable
internal fun HomeEditTab(
    projects: List<VideoProject>,
    filteredProjects: List<VideoProject>,
    searchQuery: String,
    filterChoice: Int,
    lazyListState: LazyListState,
    scrollOffsetProvider: () -> Float,
    onOpenSettings: () -> Unit,
    onNewProjectClick: () -> Unit,
    onQuickCamera: () -> Unit,
    onQuickTool: (QuickTool) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectFilter: (Int) -> Unit,
    onSelectProject: (VideoProject) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRenameProject: (id: String, title: String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onRequestDeleteProject: (VideoProject) -> Unit,
    onBackupProject: (VideoProject) -> Unit = {},
    onRestoreBackup: (() -> Unit)? = null
) {
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
                    onOpenSettings = onOpenSettings,
                    onRestoreBackup = onRestoreBackup
                )
            }

            // Hero "New Project" Card overlapping cover video bottom edge
            item {
                Box(modifier = Modifier.offset(y = (-20).dp)) {
                    HeroNewProjectSection(
                        onNewProjectClick = onNewProjectClick,
                        onQuickCamera = onQuickCamera
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
                    onSearchQueryChanged = onSearchQueryChanged,
                    filterChoice = filterChoice,
                    onSelectFilter = onSelectFilter,
                    totalDraftsCount = projects.size,
                    pinnedCount = projects.count { it.isFavorite }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Projects list
            homeProjectListItems(
                filteredProjects = filteredProjects,
                searchQuery = searchQuery,
                filterChoice = filterChoice,
                onStartCreating = onNewProjectClick,
                onSelectProject = onSelectProject,
                onToggleFavorite = onToggleFavorite,
                onRenameProject = onRenameProject,
                onDuplicateProject = onDuplicateProject,
                onRequestDeleteProject = onRequestDeleteProject,
                onBackupProject = onBackupProject
            )
        }
    }
}
