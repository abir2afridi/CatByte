package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CapCutMeadowCanvas

/**
 * The watermark sub-panel: preview canvas, Keep/Remove choice and the
 * four-corner position grid. The watermark itself is burned into the export
 * by `TimelineCanvasOverlay` (com.example.data.export) using this position.
 */
@Composable
internal fun ExportWatermarkPanel(
    watermarkEnabled: Boolean,
    watermarkPosition: String,
    onToggleWatermark: (Boolean) -> Unit,
    onUpdatePosition: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Watermark", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1E24)),
            contentAlignment = Alignment.Center
        ) {
            CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

            val alignment = when (watermarkPosition) {
                "Upper left" -> Alignment.TopStart
                "Upper right" -> Alignment.TopEnd
                "Lower left" -> Alignment.BottomStart
                else -> Alignment.BottomEnd
            }
            Text(
                text = "CapCut",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(alignment)
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Watermark: CapCut", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onToggleWatermark(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (watermarkEnabled) Color(0xFF2A2B34) else Color(0xFF1E2026)
                ),
                border = if (watermarkEnabled) {
                    androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                } else {
                    null
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Text("Keep", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onToggleWatermark(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!watermarkEnabled) Color(0xFF2A2B34) else Color(0xFF1E2026)
                ),
                border = if (!watermarkEnabled) {
                    androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                } else {
                    null
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Position", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        val positions = listOf("Upper left", "Upper right", "Lower left", "Lower right")
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                listOf(positions[0], positions[1]).forEach { pos ->
                    val selected = watermarkPosition == pos
                    Button(
                        onClick = { onUpdatePosition(pos) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF2A2B34) else Color(0xFF1E2026)
                        ),
                        border = if (selected) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(pos, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                listOf(positions[2], positions[3]).forEach { pos ->
                    val selected = watermarkPosition == pos
                    Button(
                        onClick = { onUpdatePosition(pos) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF2A2B34) else Color(0xFF1E2026)
                        ),
                        border = if (selected) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(pos, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
