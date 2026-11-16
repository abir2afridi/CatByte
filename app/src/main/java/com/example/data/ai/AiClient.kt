package com.example.data.ai

import com.example.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

/**
 * Minimal OpenAI-compatible HTTP client used by the editor AI features.
 *
 * Deliberately dependency-free (HttpURLConnection + org.json) so the APK does
 * not grow and so any compatible gateway works: OpenAI, Azure-style proxies,
 * LiteLLM, LM Studio, Ollama's /v1 shim, etc.
 *
 * Every call returns [AiResult] instead of throwing: the editor shows the
 * failure reason to the user rather than crashing or silently doing nothing.
 */
object AiClient {

    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 120_000

    /** Text completion. Returns the assistant message. */
    suspend fun chat(
        config: AiConfig,
        prompt: String,
        system: String = "You are a helpful video-editing assistant. Answer with plain text only.",
        maxTokens: Int = 700
    ): AiResult<String> = withContext(Dispatchers.IO) {
        if (!config.isConfigured) return@withContext AiResult.Failure(NO_ENDPOINT)
        if (!AiEndpoints.isValidBaseUrl(config.baseUrl)) {
            return@withContext AiResult.Failure("The AI base URL must start with http:// or https://")
        }

        val body = JSONObject().apply {
            put("model", config.model.ifBlank { "gpt-4o-mini" })
            put("max_tokens", maxTokens)
            put("temperature", 0.7)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
            )
        }

        when (val response = post(config, AiEndpoints.chatCompletions(config.baseUrl), body.toString())) {
            is AiResult.Failure -> response
            is AiResult.Ok -> parseChat(response.value)
        }
    }

    /**
     * Image generation. Returns raw image bytes (base64 payload or a downloaded
     * URL), ready to be written into the project's media folder.
     */
    suspend fun generateImage(
        config: AiConfig,
        prompt: String,
        size: String = "1024x1024"
    ): AiResult<ByteArray> = withContext(Dispatchers.IO) {
        if (!config.isConfigured) return@withContext AiResult.Failure(NO_ENDPOINT)
        if (!AiEndpoints.isValidBaseUrl(config.baseUrl)) {
            return@withContext AiResult.Failure("The AI base URL must start with http:// or https://")
        }

        val body = JSONObject().apply {
            put("model", config.imageModel.ifBlank { "dall-e-3" })
            put("prompt", prompt)
            put("n", 1)
            put("size", size)
            put("response_format", "b64_json")
        }

        when (val response = post(config, AiEndpoints.imageGenerations(config.baseUrl), body.toString())) {
            is AiResult.Failure -> response
            is AiResult.Ok -> parseImage(response.value)
        }
    }

    /** Pings the endpoint with a tiny completion so the settings screen can say "Connected". */
    suspend fun testConnection(config: AiConfig): AiResult<String> {
        if (!config.isConfigured) return AiResult.Failure(NO_ENDPOINT)
        return when (val result = chat(config, "Reply with the single word: ready", maxTokens = 16)) {
            is AiResult.Failure -> result
            is AiResult.Ok -> AiResult.Ok(result.value.trim().ifBlank { "Connected" })
        }
    }

    // ---------------------------------------------------------------- HTTP

    private fun post(config: AiConfig, url: String, body: String): AiResult<String> {
        if (url.isBlank()) return AiResult.Failure("The AI base URL is empty")
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (config.apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }.orEmpty()

            if (code !in 200..299) {
                val reason = runCatching {
                    JSONObject(text).getJSONObject("error").optString("message")
                }.getOrNull()
                AppLog.warn("AiClient", "HTTP $code from $url: ${reason ?: text.take(200)}")
                AiResult.Failure(
                    when {
                        code == 401 || code == 403 -> "The AI endpoint rejected the API key (HTTP $code)"
                        code == 404 -> "Endpoint not found (HTTP 404). Check the base URL: $url"
                        code == 429 -> "The AI endpoint is rate limited (HTTP 429). Try again shortly."
                        reason != null -> "AI error (HTTP $code): $reason"
                        else -> "AI request failed (HTTP $code)"
                    }
                )
            } else {
                AiResult.Ok(text)
            }
        } catch (error: Exception) {
            AppLog.warn("AiClient", "AI request failed: ${error.message}")
            AiResult.Failure(
                when (error) {
                    is java.net.SocketTimeoutException -> "The AI endpoint timed out"
                    is java.net.UnknownHostException -> "Cannot reach the AI endpoint (unknown host)"
                    is java.net.ConnectException -> "Cannot connect to the AI endpoint"
                    else -> "AI request failed: ${error.message ?: error.javaClass.simpleName}"
                }
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseChat(raw: String): AiResult<String> {
        return runCatching {
            val choices = JSONObject(raw).optJSONArray("choices")
            val content = choices
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (content.isBlank()) {
                AiResult.Failure("The AI endpoint returned an empty answer")
            } else {
                AiResult.Ok(content.trim())
            }
        }.getOrElse {
            AiResult.Failure("Could not read the AI answer: ${it.message}")
        }
    }

    private fun parseImage(raw: String): AiResult<ByteArray> {
        return runCatching {
            val data = JSONObject(raw).optJSONArray("data")
                ?: return@runCatching AiResult.Failure("The AI endpoint returned no image")
            val first = data.optJSONObject(0)
                ?: return@runCatching AiResult.Failure("The AI endpoint returned no image")
            val base64 = first.optString("b64_json")
            if (base64.isNotBlank()) {
                return@runCatching AiResult.Ok(Base64.decode(base64, Base64.DEFAULT))
            }
            val url = first.optString("url")
            if (url.isNotBlank()) {
                return@runCatching download(url)
            }
            AiResult.Failure("The AI endpoint returned an image entry without data")
        }.getOrElse {
            AiResult.Failure("Could not read the generated image: ${it.message}")
        }
    }

    private fun download(url: String): AiResult<ByteArray> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            if (connection.responseCode !in 200..299) {
                AiResult.Failure("Could not download the generated image (HTTP ${connection.responseCode})")
            } else {
                AiResult.Ok(connection.inputStream.use { it.readBytes() })
            }
        } catch (error: Exception) {
            AiResult.Failure("Could not download the generated image: ${error.message}")
        } finally {
            connection?.disconnect()
        }
    }

    const val NO_ENDPOINT: String =
        "No AI endpoint configured. Add a base URL in Settings → AI to use this feature."
}
