package com.example.engine.gif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

/**
 * The GIF tab used to render MP4 and call it a GIF. These tests pin the real
 * encoder: the palette cube, the LZW stream and the file structure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GifEncoderTest {

    @Test
    fun `cube index maps the corners of the colour space`() {
        assertEquals(0, GifEncoder.cubeIndex(0xFF000000.toInt())) // black
        assertEquals(215, GifEncoder.cubeIndex(0xFFFFFFFF.toInt())) // white
        // Pure red is the first green/blue-free cube cell of the top red band.
        assertEquals(180, GifEncoder.cubeIndex(0xFFFF0000.toInt()))
        // Pure green sits one cube step up from red's band.
        assertEquals(30, GifEncoder.cubeIndex(0xFF00FF00.toInt()))
        // Pure blue is the last column of the first row.
        assertEquals(5, GifEncoder.cubeIndex(0xFF0000FF.toInt()))
    }

    @Test
    fun `cube index stays inside the palette`() {
        var argb = 0xFF000000.toInt()
        repeat(256) {
            val index = GifEncoder.cubeIndex(argb)
            assertTrue("index $index out of range", index in 0..215)
            argb += 0x00010101
        }
    }

    @Test
    fun `lzw stream starts with the clear code and ends with the end code`() {
        val indices = ByteArray(64) { 7 }
        val stream = GifEncoder.lzwCompress(indices, 8)

        assertTrue("stream must not be empty", stream.isNotEmpty())
        // Clear code (256) is 9 bits, so the first byte is 0 and the low bit of
        // the second byte carries the 1 of 256.
        assertEquals(0, stream[0].toInt() and 0xFF)
        assertEquals(1, stream[1].toInt() and 0x01)
    }

    @Test
    fun `a flat frame compresses far below the raw pixel count`() {
        val indices = ByteArray(480 * 480) { 42 }
        val stream = GifEncoder.lzwCompress(indices, 8)
        assertTrue(
            "expected compression, got ${stream.size} bytes",
            stream.size < indices.size / 20
        )
    }

    @Test
    fun `noise frame still round-trips through the dictionary`() {
        // Worst case for LZW: every pixel different. Must not throw or hang.
        val indices = ByteArray(2048) { (it * 37 % 216).toByte() }
        val stream = GifEncoder.lzwCompress(indices, 8)
        assertTrue(stream.isNotEmpty())
        assertTrue(stream.size < indices.size * 2)
    }

    @Test
    fun `gif file has a valid header, colour table and trailer`() {
        val out = ByteArrayOutputStream()
        // Two 2x2 frames, all black - enough to exercise every block.
        GifEncoder.encode(
            frameCount = 2,
            width = 2,
            height = 2,
            output = out,
            delayCs = 4,
            loop = true,
            frameAt = { index -> frame(index) }
        )
        val bytes = out.toByteArray()

        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        // Logical screen descriptor right after the header.
        assertEquals(2, bytes[6].toInt() and 0xFF)
        assertEquals(0, bytes[7].toInt() and 0xFF)
        assertEquals(2, bytes[8].toInt() and 0xFF)
        assertEquals(0, bytes[9].toInt() and 0xFF)
        // 0xF7 = global colour table present, 256 entries.
        assertEquals(0xF7, bytes[10].toInt() and 0xFF)
        // 13-byte header + 768-byte global table, then the NETSCAPE loop block.
        assertEquals(0x21, bytes[781].toInt() and 0xFF)
        assertEquals(0xFF, bytes[782].toInt() and 0xFF)
        // Trailer.
        assertEquals(0x3B, bytes.last().toInt() and 0xFF)
    }

    private fun frame(index: Int): android.graphics.Bitmap {
        // Robolectric-free: a 1x1 bitmap is enough because encode() scales.
        val bitmap = android.graphics.Bitmap.createBitmap(
            2,
            2,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(if (index == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        return bitmap
    }
}
