package com.example.ui.ailab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PencilTheme

/**
 * Shared bottom-sheet container for every AI Lab tool.
 *
 * A plain [Dialog] is used (rather than Material's experimental sheet) so the
 * Lab behaves like the editor's own tool sheets: rounded top corners, a drag
 * handle and a scrim tap to dismiss.
 */
@Composable
internal fun AiLabSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    accent: AiLabAccent = AiLabAccent.CYAN,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PencilTheme.colors
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onDismiss() }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .testTag("ai_lab_sheet")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.soft()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = accent.color(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            subtitle,
                            color = colors.textTertiary,
                            fontSize = 10.sp,
                            maxLines = 2
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                content()
            }
        }
    }
}

/** A full-width action button used for "Generate", "Translate", … */
@Composable
internal fun AiLabPrimaryButton(
    label: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val colors = PencilTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accentCyan,
            contentColor = if (colors.isLight) Color.White else Color.Black,
            disabledContainerColor = colors.surfaceElevated,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier
            .height(44.dp)
            .testTag("ai_lab_primary")
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = if (colors.isLight) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Secondary full-width button (copy, save, …). */
@Composable
internal fun AiLabSecondaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Text(label, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A labelled text field used by every prompt-based tool. */
@Composable
internal fun AiLabInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 6,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        placeholder = { Text(placeholder, color = colors.textTertiary, fontSize = 12.sp) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentCyan,
            unfocusedBorderColor = colors.borderSubtle,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accentCyan
        ),
        textStyle = TextStyle(fontSize = 13.sp, color = colors.textPrimary)
    )
}

/** Result card with a copy button, used by every text-producing tool. */
@Composable
internal fun AiLabResultCard(
    title: String,
    text: String,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = colors.textTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onCopy(text) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.accentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text,
                color = colors.textPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

/** Inline error message shown under the primary button. */
@Composable
internal fun AiLabErrorText(message: String) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.accentRed.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(message, color = colors.accentRed, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

/** Scrollable body wrapper for the sheet contents. */
@Composable
internal fun AiLabSheetBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
            .padding(bottom = 18.dp)
    ) {
        content()
    }
}

/** Section label used inside the sheets. */
@Composable
internal fun AiLabSectionLabel(text: String, modifier: Modifier = Modifier) {
    val colors = PencilTheme.colors
    Text(
        text,
        color = colors.textSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

/** Copies [text] and confirms with a toast. */
internal fun copyToClipboard(context: Context, text: String, label: String = "Copied") {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("CatByte", text))
    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
}
