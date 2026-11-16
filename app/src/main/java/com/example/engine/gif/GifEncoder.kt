package com.example.engine.gif

import android.graphics.Bitmap
import java.io.OutputStream

/**
 * A real GIF89a encoder (no dependencies, no NDK).
 *
 * The GIF tab used to render MP4 through the video pipeline, so the file was
 * never actually a GIF. This encoder writes a proper animated GIF:
 * GIF89a header, a fixed 6x6x6 colour cube palette, per-frame Graphic Control
 * Extension (delay + disposal), LZW-compressed image data and the NETSCAPE2.0
 * loop block.
 *
 * The palette is a cube rather than a per-frame median cut on purpose: mapping
 * a pixel is O(1) (three integer divisions), which keeps a 50-frame export
 * fast on a phone, and photographic gradients still look acceptable.
 */
object GifEncoder {

    private const val CUBE_STEPS = 6
    private const val PALETTE_SIZE = 256
    private const val MIN_CODE_SIZE = 8

    /**
     * Encodes [frames] (already scaled to [width] x [height]) into [output].
     *
     * @param delayCs per-frame delay in hundredths of a second (2 = 20 ms).
     */
    fun encode(
        frameCount: Int,
        width: Int,
        height: Int,
        output: OutputStream,
        /** Delay between frames in hundredths of a second (2 = 20 ms). */
        delayCs: Int = 4,
        loop: Boolean = true,
        /**
         * Produces frame [index], already at any size (it is scaled here).
         * Frames are recycled right after they are written, so a long export
         * never holds more than one bitmap in memory.
         */
        frameAt: (Int) -> Bitmap?
    ) {
        require(frameCount > 0) { "A GIF needs at least one frame" }
        require(width > 0 && height > 0) { "Invalid GIF size ${width}x$height" }

        output.write("GIF89a".toByteArray(Charsets.US_ASCII))

        // Logical screen descriptor.
        writeShort(output, width)
        writeShort(output, height)
        output.write(0xF7) // global colour table, 8-bit colour, 256 entries
        output.write(0x00) // background colour index
        output.write(0x00) // pixel aspect ratio

        writeGlobalColorTable(output)

        if (loop) writeLoopExtension(output)

        for (index in 0 until frameCount) {
            val frame = frameAt(index) ?: continue
            writeGraphicControl(output, delayCs.coerceAtLeast(1))
            writeImageDescriptor(output, width, height)
            val indices = quantize(frame, width, height)
            if (!frame.isRecycled) frame.recycle()
            writeLzwData(output, indices)
        }

        output.write(0x3B) // trailer
        output.flush()
    }

    // ------------------------------------------------------------ palette

    /** Fixed 6x6x6 RGB cube (216 colours) padded to 256 with a grey ramp. */
    private fun writeGlobalColorTable(output: OutputStream) {
        val step = 255 / (CUBE_STEPS - 1)
        for (r in 0 until CUBE_STEPS) {
            for (g in 0 until CUBE_STEPS) {
                for (b in 0 until CUBE_STEPS) {
                    output.write(r * step)
                    output.write(g * step)
                    output.write(b * step)
                }
            }
        }
        // Pad the remaining entries with a neutral ramp so the table is 256.
        for (i in CUBE_STEPS * CUBE_STEPS * CUBE_STEPS until PALETTE_SIZE) {
            val grey = (i - 216) * 6
            output.write(grey)
            output.write(grey)
            output.write(grey)
        }
    }

    /**
     * Maps every pixel to its cube index. Pure function of the bitmap, exposed
     * for tests: index = r6*36 + g6*6 + b6.
     */
    fun quantize(frame: Bitmap, width: Int, height: Int): ByteArray {
        val scaled = if (frame.width != width || frame.height != height) {
            Bitmap.createScaledBitmap(frame, width, height, true)
        } else {
            frame
        }
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== frame) scaled.recycle()

