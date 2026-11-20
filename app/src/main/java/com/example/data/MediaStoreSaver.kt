package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Saves a rendered video into the device gallery so users can find and share it
 * with any other application.
 */
object MediaStoreSaver {

    private const val RELATIVE_FOLDER = "Movies/CatByte"

    /**
     * Copies [source] into the gallery. Returns the gallery Uri when the copy
     * succeeded, or null when it failed.
     */
    fun saveVideo(context: Context, source: File, displayName: String): Uri? {
        if (!source.exists()) return null
        val name = if (displayName.endsWith(".mp4")) displayName else "$displayName.mp4"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, source, name)
        } else {
            saveLegacy(context, source, name)
        }
    }

    /**
     * Copies an audio-only export (.m4a) into the Music/CatByte folder.
     * Returns the gallery Uri, or null when the copy failed.
     */
    fun saveAudio(context: Context, source: File, displayName: String): Uri? {
        if (!source.exists()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val name = if (displayName.endsWith(".m4a")) displayName else "$displayName.m4a"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/CatByte")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open the music output stream")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
            AppLog.error("MediaStoreSaver", "Could not save the audio export to the gallery", it)
            null
        }
    }

    /**
     * Copies a generated image into the Pictures/CatByte gallery folder.
     * Returns the gallery Uri, or null when the copy failed.
     */
    fun saveImage(context: Context, bytes: ByteArray, displayName: String, mimeType: String = "image/png"): Uri? {
        if (bytes.isEmpty()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val name = when {
            displayName.endsWith(".png") || displayName.endsWith(".jpg") ||
                displayName.endsWith(".jpeg") || displayName.endsWith(".webp") -> displayName
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "$displayName.jpg"
            mimeType.contains("webp") -> "$displayName.webp"
            else -> "$displayName.png"
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CatByte")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output -> output.write(bytes) }
                ?: error("Could not open the image output stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
            AppLog.error("MediaStoreSaver", "Could not save the image to the gallery", it)
            null
        }
    }

    /**
     * Copies an audio file with an explicit MIME type (WAV voice-overs, MP3,
     * …) into the Music/CatByte folder. Returns the gallery Uri or null.
     */
    fun saveAudioFile(context: Context, source: File, displayName: String, mimeType: String): Uri? {
        if (!source.exists() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val extension = when {
            mimeType.contains("wav") -> ".wav"
            mimeType.contains("mpeg") || mimeType.contains("mp3") -> ".mp3"
            mimeType.contains("ogg") -> ".ogg"
            mimeType.contains("flac") -> ".flac"
            else -> ".m4a"
        }
        val name = if (displayName.endsWith(extension)) displayName else displayName + extension
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/CatByte")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open the audio output stream")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
            AppLog.error("MediaStoreSaver", "Could not save the audio file to the gallery", it)
            null
        }
    }

    private fun saveWithMediaStore(context: Context, source: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_FOLDER)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = runCatching {
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return null

        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open the gallery output stream")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) }
            AppLog.error("MediaStoreSaver", "Could not save the export to the gallery", it)
            null
        }
    }

    private fun saveLegacy(context: Context, source: File, displayName: String): Uri? {
        return runCatching {
            @Suppress("DEPRECATION")
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val targetDir = File(moviesDir, "CatByte").apply { mkdirs() }
            val target = File(targetDir, displayName)
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            var scanned: Uri? = null
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("video/mp4")) { _, uri ->
                scanned = uri
            }
            scanned ?: Uri.fromFile(target)
        }.getOrElse {
            AppLog.error("MediaStoreSaver", "Could not save the export to the gallery", it)
            null
        }
    }
}
