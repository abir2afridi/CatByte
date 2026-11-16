package com.example.data.ai

/**
 * Builds the concrete request URLs from whatever base URL the user typed.
 *
 * Users paste very different shapes of base URL, so normalisation happens here
 * (and is unit-tested) instead of being scattered through the client:
 *
 *   https://api.openai.com                 -> https://api.openai.com/v1/chat/completions
 *   https://api.openai.com/                -> https://api.openai.com/v1/chat/completions
 *   https://api.openai.com/v1              -> https://api.openai.com/v1/chat/completions
 *   https://api.openai.com/v1/              -> https://api.openai.com/v1/chat/completions
 *   https://api.openai.com/v1/chat/completions -> used as-is
 *   http://192.168.0.10:1234/v1            -> http://192.168.0.10:1234/v1/chat/completions
 */
object AiEndpoints {

    private const val CHAT_PATH = "chat/completions"
    private const val IMAGES_PATH = "images/generations"

    /** Endpoint for chat/completions style text features. */
    fun chatCompletions(baseUrl: String): String = endpoint(baseUrl, CHAT_PATH)

    /** Endpoint for image generation features. */
    fun imageGenerations(baseUrl: String): String = endpoint(baseUrl, IMAGES_PATH)

    private fun endpoint(baseUrl: String, path: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        // Already a full endpoint (the user pasted .../chat/completions).
        if (trimmed.endsWith("/$path")) return trimmed
        // Already versioned (.../v1 or .../openai/v1).
        if (trimmed.endsWith("/v1") || trimmed.endsWith("/v1beta")) {
            return "$trimmed/$path"
        }
        return "$trimmed/v1/$path"
    }

    /** True when the string looks like an http(s) URL. */
    fun isValidBaseUrl(baseUrl: String): Boolean {
        val trimmed = baseUrl.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
    }
}
