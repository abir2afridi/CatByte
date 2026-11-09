package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BanglaTextTest {

    @Test
    fun `digits convert both ways`() {
        assertEquals("২০২৬", BanglaNumerals.toBangla("2026"))
        assertEquals("2026", BanglaNumerals.toLatin("২০২৬"))
        assertEquals("abc", BanglaNumerals.toBangla("abc"))
        assertTrue(BanglaNumerals.isBanglaDigit('৫'))
        assertFalse(BanglaNumerals.isBanglaDigit('5'))
    }

    @Test
    fun `timecode follows the app language`() {
        assertEquals("01:05", BanglaNumerals.timecodeLabel(65_000L, bangla = false))
        assertEquals("০১:০৫", BanglaNumerals.timecodeLabel(65_000L, bangla = true))
    }

    @Test
    fun `wrapping never severs a vowel sign`() {
        val chunks = BanglaLineBreaker.splitLongWord("বাংলাদেশের", 4)
        assertTrue(chunks.isNotEmpty())
        chunks.forEach { chunk ->
            assertTrue("chunk starts with a mark: $chunk", BanglaLineBreaker.canBreakBefore(chunk.first()))
        }
    }

    @Test
    fun `shaping safety rejects a leading mark`() {
        assertTrue(BanglaLineBreaker.isShapingSafe("বাংলা"))
        assertFalse(BanglaLineBreaker.isShapingSafe("\u09BFবাংলা"))
        assertFalse(BanglaLineBreaker.canBreakBefore('\u09CD'))
        assertTrue(BanglaLineBreaker.canBreakBetween('ক', 'খ'))
        assertFalse(BanglaLineBreaker.canBreakBetween('ক', '\u09CD'))
    }

    @Test
    fun `wrap respects characters and lines`() {
        val lines = BanglaLineBreaker.wrap(
            "আমি বাংলায় ভিডিও এডিট করি প্রতিদিন",
            maxCharsPerLine = 12,
            maxLines = 2
        )
        assertTrue(lines.size in 1..2)
        lines.forEach { assertTrue("line too long: $it", it.length <= 12) }
    }

    @Test
    fun `caption fit keeps two lines and a readable size`() {
        val fit = CaptionLayout.fit(
            text = "একটি দীর্ঘ ক্যাপশন টেক্সট যা দুই লাইনে ফিট হতে হবে",
            canvasWidthPx = 1080
        )
        assertTrue(fit.lineCount <= 2)
        assertTrue(fit.fontSizeSp >= 11f)
        assertTrue(fit.lines.all { it.isNotBlank() })
    }

    @Test
    fun `empty text produces no lines`() {
        assertTrue(BanglaLineBreaker.wrap("   ", 10).isEmpty())
        assertEquals(0, CaptionLayout.fit("", 1080).lineCount)
    }
}
