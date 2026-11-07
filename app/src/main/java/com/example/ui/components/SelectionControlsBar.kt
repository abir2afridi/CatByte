package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/**
 * Group actions shown when more than one clip is selected.
 *
 * Move/delete/copy/paste operate on the whole selection; overlay members keep
 * their relative offsets through `TimelineEdits.shiftGroup`.
 */
@Composable
fun SelectionControlsBar(
    selectedCount: Int,
    isMultiSelect: Boolean,
    canMoveOverlays: Boolean,
    canMoveMainBlock: Boolean,
    canPaste: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveBlockLeft: () -> Unit = {},
    onMoveBlockRight: () -> Unit = {},
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onCopyAttributes: () -> Unit = {},
    onPasteAttributes: () -> Unit = {},
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isMultiSelect) colors.accentCyan.copy(alpha = 0.18f) else colors.surface)
                .border(1.2.dp, colors.accentCyan, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("selection_count"),
        ) {
            Text(
                text = "$selectedCount",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }

        SelectionButton(
            text = "◀ 100ms",
            enabled = canMoveOverlays,
            onClick = onMoveLeft,
            testTag = "selection_move_left"
        )
        SelectionButton(
            text = "100ms ▶",
            enabled = canMoveOverlays,
            onClick = onMoveRight,
            testTag = "selection_move_right"
        )
        SelectionButton(
            text = "◀ Block",
            enabled = canMoveMainBlock,
            onClick = onMoveBlockLeft,
            testTag = "selection_block_left"
        )
        SelectionButton(
            text = "Block ▶",
            enabled = canMoveMainBlock,
            onClick = onMoveBlockRight,
            testTag = "selection_block_right"
        )
        SelectionButton(
            text = "Delete",
            enabled = true,
            onClick = onDelete,
            testTag = "selection_delete"
        )
        SelectionButton(
            text = "Copy",
            enabled = true,
            onClick = onCopy,
            testTag = "selection_copy"
        )
        SelectionButton(
            text = "Paste",
            enabled = canPaste,
            onClick = onPaste,
            testTag = "selection_paste"
        )
        SelectionButton(
            text = "Copy style",
            enabled = true,
            onClick = onCopyAttributes,
            testTag = "selection_copy_attributes"
        )
        SelectionButton(
            text = "Paste style",
            enabled = true,
            onClick = onPasteAttributes,
            testTag = "selection_paste_attributes"
        )
        SelectionButton(
            text = "Clear",
            enabled = true,
            onClick = onClear,
            testTag = "selection_clear"
        )
    }
}

@Composable
private fun SelectionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) colors.surfaceElevated else colors.surface)
            .border(
                width = 1.1.dp,
                color = if (enabled) colors.border else colors.borderSubtle,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
