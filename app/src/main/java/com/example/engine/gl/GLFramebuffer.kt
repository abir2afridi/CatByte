package com.example.engine.gl

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import androidx.annotation.Keep
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Offscreen framebuffer for render-to-texture operations.
 * Supports multiple color attachments and depth/stencil.
 */
@Keep
class GLFramebuffer(
    val width: Int,
    val height: Int,
    val colorAttachmentCount: Int = 1,
    val hasDepth: Boolean = true,
    val hasStencil: Boolean = false
) {

    companion object {
        private const val TAG = "GLFramebuffer"
    }

    var framebufferId: Int = 0
    private val colorTextureIds = IntArray(colorAttachmentCount)
    private var depthTextureId = 0
    private var depthStencilRenderbufferId = 0

    init {
        create()
    }

    private fun create() {
        // Create framebuffer
        val fb = IntArray(1)
        GLES30.glGenFramebuffers(1, fb, 0)
        framebufferId = fb[0]
        if (framebufferId == 0) {
            Log.e(TAG, "Failed to create framebuffer")
            return
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)

        // Create color attachments
        val texIds = IntArray(colorAttachmentCount)
        GLES30.glGenTextures(colorAttachmentCount, texIds, 0)
        for (i in 0 until colorAttachmentCount) {
            val texId = texIds[i]
            colorTextureIds[i] = texId
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
                width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
            )
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0 + i,
                GLES30.GL_TEXTURE_2D,
                texId,
                0
            )
        }

        // Create depth/stencil attachment
        if (hasDepth) {
            if (hasStencil) {
                // Combined depth24+stencil8 renderbuffer
                val rb = IntArray(1)
                GLES30.glGenRenderbuffers(1, rb, 0)
                depthStencilRenderbufferId = rb[0]
                GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthStencilRenderbufferId)
                GLES30.glRenderbufferStorage(
                    GLES30.GL_RENDERBUFFER,
                    GLES30.GL_DEPTH24_STENCIL8,
                    width, height
                )
                GLES30.glFramebufferRenderbuffer(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_DEPTH_STENCIL_ATTACHMENT,
                    GLES30.GL_RENDERBUFFER,
                    depthStencilRenderbufferId
                )
            } else {
                // Depth texture (for sampling in shaders)
                val dt = IntArray(1)
                GLES30.glGenTextures(1, dt, 0)
                depthTextureId = dt[0]
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTextureId)
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
                    width, height, 0,
                    GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
                )
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glFramebufferTexture2D(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_DEPTH_ATTACHMENT,
                    GLES30.GL_TEXTURE_2D,
                    depthTextureId,
                    0
                )
            }
        }

        // Set draw buffers
        val drawBuffers = IntArray(colorAttachmentCount) { GLES30.GL_COLOR_ATTACHMENT0 + it }
        GLES30.glDrawBuffers(colorAttachmentCount, drawBuffers, 0)

        // Check completeness
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer incomplete: ${framebufferStatusToString(status)}")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)
        GLES30.glViewport(0, 0, width, height)
    }

    fun unbind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun getColorTexture(index: Int = 0): Int = colorTextureIds.getOrElse(index) { 0 }

    fun getDepthTexture(): Int = depthTextureId

    fun readPixels(): Bitmap {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        // GL reads bottom-up, flip vertically
        val flipped = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(flipped)
        canvas.scale(1f, -1f, width / 2f, height / 2f)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return flipped
    }

    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return
        release()
        // Note: This class is immutable for width/height, so create new instance instead
        // This method is kept for API compatibility but does nothing
        Log.w(TAG, "GLFramebuffer is immutable, create new instance for resize")
    }

    fun release() {
        if (framebufferId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
        }
        if (colorTextureIds.any { it != 0 }) {
            GLES30.glDeleteTextures(colorAttachmentCount, colorTextureIds, 0)
            for (i in colorTextureIds.indices) colorTextureIds[i] = 0
        }
        if (depthTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(depthTextureId), 0)
            depthTextureId = 0
        }
        if (depthStencilRenderbufferId != 0) {
            GLES30.glDeleteRenderbuffers(1, intArrayOf(depthStencilRenderbufferId), 0)
            depthStencilRenderbufferId = 0
        }
    }

    private fun framebufferStatusToString(status: Int): String = when (status) {
        GLES30.GL_FRAMEBUFFER_COMPLETE -> "COMPLETE"
        GLES30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "INCOMPLETE_ATTACHMENT"
        GLES30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "MISSING_ATTACHMENT"
        GLES30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> "INCOMPLETE_DIMENSIONS"
        GLES30.GL_FRAMEBUFFER_UNSUPPORTED -> "UNSUPPORTED"
        else -> "UNKNOWN($status)"
    }
}

/**
 * Builder for framebuffers with common presets.
 */
object GLFramebufferFactory {
    fun forPreview(width: Int, height: Int): GLFramebuffer = GLFramebuffer(
        width = width,
        height = height,
        colorAttachmentCount = 1,
        hasDepth = true,
        hasStencil = false
    )

    fun forEffectPass(width: Int, height: Int): GLFramebuffer = GLFramebuffer(
        width = width,
        height = height,
        colorAttachmentCount = 2, // ping-pong for multi-pass effects
        hasDepth = false,
        hasStencil = false
    )

    fun forMaskPass(width: Int, height: Int): GLFramebuffer = GLFramebuffer(
        width = width,
        height = height,
        colorAttachmentCount = 1,
        hasDepth = false,
        hasStencil = true // stencil for mask operations
    )
}