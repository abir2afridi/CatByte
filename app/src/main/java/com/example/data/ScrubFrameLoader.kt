package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * How accurately a scrub frame is decoded.
 *
 * [EXACT] decodes to the requested frame: the picture is frame-accurate, which
 * is what the split workflow needs ("which frame will this split land on?").
 * It is the accuracy used for every scrub frame today. [KEYFRAME] returns the
 * nearest previous sync frame instead — roughly an order of magnitude cheaper,
 * but frozen inside a GOP, so it is kept only as an escape hatch for sources
 * whose exact decoding cannot keep up at all.
 */
enum class ScrubFrameAccuracy { EXACT, KEYFRAME }

/**
 * Decodes the frames shown in the preview while the timeline is scrubbed.
 *
 * The composition player cannot do this: every seek stops its render loop until
 * the seek settles, so the preview surface is black for the whole drag. A
 * [MediaMetadataRetriever] reads frames directly from the source file instead,
 * completely independently of the player.
 *
 * Instances are NOT thread-safe. The ViewModel owns one loader and calls it from
 * a single-threaded dispatcher, which also guarantees that a fast drag never
 * queues up dozens of decodes: the request channel is conflated and only the
 * newest playhead is decoded.
 */
class ScrubFrameLoader(
    /** Longest edge of the frame decoded when the gesture settles. */
    private val maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
    /**
     * Longest edge while the finger is moving. Smaller than [maxDimensionPx]
     * because a drag frame is on screen for a few dozen milliseconds: decoding
     * (and uploading) a smaller bitmap is what lets the preview keep up with a
     * fast fling. The exact settle frame replaces it at full size.
     */
    private val dragMaxDimensionPx: Int = DEFAULT_DRAG_MAX_DIMENSION_PX,
    /** How many source files may keep a decoder open at the same time. */
    private val maxCachedRetrievers: Int = MAX_CACHED_RETRIEVERS
) {

    /**
     * Access-ordered cache: the retriever for the clip under the playhead is
     * reused across frames, which avoids re-opening the file for every seek.
     * The source's rotation metadata is read once per file and cached with it.
     */
    private val sources = object : LinkedHashMap<String, SourceHandle>(
        maxCachedRetrievers.coerceAtLeast(1),
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, SourceHandle>
        ): Boolean {
            if (size <= maxCachedRetrievers) return false
            runCatching { eldest.value.retriever.release() }
            return true
        }
    }

    /** One open source file plus the orientation metadata read from it. */
    private class SourceHandle(
        val retriever: MediaMetadataRetriever,
        val rotationDegrees: Int,
        val rawWidth: Int,
        val rawHeight: Int
    )

    /**
     * Returns the source frame at [ScrubFrameResolver.FrameSource.sourceTimeMs],
     * or null when the file cannot be read. The bitmap is scaled so its longest
     * edge is at most [maxDimensionPx] while keeping the source aspect ratio,
     * and it is rotated the way the preview player rotates it.
     */
    @WorkerThread
    fun load(
        context: Context,
        source: ScrubFrameResolver.FrameSource,
        accuracy: ScrubFrameAccuracy = ScrubFrameAccuracy.EXACT
    ): Bitmap? {
        val handle = sourceFor(context, source.uriString) ?: return null
        val durationUs = source.sourceDurationMs.coerceAtLeast(1L) * 1000L
        val timeUs = (source.sourceTimeMs.coerceAtLeast(0L) * 1000L)
            .coerceIn(0L, durationUs - 1L)

        val frame = decodeFrame(handle.retriever, timeUs, accuracy)
        if (frame == null) {
            // A retriever that failed once may be wedged on a broken file; drop
            // it so the next request starts from a clean decoder.
            sources.remove(source.uriString)?.let { runCatching { it.retriever.release() } }
            return null
        }
        // Orientation first (the source's rotation metadata), then the clip's
        // own transform — the same order the composition player uses.
        val oriented = orientFrame(frame, handle)
        return applyClipTransform(oriented, source.transform)
    }

    /** Releases every cached decoder. Call when the editor is destroyed. */
    fun clear() {
        sources.values.forEach { runCatching { it.retriever.release() } }
        sources.clear()
    }

    private fun sourceFor(context: Context, uriString: String): SourceHandle? {
        sources[uriString]?.let { return it }
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(uriString)
            when {
                // Camera captures are stored as plain absolute paths; there is
                // no ContentResolver scheme to open them through.
                uri.scheme == null || uri.scheme == "file" -> {
                    retriever.setDataSource(uri.path ?: uriString)
                }

                else -> retriever.setDataSource(context, uri)
            }
            val handle = SourceHandle(
                retriever = retriever,
                rotationDegrees = retriever.rotationDegrees(),
                rawWidth = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),
                rawHeight = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            )
            sources[uriString] = handle
            handle
        } catch (e: Exception) {
            runCatching { retriever.release() }
            AppLog.warn(TAG, "Cannot open scrub frame source $uriString", e)
            null
        }
    }

    private fun MediaMetadataRetriever.rotationDegrees(): Int =
        metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

    private fun MediaMetadataRetriever.metadataInt(key: Int): Int =
        runCatching { extractMetadata(key)?.trim()?.toIntOrNull() ?: 0 }.getOrDefault(0)

    /**
     * Rotates the decoded frame into the orientation the preview player shows.
     *
     * The composition player applies the source's rotation metadata; the
     * retriever does not. Without this the preview would flip to the raw
     * (usually landscape) orientation the moment the scrub overlay appears.
     */
    private fun orientFrame(frame: Bitmap, handle: SourceHandle): Bitmap {
        val rotation = ScrubFrameOrientation.normalizedRotation(handle.rotationDegrees)
        if (rotation == 0) return frame
        if (!ScrubFrameOrientation.needsRotation(
                rotationDegrees = rotation,
                frameWidth = frame.width,
                frameHeight = frame.height,
                rawWidth = handle.rawWidth,
                rawHeight = handle.rawHeight
            )
        ) {
            return frame
        }
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return runCatching {
            val rotated = Bitmap.createBitmap(
                frame,
                0,
                0,
                frame.width,
                frame.height,
                matrix,
                true
            )
            if (rotated !== frame) frame.recycle()
            rotated
        }.getOrElse {
            // A hardware bitmap cannot be transformed: keep the raw frame
            // rather than losing the picture entirely.
            frame
        }
    }

    /**
     * Applies the clip's own transform (scale, rotation, mirror, position) to
     * the decoded frame.
     *
     * The composition player applies the same transform before it scales the
     * video into the canvas, so drawing the transformed frame with
     * `ContentScale.Fit` reproduces the preview exactly. The pivot is the frame
     * centre and the position offset is relative to the frame size, mirroring
     * `ClipTransformEffect`'s NDC maths.
     */
    private fun applyClipTransform(
        frame: Bitmap,
        transform: ScrubFrameResolver.ClipTransform
    ): Bitmap {
        if (transform.isIdentity) return frame
        val centerX = frame.width / 2f
        val centerY = frame.height / 2f
        val scaleX = if (transform.mirrored) -transform.scale else transform.scale
        val matrix = Matrix()
        matrix.setScale(scaleX, transform.scale, centerX, centerY)
        if (abs(transform.rotationDegrees) > 0.001f) {
            matrix.postRotate(transform.rotationDegrees, centerX, centerY)
        }
        val translateX = (transform.posX - 0.5f) * frame.width
        val translateY = (0.5f - transform.posY) * frame.height
        if (abs(translateX) > 0.01f || abs(translateY) > 0.01f) {
            matrix.postTranslate(translateX, translateY)
        }
        return runCatching {
            val transformed = Bitmap.createBitmap(
                frame,
                0,
                0,
                frame.width,
                frame.height,
                matrix,
                true
            )
            if (transformed !== frame) frame.recycle()
            transformed
        }.getOrElse {
            // A hardware bitmap cannot be transformed: keep the raw frame
            // rather than losing the picture entirely.
            frame
        }
    }

    private fun decodeFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        accuracy: ScrubFrameAccuracy
    ): Bitmap? {
        // PREVIOUS_SYNC never shows content from beyond the playhead, which is
        // what an editor's scrub preview should do. It is also much cheaper
        // than CLOSEST (no forward decode from the previous keyframe), but it
        // freezes inside a GOP — so it is only for sources where EXACT cannot
        // keep up at all.
        val option = when (accuracy) {
            ScrubFrameAccuracy.EXACT -> MediaMetadataRetriever.OPTION_CLOSEST
            ScrubFrameAccuracy.KEYFRAME -> MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
        }
        val maxDimension = when (accuracy) {
            ScrubFrameAccuracy.EXACT -> maxDimensionPx
            ScrubFrameAccuracy.KEYFRAME -> dragMaxDimensionPx
        }
        // getScaledFrameAtTime scales the decoded frame to the requested box on
        // API 27+ (aspect ratio preserved), which avoids holding a full-size 4K
        // bitmap in memory.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    option,
                    maxDimension,
                    maxDimension
                )
            }.getOrNull()?.let { return it }
        }

        @Suppress("DEPRECATION")
        val raw = runCatching {
            retriever.getFrameAtTime(timeUs, option)
        }.getOrNull() ?: return null
        return scaleDown(raw, maxDimension)
    }

    private fun scaleDown(source: Bitmap, maxDimension: Int): Bitmap {
        val longestEdge = max(source.width, source.height)
        if (longestEdge <= maxDimension) return source
        val scale = maxDimension.toFloat() / longestEdge.toFloat()
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        if (scaled !== source) source.recycle()
        return scaled
    }

    private companion object {
        const val TAG = "ScrubFrameLoader"
        const val DEFAULT_MAX_DIMENSION_PX = 720
        const val DEFAULT_DRAG_MAX_DIMENSION_PX = 480
        const val MAX_CACHED_RETRIEVERS = 3
    }
}
