package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/** Storage & cache: cache clearing and the clear-all-projects danger zone. */
@Composable
internal fun SettingsStorageSection(
    cacheSizeMb: Int,
    onClearCache: () -> Unit,
    onRequestClearAllProjects: () -> Unit
) {
    val colors = PencilTheme.colors

    SettingsSection(
        title = stringResource(R.string.section_storage_drafts),
        icon = Icons.Outlined.DeleteSweep,
        iconTint = colors.accentRed
    ) {
        // Clear Cache Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.draft_cache_mb, cacheSizeMb),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (cacheSizeMb > 0) stringResource(R.string.free_up_space) else stringResource(R.string.cache_completely_clear),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onClearCache,
                enabled = cacheSizeMb > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentRed,
                    disabledContainerColor = colors.surfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    if (cacheSizeMb > 0) stringResource(R.string.clear_cache_short) else stringResource(R.string.cache_clean_short),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clear All Projects Row (Danger Zone)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.accentRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.clear_all_projects),
                    color = colors.accentRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.clear_all_projects_desc),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onRequestClearAllProjects,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    stringResource(R.string.clear_all_projects),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
