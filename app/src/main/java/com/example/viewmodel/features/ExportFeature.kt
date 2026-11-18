package com.example.viewmodel.features

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.ExportServiceScheduler
import com.example.data.GifExporter
import com.example.engine.AudioBoost
import com.example.export.ExportSettings
import com.example.export.ExportUiState
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import java.io.File

/** Export settings, in-app rendering, background export and cancellation. */
internal object ExportFeature {

    fun updateExportSettings(vm: EditorViewModel, settings: ExportSettings) {
        vm._exportSettings.value = settings
    }

    /**
     * One-click export with the pre-selected settings. The GIF tab renders
     * through the video pipeline at the matching resolution until a dedicated
     * GIF encoder lands in [com.example.data.ExportManager].
     */
    fun startSelectedExport(vm: EditorViewModel) {
        val settings = vm._exportSettings.value
        val boost = AudioBoost.gainFor(settings.boostLoudness)
        if (settings.tab == "gif") {
            // Real GIF: render the timeline, then encode the frames into a
            // GIF89a file (the quality chip picks the render resolution).
            val resolution = when (settings.gifQuality) {
                "320P" -> "720P"
                "640P" -> "1080P"
                else -> "480P"
            }
            startGifExport(vm, resolution = resolution, audioBoost = boost)
        } else {
            startExport(
                vm,
                resolution = settings.resolution,
                fps = settings.fps,
                bitrateMbps = settings.bitrateMbps,
                audioBoost = boost
            )
        }
    }

    /**
     * GIF export: renders the timeline at [resolution] (so filters, text,
     * stickers and the watermark are included), then encodes the frames into a
     * real animated GIF and publishes it to Pictures/CatByte.
     */
    fun startGifExport(vm: EditorViewModel, resolution: String = "480P", audioBoost: Float = 1f) {
        val project = vm._currentProject.value ?: return
        val manager = vm.exportManager
        if (manager == null) {
            vm._exportState.value = ExportUiState(error = "Export is not available")
            return
        }
        vm.pausePlayback()
        vm.exportJob?.cancel()
        vm._exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Rendering frames for GIF...",
            resolution = resolution,
            fps = GifExporter.DEFAULT_FPS
        )

