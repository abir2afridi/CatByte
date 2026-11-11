package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
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
     */
    private val retrievers = object : LinkedHashMap<String, MediaMetadataRetriever>(
        maxCachedRetrievers.coerceAtLeast(1),
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, MediaMetadataRetriever>
        ): Boolean {
            if (size <= maxCachedRetrievers) return false
            runCatching { eldest.value.release() }
            return true
        }
    }

    /**
     * Returns the source frame at [ScrubFrameResolver.FrameSource.sourceTimeMs],
     * or null when the file cannot be read. The bitmap is scaled so its longest
     * edge is at most [maxDimensionPx] while keeping the source aspect ratio.
     */
    @WorkerThread
    fun load(
        context: Context,
        source: ScrubFrameResolver.FrameSource,
        accuracy: ScrubFrameAccuracy = ScrubFrameAccuracy.EXACT
    ): Bitmap? {
        val retriever = retrieverFor(context, source.uriString) ?: return null
        val durationUs = source.sourceDurationMs.coerceAtLeast(1L) * 1000L
        val timeUs = (source.sourceTimeMs.coerceAtLeast(0L) * 1000L)
            .coerceIn(0L, durationUs - 1L)

        val frame = decodeFrame(retriever, timeUs, accuracy)
        if (frame == null) {
            // A retriever that failed once may be wedged on a broken file; drop
            // it so the next request starts from a clean decoder.
            retrievers.remove(source.uriString)?.let { runCatching { it.release() } }
        }
        return frame
    }

    /** Releases every cached decoder. Call when the editor is destroyed. */
    fun clear() {
        retrievers.values.forEach { runCatching { it.release() } }
        retrievers.clear()
    }

    private fun retrieverFor(context: Context, uriString: String): MediaMetadataRetriever? {
        retrievers[uriString]?.let { return it }
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
            retrievers[uriString] = retriever
            retriever
        } catch (e: Exception) {
            runCatching { retriever.release() }
            AppLog.warn(TAG, "Cannot open scrub frame source $uriString", e)
            null
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
