package com.example.data.export

import android.content.Context
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.data.AppLog
import com.example.data.ExportManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * The actual Media3 Transformer run.
 *
 * Kept apart from the export orchestration so the renderer can be fixed
 * without touching the capability checks, the storage pre-check or the
 * gallery hand-off.
 */

/** Outcome of a single Transformer run. */
internal data class TransformerRun(
    val result: ExportResult? = null,
    val failure: Throwable? = null
)

/**
 * Runs the render. When [videoMime] is set the Transformer is asked for that
 * output codec (e.g. HEVC); `null` leaves Media3's default encoder choice.
 */
@UnstableApi
@androidx.annotation.OptIn(ExperimentalApi::class)
internal suspend fun ExportManager.runTransformer(
    composition: Composition,
    outputFile: File,
    videoMime: String? = null,
    videoBitrateBps: Int = 0
): TransformerRun = coroutineScope {
    var outcome = TransformerRun()

    // CapCut's Bitrate selector is a real encoder setting, not an estimate
    // label: pin the requested bitrate when the user picked one.
    val encoderFactory = if (videoBitrateBps > 0) {
        DefaultEncoderFactory.Builder(appContext)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(videoBitrateBps)
                    .build()
            )
            .build()
    } else {
        null
    }

    val newTransformer = Transformer.Builder(appContext)
        .apply { videoMime?.let { mime -> setVideoMimeType(mime) } }
        .apply { encoderFactory?.let { factory -> setEncoderFactory(factory) } }
        .build()
    transformer = newTransformer

    suspendCancellableCoroutine<Unit> { continuation ->
        val holder = ProgressHolder()
        val poller = launch {
            while (isActive) {
                val state = runCatching { newTransformer.getProgress(holder) }.getOrDefault(0)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val fraction = (holder.progress / 100f).coerceIn(0f, 1f)
                    _progress.value = _progress.value.copy(
                        progress = fraction,
                        stage = "Rendering video... ${holder.progress}%"
                    )
                }
                delay(180L)
            }
        }

        continuation.invokeOnCancellation {
            poller.cancel()
            runCatching { newTransformer.cancel() }
        }

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                outcome = TransformerRun(result = exportResult)
                poller.cancel()
                if (continuation.isActive) continuation.resume(Unit)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                outcome = TransformerRun(failure = exportException)
                poller.cancel()
                AppLog.error("ExportManager", "Video export failed", exportException)
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
        newTransformer.addListener(listener)

        runCatching { newTransformer.start(composition, outputFile.absolutePath) }
            .onFailure { startError ->
                outcome = TransformerRun(failure = startError)
                poller.cancel()
                AppLog.error("ExportManager", "Could not start the render", startError)
                if (continuation.isActive) continuation.resume(Unit)
            }
    }

    transformer = null
    outcome
}
