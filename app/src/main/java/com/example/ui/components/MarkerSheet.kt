package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimelineMarker
import com.example.ui.theme.PencilTheme

/** Colours offered for a marker pin (CapCut-style palette). */
internal val MarkerColors = listOf(
    0xFFFFC94A, // amber
    0xFF26E5D7, // cyan
    0xFFFF6B6B, // red
    0xFF7C5CFF, // violet
    0xFF4CAF50  // green
)

/**
 * Timeline markers sheet: add at the playhead, jump, rename, recolour, delete.
 * The ruler already draws the pins and snapping already uses them; this is the
 * missing management UI.
 */
@Composable
fun MarkerSheet(
    markers: List<TimelineMarker>,
    onAddAtPlayhead: () -> Unit,
    onJump: (String) -> Unit,
    onMoveToPlayhead: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onColor: (String, Long) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 420.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(
                1.dp,
                colors.borderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("marker_sheet")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Markers (${markers.size})",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentCyan)
                        .clickable { onAddAtPlayhead() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("marker_add_at_playhead"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Done",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("marker_sheet_close")
                )
            }
        }

        if (markers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No markers yet",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Park the playhead and tap Add - markers snap, jump and show on the ruler.",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(markers.sortedBy { it.atMs }, key = { it.id }) { marker ->
                MarkerRow(
                    marker = marker,
                    onJump = { onJump(marker.id) },
                    onMoveToPlayhead = { onMoveToPlayhead(marker.id) },
                    onRename = { onRename(marker.id, it) },
                    onColor = { onColor(marker.id, it) },
                    onDelete = { onDelete(marker.id) }
                )
            }
        }
    }
}

@Composable
private fun MarkerRow(
    marker: TimelineMarker,
    onJump: () -> Unit,
    onMoveToPlayhead: () -> Unit,
    onRename: (String) -> Unit,
    onColor: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val colors = PencilTheme.colors
    val focusManager = LocalFocusManager.current
    var draft by remember(marker.id) { mutableStateOf(marker.label) }
    var committedLabel by remember(marker.id) { mutableStateOf(marker.label) }
    var isFocused by remember(marker.id) { mutableStateOf(false) }

    LaunchedEffect(marker.label) {
        if (!isFocused) {
            draft = marker.label
            committedLabel = marker.label
        }
    }

    fun commitDraft() {
        val next = draft.trim().take(24)
        draft = next
        if (next != committedLabel) {
            committedLabel = next
            onRename(next)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pin + time
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(marker.colorArgb))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatMarkerTime(marker.atMs),
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(24) },
            singleLine = true,
            placeholder = { Text("Label", fontSize = 11.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    commitDraft()
                    focusManager.clearFocus()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (!state.isFocused) commitDraft()
                }
                .testTag("marker_label_${marker.id}")
        )

        // Colour swatches
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MarkerColors.forEach { argb ->
                val selected = marker.colorArgb == argb
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = if (selected) colors.textPrimary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColor(argb) }
                )
            }
        }

        IconButton(
            onClick = onMoveToPlayhead,
            modifier = Modifier
                .size(32.dp)
                .testTag("marker_move_to_playhead_${marker.id}")
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Move marker to playhead",
                tint = colors.textSecondary,
                modifier = Modifier.size(17.dp)
            )
        }
        IconButton(onClick = onJump, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Jump to marker",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete marker",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** mm:ss.S timecode used by the marker rows. */
internal fun formatMarkerTime(atMs: Long): String {
    val safe = atMs.coerceAtLeast(0L)
    val totalSeconds = safe / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (safe % 1000) / 100
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}
