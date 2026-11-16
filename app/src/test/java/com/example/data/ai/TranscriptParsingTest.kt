package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Endpoint payloads vary; the parser must accept both shapes gracefully. */
class TranscriptParsingTest {

    @Test
    fun `verbose json segments are converted to milliseconds`() {
        val raw = """
            {
              "text": "hello world",
              "segments": [
                {"start": 0.5, "end": 1.75, "text": " hello "},
                {"start": 2.0, "end": 3.2, "text": "world"}
              ]
            }
        """.trimIndent()

        val result = AiClient.parseTranscript(raw, fallbackDurationMs = 10_000L)
        val transcript = (result as AiResult.Ok).value
        assertEquals(2, transcript.segments.size)
        assertEquals(500L, transcript.segments[0].startMs)
        assertEquals(1_750L, transcript.segments[0].endMs)
        assertEquals("hello", transcript.segments[0].text)
        assertEquals(3_200L, transcript.segments[1].endMs)
    }

    @Test
    fun `plain text response spans the fallback duration`() {
        val result = AiClient.parseTranscript("""{"text":"just text"}""", fallbackDurationMs = 8_000L)
        val transcript = (result as AiResult.Ok).value
        assertEquals(1, transcript.segments.size)
        assertEquals(0L, transcript.segments.single().startMs)
        assertEquals(8_000L, transcript.segments.single().endMs)
        assertEquals("just text", transcript.segments.single().text)
    }

    @Test
    fun `blank segments and text fail with a readable reason`() {
        val result = AiClient.parseTranscript("""{"text":"","segments":[]}""", 1_000L)
        assertTrue(result is AiResult.Failure)
        assertTrue((result as AiResult.Failure).message.contains("no speech"))
    }

    @Test
    fun `malformed json fails instead of throwing`() {
        val result = AiClient.parseTranscript("not json", 1_000L)
        assertTrue(result is AiResult.Failure)
    }

    @Test
    fun `empty segment text is skipped`() {
        val raw = """{"segments":[{"start":0,"end":1,"text":"  "},{"start":1,"end":2,"text":"kept"}]}"""
        val transcript = (AiClient.parseTranscript(raw, 0L) as AiResult.Ok).value
        assertEquals(1, transcript.segments.size)
        assertEquals("kept", transcript.segments.single().text)
    }
}
