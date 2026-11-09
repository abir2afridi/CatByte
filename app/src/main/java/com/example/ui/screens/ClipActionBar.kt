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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Context action bar shown while a clip is selected, matching the CapCut
 * reference: back chevron, then Split, Volume, Animations, Effects, Delete,
 * Speed, Beats, Crop, Duplicate, Replace, Overlay, Adjust, Stickers, Generate
 * media, AI avatar, Aspect ratio and Background.
 *
 * Every entry is a real callback. Cloud-only AI features live behind the
 * "AI Tools" entry so they can explain why they need an online model instead of
 * pretending to work offline.
 */
@Composable
internal fun ClipActionBar(
    onBack: () -> Unit,
    onSplit: () -> Unit,
    onVolume: () -> Unit,
    onAnimations: () -> Unit,
    onEffects: () -> Unit,
    onDelete: () -> Unit,
    onSpeed: () -> Unit,
    onBeats: () -> Unit,
    onCrop: () -> Unit,
    onDuplicate: () -> Unit,
    onReplace: () -> Unit,
    onOverlay: () -> Unit,
    onAdjust: () -> Unit,
    onStickers: () -> Unit,
    onAspectRatio: () -> Unit,
    onBackground: () -> Unit,
    onOpacity: () -> Unit,
    onFreeze: () -> Unit,
    onReverse: () -> Unit,
    onExtractAudio: () -> Unit,
    onMore: () -> Unit,
    onAiTools: () -> Unit
) {
    // The editor shell is intentionally dark regardless of the app theme, so
    // this bar uses the same studio palette as CapCutBottomActionBar instead of
    // PencilTheme (which is white in light mode).
    val barBackground = Color(0xFF121215)
    val barBorder = Color(0xFF262830)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(barBackground)
            .border(width = 0.8.dp, color = barBorder)
            .testTag("clip_action_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .width(38.dp)
                .height(44.dp)
                .testTag("clip_action_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to tools",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClipActionItem(Icons.Default.ContentCut, "Split", "split", onSplit)
            ClipActionItem(Icons.Default.VolumeUp, "Volume", "volume", onVolume)
            ClipActionItem(Icons.Default.AutoAwesome, "Animations", "animations", onAnimations)
            ClipActionItem(Icons.Default.FilterCenterFocus, "Effects", "effects", onEffects)
            ClipActionItem(Icons.Default.Delete, "Delete", "delete", onDelete)
            ClipActionItem(Icons.Default.Speed, "Speed", "speed", onSpeed)
            ClipActionItem(Icons.Default.MusicNote, "Beats", "beats", onBeats)
            ClipActionItem(Icons.Default.ContentCut, "Crop", "crop", onCrop)
            ClipActionItem(Icons.Default.ContentCopy, "Duplicate", "duplicate", onDuplicate)
            ClipActionItem(Icons.Default.SwapHoriz, "Replace", "replace", onReplace)
            ClipActionItem(Icons.Default.Layers, "Overlay", "overlay", onOverlay)
            ClipActionItem(Icons.Default.Tune, "Adjust", "adjust", onAdjust)
            ClipActionItem(Icons.Default.Palette, "Stickers", "stickers", onStickers)
            ClipActionItem(Icons.Default.Extension, "AI Tools", "ai_tools", onAiTools)
            ClipActionItem(Icons.Default.AspectRatio, "Ratio", "ratio", onAspectRatio)
            ClipActionItem(Icons.Default.Wallpaper, "Background", "background", onBackground)
            ClipActionItem(Icons.Default.Opacity, "Opacity", "opacity", onOpacity)
            ClipActionItem(Icons.Default.AcUnit, "Freeze", "freeze", onFreeze)
            ClipActionItem(Icons.Default.Replay, "Reverse", "reverse", onReverse)
            ClipActionItem(Icons.Default.GraphicEq, "Extract audio", "extract_audio", onExtractAudio)
            ClipActionItem(Icons.Default.MoreHoriz, "More", "more", onMore)
        }
    }
}

@Composable
private fun ClipActionItem(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("clip_action_$tag")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color(0xFFA1A5B0),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
