package com.example.ui.ailab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Which capability a lab tool needs. The badge on the card and the reason a
 * disabled tool gives both come from this.
 */
enum class AiLabToolKind {
    /** Chat-completions model: needs the endpoint from Settings → AI. */
    TEXT,

    /** Image-generation model: needs the endpoint's image model. */
    IMAGE,

    /** Whisper-compatible speech-to-text: needs the endpoint. */
    SPEECH,

    /** Fully on-device: works with no network and no endpoint. */
    ON_DEVICE,

    /** Needs a different kind of model entirely; the app says so instead of pretending. */
    SPECIALISED
}

/** Filter categories shown as chips above the tool grid. */
enum class AiLabCategory(val label: String) {
    ALL("All"),
    TEXT("Text"),
    IMAGE("Image"),
    AUDIO("Audio"),
    VIDEO("Video")
}

/** Which theme accent a card tints itself with, so both themes stay readable. */
enum class AiLabAccent { CYAN, BLUE, PURPLE, PINK, GREEN, YELLOW, TEAL, RED }

/** One tool on the AI Lab page. */
data class AiLabTool(
    val id: String,
    val title: String,
    val description: String,
    val kind: AiLabToolKind,
    val category: AiLabCategory,
    val icon: ImageVector,
    val accent: AiLabAccent
) {
    /** True when the tool can only run once an AI endpoint is configured. */
    val needsEndpoint: Boolean
        get() = kind == AiLabToolKind.TEXT ||
            kind == AiLabToolKind.IMAGE ||
            kind == AiLabToolKind.SPEECH

    /** False for tools that genuinely need a model the app does not have. */
    val isRunnable: Boolean get() = kind != AiLabToolKind.SPECIALISED

    val badge: String
        get() = when (kind) {
            AiLabToolKind.TEXT, AiLabToolKind.IMAGE, AiLabToolKind.SPEECH -> "Online"
            AiLabToolKind.ON_DEVICE -> "On device"
            AiLabToolKind.SPECIALISED -> "Needs model"
        }
}

/**
 * Every tool on the AI Lab page.
 *
 * The three "needs endpoint" kinds are backed by [com.example.data.ai.AiClient]
 * (any OpenAI-compatible base URL). [AiLabToolKind.ON_DEVICE] tools run entirely
 * offline. [AiLabToolKind.SPECIALISED] rows are shown but disabled with an
 * honest reason: segmentation, lip sync and avatars need a model family a chat
 * endpoint cannot provide, and claiming otherwise would be a lie.
 */
val AiLabTools: List<AiLabTool> = listOf(
    AiLabTool(
        id = "script",
        title = "Script Writer",
        description = "Turn a topic into a ready-to-read short video script",
        kind = AiLabToolKind.TEXT,
        category = AiLabCategory.TEXT,
        icon = Icons.Outlined.EditNote,
        accent = AiLabAccent.CYAN
    ),
    AiLabTool(
        id = "titles",
        title = "Titles & Hooks",
        description = "Three scroll-stopping titles and opening lines",
        kind = AiLabToolKind.TEXT,
        category = AiLabCategory.TEXT,
        icon = Icons.Outlined.Title,
        accent = AiLabAccent.PINK
    ),
    AiLabTool(
        id = "translate",
        title = "Translator",
        description = "Translate a script into another language",
        kind = AiLabToolKind.TEXT,
        category = AiLabCategory.TEXT,
        icon = Icons.Outlined.Translate,
        accent = AiLabAccent.BLUE
    ),
    AiLabTool(
        id = "hashtags",
        title = "Hashtags & SEO",
        description = "A tag set and a one-line description for your video",
        kind = AiLabToolKind.TEXT,
        category = AiLabCategory.TEXT,
        icon = Icons.Outlined.Tag,
        accent = AiLabAccent.GREEN
    ),
    AiLabTool(
        id = "chat",
        title = "AI Assistant",
        description = "Free-form chat for ideas, jokes and research",
        kind = AiLabToolKind.TEXT,
        category = AiLabCategory.TEXT,
        icon = Icons.Outlined.Forum,
        accent = AiLabAccent.PURPLE
    ),
    AiLabTool(
        id = "image",
        title = "Image Generator",
        description = "Generate a cover, background or sticker from a prompt",
        kind = AiLabToolKind.IMAGE,
        category = AiLabCategory.IMAGE,
        icon = Icons.Outlined.Image,
        accent = AiLabAccent.PURPLE
    ),
    AiLabTool(
        id = "upscale",
        title = "Image Upscaler",
        description = "Sharpen a photo with a 2x or 4x Lanczos upscale",
        kind = AiLabToolKind.ON_DEVICE,
        category = AiLabCategory.IMAGE,
        icon = Icons.Outlined.AspectRatio,
        accent = AiLabAccent.TEAL
    ),
    AiLabTool(
        id = "captions",
        title = "Auto Captions",
        description = "Transcribe a video and export timed SRT subtitles",
        kind = AiLabToolKind.SPEECH,
        category = AiLabCategory.VIDEO,
        icon = Icons.Outlined.ClosedCaption,
        accent = AiLabAccent.GREEN
    ),
    AiLabTool(
        id = "voiceover",
        title = "AI Voiceover",
        description = "Read your script aloud and save the voice-over",
        kind = AiLabToolKind.ON_DEVICE,
        category = AiLabCategory.AUDIO,
        icon = Icons.Outlined.RecordVoiceOver,
        accent = AiLabAccent.PINK
    ),
    AiLabTool(
        id = "music",
        title = "AI Music",
        description = "Compose a royalty-free bed in three moods",
        kind = AiLabToolKind.ON_DEVICE,
        category = AiLabCategory.AUDIO,
        icon = Icons.Outlined.MusicNote,
        accent = AiLabAccent.YELLOW
    ),
    AiLabTool(
        id = "beats",
        title = "Beat Finder",
        description = "Estimate tempo, beats and silent gaps in a track",
        kind = AiLabToolKind.ON_DEVICE,
        category = AiLabCategory.AUDIO,
        icon = Icons.Outlined.GraphicEq,
        accent = AiLabAccent.CYAN
    ),
    AiLabTool(
        id = "remove_bg",
        title = "Remove Background",
        description = "Segment a subject and drop the background",
        kind = AiLabToolKind.SPECIALISED,
        category = AiLabCategory.IMAGE,
        icon = Icons.Outlined.Science,
        accent = AiLabAccent.BLUE
    ),
    AiLabTool(
        id = "avatar",
        title = "AI Avatar",
        description = "Generate a talking avatar from a single photo",
        kind = AiLabToolKind.SPECIALISED,
        category = AiLabCategory.VIDEO,
        icon = Icons.Outlined.AutoAwesome,
        accent = AiLabAccent.TEAL
    )
)

/** Tools in [category] (or everything when it is [AiLabCategory.ALL]), optionally filtered. */
fun List<AiLabTool>.filtered(category: AiLabCategory, query: String): List<AiLabTool> {
    val needle = query.trim()
    return filter { tool ->
        val categoryMatches = category == AiLabCategory.ALL || tool.category == category
        val queryMatches = needle.isBlank() ||
            tool.title.contains(needle, ignoreCase = true) ||
            tool.description.contains(needle, ignoreCase = true)
        categoryMatches && queryMatches
    }
}
