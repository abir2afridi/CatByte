package com.example.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class DuckGainProviderTest {

    private fun provider(
        startTimelineMs: Long,
        voiceRanges: List<LongRange>,
        gain: Float = 1f
    ) = DuckGainProvider(
        base = ConstantGainProvider(
            gain = gain,
            fadeInMs = 0L,
            fadeOutMs = 0L,
            durationMs = 10_000L
        ),
        startTimelineMs = startTimelineMs,
        voiceRanges = voiceRanges
    )

    private fun DuckGainProvider.gainAt(positionMs: Long): Float =
        getGainFactorAtSamplePosition(positionMs * 1000L, 44_100)

    @Test
    fun musicDucksWhileVoiceoverIsAudible() {
        val duck = provider(startTimelineMs = 0L, voiceRanges = listOf(2_000L..4_000L))
        assertEquals(1f, duck.gainAt(500L), 0.001f)
        assertEquals(0.65f, duck.gainAt(3_000L), 0.001f)
        assertEquals(1f, duck.gainAt(6_000L), 0.001f)
    }

    @Test
    fun itemPositionIsLiftedOntoTimeline() {
        // Music starts at 2s on the timeline, voice-over at 4s.
        val duck = provider(startTimelineMs = 2_000L, voiceRanges = listOf(4_000L..6_000L))
        // 1s into the music = 3s on the timeline: still outside the voice-over.
        assertEquals(1f, duck.gainAt(1_000L), 0.001f)
        // 3s into the music = 5s on the timeline: inside the voice-over.
        assertEquals(0.65f, duck.gainAt(3_000L), 0.001f)
    }

    @Test
    fun attackAndReleaseAreSmooth() {
        val duck = provider(startTimelineMs = 0L, voiceRanges = listOf(2_000L..4_000L))
        // 60ms into a 120ms attack.
        assertEquals(1f - 0.35f * 0.5f, duck.gainAt(1_940L), 0.005f)
        // 150ms into a 300ms release.
        assertEquals(1f - 0.35f * 0.5f, duck.gainAt(4_150L), 0.005f)
    }

    @Test
    fun noVoiceoverLeavesGainUntouched() {
        val duck = provider(startTimelineMs = 0L, voiceRanges = emptyList())
        assertEquals(1f, duck.gainAt(3_000L), 0.001f)
    }

    @Test
    fun duckingMultipliesClipVolume() {
        val duck = provider(startTimelineMs = 0L, voiceRanges = listOf(2_000L..4_000L), gain = 0.8f)
        assertEquals(0.8f, duck.gainAt(500L), 0.001f)
        assertEquals(0.8f * 0.65f, duck.gainAt(3_000L), 0.001f)
    }
}
