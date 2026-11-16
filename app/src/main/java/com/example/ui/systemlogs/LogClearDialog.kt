package com.example.ui.systemlogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.PencilTheme

/** Confirmation before wiping every captured log and crash report. */
@Composable
internal fun LogClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = PencilTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("Clear all logs?") },
        text = {
            Text(
                "This removes every captured log and crash report, including " +
                    "entries saved from previous sessions. This cannot be undone."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear", color = colors.accentRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
