package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** Confirmation shown before deleting a project. */
@Composable
internal fun ProjectCardDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = PencilTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete project?", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = { Text("This project will be removed. You can undo within a few seconds.", color = colors.textSecondary, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = colors.accentRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