        val out = ByteArray(pixels.size)
        for (i in pixels.indices) {
            out[i] = cubeIndex(pixels[i]).toByte()
        }
        return out
    }

    /** Cube index of an ARGB pixel (alpha is ignored: GIF has no alpha). */
    fun cubeIndex(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val r6 = r * CUBE_STEPS / 256
        val g6 = g * CUBE_STEPS / 256
        val b6 = b * CUBE_STEPS / 256
        return r6 * CUBE_STEPS * CUBE_STEPS + g6 * CUBE_STEPS + b6
    }

    // -------------------------------------------------------------- blocks

    private fun writeLoopExtension(output: OutputStream) {
        output.write(0x21) // extension introducer
        output.write(0xFF) // application extension
        output.write(0x0B) // block size 11
        output.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        output.write(0x03) // sub-block size
        output.write(0x01) // loop sub-block id
        writeShort(output, 0) // 0 = forever
        output.write(0x00) // block terminator
    }

    private fun writeGraphicControl(output: OutputStream, delayCs: Int) {
        output.write(0x21) // extension
        output.write(0xF9) // graphic control label
        output.write(0x04) // block size
        output.write(0x08) // disposal method 2 (restore to background), no transparency
        writeShort(output, delayCs)
        output.write(0x00) // transparent colour index
        output.write(0x00) // block terminator
    }

    private fun writeImageDescriptor(output: OutputStream, width: Int, height: Int) {
        output.write(0x2C) // image separator
        writeShort(output, 0) // left
        writeShort(output, 0) // top
        writeShort(output, width)
        writeShort(output, height)
        output.write(0x00) // no local colour table, not interlaced
    }

    private fun writeLzwData(output: OutputStream, indices: ByteArray) {
        output.write(MIN_CODE_SIZE)
        val compressed = lzwCompress(indices, MIN_CODE_SIZE)
        var offset = 0
        while (offset < compressed.size) {
            val chunk = minOf(255, compressed.size - offset)
            output.write(chunk)
            output.write(compressed, offset, chunk)
            offset += chunk
        }
        output.write(0x00) // block terminator
    }

    // ---------------------------------------------------------------- LZW

    /**
     * GIF-flavoured LZW: clear code = 2^minCodeSize, end code = clear + 1,
     * first free code = clear + 2, code width grows 9 -> 12 then a clear code
     * is emitted and the dictionary resets.
     */
    fun lzwCompress(indices: ByteArray, minCodeSize: Int): ByteArray {
        val clearCode = 1 shl minCodeSize
        val endCode = clearCode + 1
        var codeSize = minCodeSize + 1
        var nextCode = endCode + 1

        val out = BitWriter()
        val dictionary = HashMap<Int, Int>(4096)

        out.write(clearCode, codeSize)
        if (indices.isEmpty()) {
            out.write(endCode, codeSize)
            return out.finish()
        }

        var current = indices[0].toInt() and 0xFF
        for (i in 1 until indices.size) {
            val next = indices[i].toInt() and 0xFF
            val key = (current shl 8) or next
            val existing = dictionary[key]
            if (existing != null) {
                current = existing
                continue
            }
            out.write(current, codeSize)
            if (nextCode == 4096) {
                // Dictionary is full: reset it, exactly like the decoder does.
                out.write(clearCode, codeSize)
                dictionary.clear()
                codeSize = minCodeSize + 1
                nextCode = endCode + 1
            } else {
                dictionary[key] = nextCode
                nextCode++
                // The decoder bumps its code width when the next free slot
                // reaches 2^codeSize; the encoder must stay in lock-step.
                if (nextCode == (1 shl codeSize) && codeSize < 12) {
                    codeSize++
                }
            }
            current = next
        }

        out.write(current, codeSize)
        out.write(endCode, codeSize)
        return out.finish()
    }

    private fun writeShort(output: OutputStream, value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }

    /** Packs variable-width LZW codes into bytes, least significant bit first. */
    private class BitWriter {
        private val bytes = ArrayList<Byte>(1024)
        private var accumulator = 0
        private var bitCount = 0

        fun write(code: Int, codeSize: Int) {
            accumulator = accumulator or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                bytes.add((accumulator and 0xFF).toByte())
                accumulator = accumulator shr 8
                bitCount -= 8
            }
        }

        fun finish(): ByteArray {
            if (bitCount > 0) {
                bytes.add((accumulator and 0xFF).toByte())
                accumulator = 0
                bitCount = 0
            }
            return bytes.toByteArray()
        }
    }
}
