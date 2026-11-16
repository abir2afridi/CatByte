package com.example.data.export

import android.graphics.Bitmap
import android.graphics.Color
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay

/**
 * A texture overlay that is only visible between [startUs] and [endUs].
 * Outside of that window a fully transparent texture is used so the GL
 * pipeline always has a valid texture bound.
 */
class TimedBitmapOverlay(
    private val bitmap: Bitmap,
    private val startUs: Long,
    private val endUs: Long,
    private val overlaySettings: StaticOverlaySettings = StaticOverlaySettings.Builder().build()
) : TextureOverlay() {

    private var contentTextureId: Int = 0
    private var blankTextureId: Int = 0
    private var released = false

    override fun getTextureId(presentationTimeUs: Long): Int {
        if (released) return 0
        val active = presentationTimeUs >= startUs && presentationTimeUs <= endUs
        if (active) {
            if (contentTextureId == 0) {
                contentTextureId = runCatching { GlUtil.createTexture(bitmap) }.getOrDefault(0)
            }
            return contentTextureId
        }
        if (blankTextureId == 0) {
            blankTextureId = runCatching {
                val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                blank.setPixel(0, 0, Color.TRANSPARENT)
                GlUtil.createTexture(blank)
            }.getOrDefault(0)
        }
        return blankTextureId
    }

    override fun getTextureSize(presentationTimeUs: Long): Size =
        Size(bitmap.width, bitmap.height)

    override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings = overlaySettings

    override fun release() {
        released = true
        if (contentTextureId != 0) {
            runCatching { GlUtil.deleteTexture(contentTextureId) }
            contentTextureId = 0
        }
        if (blankTextureId != 0) {
            runCatching { GlUtil.deleteTexture(blankTextureId) }
            blankTextureId = 0
        }
    }
}
