package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The karaoke caption model: active-word lookup and the offsets the renderers
 * use to place the highlight box over the right substring.
 */
class KaraokeTextTest {

    private fun overlay(karaokeEnabled: Boolean = true): TextOverlay {
        val words = listOf(
            KaraokeWord("আমি", 200, 500),
            KaraokeWord("ভালো", 600, 900),
            KaraokeWord("আছি", 1000, 1300)
        )
        return TextOverlay(
            id = "t1",
            text = "আমি ভালো আছি",
            startTimelineMs = 1_000,
            durationMs = 2_000,
            karaokeWords = words,
            karaokeEnabled = karaokeEnabled
        )
    }

    @Test
    fun `active word follows the local time`() {
        val overlay = overlay()
        assertNull(overlay.activeKaraokeIndex(150))   // before speech
        assertEquals(0, overlay.activeKaraokeIndex(300))
        assertEquals(1, overlay.activeKaraokeIndex(700))
        assertEquals(2, overlay.activeKaraokeIndex(1_100))
        assertNull(overlay.activeKaraokeIndex(1_500)) // speech over, cue still up
    }

    @Test
    fun `disabled karaoke never highlights`() {
        assertNull(overlay(karaokeEnabled = false).activeKaraokeIndex(300))
    }

    @Test
    fun `offsets map words into the caption text`() {
        val offsets = overlay().karaokeOffsetsInText()
        // "আমি" (3 code units) + space, then "ভালো" (4) + space.
        assertEquals(listOf(0, 4, 9), offsets)
        // The highlighted substring is exactly the word.
        val text = "আমি ভালো আছি"
        offsets!!.forEachIndexed { index, start ->
            val word = overlay().karaokeWords[index]
            assertEquals(word.text, text.substring(start, start + word.text.length))
        }
    }

    @Test
    fun `mismatched words yield no offsets instead of wrong highlights`() {
        val broken = overlay().copy(
            text = "সম্পূর্ণ ভিন্ন লেখা",
            karaokeWords = listOf(KaraokeWord("আমি", 0, 100))
        )
        assertNull(broken.karaokeOffsetsInText())
    }

    @Test
    fun `plain text overlays carry no karaoke data`() {
        val plain = TextOverlay(id = "t2", text = "Hello")
        assertTrue(plain.karaokeWords.isEmpty())
        assertNull(plain.activeKaraokeIndex(0))
        assertNull(plain.karaokeOffsetsInText())
    }
}
