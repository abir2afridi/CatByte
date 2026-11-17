package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Lut3D
import com.example.engine.LutApply
import com.example.engine.LutCubeParser
import com.example.ui.theme.PencilTheme

/**
 * Imports a custom `.cube` 3D LUT from device storage and registers it as a
 * named colour grade. Imported LUTs appear at the top of the Filters sheet.
 */
data class ImportedLut(
    val id: String,
    val displayName: String,
    val lut: Lut3D,
    val sourceUri: String? = null
)

@Composable
fun LutImportSheet(
    importedLuts: List<ImportedLut>,
    onImportLut: (ImportedLut) -> Unit,
    onRemoveLut: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        statusMessage = importCubeFromUri(context, uri)?.let { lut ->
            val name = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".cube")
                ?: "Custom LUT"
            val imported = ImportedLut(
                id = "lut_${System.currentTimeMillis()}",
                displayName = name.removeSuffix(".cube").ifBlank { "Custom LUT" },
                lut = lut,
                sourceUri = uri.toString()
            )
            onImportLut(imported)
            "Imported ${imported.displayName} (${lut.size}³)"
        } ?: "Could not parse that .cube file"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("lut_import_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
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
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Custom 3D LUTs", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accentCyan.copy(alpha = 0.12f))
                .border(1.dp, colors.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .clickable { picker.launch(arrayOf("*/*")) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("lut_import_pick")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(18.dp))
                Text("Import .cube LUT from storage", color = colors.accentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        statusMessage?.let {
            Text(it, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(importedLuts, key = { it.id }) { lut ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(lut.displayName, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${lut.lut.size}³ LUT · ${(LutApply.estimatedTextureBytes(lut.lut) / 1024)} KB", color = colors.textSecondary, fontSize = 10.sp)
                    }
                    IconButton(onClick = { onRemoveLut(lut.id) }, modifier = Modifier.size(26.dp).clip(CircleShape)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove LUT", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/** Reads and parses a `.cube` file from a content URI. Pure JVM-testable. */
fun importCubeFromUri(context: Context, uri: Uri): Lut3D? {
    val text = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull() ?: return null
    val lut = LutCubeParser.parse(text) ?: return null
    return lut.takeIf { LutApply.isAcceptableSize(it) }
}
