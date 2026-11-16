package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

/**
 * Real thumbnails for the home list.
 *
 * The cover is either a custom image ([VideoProject.coverImageUri]) or a frame
 * extracted from the project's own video at [VideoProject.coverFrameMs], so a
 * project card shows what the user actually edited instead of a generic icon.
 * Results are cached by cover identity and scaled down to card size.
 */
object ProjectThumbnailLoader {

    private const val MAX_EDGE = 360

    private val cache = object : LruCache<String, Bitmap>(24) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /** Stable cache key: any change to the cover produces a new key. */
    fun cacheKey(project: VideoProject): String =
        "${project.id}|${project.coverFrameMs}|${project.coverImageUri}"

    fun clear() = cache.evictAll()

    suspend fun load(context: Context, project: VideoProject): Bitmap? = withContext(Dispatchers.IO) {
        if (project.thumbnailResId != null) return@withContext null
        val key = cacheKey(project)
        cache.get(key)?.let { if (!it.isRecycled) return@withContext it }

        val bitmap = if (project.coverImageUri.isNotBlank()) {
            CanvasBackgroundLoader.load(context, project.coverImageUri)?.let { scaleDown(it) }
        } else {
            extractFrame(context, project)
        } ?: return@withContext null

        cache.put(key, bitmap)
        bitmap
    }

    /**
     * Draft preview used by the cover editor: the same loader, but for a frame /
     * image pair that has not been saved to the project yet.
     */
    suspend fun load(
        context: Context,
        project: VideoProject,
        frameMs: Long,
        coverImageUri: String
    ): Bitmap? = load(
        context,
        project.copy(coverFrameMs = frameMs, coverImageUri = coverImageUri)
    )

    private fun extractFrame(context: Context, project: VideoProject): Bitmap? {
        val (clip, localMs) = mainClipAt(project, project.coverFrameMs) ?: return null
        val uriString = clip.uriString ?: return null
        val speed = clip.speed.coerceAtLeast(0.01f)
        val sourceMs = (clip.trimStartMs + (localMs * speed).roundToLong())
            .coerceAtLeast(0L)

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uriString))
            val frame = retriever.getFrameAtTime(
                sourceMs * 1_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return null
            scaleDown(frame.rotateIfNeeded(retriever))
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Clip + clip-local time for a global timeline position on the main track. */
    internal fun mainClipAt(project: VideoProject, timelineMs: Long): Pair<MediaClip, Long>? {
        val main = project.clips
        if (main.isEmpty()) return null
        var cursor = 0L
        for (clip in main) {
            val end = cursor + clip.durationMs
            if (timelineMs < end) {
                return clip to (timelineMs - cursor).coerceIn(0L, clip.durationMs)
            }
            cursor = end
        }
        val last = main.last()
        return last to last.durationMs
    }

    private fun Bitmap.rotateIfNeeded(retriever: MediaMetadataRetriever): Bitmap {
        val degrees = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        if (degrees % 360 == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val factor = MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1),
            true
        )
    }
}
