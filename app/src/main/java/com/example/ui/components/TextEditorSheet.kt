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
    var textInput by remember { mutableStateOf(textOverlay?.text ?: "Viral TikTok Text 🔥") }
    var fontStyle by remember { mutableStateOf(textOverlay?.fontStyle ?: TextFontStyle.CAPCUT_BOLD) }
    var selectedColor by remember { mutableLongStateOf(textOverlay?.color ?: 0xFFFFFFFF) }
    var animation by remember { mutableStateOf(textOverlay?.animation ?: TextAnimationType.POP_UP) }

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("text_editor_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add & Style Text", color = CapCutTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onAutoCaptions != null) {
                        Button(
                            onClick = { onAutoCaptions.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentYellow),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Auto Captions 🤖", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (textOverlay != null) {
                        IconButton(onClick = onDeleteText) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CapCutRed)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
                    }
                }
            }

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Enter text...", color = CapCutTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CapCutCyan,
                unfocusedBorderColor = CapCutBorder,
                focusedTextColor = CapCutTextPrimary,
                unfocusedTextColor = CapCutTextPrimary,
                focusedContainerColor = CapCutSurfaceElevated,
                unfocusedContainerColor = CapCutSurfaceElevated
            )
        )

        if (onImportCaptions != null || onExportCaptions != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onImportCaptions != null) {
                    Button(
                        onClick = { onImportCaptions.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentYellow),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("Import SRT / VTT 📥", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                if (onExportCaptions != null) {
                    OutlinedButton(
                        onClick = { onExportCaptions.invoke() },
                        enabled = captionExportCount > 0,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (captionExportCount > 0) CapCutCyan else CapCutBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text(
                            if (captionExportCount > 0) "Export SRT ($captionExportCount)" else "Export SRT",
                            fontSize = 11.sp,
                            color = if (captionExportCount > 0) CapCutCyan else CapCutTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Font Style", color = CapCutTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(TextFontStyle.values()) { style ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (fontStyle == style) CapCutCyan else CapCutSurfaceElevated)
                        .clickable { fontStyle = style }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = style.displayName,
                        color = if (fontStyle == style) Color.Black else CapCutTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Color", color = CapCutTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            listOf(0xFFFFFFFF, 0xFFFFD166, 0xFF00E5FF, 0xFFFF3358, 0xFFFF006E, 0xFF06D6A0, 0xFF000000).forEach { colorLong ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(
                            width = if (selectedColor == colorLong) 2.dp else 1.dp,
                            color = if (selectedColor == colorLong) CapCutCyan else CapCutBorder,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = colorLong }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Animation", color = CapCutTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(TextAnimationType.values()) { anim ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (animation == anim) CapCutYellow else CapCutSurfaceElevated)
                        .clickable { animation = anim }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = anim.displayName,
                        color = if (animation == anim) Color.Black else CapCutTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                onAddOrUpdateText(textInput, fontStyle, selectedColor, animation)
                onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply to Timeline", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
}
