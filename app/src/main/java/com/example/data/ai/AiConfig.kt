package com.example.data.ai

/**
 * Connection settings for an OpenAI-compatible AI endpoint.
 *
 * The user types a base URL (and optionally a key/model) in App settings, so
 * any compatible gateway works: api.openai.com, a self-hosted LiteLLM proxy,
 * LM Studio, Ollama's OpenAI shim, Azure-style gateways, etc.
 */
data class AiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val imageModel: String = "dall-e-3",
    /** Speech-to-text model used by AI auto captions (Whisper-compatible). */
    val speechModel: String = "whisper-1"
) {
    /** AI features are only offered when an endpoint is configured. */
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

/** Result of an AI call: a value or a human-readable failure reason. */
sealed interface AiResult<out T> {
    data class Ok<T>(val value: T) : AiResult<T>
    data class Failure(val message: String) : AiResult<Nothing>
}

/** Unwraps a successful result or returns null. */
fun <T> AiResult<T>.valueOrNull(): T? = (this as? AiResult.Ok)?.value

/** Human-readable failure, or null when the call succeeded. */
fun AiResult<*>.errorOrNull(): String? = (this as? AiResult.Failure)?.message
