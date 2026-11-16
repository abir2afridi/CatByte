package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PencilTheme

/** Overflow menu for a project card: rename, duplicate or delete. */
@Composable
internal fun ProjectCardMenu(
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = PencilTheme.colors

    Box {
        IconButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = colors.textTertiary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier
                .background(colors.surface)
                .border(1.2.dp, colors.border)
        ) {
            DropdownMenuItem(
                text = { Text("Rename", color = colors.textPrimary) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textPrimary) },
                onClick = {
                    onDismiss()
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Duplicate", color = colors.textPrimary) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.textPrimary) },
                onClick = {
                    onDismiss()
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = colors.accentRed) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.accentRed) },
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}
