package com.example.export

import android.net.Uri

/**
 * Live progress reported by the ExportManager render pipeline
 * (Media3 Transformer) while an export runs.
 */
data class ExportProgress(
    val progress: Float = 0f,
    val stage: String = "",
    val isDone: Boolean = false,
    val outputPath: String? = null,
    val galleryUri: Uri? = null,
    val error: String? = null,
    val isCancelled: Boolean = false,
    /** Features that could not be rendered exactly (never silently dropped). */
    val warnings: List<String> = emptyList()
)
