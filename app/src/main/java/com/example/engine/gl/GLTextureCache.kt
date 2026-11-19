package com.example.engine.gl

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import androidx.annotation.Keep

/**
 * Manages OpenGL texture lifecycle with LRU eviction and memory budget.
 * All operations must be called on the GL thread.
 */
@Keep
class GLTextureCache(
    private val maxMemoryBytes: Long = 64 * 1024 * 1024 // 64MB default
) {

    companion object {
        private const val TAG = "GLTextureCache"
    }

    private val textures = mutableMapOf<String, GLTexture>()
    private val accessOrder = mutableListOf<String>()
    private var currentMemoryBytes = 0L

    data class GLTexture(
        val id: Int,
        val width: Int,
        val height: Int,
        val format: Int = GLES30.GL_RGBA,
        val memoryBytes: Long
    )

    /**
     * Gets or creates a texture from a bitmap.
     * Returns the texture ID, or 0 if failed.
     */
    fun getOrCreate(key: String, bitmap: Bitmap): Int {
        // Check if already cached
        val existing = textures[key]
        if (existing != null) {
            touch(key)
            return existing.id
        }

        // Create new texture
        val textureId = createTextureFromBitmap(bitmap)
        if (textureId == 0) return 0

        val memoryBytes = (bitmap.width * bitmap.height * 4L) // RGBA8888
        val texture = GLTexture(textureId, bitmap.width, bitmap.height, memoryBytes = memoryBytes)

        // Evict if over budget
        while (currentMemoryBytes + memoryBytes > maxMemoryBytes && accessOrder.isNotEmpty()) {
            evictOldest()
        }

        textures[key] = texture
        accessOrder.add(key)
        currentMemoryBytes += memoryBytes

        Log.d(TAG, "Created texture $key: ${bitmap.width}x${bitmap.height}, cache: ${formatBytes(currentMemoryBytes)}")
        return textureId
    }

    private fun createTextureFromBitmap(bitmap: Bitmap): Int {
        val textureId = IntArray(1)
        GLES30.glGenTextures(1, textureId, 0)
        val id = textureId[0]
        if (id == 0) return 0

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        return id
    }

    /**
     * Creates a texture from raw pixel data.
     */
    fun createFromPixels(key: String, width: Int, height: Int, pixels: ByteArray, format: Int = GLES30.GL_RGBA): Int {
        val existing = textures[key]
        if (existing != null) {
            touch(key)
            return existing.id
        }

        val textureId = IntArray(1)
        GLES30.glGenTextures(1, textureId, 0)
        val id = textureId[0]
        if (id == 0) return 0

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val buffer = java.nio.ByteBuffer.wrap(pixels)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES30.GL_UNSIGNED_BYTE, buffer)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        val memoryBytes = (width * height * 4L)
        val texture = GLTexture(id, width, height, format, memoryBytes)

        while (currentMemoryBytes + memoryBytes > maxMemoryBytes && accessOrder.isNotEmpty()) {
            evictOldest()
        }

        textures[key] = texture
        accessOrder.add(key)
        currentMemoryBytes += memoryBytes

        return id
    }

    /**
     * Updates an existing texture with new bitmap data.
     */
    fun update(key: String, bitmap: Bitmap): Boolean {
        val texture = textures[key] ?: return false
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.id)
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, bitmap)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        touch(key)
        return true
    }

    fun get(key: String): GLTexture? = textures[key]?.also { touch(key) }

    fun getId(key: String): Int = textures[key]?.id ?: 0

    fun getSize(key: String): Pair<Int, Int>? = textures[key]?.let { it.width to it.height }

    private fun touch(key: String) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }

    private fun evictOldest() {
        val oldest = accessOrder.removeAt(0)
        textures.remove(oldest)?.let { tex ->
            GLES30.glDeleteTextures(1, intArrayOf(tex.id), 0)
            currentMemoryBytes -= tex.memoryBytes
            Log.d(TAG, "Evicted texture $oldest, cache: ${formatBytes(currentMemoryBytes)}")
        }
    }

    fun clear() {
        textures.values.forEach { tex ->
            GLES30.glDeleteTextures(1, intArrayOf(tex.id), 0)
        }
        textures.clear()
        accessOrder.clear()
        currentMemoryBytes = 0
    }

    fun getMemoryUsage(): Long = currentMemoryBytes

    fun getTextureCount(): Int = textures.size

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }
}