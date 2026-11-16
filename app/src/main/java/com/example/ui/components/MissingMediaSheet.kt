package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MissingMediaRef
import com.example.ui.theme.PencilTheme

/**
 * Amber strip shown above the preview when one or more project media files can
 * no longer be opened. Tapping it opens the relink sheet.
 */
@Composable
fun MissingMediaBanner(
    missing: List<MissingMediaRef>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (missing.isEmpty()) return
    val colors = PencilTheme.colors
    val count = missing.size
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF4A3A12))
            .border(1.dp, Color(0xFFFFC94A).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("missing_media_banner"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFFFC94A),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (count == 1) "1 media file is missing" else "$count media files are missing",
            color = Color(0xFFFFE7A8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Relink",
            color = colors.accentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Bottom sheet that lists every missing media file and lets the user pick a
 * replacement from storage. The replacement keeps the clip's edits.
 */
@Composable
fun MissingMediaSheet(
    missing: List<MissingMediaRef>,
    onRelink: (clipId: String, uriString: String) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var pendingClipId by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val clipId = pendingClipId
        pendingClipId = null
        if (uri != null && clipId != null) {
            onRelink(clipId, uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 420.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(
                1.dp,
                colors.borderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("missing_media_sheet")
    ) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Missing media",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Close",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            "These clips point at files the app can no longer open. Pick a replacement; trims, speed and effects are kept.",
            color = colors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        ) {
            items(missing, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isAudio) Icons.Default.MusicNote else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title.ifBlank { "Untitled clip" },
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.uriString,
                            color = colors.textTertiary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Relink",
                        color = colors.accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                pendingClipId = item.id
                                picker.launch(
                                    if (item.isAudio) {
                                        arrayOf("audio/*")
                                    } else {
                                        arrayOf("video/*", "image/*")
                                    }
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("relink_${item.id}")
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}
