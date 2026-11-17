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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.components.CapCutIconCaptions
import com.example.ui.components.CapCutIconEffectsStar
import com.example.ui.components.CapCutIconFilters
import com.example.ui.components.CapCutIconOverlay

/**
 * Minimalist CapCut workspace bottom navigation bar:
 * Compact height (56.dp), sleek minimalist icon size (20.dp white), 10.sp typography.
 *
 * The bar has two levels:
 *  - the main row with the 15 workspace features;
 *  - the sub-feature row of the selected main feature, with a back action that
 *    returns to the main row.
 */
@Composable
internal fun CapCutBottomActionBar(
    activeTab: EditorMainTab?,
    onMainTabSelected: (EditorMainTab) -> Unit,
    onSubFeatureSelected: (EditorMainTab, EditorSubFeature) -> Unit,
    onBackToMain: () -> Unit
) {
    val barBackground = Color(0xFF121215)
    val tab = activeTab
    val subFeatures = tab?.subFeatures().orEmpty()

    if (tab != null && subFeatures.isNotEmpty()) {
        BottomSubFeatureRow(
            tab = tab,
            subFeatures = subFeatures,
            onSubFeatureSelected = { sub -> onSubFeatureSelected(tab, sub) },
            onBackToMain = onBackToMain,
            barBackground = barBackground
        )
        return
    }

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
            label = EditorMainTab.EDIT.label,
            tag = EditorMainTab.EDIT.tag,
            onClick = { onMainTabSelected(EditorMainTab.EDIT) }
        )

        // 2. Audio (Music Note)
        BottomActionItemVector(
            icon = Icons.Default.MusicNote,
            label = EditorMainTab.AUDIO.label,
            tag = EditorMainTab.AUDIO.tag,
            onClick = { onMainTabSelected(EditorMainTab.AUDIO) }
        )

        // 3. Text (T)
        BottomActionItemVector(
            icon = Icons.Default.TextFields,
            label = EditorMainTab.TEXT.label,
            tag = EditorMainTab.TEXT.tag,
            onClick = { onMainTabSelected(EditorMainTab.TEXT) }
        )

        // 4. Effect (Star outline)
        BottomActionItemCustom(
            label = EditorMainTab.EFFECT.label,
            tag = EditorMainTab.EFFECT.tag,
            onClick = { onMainTabSelected(EditorMainTab.EFFECT) }
        ) {
            CapCutIconEffectsStar(tint = Color.White, size = 20.dp)
        }

        // 5. Overlay (Overlapping frames)
        BottomActionItemCustom(
            label = EditorMainTab.OVERLAY.label,
            tag = EditorMainTab.OVERLAY.tag,
            onClick = { onMainTabSelected(EditorMainTab.OVERLAY) }
        ) {
            CapCutIconOverlay(tint = Color.White, size = 20.dp)
        }

        // 6. Captions (Subtitle box)
        BottomActionItemCustom(
            label = EditorMainTab.CAPTIONS.label,
            tag = EditorMainTab.CAPTIONS.tag,
            onClick = { onMainTabSelected(EditorMainTab.CAPTIONS) }
        ) {
            CapCutIconCaptions(tint = Color.White, size = 20.dp)
        }

        // 7. Filter (Venn diagram 3 circles)
        BottomActionItemCustom(
            label = EditorMainTab.FILTER.label,
            tag = EditorMainTab.FILTER.tag,
            onClick = { onMainTabSelected(EditorMainTab.FILTER) }
        ) {
            CapCutIconFilters(tint = Color.White, size = 20.dp)
        }

        // 8. Adjust
        BottomActionItemVector(
            icon = Icons.Default.Tune,
            label = EditorMainTab.ADJUST.label,
            tag = EditorMainTab.ADJUST.tag,
            onClick = { onMainTabSelected(EditorMainTab.ADJUST) }
        )

        // 9. Sticker
        BottomActionItemVector(
            icon = Icons.Default.Mood,
            label = EditorMainTab.STICKER.label,
            tag = EditorMainTab.STICKER.tag,
            onClick = { onMainTabSelected(EditorMainTab.STICKER) }
        )

        // 10. Generate media
        BottomActionItemVector(
            icon = Icons.Default.AutoFixHigh,
            label = EditorMainTab.GENERATE_MEDIA.label,
            tag = EditorMainTab.GENERATE_MEDIA.tag,
            onClick = { onMainTabSelected(EditorMainTab.GENERATE_MEDIA) }
        )

        // 11. AI Avatar
        BottomActionItemVector(
            icon = Icons.Default.Face,
            label = EditorMainTab.AI_AVATAR.label,
            tag = EditorMainTab.AI_AVATAR.tag,
            onClick = { onMainTabSelected(EditorMainTab.AI_AVATAR) }
        )

        // 12. Transcript
        BottomActionItemVector(
            icon = Icons.Default.Subtitles,
            label = EditorMainTab.TRANSCRIPT.label,
            tag = EditorMainTab.TRANSCRIPT.tag,
            onClick = { onMainTabSelected(EditorMainTab.TRANSCRIPT) }
        )

        // 13. Aspect ratio
        BottomActionItemVector(
            icon = Icons.Default.AspectRatio,
            label = EditorMainTab.ASPECT_RATIO.label,
            tag = EditorMainTab.ASPECT_RATIO.tag,
            onClick = { onMainTabSelected(EditorMainTab.ASPECT_RATIO) }
        )

        // 14. Background
        BottomActionItemVector(
            icon = Icons.Default.Wallpaper,
            label = EditorMainTab.BACKGROUND.label,
            tag = EditorMainTab.BACKGROUND.tag,
            onClick = { onMainTabSelected(EditorMainTab.BACKGROUND) }
        )

        // 15. Template
        BottomActionItemVector(
            icon = Icons.Default.Dashboard,
            label = EditorMainTab.TEMPLATE.label,
            tag = EditorMainTab.TEMPLATE.tag,
            onClick = { onMainTabSelected(EditorMainTab.TEMPLATE) }
        )
    }
}

/**
 * The second level of the bottom navigation: the sub-features of the active
 * main feature, with a back action that returns to the main row.
 */
@Composable
private fun BottomSubFeatureRow(
    tab: EditorMainTab,
    subFeatures: List<EditorSubFeature>,
    onSubFeatureSelected: (EditorSubFeature) -> Unit,
    onBackToMain: () -> Unit,
    barBackground: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(barBackground)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("bottom_sub_row_${tab.tag}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackToMain,
            modifier = Modifier
                .size(34.dp)
                .testTag("bottom_nav_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to the main features",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = tab.label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(end = 8.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subFeatures.forEach { subFeature ->
                BottomSubFeatureItem(
                    label = subFeature.label,
                    tag = "${tab.tag}_${subFeature.id}",
                    onClick = { onSubFeatureSelected(subFeature) }
                )
            }
        }
    }
}

@Composable
private fun BottomSubFeatureItem(
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    val isAi = tag.contains("ai_") || tag.contains("auto_") || tag.contains("transcript")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isAi) Color(0xFF1E262A) else Color(0xFF222329))
            .border(
                1.dp,
                if (isAi) Color(0xFF26E5D7) else Color(0xFF32343E),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("bottom_sub_$tag")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isAi) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF26E5D7))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "AI",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
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
