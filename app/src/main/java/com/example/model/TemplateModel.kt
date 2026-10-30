package com.example.model

import java.util.UUID

data class MediaPlaceholderSlot(
    val slotId: String = UUID.randomUUID().toString(),
    val label: String = "Tap to replace with your media",
    val requiredType: String = "VIDEO", // VIDEO | IMAGE
    val durationMs: Long = 3000L
)

data class ViralTemplate(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val coverUrl: String?,
    val aspectRatio: AspectRatioType = AspectRatioType.RATIO_9_16,
    val placeholderSlots: List<MediaPlaceholderSlot>,
    val baseProjectConfig: Map<String, String> = emptyMap() // layout, keyframes, filters preset
)
