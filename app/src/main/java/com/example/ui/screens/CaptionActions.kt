package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Caption import/export file plumbing, kept separate from the editor screen so
 * the caption feature can be fixed or tested without touching timeline code.
 */

/** Reads an SRT/VTT file into a string, or null when unreadable or over 2 MB. */
internal fun readCaptionFile(context: Context, uri: Uri): String? {
    val maxBytes = 2 * 1024 * 1024
    val buffer = ByteArray(maxBytes + 1)
    var offset = 0
    val stream = context.contentResolver.openInputStream(uri) ?: return null
    stream.use { input ->
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read <= 0) break
            offset += read
        }
    }
    if (offset > maxBytes) return null
    return String(buffer, 0, offset, Charsets.UTF_8)
}

/** Writes the SRT export into the cache and opens a share sheet for it. */
internal fun shareSubtitleExport(context: Context, projectId: String, content: String) {
    val file = File(context.cacheDir, "CatByte_${projectId}_${System.currentTimeMillis()}.srt")
    val written = runCatching { file.writeText(content, Charsets.UTF_8) }.isSuccess
    if (!written) {
        Toast.makeText(context, "Could not write the SRT file", Toast.LENGTH_LONG).show()
        return
    }
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrElse {
        Toast.makeText(context, "Could not share the SRT file", Toast.LENGTH_LONG).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/x-subrip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Export subtitles")) }
        .onFailure { Toast.makeText(context, "No app can handle .srt files", Toast.LENGTH_LONG).show() }
}
