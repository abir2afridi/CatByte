package com.example.ui.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PencilTheme

/** Destructive confirmation before wiping every project. */
@Composable
internal fun SettingsClearAllDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = PencilTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_clear_all), color = colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.confirm_clear_all_desc), color = colors.textSecondary, fontSize = 13.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.clear_all_projects), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.cancel), color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