        vm.exportJob = vm.viewModelScope.launch {
            val progressJob = launch {
                manager.progress.collect { renderProgress ->
                    if (vm._exportState.value.isExporting) {
                        vm._exportState.value = vm._exportState.value.copy(
                            // Rendering is the first 80% of a GIF export.
                            progress = (renderProgress.progress * 0.8f).coerceIn(0f, 0.8f),
                            stage = renderProgress.stage.ifBlank { vm._exportState.value.stage }
                        )
                    }
                }
            }
            val rendered = manager.export(
                project = project,
                resolution = resolution,
                fps = 30,
                watermarkEnabled = vm._watermarkEnabled.value,
                videoBitrateBps = 8_000_000,
                audioBoost = audioBoost
            )
            progressJob.cancel()
            if (rendered == null) {
                val message = manager.progress.value.error ?: "GIF export failed"
                vm._exportState.value = ExportUiState(error = message)
                return@launch
            }

            vm._exportState.value = vm._exportState.value.copy(
                progress = 0.85f,
                stage = "Encoding GIF..."
            )
            val context = vm.context
            val gif = if (context != null) {
                GifExporter.convert(context, rendered.file)
            } else {
                null
            }
            if (gif == null) {
                vm._exportState.value = ExportUiState(error = "Could not encode the GIF")
                return@launch
            }
            val galleryUri = context?.let {
                GifExporter.saveToGallery(
                    it,
                    gif,
                    "CatByte_${project.title.take(20).replace("/", "-")}"
                )
            }
            // The rendered MP4 was only an intermediate: drop it so the user
            // does not end up with a stray video next to the GIF.
            runCatching { rendered.file.delete() }

            vm._exportState.value = vm._exportState.value.copy(
                isExporting = false,
                isCompleted = true,
                progress = 1f,
                stage = "GIF exported!",
                videoPath = gif.absolutePath,
                galleryUri = galleryUri?.toString() ?: ""
            )
        }
    }

    fun startExport(
        vm: EditorViewModel,
        resolution: String,
        fps: Int,
        bitrateMbps: Int = 10,
        /** Gain applied to the exported mix (1 = untouched). */
        audioBoost: Float = 1f
    ) {
        val project = vm._currentProject.value ?: return
        vm.pausePlayback()
        vm.exportJob?.cancel()
        vm._exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Preparing timeline...",
            resolution = resolution,
            fps = fps
        )

        val exportManager = vm.exportManager
        vm.exportJob = vm.viewModelScope.launch {
            if (exportManager != null) {
                // Mirror the renderer's live progress (including compatibility
                // warnings) into the export sheet instead of showing a frozen
                // 0% until the MP4 is finished.
                val progressJob = launch {
                    exportManager.progress.collect { renderProgress ->
                        if (vm._exportState.value.isExporting) {
                            vm._exportState.value = vm._exportState.value.copy(
                                progress = renderProgress.progress.coerceIn(0f, 1f),
                                stage = renderProgress.stage.ifBlank { vm._exportState.value.stage },
                                warnings = renderProgress.warnings
                            )
                        }
                    }
                }
                val result = exportManager.export(
                    project,
                    resolution,
                    fps,
                    vm._watermarkEnabled.value,
                    (bitrateMbps.coerceIn(1, 100) * 1_000_000),
                    audioBoost
                )
                progressJob.cancel()
                val renderWarnings = exportManager.progress.value.warnings
                if (result != null) {
                    vm._exportState.value = vm._exportState.value.copy(
                        isExporting = false,
                        isCompleted = true,
                        progress = 1.0f,
                        stage = "Export complete!",
                        videoPath = result.file.absolutePath,
                        galleryUri = result.galleryUri?.toString() ?: "",
                        warnings = renderWarnings
                    )
                } else {
                    // Keep the failure on screen: without an error flag the
                    // export sheet closed silently and the user never learned
                    // that the render had failed.
                    vm._exportState.value = vm._exportState.value.copy(
                        isExporting = false,
                        stage = exportManager.progress.value.error ?: "Export failed",
                        error = exportManager.progress.value.error ?: "Export failed",
                        warnings = renderWarnings
                    )
                }
            } else {
                // No renderer on this device: report the failure honestly.
                // A simulated "export complete" would point at a file that
                // was never written.
                vm._exportState.value = vm._exportState.value.copy(
                    isExporting = false,
                    stage = "Export is not available on this device",
                    error = "Export is not available on this device"
                )
            }
        }
    }

    /**
     * Audio-only export (.m4a): the same mix as the video export, encoded
     * without a video track. Useful for podcasting / music hand-off.
     */
    fun startAudioOnlyExport(vm: EditorViewModel) {
        val project = vm._currentProject.value ?: return
        vm.pausePlayback()
        vm.exportJob?.cancel()
        vm._exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Preparing audio export...",
            resolution = vm._defaultResolution.value,
            fps = vm._defaultFps.value
        )

        val exportManager = vm.exportManager
        vm.exportJob = vm.viewModelScope.launch {
            if (exportManager == null) {
                vm._exportState.value = ExportUiState(error = "Export is not available")
                return@launch
            }
            val progressJob = launch {
                exportManager.progress.collect { renderProgress ->
                    if (vm._exportState.value.isExporting) {
                        vm._exportState.value = vm._exportState.value.copy(
                            progress = renderProgress.progress.coerceIn(0f, 1f),
                            stage = renderProgress.stage.ifBlank { vm._exportState.value.stage }
                        )
                    }
                }
            }
            val result = exportManager.exportAudioOnly(
                project = project,
                resolution = vm._defaultResolution.value,
                fps = vm._defaultFps.value,
                audioBoost = AudioBoost.gainFor(vm._exportSettings.value.boostLoudness)
            )
            progressJob.cancel()
            if (result != null) {
                vm._exportState.value = vm._exportState.value.copy(
                    isExporting = false,
                    isCompleted = true,
                    progress = 1.0f,
                    stage = "Audio exported!",
                    videoPath = result.file.absolutePath,
                    galleryUri = result.galleryUri?.toString() ?: ""
                )
            } else {
                val message = exportManager.progress.value.error ?: "Audio export failed"
                vm._exportState.value = vm._exportState.value.copy(
                    isExporting = false,
                    stage = message,
                    error = message
                )
            }
        }
    }

    /**
     * Exports one audio track on its own (.m4a). The track keeps its volume,
     * fades, speed, loop count and effects, while the video and every other
     * track are left out of the file.
     */
    fun startAudioTrackExport(vm: EditorViewModel, audioId: String) {
        val project = vm._currentProject.value ?: return
        val track = project.audioTracks.firstOrNull { it.id == audioId }
        if (track == null) {
            vm._exportState.value = ExportUiState(error = "That audio track no longer exists")
            return
        }
        vm.pausePlayback()
        vm.exportJob?.cancel()
        vm.setActiveSheet(ActiveToolSheet.NONE)
        vm._exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Preparing ${track.title}...",
            resolution = vm._defaultResolution.value,
            fps = vm._defaultFps.value
        )

        val exportManager = vm.exportManager
        vm.exportJob = vm.viewModelScope.launch {
            if (exportManager == null) {
                vm._exportState.value = ExportUiState(error = "Export is not available")
                return@launch
            }
            val progressJob = launch {
                exportManager.progress.collect { renderProgress ->
                    if (vm._exportState.value.isExporting) {
                        vm._exportState.value = vm._exportState.value.copy(
                            progress = renderProgress.progress.coerceIn(0f, 1f),
                            stage = renderProgress.stage.ifBlank { vm._exportState.value.stage }
                        )
                    }
                }
            }
            val result = exportManager.exportAudioTrack(
                project = project,
                audioId = audioId,
                audioBoost = AudioBoost.gainFor(vm._exportSettings.value.boostLoudness)
            )
            progressJob.cancel()
            if (result != null) {
                vm._exportState.value = vm._exportState.value.copy(
                    isExporting = false,
                    isCompleted = true,
                    progress = 1.0f,
                    stage = "Audio exported!",
                    videoPath = result.file.absolutePath,
                    galleryUri = result.galleryUri?.toString() ?: ""
                )
            } else {
                val message = exportManager.progress.value.error
                    ?: "Could not export this audio track"
                vm._exportState.value = vm._exportState.value.copy(
                    isExporting = false,
                    stage = message,
                    error = message
                )
            }
        }
    }

    fun dismissExport(vm: EditorViewModel) {
        vm.exportJob?.cancel()
        vm.exportJob = null
        vm._exportState.value = ExportUiState()
        vm._activeSheet.value = ActiveToolSheet.NONE
    }

    /** Starts a background export with explicit context (called from UI). */
    fun startBackgroundExportWithContext(
        vm: EditorViewModel,
        context: Context,
        resolution: String,
        fps: Int,
        bitrateMbps: Int = 10
    ) {
        val project = vm._currentProject.value ?: return
        vm.pausePlayback()

        vm._exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Queued for background export...",
            resolution = resolution,
            fps = fps
        )

        ExportServiceScheduler.startExport(
            context,
            project,
            resolution,
            fps,
            vm._watermarkEnabled.value,
            bitrateMbps.coerceIn(1, 100),
            AudioBoost.gainFor(vm._exportSettings.value.boostLoudness)
        )
    }

    /** Stops an in-flight render and closes the export sheet. */
    fun cancelExport(vm: EditorViewModel) {
        vm.exportManager?.cancel()
        dismissExport(vm)
    }

    /** Exports a real MP4 and reports where it can be shared. */
    fun shareExportMessage(vm: EditorViewModel, platform: String): String {
        val path = vm._exportState.value.videoPath
        return when {
            path.isBlank() -> "Export the project first."
            !File(path).exists() -> "Export failed — no MP4 was produced."
            else -> "Video ready at $path — shared to $platform."
        }
    }
}
