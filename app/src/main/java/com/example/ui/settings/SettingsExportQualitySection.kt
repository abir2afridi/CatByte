package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PencilTheme

/** Default export quality: resolution and frame-rate pickers. */
@Composable
internal fun SettingsExportQualitySection(
    defaultResolution: String,
    defaultFps: Int,
    onSelectResolution: (String) -> Unit,
    onSelectFps: (Int) -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.default_export_quality_caps),
        icon = Icons.Outlined.Videocam,
        iconTint = colors.accentCyan
    ) {
        Text(stringResource(R.string.default_resolution), color = colors.textSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("720P", "1080P", "4K").forEach { res ->
                val isSel = defaultResolution == res
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                        .border(1.dp, if (isSel) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable { onSelectResolution(res) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = res,
                        color = if (isSel) Color.White else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.default_frame_rate), color = colors.textSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val fpsLabels = listOf(
                24 to stringResource(R.string.fps_label, 24),
                30 to stringResource(R.string.fps_label, 30),
                60 to stringResource(R.string.fps_label, 60)
            )
            fpsLabels.forEach { (fps, label) ->
                val isSel = defaultFps == fps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                        .border(1.dp, if (isSel) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable { onSelectFps(fps) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSel) Color.White else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
