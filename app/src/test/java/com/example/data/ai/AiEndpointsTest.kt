package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Users paste many shapes of base URL into Settings → AI. The normalisation is
 * pure logic, so it is pinned here instead of being discovered on device.
 */
class AiEndpointsTest {

    @Test
    fun `bare host gets the v1 chat path`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiEndpoints.chatCompletions("https://api.openai.com")
        )
    }

    @Test
    fun `trailing slashes are ignored`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiEndpoints.chatCompletions("https://api.openai.com/")
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiEndpoints.chatCompletions("  https://api.openai.com//  ")
        )
    }

    @Test
    fun `already versioned base keeps its version`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiEndpoints.chatCompletions("https://api.openai.com/v1")
        )
        assertEquals(
            "http://192.168.0.10:1234/v1/chat/completions",
            AiEndpoints.chatCompletions("http://192.168.0.10:1234/v1")
        )
    }

    @Test
    fun `full endpoint is passed through`() {
        val full = "https://gateway.example.com/v1/chat/completions"
        assertEquals(full, AiEndpoints.chatCompletions(full))
    }

    @Test
    fun `image endpoint mirrors the chat rules`() {
        assertEquals(
            "https://api.openai.com/v1/images/generations",
            AiEndpoints.imageGenerations("https://api.openai.com")
        )
        assertEquals(
            "https://api.openai.com/v1/images/generations",
            AiEndpoints.imageGenerations("https://api.openai.com/v1/")
        )
    }

    @Test
    fun `empty base url stays empty`() {
        assertEquals("", AiEndpoints.chatCompletions(""))
        assertEquals("", AiEndpoints.imageGenerations("   "))
    }

    @Test
    fun `only http and https are accepted`() {
        assertTrue(AiEndpoints.isValidBaseUrl("https://api.openai.com"))
        assertTrue(AiEndpoints.isValidBaseUrl("http://localhost:8080"))
        assertFalse(AiEndpoints.isValidBaseUrl("api.openai.com"))
        assertFalse(AiEndpoints.isValidBaseUrl("ftp://example.com"))
        assertFalse(AiEndpoints.isValidBaseUrl(""))
    }

    @Test
    fun `config reports whether an endpoint exists`() {
        assertFalse(AiConfig().isConfigured)
        assertTrue(AiConfig(baseUrl = "https://api.openai.com").isConfigured)
    }
}
