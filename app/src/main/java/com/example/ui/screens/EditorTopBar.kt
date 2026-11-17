package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.VideoProject

/**
 * CapCut-style editor top bar:
 * - Closed state: Close (X) + Search on the left, "AI UHD v" chip and the cyan
 *   "Export" pill on the right.
 * - Open state (AI UHD tapped): "video | GIF" tabs replace the left icons, the
 *   chip flips to a caret-up state and the export dropdown expands below.
 * The Export button has exactly one job: start the render with the settings
 * the user already chose in the AI UHD dropdown (single click, no dialog).
 */
@Composable
internal fun EditorTopBar(
    project: VideoProject,
    exportOpen: Boolean,
    exportTab: String,
    onBack: () -> Unit,
    onToggleExportDropdown: () -> Unit,
    onExportTabSelected: (String) -> Unit,
    onStartExport: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit = {},
    onExportDraft: () -> Unit = {},
    onRenameProject: ((String) -> Unit)? = null
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(project.title) { mutableStateOf(project.title) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0F))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .zIndex(10f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (exportOpen) {
                // Open state: video / GIF tabs (matches the export dropdown
                // screenshots; the chip caret points up while it is open).
                EditorExportTab(
                    label = "video",
                    selected = exportTab == "video",
                    onClick = { onExportTabSelected("video") }
                )
                Spacer(modifier = Modifier.width(16.dp))
                EditorExportTab(
                    label = "GIF",
                    selected = exportTab == "gif",
                    onClick = { onExportTabSelected("gif") }
                )
            } else {
                // Closed state: Close (X)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close editor",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Search Icon
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("editor_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search features and assets",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Project Title & Draft Saved Badge (Center Header)
                val infiniteTransition = rememberInfiniteTransition(label = "saved_pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = onRenameProject != null) {
                            renameText = project.title
                            showRenameDialog = true
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = project.title.ifBlank { "Untitled Project" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF26E5D7).copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "Draft Saved ✓",
                            color = Color(0xFFA5A8B2),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right side: Quick Resolution Badge + AI UHD format chip + Export button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resolution & FPS Badge (Clickable to inspect / switch export dropdown)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E1F24))
                        .border(0.8.dp, Color(0xFF33353E), RoundedCornerShape(4.dp))
                        .clickable { onToggleExportDropdown() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${project.resolution} • ${project.fps}fps",
                        color = Color(0xFF26E5D7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // "AI UHD ▾" chip button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF24252A))
                        .clickable { onToggleExportDropdown() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("editor_ai_uhd_chip"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (exportTab == "gif") "GIF" else "AI UHD",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "AI UHD menu",
                            tint = Color(0xFFA5A8B2),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (exportOpen) 180f else 0f)
                        )
                    }
                }

                // Vibrant Cyan "Export" button: ONE click starts the export.
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF26E5D7))
                        .clickable { onStartExport() }
                        .padding(horizontal = 18.dp)
                        .testTag("editor_export_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Export",
                        color = Color(0xFF041E21),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Project", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF26E5D7),
                            unfocusedBorderColor = Color(0xFF33353E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameText.isNotBlank()) {
                                onRenameProject?.invoke(renameText.trim())
                            }
                            showRenameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7))
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = Color(0xFFA5A8B2))
                    }
                },
                containerColor = Color(0xFF1E1F24)
            )
        }
    }
}

/** One "video"/"GIF" tab with the cyan underline when selected. */
@Composable
private fun EditorExportTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag("editor_export_tab_$label"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFA5A8B2),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.5.dp)
                    .background(Color(0xFF26E5D7), RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(4.5.dp))
        }
    }
}
