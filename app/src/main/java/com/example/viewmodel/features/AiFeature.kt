package com.example.viewmodel.features

import android.net.Uri
import com.example.data.ai.AiClient
import com.example.data.ai.AiConfig
import com.example.data.ai.AiResult
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.viewmodel.EditorViewModel
import java.io.File
import java.util.UUID

/**
 * Editor AI features backed by the endpoint the user configured in
 * Settings → AI (any OpenAI-compatible base URL).
 *
 * Every function returns a human-readable status string: the UI shows it as a
 * toast, so a missing endpoint, a rejected key or a timeout is explained
 * instead of failing silently.
 *
 * Text features insert their answer as caption-style text overlays starting at
 * the playhead; the image feature writes the PNG into app storage and uses it
 * as the canvas background.
 */
internal object AiFeature {

    private const val LINE_DURATION_MS = 3_000L
    private const val MAX_LINES = 6

    /** Rewrites the current captions (or the project title) into punchy lines. */
    suspend fun remixScript(vm: EditorViewModel): String {
        val source = currentScript(vm)
        if (source.isBlank()) return "Add some text or captions first, then try AI remix"
        val prompt = """
            Rewrite this into ${MAX_LINES} short, punchy on-screen lines for a vertical
            short video. Keep the same meaning. Return ONLY the lines, one per line,
            no numbering and no quotes.

            $source
        """.trimIndent()
        return runText(vm, prompt, "AI remix")
    }

    /** Writes a short lyric/slogan set for the project topic. */
    suspend fun generateLyrics(vm: EditorViewModel): String {
        val topic = vm._currentProject.value?.title?.ifBlank { "my video" } ?: "my video"
        val prompt = """
            Write a 4-line catchy lyric hook for a short video about "$topic".
            Return ONLY the lines, one per line, no numbering.
        """.trimIndent()
        return runText(vm, prompt, "AI lyrics")
    }

    /** Translates the existing captions into the other app language. */
    suspend fun translateCaptions(vm: EditorViewModel): String {
        val source = currentScript(vm)
        if (source.isBlank()) return "There are no captions to translate yet"
        val bengali = vm._appLanguage.value.tag?.startsWith("bn") != true
        val target = if (bengali) "Bengali" else "English"
        val prompt = """
            Translate these subtitle lines to $target. Keep the line count and order.
            Return ONLY the translated lines, one per line, no numbering.

            $source
        """.trimIndent()
        return runText(vm, prompt, "AI translate", replaceExisting = true)
    }

    /** Three packaging titles for the video. */
    suspend fun packagingTitles(vm: EditorViewModel): String {
        val topic = vm._currentProject.value?.title?.ifBlank { "my video" } ?: "my video"
        val prompt = """
            Give 3 catchy short-video titles for "$topic". Return ONLY the titles,
            one per line, no numbering, max 8 words each.
        """.trimIndent()
        return runText(vm, prompt, "AI packaging")
    }

    /** Emoji suggestions added as small text overlays. */
    suspend fun emojiSuggestions(vm: EditorViewModel): String {
        val topic = vm._currentProject.value?.title?.ifBlank { "my video" } ?: "my video"
        val prompt = """
            Suggest 5 emojis that fit a short video about "$topic".
            Return ONLY the emojis separated by spaces, nothing else.
        """.trimIndent()
        return runText(vm, prompt, "AI emoji", emojiMode = true)
    }

    /**
     * Generates an image with the configured endpoint and uses it as the canvas
     * background. The PNG is written to app storage so the project keeps
     * working offline afterwards.
     */
    suspend fun generateBackgroundImage(vm: EditorViewModel, prompt: String): String {
        val saved = generateImageFile(vm, prompt) ?: return lastImageError
        val uri = Uri.fromFile(saved).toString()
        vm.updateProject { project -> project.copy(backgroundImageUri = uri) }
        return "AI image generated and set as the canvas background"
    }

    /** Generates a cover image with the image model and saves it as the project cover. */
    suspend fun generateCoverImage(vm: EditorViewModel, prompt: String): String {
        val saved = generateImageFile(vm, prompt) ?: return lastImageError
        val uri = Uri.fromFile(saved).toString()
        vm.setProjectCover(frameMs = 0L, imageUri = uri)
        return "AI cover generated and saved to the project"
    }

