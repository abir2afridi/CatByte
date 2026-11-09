package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleIOTest {

    private val srt = """
        1
        00:00:01,000 --> 00:00:03,500
        আমি বাংলায় ভিডিও বানাই

        2
        00:00:04,000 --> 00:00:06,000
        Second cue
    """.trimIndent()

    @Test
    fun `srt parses cues with bangla text`() {
        val cues = SubtitleIO.parseSrt(srt)
        assertEquals(2, cues.size)
        assertEquals(1_000L, cues[0].startMs)
        assertEquals(3_500L, cues[0].endMs)
        assertEquals("আমি বাংলায় ভিডিও বানাই", cues[0].text)
        assertEquals(4_000L, cues[1].startMs)
    }

    @Test
    fun `srt round trips without moving timings`() {
        val cues = SubtitleIO.parseSrt(srt)
        val exported = SubtitleIO.toSrt(cues)
        val reparsed = SubtitleIO.parseSrt(exported)
        assertEquals(cues, reparsed)
    }

    @Test
    fun `vtt parses header and cue settings`() {
        val vtt = "WEBVTT\n\n00:00:01.500 --> 00:00:02.000 line:90%\nHello world\n"
        val cues = SubtitleIO.parse(vtt)
        assertEquals(1, cues.size)
        assertEquals(1_500L, cues[0].startMs)
        assertEquals(2_000L, cues[0].endMs)
        assertEquals("Hello world", cues[0].text)
    }

    @Test
    fun `vtt export is parseable again`() {
        val cues = listOf(SubtitleCue(0L, 1_250L, "a"), SubtitleCue(2_000L, 3_000L, "b"))
        val parsed = SubtitleIO.parseVtt(SubtitleIO.toVtt(cues))
        assertEquals(cues, parsed)
    }

    @Test
    fun `garbage input produces no cues`() {
        assertTrue(SubtitleIO.parse("this is not a subtitle file").isEmpty())
        assertTrue(SubtitleIO.parse("").isEmpty())
    }

    @Test
    fun `retime clamps at the start of the timeline`() {
        val shifted = SubtitleIO.retime(listOf(SubtitleCue(1_000L, 3_000L, "a")), -2_000L)
        assertEquals(0L, shifted.single().startMs)
        assertEquals(1_000L, shifted.single().endMs)
    }

    @Test
    fun `caption wrapping respects the line budget`() {
        val cue = SubtitleCue(0L, 2_000L, "একটি অনেক বড় ক্যাপশন লাইন যা ভেঙে দেখাতে হবে ঠিকভাবে")
        val lines = SubtitleIO.wrap(cue, SubtitleStyle(maxCharsPerLine = 12, maxLines = 2))
        assertTrue(lines.size in 1..2)
        lines.forEach { assertTrue("line too long: $it", it.length <= 12) }
    }

    @Test
    fun `words become readable cues`() {
        val words = (0 until 15).map { index ->
            TimedWord("word$index", index * 300L, index * 300L + 280L)
        }
        val cues = SubtitleIO.cuesFromWords(words)
        assertTrue(cues.size >= 2)
        cues.forEach { cue ->
            assertTrue(cue.durationMs >= SubtitleStyle().minCueDurationMs)
            assertTrue(cue.text.isNotBlank())
        }
    }

    @Test
    fun `karaoke words cover the cue`() {
        val cue = SubtitleCue(0L, 1_000L, "a bb ccc")
        val words = SubtitleIO.karaokeSplit(cue)
        assertEquals(3, words.size)
        assertEquals(0L, words.first().startMs)
        assertTrue(words.last().endMs <= 1_000L)
        assertTrue(words.sumOf { it.endMs - it.startMs } >= 900L)
    }

    @Test
    fun `overlapping cues are reported`() {
        val problems = SubtitleIO.validate(
            listOf(SubtitleCue(0L, 2_000L, "a"), SubtitleCue(1_000L, 3_000L, "b")),
            projectDurationMs = 10_000L
        )
        assertTrue(problems.any { it.contains("overlap") })
    }
}
