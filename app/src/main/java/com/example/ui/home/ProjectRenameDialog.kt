package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectRepository
import com.example.ui.theme.PencilTheme

/**
 * Rename dialog for a project. Pre-fills the current title, trims the input and
 * disables the confirm button while the title is blank, so a project can never
 * be renamed to "".
 */
@Composable
internal fun ProjectRenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = PencilTheme.colors
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    val focusRequester = remember { FocusRequester() }
    val normalized = ProjectRepository.normalizeTitle(title)
    val canConfirm = normalized != null && normalized != currentTitle

    fun confirm() {
        val value = normalized ?: return
        onConfirm(value)
    }

    LaunchedEffect(Unit) {
        // Put the caret in the title field so the keyboard opens immediately.
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Rename project",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(ProjectRepository.MAX_TITLE_LENGTH) },
                singleLine = true,
                label = { Text("Project name", color = colors.textSecondary, fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) confirm() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.accentCyan,
                    unfocusedBorderColor = colors.borderSubtle,
                    cursorColor = colors.accentCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("project_rename_field")
            )
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = canConfirm) {
                Text(
                    "Save",
                    color = if (canConfirm) colors.accentCyan else colors.textTertiary,
                    fontWeight = FontWeight.Bold
                )
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
