package com.example.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PencilTheme

/** The colour preset swatches of the text editor. */
@Composable
internal fun TextColorRow(
    selectedColor: Long,
    onSelectColor: (Long) -> Unit
) {
    val colors = PencilTheme.colors
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
                    .clickable { onSelectColor(colorLong) }
            )
        }
    }
}
