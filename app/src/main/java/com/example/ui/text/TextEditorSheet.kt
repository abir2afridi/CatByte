package com.example.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.ui.theme.PencilTheme

/**
 * The text overlay editor sheet.
 *
 * This file owns the sheet chrome (header, text field, apply button); the
 * pickers live in their own files under `ui/text/`:
 *  - `TextStyleRow.kt`: the font-style chips.
 *  - `TextColorRow.kt`: the colour presets.
 */
@Composable
fun TextEditorSheet(
    textOverlay: TextOverlay?,
    onAddOrUpdateText: (text: String, fontStyle: TextFontStyle, color: Long, anim: TextAnimationType) -> Unit,
    onDeleteText: () -> Unit,
    onAutoCaptions: (() -> Unit)? = null,
    onImportCaptions: (() -> Unit)? = null,
    onExportCaptions: (() -> Unit)? = null,
    captionExportCount: Int = 0,
    onClose: () -> Unit
) {
    var textInput by remember { mutableStateOf(textOverlay?.text ?: "Text Overlay") }
    var fontStyle by remember { mutableStateOf(textOverlay?.fontStyle ?: TextFontStyle.CAPCUT_BOLD) }
    var selectedColor by remember { mutableLongStateOf(textOverlay?.color ?: 0xFFFFFFFF) }
    var animation by remember { mutableStateOf(textOverlay?.animation ?: TextAnimationType.POP_UP) }

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("text_editor_sheet")
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textTertiary.copy(alpha = 0.4f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (textOverlay != null) "Edit Text" else "Add Text",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onAutoCaptions != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.accentYellow.copy(alpha = 0.2f))
                                .border(1.dp, colors.accentYellow, RoundedCornerShape(6.dp))
                                .clickable { onAutoCaptions.invoke() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🤖 Captions", fontSize = 10.sp, color = colors.accentYellow, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onImportCaptions != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                                .clickable { onImportCaptions.invoke() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📥 Import", fontSize = 10.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onExportCaptions != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                                .clickable { onExportCaptions.invoke() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📤 SRT ($captionExportCount)", fontSize = 10.sp, color = colors.accentCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (textOverlay != null) {
                        IconButton(onClick = onDeleteText, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Enter text...", color = colors.textTertiary, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accentCyan,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextStyleRow(
                selectedStyle = fontStyle,
                onSelectStyle = { fontStyle = it }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextColorRow(
                    selectedColor = selectedColor,
                    onSelectColor = { selectedColor = it }
                )

                // Apply button
                Button(
                    onClick = {
                        onAddOrUpdateText(textInput, fontStyle, selectedColor, animation)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Apply", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
