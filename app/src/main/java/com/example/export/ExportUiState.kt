package com.example.export

/**
 * UI snapshot of an export run, shown by the export dialog:
 * progress while rendering, the result when finished, [error] when it failed.
 *
 * Renderer-side live progress lives in [ExportProgress] (ExportManager);
 * this class is what the dialog actually renders.
 */
data class ExportUiState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val stage: String = "",
    val isCompleted: Boolean = false,
    val resolution: String = "1080P",
    val fps: Int = 30,
    val videoPath: String = "",
    val galleryUri: String = "",
    /** Set when the render failed; the export sheet shows the reason. */
    val error: String? = null,
    /** Features that were approximated or skipped by the renderer. */
    val warnings: List<String> = emptyList()
)
