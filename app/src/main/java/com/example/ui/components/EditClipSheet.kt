package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SampleDataProvider
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.ui.theme.CapCutBorder
import com.example.ui.theme.CapCutBorderSubtle
import com.example.ui.theme.CapCutCyan
import com.example.ui.theme.CapCutDarkBackground
import com.example.ui.theme.CapCutGreen
import com.example.ui.theme.CapCutPink
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutRed
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceElevated
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.ui.theme.CapCutYellow
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip
import com.example.ui.theme.TornPaperEdge
import com.example.ui.theme.WashiTapeCorner
import com.example.ui.theme.WashiTapeSticker
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder
import com.example.viewmodel.ExportUiState

@Composable
fun EditClipSheet(
    clip: MediaClip?,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRotate: () -> Unit,
    onMirror: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenAdjust: () -> Unit,
    onOpenKeyframe: () -> Unit = {},
    onOpenBlendMode: () -> Unit = {},
    onOpenMask: () -> Unit = {},
    onExtractAudio: () -> Unit = {},
    onFreeze: () -> Unit = {},
    onReverse: () -> Unit = {},
    onOpenVolume: () -> Unit = {},
    onOpenChroma: () -> Unit = {},
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(CapCutSurface)
            .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp)
            .testTag("edit_clip_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Clip: ${clip?.title ?: "Select a clip"}",
                color = CapCutTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = Icons.Default.ContentCut,
                label = "Split",
                accentColor = CapCutCyan,
                testTag = "action_split_button",
                onClick = onSplit
            )
            ToolButton(
                icon = Icons.Default.MusicNote,
                label = "Extract Audio",
                accentColor = CapCutYellow,
                testTag = "action_extract_audio_button",
                onClick = onExtractAudio
            )
            ToolButton(
                icon = Icons.Default.Stop,
                label = "Freeze",
                accentColor = CapCutCyan,
                testTag = "action_freeze_button",
                onClick = onFreeze
            )
            ToolButton(
                icon = Icons.Default.FastForward,
                label = "Reverse",
                accentColor = CapCutPink,
                testTag = "action_reverse_button",
                onClick = onReverse
            )
            ToolButton(
                icon = Icons.Default.VolumeUp,
                label = "Volume",
                accentColor = CapCutGreen,
                testTag = "action_volume_button",
                onClick = onOpenVolume
            )
            ToolButton(
                icon = Icons.Default.AutoAwesome,
                label = "Chroma Key",
                accentColor = CapCutCyan,
                testTag = "action_chroma_button",
                onClick = onOpenChroma
            )
            ToolButton(
                icon = Icons.Default.Tune,
                label = "Mirror",
                accentColor = Color.White,
                testTag = "action_mirror_button",
                onClick = onMirror
            )
            ToolButton(
                icon = Icons.Default.AutoAwesome,
                label = "Keyframe",
                accentColor = CapCutPink,
                testTag = "action_keyframe_button",
                onClick = onOpenKeyframe
            )
            ToolButton(
                icon = Icons.Default.Tune,
                label = "Blend",
                accentColor = CapCutPurple,
                testTag = "action_blend_button",
                onClick = onOpenBlendMode
            )
            ToolButton(
                icon = Icons.Default.Speed,
                label = "Mask",
                accentColor = CapCutGreen,
                testTag = "action_mask_button",
                onClick = onOpenMask
            )
            ToolButton(
                icon = Icons.Default.Speed,
                label = "Speed",
                accentColor = Color.Yellow,
                testTag = "action_speed_button",
                onClick = onOpenSpeed
            )
            ToolButton(
                icon = Icons.Default.Tune,
                label = "Adjust",
                accentColor = CapCutGreen,
                testTag = "action_adjust_button",
                onClick = onOpenAdjust
            )
            ToolButton(
                icon = Icons.Default.RotateRight,
                label = "Rotate",
                accentColor = Color.White,
                testTag = "action_rotate_button",
                onClick = onRotate
            )
            ToolButton(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate",
                accentColor = Color.White,
                testTag = "action_duplicate_button",
                onClick = onDuplicate
            )
            ToolButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                accentColor = CapCutRed,
                testTag = "action_delete_button",
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .pencilHatching(color = accentColor.copy(alpha = 0.08f), spacing = 5.dp, enabled = colors.isPencilSketch)
                .sketchPencilBorder(strokeWidth = 1.3.dp, color = colors.border, cornerRadius = 22.dp, isPencilStyle = colors.isPencilSketch),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
