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
fun ChromaKeySheet(
    chromaData: com.example.model.ChromaKeyData,
    isEyedropperActive: Boolean = false,
    onToggleEyedropper: () -> Unit = {},
    onUpdateChroma: (com.example.model.ChromaKeyData) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var current by remember(chromaData) { mutableStateOf(chromaData) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .border(1.2.dp, colors.borderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(16.dp)
            .testTag("chroma_key_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🟢", fontSize = 18.sp)
                Text(
                    text = "Chroma Key (Cutout)",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Chroma Key", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            androidx.compose.material3.Switch(
                checked = current.isEnabled,
                onCheckedChange = {
                    current = current.copy(isEnabled = it)
                    onUpdateChroma(current)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Key Color", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val presets = listOf(
                "Green" to 0xFF00FF00,
                "Blue" to 0xFF0000FF,
                "Cyan" to 0xFF00FFFF,
                "Magenta" to 0xFFFF00FF,
                "Black" to 0xFF000000
            )
            presets.forEach { (name, hex) ->
                val isSel = current.keyColorHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(if (isSel) 3.dp else 1.dp, if (isSel) colors.accentCyan else colors.border, CircleShape)
                        .clickable {
                            current = current.copy(keyColorHex = hex, isEnabled = true)
                            onUpdateChroma(current)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isEyedropperActive) colors.accentCyan else colors.surfaceVariant)
                .border(1.2.dp, if (isEyedropperActive) colors.accentCyan else colors.border, RoundedCornerShape(8.dp))
                .clickable { onToggleEyedropper() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🎯", fontSize = 13.sp)
                Text(
                    text = if (isEyedropperActive) "Color Picker Active (Tap Preview)" else "Eyedropper (Pick Color on Preview)",
                    color = if (isEyedropperActive) Color.Black else colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (isEyedropperActive) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap or drag the reticle on the preview canvas above to pick the key color.",
                color = colors.accentCyan,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("Tolerance / Intensity: ${(current.tolerance * 100).toInt()}%", color = colors.textSecondary, fontSize = 11.sp)
        androidx.compose.material3.Slider(
            value = current.tolerance,
            onValueChange = {
                current = current.copy(tolerance = it, isEnabled = true)
                onUpdateChroma(current)
            },
            valueRange = 0f..1f
        )

        Text("Softness / Edge: ${(current.softness * 100).toInt()}%", color = colors.textSecondary, fontSize = 11.sp)
        androidx.compose.material3.Slider(
            value = current.softness,
            onValueChange = {
                current = current.copy(softness = it, isEnabled = true)
                onUpdateChroma(current)
            },
            valueRange = 0f..1f
        )

        Text("Spill Suppression: ${(current.spill * 100).toInt()}%", color = colors.textSecondary, fontSize = 11.sp)
        androidx.compose.material3.Slider(
            value = current.spill,
            onValueChange = {
                current = current.copy(spill = it, isEnabled = true)
                onUpdateChroma(current)
            },
            valueRange = 0f..1f
        )
    }
}