    /** Generates an image and inserts it as a picture-in-picture overlay clip. */
    suspend fun generateOverlayImage(vm: EditorViewModel, prompt: String): String {
        val saved = generateImageFile(vm, prompt) ?: return lastImageError
        vm.addOverlayClipFromUri(Uri.fromFile(saved).toString(), "AI image")
        return "AI image added as an overlay at the playhead"
    }

    /** Last failure from [generateImageFile], so callers can return the reason. */
    private var lastImageError: String = "Could not generate the image"

    /** Calls the image endpoint and writes the bytes to app storage (null on failure). */
    private suspend fun generateImageFile(vm: EditorViewModel, prompt: String): File? {
        val config = vm.aiConfig.value
        if (!config.isConfigured) {
            lastImageError = AiClient.NO_ENDPOINT
            return null
        }
        val context = vm.context
        if (context == null) {
            lastImageError = "AI image needs the app context"
            return null
        }

        return when (val result = AiClient.generateImage(config, prompt, size = requestedSize(vm))) {
            is AiResult.Failure -> {
                lastImageError = result.message
                null
            }
            is AiResult.Ok -> {
                val dir = File(context.filesDir, "ai").apply { mkdirs() }
                val file = File(dir, "ai_${System.currentTimeMillis()}.png")
                runCatching { file.writeBytes(result.value) }
                    .getOrElse {
                        lastImageError = "Could not save the generated image: ${it.message}"
                        return null
                    }
                file
            }
        }
    }

    /** Image size that matches the project canvas (portrait/landscape/square). */
    private fun requestedSize(vm: EditorViewModel): String {
        val ratio = vm._currentProject.value
            ?.let { it.aspectRatio.ratioWidth / it.aspectRatio.ratioHeight }
            ?: 1f
        return com.example.data.ai.AiImageSize.forAspect(ratio)
    }

    /** Pings the endpoint so Settings can show a real "Connected" state. */
    suspend fun testConnection(config: AiConfig): String {
        return when (val result = AiClient.testConnection(config)) {
            is AiResult.Failure -> result.message
            is AiResult.Ok -> "Connected - model replied: ${result.value.take(40)}"
        }
    }

    // ------------------------------------------------------------ helpers

    private suspend fun runText(
        vm: EditorViewModel,
        prompt: String,
        label: String,
        replaceExisting: Boolean = false,
        emojiMode: Boolean = false
    ): String {
        val config = vm.aiConfig.value
        if (!config.isConfigured) return AiClient.NO_ENDPOINT

        return when (val result = AiClient.chat(config, prompt)) {
            is AiResult.Failure -> result.message
            is AiResult.Ok -> {
                val lines = result.value
                    .lineSequence()
                    .map { it.trim().trim('-', '*', '"', '\'', ' ') }
                    .filter { it.isNotBlank() }
                    .take(MAX_LINES)
                    .toList()
                if (lines.isEmpty()) {
                    "$label returned nothing usable"
                } else {
                    insertLines(vm, lines, replaceExisting, emojiMode)
                    "$label added ${lines.size} line${if (lines.size == 1) "" else "s"} at the playhead"
                }
            }
        }
    }

    private fun insertLines(
        vm: EditorViewModel,
        lines: List<String>,
        replaceExisting: Boolean,
        emojiMode: Boolean
    ) {
        val startMs = vm._currentPositionMs.value
        val created = lines.mapIndexed { index, line ->
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = line,
                startTimelineMs = startMs + index * LINE_DURATION_MS,
                durationMs = LINE_DURATION_MS,
                fontStyle = if (emojiMode) TextFontStyle.CAPCUT_BOLD else TextFontStyle.CAPCUT_BOLD,
                color = 0xFFFFFFFF,
                animation = TextAnimationType.FADE_IN,
                posY = if (emojiMode) 0.78f else 0.5f,
                scale = if (emojiMode) 1.4f else 1f
            )
        }
        vm.updateProject { project ->
            val base = if (replaceExisting) {
                val end = startMs + created.size * LINE_DURATION_MS
                project.textOverlays.filterNot { it.startTimelineMs in startMs until end }
            } else {
                project.textOverlays
            }
            project.copy(textOverlays = base + created)
        }
    }

    /** Existing caption text, in timeline order. */
    private fun currentScript(vm: EditorViewModel): String =
        vm._currentProject.value
            ?.textOverlays
            ?.sortedBy { it.startTimelineMs }
            ?.joinToString("\n") { it.text }
            .orEmpty()
}
