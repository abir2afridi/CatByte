package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File

/**
 * Loads and caches the project's canvas background image.
 *
 * The export composition and the live preview share one decoded bitmap, so a
 * background image looks the same in both and is decoded only once per file.
 * Images are downscaled to at most [MAX_EDGE] px: a background never needs more
 * than the canvas resolution, and a 12 MP camera photo would otherwise cost
 * ~48 MB of heap per project.
 */
object CanvasBackgroundLoader {

    private const val MAX_EDGE = 1440
    private const val CACHE_ENTRIES = 3

    private val cache = object : LruCache<String, Bitmap>(CACHE_ENTRIES) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /** Decodes [uriString] (content/file/asset), or returns null when unusable. */
    fun load(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        cache.get(uriString)?.let { if (!it.isRecycled) return it }

        val bitmap = runCatching { decode(context, uriString) }.getOrNull() ?: return null
        cache.put(uriString, bitmap)
        return bitmap
    }

    fun clear() {
        cache.evictAll()
    }

    private fun decode(context: Context, uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun openStream(context: Context, uri: Uri): java.io.InputStream? {
        return when (uri.scheme?.lowercase()) {
            "content", "android.resource" -> context.contentResolver.openInputStream(uri)
            "asset" -> {
                val path = uri.path?.removePrefix("/") ?: return null
                context.assets.open(path)
            }
            "file" -> {
                val path = uri.path ?: return null
                File(path).takeIf { it.exists() }?.inputStream()
            }
            null -> File(uri.toString()).takeIf { it.exists() }?.inputStream()
            else -> null
        }
    }

    /** Smallest sample size that keeps the longer edge at or below [MAX_EDGE]. */
    internal fun sampleSizeFor(width: Int, height: Int): Int {
        val longest = maxOf(width, height)
        if (longest <= MAX_EDGE) return 1
        val sample = (longest + MAX_EDGE - 1) / MAX_EDGE
        return sample.coerceAtLeast(1)
    }
}
