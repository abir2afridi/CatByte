package com.example.model

data class VideoProject(
    val id: String,
    val title: String,
    val aspectRatio: AspectRatioType = AspectRatioType.RATIO_9_16,
    val clips: List<MediaClip> = emptyList(),
    val overlayTracks: List<VideoTrack> = emptyList(),
    val audioTracks: List<AudioClip> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val stickers: List<StickerOverlay> = emptyList(),
    /** Freehand drawing strokes (Text → Draw). */
    val strokes: List<StrokeOverlay> = emptyList(),
    val effects: List<TimelineEffect> = emptyList(),
    val thumbnailResId: Int? = null,
    /** Timeline position of the frame used as the project cover (home thumbnail). */
    val coverFrameMs: Long = 0L,
    /** Custom cover image (uri string). Empty means "use [coverFrameMs] from the video". */
    val coverImageUri: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val resolution: String = "1080P",
    val fps: Int = 30,
    val isFavorite: Boolean = false,
    val paperStyle: String = "Drafting Grid",
    /**
     * Canvas background image (uri string). When set it is drawn behind the
     * clips in both the preview and the exported file, and overrides
     * [paperStyle]. Empty means "use the paper style".
     */
    val backgroundImageUri: String = "",
    /**
     * What fills the bars around a non-fitting video: "color" (the paper
     * style), "image" ([backgroundImageUri]) or "blur" (a blurred copy of the
     * video filling the whole canvas behind the sharp video). Preview and
     * export share the composition, so both show the same thing.
     */
    val backgroundMode: String = "color",
    /** Main video track controls (the main clips have no `VideoTrack` object). */
    val isMainTrackMuted: Boolean = false,
    val isMainTrackLocked: Boolean = false,
    val isMainTrackSolo: Boolean = false,
    /**
     * Auto-ducking: music dips while a voice-over is audible. When off, the
     * music keeps its level and the voice-over is mixed on top of it.
     */
    val autoDuckEnabled: Boolean = true,
    /**
     * How deep the music dips under a voice-over. 0 keeps the music at full
     * level, 1 mutes it completely; 0.35 leaves it at 65 %.
     */
    val autoDuckStrength: Float = 0.35f,
    /** Named, coloured timeline markers (jump targets and snap magnets). */
    val markers: List<TimelineMarker> = emptyList()
) {
    val allVideoClips: List<MediaClip>
        get() = clips + overlayTracks.flatMap { it.clips }

    /** Track that owns [clipId]; main clips belong to [TimelineTrackIds.MAIN]. */
    fun trackIdForClip(clipId: String): String? =
        if (clips.any { it.id == clipId }) {
            TimelineTrackIds.MAIN
        } else {
            overlayTracks.firstOrNull { it.clips.any { c -> c.id == clipId } }?.id
        }

    fun isTrackLocked(trackId: String): Boolean =
        if (trackId == TimelineTrackIds.MAIN) {
            isMainTrackLocked
        } else {
            overlayTracks.firstOrNull { it.id == trackId }?.isLocked ?: false
        }

    fun isTrackLockedForClip(clipId: String): Boolean {
        val trackId = trackIdForClip(clipId) ?: return false
        return isTrackLocked(trackId)
    }

    val totalDurationMs: Long
        get() {
            val maxMainClipEnd = clips.sumOf { it.durationMs }
            val maxOverlayClipEnd = overlayTracks.flatMap { it.clips }.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val maxAudioEnd = audioTracks.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val maxTextEnd = textOverlays.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val maxEffectEnd = effects.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            return maxOf(maxMainClipEnd, maxOverlayClipEnd, maxAudioEnd, maxTextEnd, maxEffectEnd, 1000L)
        }
}