package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CapCutIconCaptions
import com.example.ui.components.CapCutIconEffectsStar
import com.example.ui.components.CapCutIconFilters
import com.example.ui.components.CapCutIconOverlay

/**
 * Minimalist CapCut workspace bottom navigation bar:
 * Compact height (56.dp), sleek minimalist icon size (20.dp white), 10.sp typography.
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
    val barBackground = Color(0xFF121215)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(barBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Edit (Scissors)
        BottomActionItemVector(
            icon = Icons.Default.ContentCut,
            label = "Edit",
            tag = "edit",
            onClick = onEdit
        )

        // 2. Audio (Music Note)
        BottomActionItemVector(
            icon = Icons.Default.MusicNote,
            label = "Audio",
            tag = "audio",
            onClick = onAudio
        )

        // 3. Text (T)
        BottomActionItemVector(
            icon = Icons.Default.TextFields,
            label = "Text",
            tag = "text",
            onClick = onText
        )

        // 4. Effects (Star outline)
        BottomActionItemCustom(
            label = "Effects",
            tag = "effects",
            onClick = onEffects
        ) {
            CapCutIconEffectsStar(tint = Color.White, size = 20.dp)
        }

        // 5. Overlay (Overlapping frames)
        BottomActionItemCustom(
            label = "Overlay",
            tag = "overlay",
            onClick = onOverlay
        ) {
            CapCutIconOverlay(tint = Color.White, size = 20.dp)
        }

        // 6. Captions (Subtitle box)
        BottomActionItemCustom(
            label = "Captions",
            tag = "captions",
            onClick = onCaptions
        ) {
            CapCutIconCaptions(tint = Color.White, size = 20.dp)
        }

        // 7. Filters (Venn diagram 3 circles)
        BottomActionItemCustom(
            label = "Filters",
            tag = "filters",
            onClick = onFilters
        ) {
            CapCutIconFilters(tint = Color.White, size = 20.dp)
        }

        // Additional tools when scrolled
        BottomActionItemVector(
            icon = Icons.Default.Tune,
            label = "Adjust",
            tag = "adjust",
            onClick = onAdjust
        )

        BottomActionItemVector(
            icon = Icons.Default.AspectRatio,
            label = "Ratio",
            tag = "ratio",
            onClick = onRatio
        )

        BottomActionItemVector(
            icon = Icons.Default.Wallpaper,
            label = "Background",
            tag = "background",
            onClick = onBackground
        )

        BottomActionItemVector(
            icon = Icons.Default.Build,
            label = "Tools",
            tag = "tools",
            onClick = onTools
        )
    }
}

@Composable
internal fun BottomActionItemVector(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    BottomActionItemCustom(
        label = label,
        tag = tag,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun BottomActionItemCustom(
    label: String,
    tag: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("bottom_action_$tag")
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
