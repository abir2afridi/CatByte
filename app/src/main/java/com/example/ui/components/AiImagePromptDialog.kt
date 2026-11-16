package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** Prompt dialog for the endpoint-backed AI image generator. */
@Composable
fun AiImagePromptDialog(
    defaultPrompt: String,
    onGenerate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PencilTheme.colors
    var prompt by remember { mutableStateOf(defaultPrompt) }
    val valid = prompt.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("AI image", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt", fontSize = 12.sp) },
                placeholder = { Text("cinematic night sky, vertical background", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.accentCyan,
                    unfocusedBorderColor = colors.borderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_image_prompt")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(prompt.trim()) },
                enabled = valid
            ) {
                Text(
                    "Generate",
                    color = if (valid) colors.accentCyan else colors.textTertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textPrimary)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .background(colors.surface)
            .border(2.dp, colors.border, RoundedCornerShape(16.dp))
    )
}
