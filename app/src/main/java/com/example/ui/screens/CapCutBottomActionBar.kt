package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/**
 * CapCut bottom navigation. Order follows the reference walkthrough:
 * Edit, Audio, Text, Effects, Overlay, Captions, Stickers, Filters, Adjust,
 * Ratio, Background. A final Tools entry keeps our extra timeline utilities
 * (markers, beat sync, multi-select, snap, zoom) reachable without changing the
 * reference layout.
 */
@Composable
internal fun CapCutBottomActionBar(
    onEdit: () -> Unit,
    onAudio: () -> Unit,
    onText: () -> Unit,
    onEffects: () -> Unit,
    onOverlay: () -> Unit,
    onCaptions: () -> Unit,
    onStickers: () -> Unit,
    onFilters: () -> Unit,
    onAdjust: () -> Unit,
    onRatio: () -> Unit,
    onBackground: () -> Unit,
    onTools: () -> Unit
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.surface)
            .border(width = 0.8.dp, color = colors.borderSubtle.copy(alpha = 0.5f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomActionItem(Icons.Default.ContentCut, "Edit", "edit", onEdit)
        BottomActionItem(Icons.Default.MusicNote, "Audio", "audio", onAudio)
        BottomActionItem(Icons.Default.TextFields, "Text", "text", onText)
        BottomActionItem(Icons.Default.AutoAwesome, "Effects", "effects", onEffects)
        BottomActionItem(Icons.Default.Layers, "Overlay", "overlay", onOverlay)
        BottomActionItem(Icons.Default.ClosedCaption, "Captions", "captions", onCaptions)
        BottomActionItem(Icons.Default.Mood, "Stickers", "stickers", onStickers)
        BottomActionItem(Icons.Default.Filter, "Filters", "filters", onFilters)
        BottomActionItem(Icons.Default.Tune, "Adjust", "adjust", onAdjust)
        BottomActionItem(Icons.Default.AspectRatio, "Ratio", "ratio", onRatio)
        BottomActionItem(Icons.Default.Wallpaper, "Background", "background", onBackground)
        BottomActionItem(Icons.Default.Build, "Tools", "tools", onTools)
    }
}

@Composable
internal fun BottomActionItem(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("bottom_action_$tag")
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.textPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
