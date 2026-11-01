package com.example.data.export

import com.example.model.AspectRatioType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure math of the export pipeline: preset sizes and the capability
 * fallback ladder. On the JVM no [android.media.MediaCodecList] exists, so
 * [CodecCapabilityChecker.resolve] must degrade gracefully to the requested
 * size with the default encoder instead of crashing or proposing a bigger
 * output than the user asked for.
 */
class ExportMathTest {

    @Test
    fun `heightFor maps all known labels`() {
        assertEquals(360, ExportPresets.heightFor("360P"))
        assertEquals(720, ExportPresets.heightFor("720P"))
        assertEquals(1080, ExportPresets.heightFor("1080P"))
        assertEquals(1080, ExportPresets.heightFor("1080p"))
        assertEquals(1080, ExportPresets.heightFor("1080"))
        assertEquals(2160, ExportPresets.heightFor("2160P"))
        assertEquals(2160, ExportPresets.heightFor("4K"))
        assertEquals(1080, ExportPresets.heightFor("garbage"))
    }

    @Test
    fun `outputSize keeps every dimension even`() {
        val (width, height) = ExportPresets.outputSize(AspectRatioType.RATIO_9_16, "1080P")
        assertEquals(0, width % 2)
        assertEquals(0, height % 2)
        assertEquals(1080, height)
        // 9 / 16 at 1080p height: rounded down to even.
        assertEquals(606, width)
    }

    @Test
    fun `outputSize for 16x9 at 1080p is exactly 1920x1080`() {
        assertEquals(1920 to 1080, ExportPresets.outputSize(AspectRatioType.RATIO_16_9, "1080P"))
    }

    @Test
    fun `outputSize for square 720p`() {
        assertEquals(720 to 720, ExportPresets.outputSize(AspectRatioType.RATIO_1_1, "720P"))
    }

    @Test
    fun `outputSize for 4x5 keeps the aspect`() {
        val (width, height) = ExportPresets.outputSize(AspectRatioType.RATIO_4_5, "1080P")
        assertEquals(864, width)
        assertEquals(1080, height)
    }

    @Test
    fun `labelFor maps ladder heights`() {
        assertEquals("4K", CodecCapabilityChecker.labelFor(2160))
        assertEquals("1080P", CodecCapabilityChecker.labelFor(1080))
        assertEquals("720P", CodecCapabilityChecker.labelFor(720))
        assertEquals("480P", CodecCapabilityChecker.labelFor(480))
        assertEquals("360P", CodecCapabilityChecker.labelFor(360))
        assertEquals("1080P", CodecCapabilityChecker.labelFor(999))
    }

    @Test
    fun `resolve degrades gracefully when no encoder matches`() {
        val target = CodecCapabilityChecker.resolve(AspectRatioType.RATIO_16_9, "4K", 30)
        assertEquals(null, target.mime)
        assertEquals(3840, target.width)
        assertEquals(2160, target.height)
        assertFalse(target.downgraded)
        assertEquals("4K", target.resolutionLabel)
    }

    @Test
    fun `resolve never proposes a larger output than requested`() {
        val target = CodecCapabilityChecker.resolve(AspectRatioType.RATIO_1_1, "720P", 60)
        assertEquals(720, target.height)
        assertEquals(720, target.width)
        assertEquals("720P", target.resolutionLabel)
        assertFalse(target.downgraded)
    }

    @Test
    fun `resolve keeps the requested label when not downgraded`() {
        val target = CodecCapabilityChecker.resolve(AspectRatioType.RATIO_9_16, "720P", 30)
        assertTrue(target.width > 0)
        assertTrue(target.height > 0)
        assertEquals(720, target.height)
    }
}
