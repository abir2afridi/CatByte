package com.example.data

import android.content.Context
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Transformer
import com.example.data.export.querySupportedCodecMimeTypes
import com.example.data.export.runExportPipeline
import com.example.data.export.validateExportFile
import com.example.export.ExportOutput
import com.example.export.ExportProgress
import com.example.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Renders a CatByte project into a real MP4 file using the Media3 Transformer.
 *
 * This file owns the state; the behaviour lives in its own files under
 * `data/export/`:
 *  - `ExportRenderPipeline.kt`: capability check, storage pre-check, render
 *    graph build, render, gallery hand-off and validation.
 *  - `ExportRender.kt`: the Media3 Transformer run itself.
 *  - `ExportValidation.kt`: inspection of the rendered MP4 before it is
 *    reported as a success.
 *  - `ExportSizeEstimate.kt`: the storage pre-check estimate.
 *  - `ExportCodecSupport.kt`: encoder capability queries and the failure
 *    classification that triggers the default-encoder retry.
 *  - `ExportAudioProviders.kt`: the shared speed/gain providers.
 */
@UnstableApi
@androidx.annotation.OptIn(ExperimentalApi::class)
class ExportManager(context: Context) {

    // Render models (ExportProgress / ExportOutput) live in com.example.export.

    internal val appContext: Context = context

    internal val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress.asStateFlow()

    @Volatile
    internal var transformer: Transformer? = null

    /**
     * Exports [project] to an MP4 file. Suspends until completion or failure and
     * returns the rendered file, or null when the export failed.
     */
    suspend fun export(
        project: VideoProject,
        resolution: String,
        fps: Int,
        watermarkEnabled: Boolean = false,
        /** Requested video bitrate in bits per second, 0 = encoder default. */
        videoBitrateBps: Int = 0
    ): ExportOutput? = runExportPipeline(
        project = project,
        resolution = resolution,
        fps = fps,
        watermarkEnabled = watermarkEnabled,
        videoBitrateBps = videoBitrateBps
    )

    /** Cancels an in-flight render. */
    fun cancel() {
        transformer?.cancel()
        transformer = null
        AppLog.warn("ExportManager", "Export cancelled by user")
        _progress.value = _progress.value.copy(error = "Export cancelled", isCancelled = true, isDone = false)
    }

    /**
     * Validates an exported MP4 file using MediaMetadataRetriever.
     * Checks: file exists, has video track, duration within tolerance.
     */
    fun validateExport(outputFile: File, project: VideoProject): ValidationResult =
        validateExportFile(outputFile, project)

    /**
     * Query MediaCodecList for encoders supporting the given resolution/fps.
     * Returns ordered mime types (HEVC before AVC).
     */
    fun querySupportedCodecs(width: Int, height: Int, fps: Int): List<String> =
        querySupportedCodecMimeTypes(width, height, fps)

    data class ValidationResult(
        val isValid: Boolean,
        val error: String?
    )
}
