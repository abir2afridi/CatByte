package com.example.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme

/**
 * Project rows for the home list, plus the matching empty state: a "no drafts
 * match" message while filtering/searching, otherwise the start-editing card.
 */
internal fun LazyListScope.homeProjectListItems(
    filteredProjects: List<VideoProject>,
    searchQuery: String,
    filterChoice: Int,
    onStartCreating: () -> Unit,
    onSelectProject: (VideoProject) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRenameProject: (id: String, title: String) -> Unit = { _, _ -> },
    onDuplicateProject: (String) -> Unit,
    onRequestDeleteProject: (VideoProject) -> Unit
) {
    if (filteredProjects.isEmpty()) {
        item {
            if (searchQuery.isNotBlank() || filterChoice != 0) {
                val colors = PencilTheme.colors
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
                EmptyProjectsPlaceholder(onStartCreating = onStartCreating)
            }
        }
    } else {
        items(filteredProjects, key = { it.id }) { project ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                ProjectCardItem(
                    project = project,
                    onSelect = { onSelectProject(project) },
                    onToggleFavorite = { onToggleFavorite(project.id) },
                    onRename = { title -> onRenameProject(project.id, title) },
                    onDuplicate = { onDuplicateProject(project.id) },
                    onDelete = { onRequestDeleteProject(project) }
                )
            }
        }
    }
}
