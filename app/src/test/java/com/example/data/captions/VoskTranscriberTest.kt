package com.example.data.captions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser checks for the Vosk result format. The recogniser binary only runs
 * on a device, but the JSON contract is stable and small enough to verify on
 * the JVM with the real org.json artifact.
 */
class VoskTranscriberTest {

    @Test
    fun `parses vosk word results into millisecond timings`() {
        val json = """
            {
              "result": [
                {"word": "hello", "start": 0.12, "end": 0.45, "conf": 1.0},
                {"word": "world", "start": 0.50, "end": 1.10, "conf": 0.9}
              ],
              "text": "hello world"
            }
        """.trimIndent()
        val words = VoskTranscriber.wordsFromResult(json)
        assertEquals(2, words.size)
        assertEquals("hello", words[0].text)
        assertEquals(120L, words[0].startMs)
        assertEquals(450L, words[0].endMs)
        assertEquals("world", words[1].text)
        assertEquals(1100L, words[1].endMs)
    }

    @Test
    fun `malformed entries are dropped instead of crashing`() {
        val json = """
            {
              "result": [
                {"word": "", "start": 0.1, "end": 0.2},
                {"word": "noend", "start": 0.3},
                {"word": "ok", "start": 1.0, "end": 1.5}
              ]
            }
        """.trimIndent()
        val words = VoskTranscriber.wordsFromResult(json)
        assertEquals(1, words.size)
        assertEquals("ok", words[0].text)
    }

    @Test
    fun `empty blank and non-json results yield no words`() {
        assertTrue(VoskTranscriber.wordsFromResult("").isEmpty())
        assertTrue(VoskTranscriber.wordsFromResult("   ").isEmpty())
        assertTrue(VoskTranscriber.wordsFromResult("not json at all").isEmpty())
        assertTrue(VoskTranscriber.wordsFromResult("""{"text": "no words array"}""").isEmpty())
    }

    @Test
    fun `pcm bytes are little endian`() {
        val bytes = VoskTranscriber.toLittleEndianBytes(shortArrayOf(0x0102, -1))
        // 0x0102 little-endian: 02 then 01.
        assertEquals(0x02, bytes[0].toInt() and 0xFF)
        assertEquals(0x01, bytes[1].toInt() and 0xFF)
        // -1 (0xFFFF): FF FF.
        assertEquals(0xFF, bytes[2].toInt() and 0xFF)
        assertEquals(0xFF, bytes[3].toInt() and 0xFF)
    }
}
