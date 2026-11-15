package com.example.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TextFontStyle
import com.example.ui.theme.PencilTheme

/** The horizontal row of font-style chips. */
@Composable
internal fun TextStyleRow(
    selectedStyle: TextFontStyle,
    onSelectStyle: (TextFontStyle) -> Unit
) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Style:", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        TextFontStyle.values().forEach { style ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedStyle == style) colors.accentCyan.copy(alpha = 0.2f) else colors.surfaceElevated)
                    .border(
                        1.dp,
                        if (selectedStyle == style) colors.accentCyan else colors.borderSubtle,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectStyle(style) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = style.displayName,
                    color = if (selectedStyle == style) colors.accentCyan else colors.textPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
