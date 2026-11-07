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
fun TextEditorSheet(
    textOverlay: TextOverlay?,
    onAddOrUpdateText: (text: String, fontStyle: TextFontStyle, color: Long, anim: TextAnimationType) -> Unit,
    onDeleteText: () -> Unit,
    onAutoCaptions: (() -> Unit)? = null,
    onImportCaptions: (() -> Unit)? = null,
    onExportCaptions: (() -> Unit)? = null,
    captionExportCount: Int = 0,
    onClose: () -> Unit
) {
    var textInput by remember { mutableStateOf(textOverlay?.text ?: "Text Overlay") }
    var fontStyle by remember { mutableStateOf(textOverlay?.fontStyle ?: TextFontStyle.CAPCUT_BOLD) }
    var selectedColor by remember { mutableLongStateOf(textOverlay?.color ?: 0xFFFFFFFF) }
    var animation by remember { mutableStateOf(textOverlay?.animation ?: TextAnimationType.POP_UP) }

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Font styles
                Text("Style:", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                TextFontStyle.values().forEach { style ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (fontStyle == style) colors.accentCyan.copy(alpha = 0.2f) else colors.surfaceElevated)
                            .border(1.dp, if (fontStyle == style) colors.accentCyan else colors.borderSubtle, RoundedCornerShape(6.dp))
                            .clickable { fontStyle = style }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = style.displayName,
                            color = if (fontStyle == style) colors.accentCyan else colors.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0xFFFFFFFF, 0xFFFFD166, 0xFF00E5FF, 0xFFFF3358, 0xFF06D6A0, 0xFF000000).forEach { colorLong ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (selectedColor == colorLong) 2.dp else 1.dp,
                                    color = if (selectedColor == colorLong) colors.accentCyan else colors.borderSubtle,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorLong }
                        )
                    }
                }

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
