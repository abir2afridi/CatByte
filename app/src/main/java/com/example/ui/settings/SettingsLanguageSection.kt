package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
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
import com.example.data.AppLanguage
import com.example.ui.theme.PencilTheme

/** App language picker: system default, English, Bengali. */
@Composable
internal fun SettingsLanguageSection(
    appLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.language),
        icon = Icons.Outlined.Language,
        iconTint = colors.accentPurple
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AppLanguage.SYSTEM to stringResource(R.string.system_default),
                AppLanguage.ENGLISH to stringResource(R.string.english),
                AppLanguage.BENGALI to stringResource(R.string.bengali)
            ).forEach { (lang, label) ->
                val isSel = appLanguage == lang
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                        .border(1.dp, if (isSel) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable { onSelectLanguage(lang) }
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
