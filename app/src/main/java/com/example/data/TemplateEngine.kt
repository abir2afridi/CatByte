package com.example.data

import com.example.model.AspectRatioType
import com.example.model.MediaClip
import com.example.model.MediaPlaceholderSlot
import com.example.model.VideoProject
import com.example.model.ViralTemplate
import java.util.UUID

object TemplateEngine {

    /** Sample viral templates built-in. */
    fun getViralTemplates(): List<ViralTemplate> = listOf(
        ViralTemplate(
            title = "🚀 Viral Reel Jump",
            description = "Fast-cut montage with beat sync speed ramps",
            coverUrl = null,
            aspectRatio = AspectRatioType.RATIO_9_16,
            placeholderSlots = listOf(
                MediaPlaceholderSlot(label = "Clip 1 - Hook", durationMs = 1500L),
                MediaPlaceholderSlot(label = "Clip 2 - Build Up", durationMs = 2000L),
                MediaPlaceholderSlot(label = "Clip 3 - Climax", durationMs = 2500L)
            )
        ),
        ViralTemplate(
            title = "💃 Dance Trend Starter",
            description = "Perfect for TikTok dance challenges",
            coverUrl = null,
            aspectRatio = AspectRatioType.RATIO_9_16,
            placeholderSlots = listOf(
                MediaPlaceholderSlot(label = "Dance Clip 1", durationMs = 3000L),
                MediaPlaceholderSlot(label = "Dance Clip 2", durationMs = 3000L)
            )
        ),
        ViralTemplate(
            title = "🎬 Cinematic Travel Vlog",
            description = "Slow-mo scenic travel montage",
            coverUrl = null,
            aspectRatio = AspectRatioType.RATIO_16_9,
            placeholderSlots = listOf(
                MediaPlaceholderSlot(label = "Scenic Shot 1", durationMs = 4000L),
                MediaPlaceholderSlot(label = "Scenic Shot 2", durationMs = 4000L),
                MediaPlaceholderSlot(label = "Scenic Shot 3", durationMs = 5000L)
            )
        )
    )

    /**
     * Instantiates a [VideoProject] from a [ViralTemplate], swapping in user media URIs
     * for the placeholder slots.
     */
    fun instantiateTemplate(
        template: ViralTemplate,
        userMediaUris: List<String>
    ): VideoProject {
        val clips = template.placeholderSlots.mapIndexed { index, slot ->
            val uri = userMediaUris.getOrNull(index)
            MediaClip(
                id = "tpl_${UUID.randomUUID().toString().take(8)}",
                title = slot.label,
                uriString = uri,
                durationMs = slot.durationMs,
                originalDurationMs = slot.durationMs,
                startTimelineMs = template.placeholderSlots.take(index).sumOf { it.durationMs }
            )
        }
        return VideoProject(
            id = "tpl_proj_${UUID.randomUUID().toString().take(8)}",
            title = "${template.title} Draft",
            aspectRatio = template.aspectRatio,
            clips = clips,
            lastModified = System.currentTimeMillis()
        )
    }
}
