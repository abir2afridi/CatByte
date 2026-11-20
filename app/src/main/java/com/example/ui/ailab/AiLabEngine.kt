package com.example.ui.ailab

import com.example.data.ai.AiClient
import com.example.data.ai.AiConfig
import com.example.data.ai.AiImageSize
import com.example.data.ai.AiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One turn of the AI Assistant conversation. */
data class AiLabChatTurn(val role: String, val text: String)

/** Titles plus opening hooks produced by the Titles & Hooks tool. */
data class AiLabTitles(val titles: List<String>, val hooks: List<String>)

/** Hashtags plus a one-line description produced by Hashtags & SEO. */
data class AiLabHashtags(val tags: List<String>, val description: String)

/** Result of the translator tool. */
data class AiLabTranslation(val text: String)

/**
 * The AI Lab's own prompts, on top of [AiClient].
 *
 * The editor's [com.example.viewmodel.features.AiFeature] writes straight into
 * the open project; the Lab is a standalone playground, so its answers come back
 * as data instead of mutating a timeline. Sharing the client keeps one place that
 * knows how to talk to an OpenAI-compatible endpoint.
 */
object AiLabEngine {

    private const val MAX_LINES = 12

    /** Tone presets offered by the script writer. */
    val ScriptTones = listOf(
        "Friendly",
        "Bold",
        "Educational",
        "Funny",
        "Cinematic"
    )

    /** Target languages offered by the translator. */
    val TranslationTargets = listOf(
        "Bengali",
        "English",
        "Hindi",
        "Arabic",
        "Spanish",
        "Portuguese",
        "Indonesian"
    )

    /** Writes a short, readable script for a vertical short. */
    suspend fun writeScript(
        config: AiConfig,
        topic: String,
        tone: String
    ): AiResult<List<String>> {
        if (topic.isBlank()) return AiResult.Failure("Write a topic first")
        val prompt = """
            Write a ${MAX_LINES}-line script for a vertical short video about "$topic".
            Tone: $tone.
            Each line must be short enough to read on screen in under three seconds.
            Return ONLY the lines, one per line, no numbering, no quotation marks,
            no headings.
        """.trimIndent()
        return lines(config, prompt)
    }

    /** Three titles and three opening hooks for a topic. */
    suspend fun titlesAndHooks(config: AiConfig, topic: String): AiResult<AiLabTitles> {
        if (topic.isBlank()) return AiResult.Failure("Write a topic first")
        val prompt = """
            For a short video about "$topic" give me:
            3 catchy titles (max 8 words each) and
            3 opening hooks (max 14 words each) that make people keep watching.
            Return exactly this format, nothing else:

            TITLES
            1. ...
            2. ...
            3. ...
            HOOKS
            1. ...
            2. ...
            3. ...
        """.trimIndent()
        return when (val result = AiClient.chat(config, prompt)) {
            is AiResult.Failure -> result
            is AiResult.Ok -> {
                val sections = splitSections(result.value)
                val titles = numbered(sections["TITLES"]).take(3)
                val hooks = numbered(sections["HOOKS"]).take(3)
                if (titles.isEmpty() && hooks.isEmpty()) {
                    AiResult.Failure("The AI endpoint returned nothing usable")
                } else {
                    AiResult.Ok(AiLabTitles(titles, hooks))
                }
            }
        }
    }

    /** Translates [text] into [targetLanguage], keeping the line count. */
    suspend fun translate(
        config: AiConfig,
        text: String,
        targetLanguage: String
    ): AiResult<AiLabTranslation> {
        val source = text.trim()
        if (source.isBlank()) return AiResult.Failure("Paste some text first")
        val prompt = """
            Translate the following text into $targetLanguage.
            Keep the same number of lines and the same order.
            Return ONLY the translation, nothing else.

            $source
        """.trimIndent()
        return when (val result = AiClient.chat(config, prompt)) {
            is AiResult.Failure -> result
            is AiResult.Ok -> {
                val translated = result.value.trim()
                if (translated.isBlank()) {
                    AiResult.Failure("The AI endpoint returned an empty translation")
                } else {
                    AiResult.Ok(AiLabTranslation(translated))
                }
            }
        }
    }

