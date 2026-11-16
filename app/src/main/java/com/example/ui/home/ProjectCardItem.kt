package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme

/** One project row: thumbnail, title/meta, pin toggle and overflow menu. */
@Composable
internal fun ProjectCardItem(
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

            ProjectCardThumbnail(project = project)

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (project.isFavorite) {
                        Text("≡ƒôî", fontSize = 12.sp)
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
                    text = "${String.format("%.1fs", durationSec)} ΓÇó ${project.clips.size} clips",
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
            ProjectCardMenu(
                expanded = menuExpanded,
                onOpen = { menuExpanded = true },
                onDismiss = { menuExpanded = false },
                onDuplicate = onDuplicate,
                onDelete = { showDeleteConfirm = true }
            )
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
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        ProjectCardDeleteDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    }
}
