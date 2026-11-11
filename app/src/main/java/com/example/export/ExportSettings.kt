package com.example.export

/**
 * CapCut-style export settings chosen in the AI UHD dropdown. They persist
 * across dropdown open/close so a single click on Export starts the render
 * with exactly the format the user pre-selected.
 *
 * Debugging map:
 * - [resolution] / [fps] / [bitrateMbps] drive the video pipeline
 *   (see `com.example.data.export.ExportResolution` for pixel mapping).
 * - [gifQuality] drives the GIF tab (rendered through the video pipeline
 *   until a dedicated GIF encoder lands in ExportManager).
 * - [watermarkPosition] drives the watermark sub-panel.
 */
data class ExportSettings(
    /** "video" or "gif" - which tab is active in the dropdown. */
    val tab: String = "video",
    val resolution: String = "720P",
    val fps: Int = 30,
    val bitrateMbps: Int = 10,
    val gifQuality: String = "240P",
    val watermarkPosition: String = "Upper left"
)