    /** Suggests hashtags and a one-line description for a topic. */
    suspend fun hashtags(config: AiConfig, topic: String): AiResult<AiLabHashtags> {
        if (topic.isBlank()) return AiResult.Failure("Write a topic first")
        val prompt = """
            For a short video about "$topic" give me:
            - exactly 10 lowercase hashtags without the # symbol, one per line, and
            - one short search-friendly description sentence.
            Return exactly this format, nothing else:

            TAGS
            word1
            word2
            ...
            DESCRIPTION
            ...
        """.trimIndent()
        return when (val result = AiClient.chat(config, prompt)) {
            is AiResult.Failure -> result
            is AiResult.Ok -> {
                val sections = splitSections(result.value)
                val tags = numbered(sections["TAGS"])
                    .ifEmpty { sections["TAGS"].orEmpty().split(Regex("\\s+")) }
                    .map { it.trim().trimStart('#').trim() }
                    .filter { it.isNotBlank() }
                    .take(10)
                val description = sections["DESCRIPTION"].orEmpty().trim()
                if (tags.isEmpty() && description.isBlank()) {
                    AiResult.Failure("The AI endpoint returned nothing usable")
                } else {
                    AiResult.Ok(AiLabHashtags(tags, description))
                }
            }
        }
    }

    /**
     * One turn of the assistant conversation. [history] is flattened into the
     * prompt because [AiClient.chat] speaks to a single-user-turn endpoint.
     */
    suspend fun chat(
        config: AiConfig,
        history: List<AiLabChatTurn>,
        message: String
    ): AiResult<String> {
        val prompt = message.trim()
        if (prompt.isBlank()) return AiResult.Failure("Type a message first")
        val transcript = buildString {
            history.takeLast(8).forEach { turn ->
                append(if (turn.role == "user") "User: " else "Assistant: ")
                append(turn.text)
                append('\n')
            }
            append("User: ")
            append(prompt)
        }
        return when (val result = AiClient.chat(config, transcript, maxTokens = 500)) {
            is AiResult.Failure -> result
            is AiResult.Ok -> {
                val answer = result.value.trim()
                if (answer.isBlank()) {
                    AiResult.Failure("The AI endpoint returned an empty answer")
                } else {
                    AiResult.Ok(answer)
                }
            }
        }
    }

    /** Generates raw image bytes with the configured image model. */
    suspend fun generateImage(config: AiConfig, prompt: String, size: String): AiResult<ByteArray> {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return AiResult.Failure("Describe the image first")
        val requested = when (size) {
            AiImageSize.PORTRAIT, AiImageSize.LANDSCAPE, AiImageSize.SQUARE -> size
            else -> AiImageSize.SQUARE
        }
        return AiClient.generateImage(config, trimmed, size = requested)
    }

    // ------------------------------------------------------------- helpers

    private suspend fun lines(config: AiConfig, prompt: String): AiResult<List<String>> =
        when (val result = AiClient.chat(config, prompt)) {
            is AiResult.Failure -> result
            is AiResult.Ok -> {
                val cleaned = result.value
                    .lineSequence()
                    .map { cleanLine(it) }
                    .filter { it.isNotBlank() }
                    .take(MAX_LINES)
                    .toList()
                if (cleaned.isEmpty()) {
                    AiResult.Failure("The AI endpoint returned nothing usable")
                } else {
                    AiResult.Ok(cleaned)
                }
            }
        }

    /** Strips bullet markers, numbering and quotation marks from one line. */
    internal fun cleanLine(raw: String): String =
        raw.trim()
            .trim('-', '*', '•', '"', '\'', '“', '”', '‘', '’')
            .replace(Regex("""^\d+[.)]\s*"""), "")
            .trim()

    /**
     * Splits a response into "LABEL" sections so a single completion can carry
     * two lists without depending on a structured-output mode the endpoint may
     * not implement.
     */
    internal fun splitSections(raw: String): Map<String, String> {
        val labels = setOf("TITLES", "HOOKS", "TAGS", "DESCRIPTION")
        val lines = raw.lines()
        val sections = LinkedHashMap<String, StringBuilder>()
        var current: StringBuilder? = null
        for (line in lines) {
            val label = line.trim().trim(':', ' ').uppercase()
            if (label in labels) {
                current = StringBuilder()
                sections[label] = current
                continue
            }
            current?.appendLine(line)
        }
        return sections.mapValues { it.value.toString() }
    }

    /** Pulls the numbered (or bulleted) items out of one section. */
    internal fun numbered(section: String?): List<String> {
        if (section.isNullOrBlank()) return emptyList()
        return section
            .lines()
            .map { cleanLine(it) }
            .filter { it.isNotBlank() }
    }
}
