package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pipeline checks for the real auto-caption path: resampler -> fake
 * transcriber -> CaptionAssembler. The recogniser itself is faked, which is
 * the point - everything around it is verified without a model.
 */
class CaptionPipelineTest {

    // ------------------------------------------------------------------
    // Resampler
    // ------------------------------------------------------------------

    @Test
    fun `resampler keeps duration and downmixes stereo`() {
        // 1 second of 48 kHz stereo: every sample 1000 in L, -2000 in R.
        val input = ShortArray(48_000 * 2) { i ->
            if (i % 2 == 0) 1000 else -2000
        }
        val out = Resampler.toMonoAtRate(input, 2, 48_000, 16_000)
        assertEquals(16_000, out.size)
        // Every output sample is the stereo average: (1000 + -2000) / 2 = -500.
        assertTrue(out.all { it.toInt() == -500 })
    }

    @Test
    fun `resampler to the same rate is identity for mono`() {
        val input = ShortArray(1000) { (it % 327).toShort() }
        val out = Resampler.toMonoAtRate(input, 1, 16_000, 16_000)
        assertEquals(1000, out.size)
        assertTrue(input.contentEquals(out))
    }

    @Test
    fun `resampler upsampling stretches values without skipping`() {
        // 4 samples at 1 kHz -> 8 samples at 2 kHz (2x upsample).
        val input = shortArrayOf(100, 200, 300, 400)
        val out = Resampler.toMonoAtRate(input, 1, 1_000, 2_000)
        assertEquals(8, out.size)
        assertEquals(100, out[0].toInt())
        assertEquals(150, out[1].toInt()) // interpolated halfway
        assertEquals(200, out[2].toInt())
        assertEquals(250, out[3].toInt()) // interpolated halfway
        assertEquals(350, out[5].toInt()) // halfway between 300 and 400
        assertEquals(400, out[7].toInt()) // past the last frame holds the final sample
    }

    @Test
    fun `resampler handles empty and degenerate inputs`() {
        assertEquals(0, Resampler.toMonoAtRate(ShortArray(0), 1, 44_100, 16_000).size)
        // A single mono frame resampled to the same rate is that frame.
        val single = Resampler.toMonoAtRate(shortArrayOf(-77), 1, 16_000, 16_000)
        assertEquals(1, single.size)
        assertEquals(-77, single[0].toInt())
    }

    // ------------------------------------------------------------------
    // CaptionAssembler with a fake transcriber
    // ------------------------------------------------------------------

    private class FakeTranscriber(
        private val words: List<WordTiming>
    ) : SpeechTranscriber {
        override fun recognize(samples: ShortArray, sampleRate: Int): List<WordTiming> = words
    }

    private fun words(vararg timings: Triple<String, Long, Long>): List<WordTiming> =
        timings.map { WordTiming(it.first, it.second, it.third) }

    @Test
    fun `assembler packs words into readable cues`() {
        val wordList = words(
            Triple("আমি", 200, 600),
            Triple("ভালো", 700, 1_100),
            Triple("আছি", 1_200, 1_600),
            Triple("ধন্যবাদ", 5_000, 5_600),
            Triple("ভিডিওটি", 5_700, 6_200),
            Triple("দেখার", 6_300, 6_700),
            Triple("জন্য", 6_800, 7_100)
        )
        val overlays = CaptionAssembler.overlaysFromWords(wordList, totalDurationMs = 8_000)

        // The 3.4 s gap splits the words into two cues.
        assertEquals(2, overlays.size)
        assertEquals("আমি ভালো আছি", overlays[0].text)
        assertEquals("ধন্যবাদ ভিডিওটি দেখার জন্য", overlays[1].text)
        // Timings survive the trip.
        assertEquals(200L, overlays[0].startTimelineMs)
        assertTrue(overlays[0].durationMs >= 1_400L)
        // Bottom-centred subtitle placement from the bridge.
        assertEquals(CaptionBridge.POS_Y, overlays[0].posY, 1e-6f)
    }

    @Test
    fun `assembler drops insane words and clamps past the video end`() {
        val wordList = words(
            Triple("", 100, 200),            // blank text -> dropped
            Triple("ghost", 3_000, 2_000),   // end before start -> dropped
            Triple("পরের", 6_000, 12_000),   // extends past the 8 s video
            Triple("শব্দ", 6_100, 6_500)
        )
        val overlays = CaptionAssembler.overlaysFromWords(wordList, totalDurationMs = 8_000)
        assertEquals(1, overlays.size)
        assertEquals("পরের শব্দ", overlays[0].text)
        assertTrue(
            "caption must not outlive the video",
            overlays[0].startTimelineMs + overlays[0].durationMs <= 8_000L
        )
    }

    @Test
    fun `assembler returns nothing for silence`() {
        val overlays = CaptionAssembler.overlaysFromWords(emptyList(), totalDurationMs = 5_000)
        assertTrue(overlays.isEmpty())
    }

    @Test
    fun `assembler keeps ids unique through the id factory`() {
        var counter = 0
        val overlays = CaptionAssembler.overlaysFromWords(
            words(
                Triple("one", 100, 300),
                Triple("two", 400, 600)
            ),
            totalDurationMs = 4_000,
            idFactory = { "id-${counter++}" }
        )
        assertEquals(1, overlays.size)
        assertEquals("id-0", overlays[0].id)
    }

    // ------------------------------------------------------------------
    // WordTiming sanity
    // ------------------------------------------------------------------

    @Test
    fun `word timing sanity flags malformed words`() {
        assertTrue(WordTiming("hi", 0, 100).isSane)
        assertTrue(!WordTiming("  ", 0, 100).isSane)
        assertTrue(!WordTiming("hi", 500, 400).isSane)
    }
}
