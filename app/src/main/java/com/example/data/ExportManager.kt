package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult

import androidx.media3.transformer.Transformer
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Real video export using Media3 [Transformer]. Builds a [Composition] from the
 * project's clips and encodes an MP4 file.
 */
@UnstableApi
class ExportManager(private val context: Context) {

    data class ExportProgress(
        val progress: Float = 0f,
        val stage: String = "",
        val isDone: Boolean = false,
        val outputPath: String? = null,
        val error: String? = null
    )

    private val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress.asStateFlow()

    private var transformer: Transformer? = null

    /**
     * Exports [project] to an MP4 file. Suspends until completion or failure.
     * Returns the output file path, or null on error.
     */
    suspend fun export(project: VideoProject, resolution: String, fps: Int): String? =
        withContext(Dispatchers.Main) {
            val clips = project.clips.filter { !it.uriString.isNullOrBlank() }
            if (clips.isEmpty()) {
                _progress.value = ExportProgress(error = "No video clips with media to export.")
                return@withContext null
            }

            _progress.value = ExportProgress(stage = "Preparing timeline...")

            val editedItems = clips.map { clip ->
                val mediaItem = MediaItem.fromUri(clip.uriString!!.toUri())
                EditedMediaItem.Builder(mediaItem)
                    .setDurationUs(clip.durationMs * 1000L)
                    .build()
            }
            val sequence = EditedMediaItemSequence.Builder(editedItems).build()
            val composition = Composition.Builder(listOf(sequence)).build()

            val outputDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
            val outputFile = File(outputDir, "CatByte_Export_${System.currentTimeMillis()}.mp4")

            val newTransformer = Transformer.Builder(context).build()
            transformer = newTransformer

            val result = suspendCancellableCoroutine<File?> { cont ->
                newTransformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        AppLog.info("ExportManager", "Export completed: ${outputFile.name}")
                        cont.resume(outputFile)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        AppLog.error("ExportManager", "Video export failed", exportException)
                        _progress.value = ExportProgress(
                            error = exportException.message ?: "Export failed"
                        )
                        cont.resume(null)
                    }
                })
                newTransformer.start(composition, outputFile.absolutePath)
            }

            transformer = null
            if (result != null) {
                _progress.value = ExportProgress(
                    progress = 1f,
                    stage = "Export complete!",
                    isDone = true,
                    outputPath = result.absolutePath
                )
            }
            result?.absolutePath
        }

    fun cancel() {
        transformer?.cancel()
        transformer = null
        AppLog.warn("ExportManager", "Export cancelled by user")
        _progress.value = ExportProgress(error = "Export cancelled")
    }
}
