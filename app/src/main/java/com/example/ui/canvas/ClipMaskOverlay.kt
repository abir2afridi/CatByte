package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.MaskType

/** Cyan mask guide drawn over the preview for the clip's mask type. */
@Composable
internal fun ClipMaskOverlay(maskType: MaskType) {
    when (maskType) {
        MaskType.RECTANGLE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .border(2.dp, Color.Cyan, RoundedCornerShape(8.dp))
            )
        }
        MaskType.CIRCLE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Cyan, CircleShape)
            )
        }
        MaskType.SPLIT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }
        MaskType.MIRROR, MaskType.FILMSTRIP -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Magenta.copy(alpha = 0.7f))
            )
        }
        MaskType.NONE -> {}
    }
}
