package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.example.engine.gif.GifEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Converts a rendered MP4 into a real animated GIF.
 *
 * The GIF tab used to hand the video pipeline a different resolution and call
 * the result a GIF. This class decodes frames from the finished render (so the
 * GIF contains the same filters, text, stickers and watermark as the video),
 * then streams them into [GifEncoder].
 *
 * Frames are decoded and written one at a time, so memory stays flat even for
 * a long clip.
 */
object GifExporter {

    /** GIF frame rate: 12 fps is the sweet spot for size vs smoothness. */
    const val DEFAULT_FPS = 12

    /** Longest GIF we render, in seconds (bounds encode time and file size). */
    private const val MAX_SECONDS = 20

    /** Widest GIF we render; taller clips keep their aspect ratio. */
    private const val MAX_WIDTH = 480

    /**
     * @return the written .gif file, or null when the source could not be read.
     */
    suspend fun convert(
        context: Context,
        videoFile: File,
        fps: Int = DEFAULT_FPS
    ): File? = withContext(Dispatchers.IO) {
        if (!videoFile.exists() || videoFile.length() == 0L) return@withContext null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return@withContext null
            val sourceWidth = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val sourceHeight = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            val safeFps = fps.coerceIn(5, 24)
            val seconds = (durationMs / 1000.0).coerceAtMost(MAX_SECONDS.toDouble())
            val frameCount = (seconds * safeFps).roundToInt().coerceAtLeast(1)
            val delayCs = (100.0 / safeFps).roundToInt().coerceAtLeast(2)

            // Target size: cap the width, keep the aspect, force even numbers.
            val (gifWidth, gifHeight) = targetSize(sourceWidth, sourceHeight)

            val outputFile = File(
                videoFile.parentFile,
                videoFile.nameWithoutExtension + ".gif"
            )
            outputFile.outputStream().buffered().use { stream ->
                GifEncoder.encode(
                    frameCount = frameCount,
                    width = gifWidth,
                    height = gifHeight,
                    output = stream,
                    delayCs = delayCs
                ) { index ->
                    val timeUs = (index * 1_000_000L) / safeFps
                    retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                }
            }
            AppLog.info(
                "GifExporter",
                "GIF written: ${outputFile.name} (${gifWidth}x$gifHeight, $frameCount frames, " +
                    "${outputFile.length() / 1024}KB)"
            )
            outputFile
        } catch (error: Throwable) {
            AppLog.error("GifExporter", "Could not convert the export to GIF", error)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Even GIF dimensions that fit [MAX_WIDTH] and keep the source aspect. */
    internal fun targetSize(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        if (sourceWidth <= 0 || sourceHeight <= 0) return MAX_WIDTH to MAX_WIDTH
        val scale = if (sourceWidth > MAX_WIDTH) {
            MAX_WIDTH.toFloat() / sourceWidth
        } else {
            1f
        }
        val width = (sourceWidth * scale).roundToInt().coerceAtLeast(2)
        val height = (sourceHeight * scale).roundToInt().coerceAtLeast(2)
        return (width - width % 2) to (height - height % 2)
    }

    /** Publishes a finished GIF into the gallery (Pictures/CatByte). */
    fun saveToGallery(context: Context, gifFile: File, displayName: String): android.net.Uri? {
        if (!gifFile.exists()) return null
        val values = android.content.ContentValues().apply {
            put(
                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                if (displayName.endsWith(".gif")) displayName else "$displayName.gif"
            )
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/gif")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(
                    android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/CatByte"
                )
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = runCatching {
            resolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
        }.getOrNull() ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                gifFile.inputStream().use { it.copyTo(output) }
            } ?: error("Could not open the gallery output stream")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
            AppLog.error("GifExporter", "Could not publish the GIF", it)
            null
        }
    }
}
