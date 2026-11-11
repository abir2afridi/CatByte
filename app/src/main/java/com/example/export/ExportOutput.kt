package com.example.export

import android.net.Uri
import java.io.File

/**
 * A finished export: the rendered file plus the facts the result screen
 * (share sheet, size estimate) needs.
 */
data class ExportOutput(
    val file: File,
    val galleryUri: Uri?,
    val durationMs: Long,
    val width: Int,
    val height: Int
)
