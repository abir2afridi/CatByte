package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads real information about the media files the user imports: the true
 * duration and an actual waveform (not a placeholder).
 */
data class MediaVideoInfo(
    val durationMs: Long,
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val displayName: String = ""
)

object MediaMetadata {

    private const val TAG = "MediaMetadata"

    /** Maximum amount of audio (in milliseconds) that is analysed for the waveform. */
    private const val WAVEFORM_ANALYSIS_LIMIT_MS = 90_000L
    /** Returns the duration of the media in milliseconds, or 0 when unknown. */
    suspend fun durationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        AppLog.debug(TAG, "durationMs() called for uri=$uri")
        // Persist URI permission for future access
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            AppLog.debug(TAG, "Persisted URI permission for duration check")
        } catch (e: Exception) {
            AppLog.warn(TAG, "Failed to persist URI permission", e)
        }
        
        val result = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                AppLog.debug(TAG, "Setting data source on MediaMetadataRetriever")
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                AppLog.info(TAG, "Got duration: ${duration}ms for uri=$uri")
                duration
            } finally {
                runCatching { retriever.release() }
            }
        }.onFailure { e ->
            AppLog.warn(TAG, "Failed to get duration for $uri", e)
        }.getOrDefault(0L)
        
        AppLog.debug(TAG, "durationMs() returning: ${result}ms")
        result
    }

    /**
     * Fast metadata read used for import: real duration, video dimensions,
     * rotation and the file display name. No waveform decode — that is the slow
     * part and video clips draw a default waveform until edited.
     */
    suspend fun videoInfo(context: Context, uri: Uri): MediaVideoInfo = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }

        val info = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
                MediaVideoInfo(duration, width, height, rotation)
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrElse { MediaVideoInfo(0) }

        info.copy(displayName = queryDisplayName(context, uri))
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
        return name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty().substringBeforeLast('.')
    }

    /** Returns a human readable name for the media. */
    suspend fun displayName(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        AppLog.debug(TAG, "displayName() called for uri=$uri")
        // Persist URI permission for future access
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            AppLog.debug(TAG, "Persisted URI permission for displayName")
        } catch (e: Exception) {
            AppLog.warn(TAG, "Failed to persist URI permission", e)
        }
        
        val result = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                AppLog.debug(TAG, "Setting data source on MediaMetadataRetriever for displayName")
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            } finally {
                runCatching { retriever.release() }
            }
        }.onFailure { e ->
            AppLog.warn(TAG, "Failed to get display name for $uri", e)
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty().substringBeforeLast('.')
        
        AppLog.debug(TAG, "displayName() returning: $result")
        result
    }
    /**
     * Builds a real waveform for the audio of [uri]. The returned list contains
     * [buckets] normalised values between 0 and 1. Returns null when the audio
     * could not be decoded.
     */
    suspend fun waveform(context: Context, uri: Uri, buckets: Int = 48): List<Float>? =
        MediaWaveform.waveform(context, uri, buckets)
}