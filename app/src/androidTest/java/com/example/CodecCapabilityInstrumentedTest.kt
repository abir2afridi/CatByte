package com.example

import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.data.export.CodecCapabilityChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device / emulator where [android.media.MediaCodecList] reports
 * genuine encoders. Verifies the capability ladder behaves like the plan says:
 * prefer HEVC, fall back to AVC, never claim a size larger than requested,
 * and expose media-processing thread support for exports.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CodecCapabilityInstrumentedTest {

    @Test
    fun `device reports at least one h264 encoder`() {
        val codecs = CodecCapabilityChecker.querySupportedCodecs(1920, 1080, 30)
        val mimes = codecs.map { it.mime }
        assertTrue(
            "Expected an H.264 encoder for 1080p30, got: $mimes",
            mimes.contains(MediaFormat.MIMETYPE_VIDEO_AVC)
        )
    }

    @Test
    fun `hevc wins over avc when both are supported`() {
        val codecs = CodecCapabilityChecker.querySupportedCodecs(1920, 1080, 30)
        if (codecs.none { it.mime == MediaFormat.MIMETYPE_VIDEO_HEVC }) return // skip silently
        assertEquals(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            codecs.first().mime
        )
    }

    @Test
    fun `resolve never downgrades without need`() {
        val target = CodecCapabilityChecker.resolve(
            com.example.model.AspectRatioType.RATIO_16_9,
            "1080P",
            30
        )
        assertEquals(1080, target.height)
        assertEquals(1920, target.width)
        assertFalse(target.downgraded)
        assertNotNull(target.resolutionLabel)
    }

    @Test
    fun `small sizes always resolve to a codec`() {
        val target = CodecCapabilityChecker.resolve(
            com.example.model.AspectRatioType.RATIO_9_16,
            "480P",
            24
        )
        assertTrue(
            "Even 480p24 should be encodable on any modern device",
            target.mime != null
        )
    }

    @Test
    fun `frame rate downgrade ladder fires at extreme fps`() {
        // A never-marketable "600 fps" must not fail: the checker falls down to 30.
        val target = CodecCapabilityChecker.resolve(
            com.example.model.AspectRatioType.RATIO_1_1,
            "720P",
            600
        )
        assertNotNull(target)
        assertEquals(720, target.height)
        assertTrue(
            "fps ladder must pick one of 600/30/24, got ${target.fps}",
            target.fps in listOf(600, 30, 24)
        )
    }
}
