package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.VideoProject

/**
 * CapCut-style editor top bar matching the user's reference image:
 * - Left: Close (X) and Search (Magnifying glass)
 * - Right: "AI UHD v" pill dropdown and vibrant cyan "Export" pill button
 * - Under Export: "Export Draft" pill
 */
@Composable
internal fun EditorTopBar(
    project: VideoProject,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenFormat: () -> Unit,
    onExport: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit = {},
    onExportDraft: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

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
            // Left: Close (X)
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

            Spacer(modifier = Modifier.weight(1f))

            // Right side column containing top buttons and "Export Draft" below
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "AI UHD ▾" dropdown chip
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF24252A))
                                .clickable { showMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("editor_ai_uhd_chip"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "AI UHD",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "AI UHD menu",
                                tint = Color(0xFFA5A8B2),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("AI UHD Export", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onExport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Aspect Ratio & Background", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onOpenFormat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Editor Settings", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Project: ${project.title}", color = Color.Gray) },
                                onClick = { showMenu = false }
                            )
                        }
                    }

                    // Cyan "Export" pill button
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xFF26E5D7))
                            .clickable { onExport() }
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

                // "Export Draft" pill directly below Export
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2B31))
                        .clickable {
                            if (onExportDraft != {}) onExportDraft() else onExport()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("editor_export_draft_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Export Draft",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
