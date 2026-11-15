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

// ==========================================
// Project Item
// ==========================================

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
internal fun EmptyProjectsPlaceholder(
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
internal fun ProjectSearchAndFilterBar(
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