package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reports and clears the app cache, including Coil's image disk cache
 * (which lives in cacheDir/image_cache by default).
 */
class CacheManager(private val context: Context) {

    suspend fun cacheSizeMb(): Int = withContext(Dispatchers.IO) {
        (directorySize(context.cacheDir) / BYTES_PER_MB).toInt()
    }

    /** Clears the cache and returns the remaining size in MB. */
    suspend fun clearCache(): Int = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        (directorySize(context.cacheDir) / BYTES_PER_MB).toInt()
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }

    private companion object {
        const val BYTES_PER_MB = 1024L * 1024L
    }
}
