package com.example.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.KeyframeProperty
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
    onUpdateTextStyle: ((bold: Boolean, italic: Boolean, letterSpacingEm: Float, lineSpacingMultiplier: Float) -> Unit)? = null,
    playheadMs: Long = 0L,
    onSetTextKeyframe: ((KeyframeProperty, Float) -> Boolean)? = null,
    onRemoveTextKeyframe: ((String) -> Unit)? = null,
    onClearTextKeyframes: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    var textInput by remember { mutableStateOf(textOverlay?.text ?: "Text Overlay") }
    var fontStyle by remember { mutableStateOf(textOverlay?.fontStyle ?: TextFontStyle.CAPCUT_BOLD) }
    var selectedColor by remember { mutableLongStateOf(textOverlay?.color ?: 0xFFFFFFFF) }
    var animation by remember { mutableStateOf(textOverlay?.animation ?: TextAnimationType.POP_UP) }
    var isBold by remember { mutableStateOf(textOverlay?.isBold ?: false) }
    var isItalic by remember { mutableStateOf(textOverlay?.isItalic ?: false) }
    var letterSpacing by remember { mutableStateOf(textOverlay?.letterSpacingEm ?: 0f) }
    var lineSpacing by remember { mutableStateOf(textOverlay?.lineSpacingMultiplier ?: 1f) }
    var keyProperty by remember { mutableStateOf(KeyframeProperty.SCALE) }
    var keyValue by remember { mutableFloatStateOf(textOverlay?.scale ?: 1f) }

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
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

            if (onUpdateTextStyle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyleToggle("B", isBold) {
                        isBold = !isBold
                        onUpdateTextStyle.invoke(isBold, isItalic, letterSpacing, lineSpacing)
                    }
                    StyleToggle("I", isItalic) {
                        isItalic = !isItalic
                        onUpdateTextStyle.invoke(isBold, isItalic, letterSpacing, lineSpacing)
                    }
                    Text("A↔", color = colors.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = letterSpacing,
                        onValueChange = {
                            letterSpacing = it
                            onUpdateTextStyle.invoke(isBold, isItalic, letterSpacing, lineSpacing)
                        },
                        valueRange = 0f..0.3f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("↕", color = colors.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = lineSpacing,
                        onValueChange = {
                            lineSpacing = it
                            onUpdateTextStyle.invoke(isBold, isItalic, letterSpacing, lineSpacing)
                        },
                        valueRange = 0.8f..2.0f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (textOverlay != null && onSetTextKeyframe != null) {
                TextKeyframeRow(
                    overlay = textOverlay,
                    playheadMs = playheadMs,
                    selectedProperty = keyProperty,
                    value = keyValue,
                    onSelectProperty = { property ->
                        keyProperty = property
                        keyValue = baseValueFor(textOverlay, property)
                    },
                    onValueChange = { keyValue = it },
                    onAddKeyframe = { onSetTextKeyframe.invoke(keyProperty, keyValue) },
                    onRemoveKeyframe = onRemoveTextKeyframe,
                    onClearKeyframes = onClearTextKeyframes
                )
            }

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

/** Compact "B" / "I" toggle chip used by the text style row. */
@Composable
private fun StyleToggle(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) colors.accentCyan else colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (label == "B") FontWeight.Black else FontWeight.Medium,
            fontStyle = if (label == "I") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
        )
    }
}

/** Base property value of [overlay], used when the keyframe property changes. */
private fun baseValueFor(overlay: TextOverlay, property: KeyframeProperty): Float = when (property) {
    KeyframeProperty.POSITION_X -> overlay.posX
    KeyframeProperty.POSITION_Y -> overlay.posY
    KeyframeProperty.SCALE -> overlay.scale
    KeyframeProperty.OPACITY -> 1f
    else -> 1f
}

private fun keyframeRange(property: KeyframeProperty): ClosedFloatingPointRange<Float> = when (property) {
    KeyframeProperty.SCALE -> 0.2f..4f
    else -> 0f..1f
}

private fun keyframeShortLabel(property: KeyframeProperty): String = when (property) {
    KeyframeProperty.POSITION_X -> "X"
    KeyframeProperty.POSITION_Y -> "Y"
    KeyframeProperty.SCALE -> "Size"
    KeyframeProperty.OPACITY -> "Fade"
    else -> property.displayName
}

/**
 * Keyframe authoring for the selected text: pick a property, set its value and
 * add the key at the playhead. Existing keys are listed with their local time
 * and can be deleted one by one (or all at once).
 */
@Composable
private fun TextKeyframeRow(
    overlay: TextOverlay,
    playheadMs: Long,
    selectedProperty: KeyframeProperty,
    value: Float,
    onSelectProperty: (KeyframeProperty) -> Unit,
    onValueChange: (Float) -> Unit,
    onAddKeyframe: () -> Unit,
    onRemoveKeyframe: ((String) -> Unit)?,
    onClearKeyframes: (() -> Unit)?
) {
    val colors = PencilTheme.colors
    val localMs = playheadMs - overlay.startTimelineMs
    val inside = localMs in 0..overlay.durationMs
    val properties = listOf(
        KeyframeProperty.POSITION_X,
        KeyframeProperty.POSITION_Y,
        KeyframeProperty.SCALE,
        KeyframeProperty.OPACITY
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Keyframes",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (inside) String.format("%.1fs", localMs / 1000f) else "Move the playhead over this text",
                color = if (inside) colors.accentCyan else colors.textTertiary,
                fontSize = 10.sp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            properties.forEach { property ->
                val active = property == selectedProperty
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) colors.accentCyan else colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onSelectProperty(property) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        keyframeShortLabel(property),
                        color = if (active) Color.Black else colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Slider(
                value = value.coerceIn(keyframeRange(selectedProperty)),
                onValueChange = onValueChange,
                valueRange = keyframeRange(selectedProperty),
                modifier = Modifier.width(150.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (inside) colors.accentCyan.copy(alpha = 0.2f) else colors.surfaceElevated)
                    .border(1.dp, if (inside) colors.accentCyan else colors.borderSubtle, RoundedCornerShape(6.dp))
                    .clickable(enabled = inside) { onAddKeyframe() }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    "+ Key",
                    color = if (inside) colors.accentCyan else colors.textTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (overlay.keyframes.isEmpty()) {
            Text(
                "Add one key, move the playhead, then add another to animate.",
                color = colors.textTertiary,
                fontSize = 9.sp
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                overlay.keyframes.sortedBy { it.timestampMs }.forEach { keyframe ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${keyframeShortLabel(keyframe.property)} ${String.format("%.1fs", keyframe.timestampMs / 1000f)}",
                            color = colors.textPrimary,
                            fontSize = 9.sp
                        )
                        if (onRemoveKeyframe != null) {
                            IconButton(
                                onClick = { onRemoveKeyframe.invoke(keyframe.id) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete keyframe",
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                if (onClearKeyframes != null && overlay.keyframes.size > 1) {
                    Text(
                        "Clear all",
                        color = colors.accentRed,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClearKeyframes.invoke() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
