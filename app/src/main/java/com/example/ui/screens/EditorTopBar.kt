package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
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
import com.example.model.VideoProject
import com.example.ui.theme.PencilTheme

/**
 * CapCut-style editor top bar.
 *
 * Layout matches the reference walkthrough: close (X), search, a trial badge,
 * the "AI UHD" menu chip and the teal Export pill on the far right. Undo/redo
 * live in the transport row, exactly like CapCut.
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
    onSearch: () -> Unit = {}
) {
    val colors = PencilTheme.colors
    var showMenu by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE")
    val unusedHistory = canUndo to canRedo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.background)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(38.dp)
                .testTag("editor_back_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close editor",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(38.dp)
                .testTag("editor_search_button")
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search features and assets",
                tint = colors.textPrimary,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Trial badge: diamond + "Free trial", like the reference top bar.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceElevated.copy(alpha = 0.55f))
                .border(1.dp, colors.borderSubtle.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .testTag("editor_trial_badge"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Trial",
                tint = colors.accentCyan,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Free trial",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // AI UHD dropdown chip. The menu keeps project/editor settings reachable
        // even though the reference top bar has no gear button.
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showMenu = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("editor_ai_uhd_chip"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "AI UHD",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "AI UHD menu",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("AI UHD export", color = colors.textPrimary) },
                    onClick = {
                        showMenu = false
                        onExport()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Aspect ratio & background", color = colors.textPrimary) },
                    onClick = {
                        showMenu = false
                        onOpenFormat()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Editor settings", color = colors.textPrimary) },
                    onClick = {
                        showMenu = false
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Project: ${project.title}", color = colors.textSecondary) },
                    onClick = { showMenu = false }
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(colors.accentCyan)
                .clickable { onExport() }
                .padding(horizontal = 15.dp)
                .testTag("editor_export_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Export",
                color = if (colors.isPencilSketch) Color.Black else Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
