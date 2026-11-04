package com.example.engine

import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import java.util.UUID

/**
 * Bridge between subtitle cues and timeline text overlays.
 *
 * Subtitles are the same object in three places — the SRT file, the burned-in
 * export and the Compose text sheet — so the conversion lives here instead of
 * being duplicated. [SubtitleIO] handles the file format; this object handles
 * "what does a cue look like on the timeline".
 */
object CaptionBridge {

    const val POS_Y = 0.85f

    /** Longest duration a caption is allowed to occupy the timeline. */
    const val MAX_CAPTION_MS = 15_000L

    const val DEFAULT_COLOR: Long = 0xFFFFFFFF
    const val DEFAULT_BACKGROUND: Long = 0x88000000

    /**
     * Turns parsed subtitle cues into ready-to-place text overlays.
     *
     * Cues with blank text or a non-positive span are skipped, never imported as
     * invisible overlays. The returned overlays are sorted by start time and
     * positioned as bottom-centred subtitles.
     */
    fun overlaysFromCues(
        cues: List<SubtitleCue>,
        idFactory: () -> String = { UUID.randomUUID().toString() }
    ): List<TextOverlay> = cues
        .asSequence()
        .filter { it.text.isNotBlank() }
        .map { cue ->
            val start = cue.startMs.coerceAtLeast(0L)
            val end = cue.endMs.coerceAtLeast(start + 100L)
            TextOverlay(
                id = idFactory(),
                text = cue.text.trim(),
                startTimelineMs = start,
                durationMs = (end - start).coerceIn(100L, MAX_CAPTION_MS),
                fontStyle = TextFontStyle.SUBTITLE,
                color = DEFAULT_COLOR,
                backgroundColor = DEFAULT_BACKGROUND,
                animation = TextAnimationType.NONE,
                posX = 0.5f,
                posY = POS_Y,
                scale = 1.0f
            )
        }
        .sortedBy { it.startTimelineMs }
        .toList()

    /**
     * Reduces the project's text overlays to subtitle cues, sorted by start
     * time. Blank overlays are dropped and durations are clamped to a sane
     * minimum so the SRT writer never emits a zero-length cue.
     */
    fun cuesFromOverlays(overlays: List<TextOverlay>): List<SubtitleCue> =
        overlays
            .asSequence()
            .filter { it.text.isNotBlank() }
            .map { overlay ->
                val start = overlay.startTimelineMs.coerceAtLeast(0L)
                val duration = overlay.durationMs.coerceAtLeast(100L)
                SubtitleCue(start, start + duration, overlay.text.trim())
            }
            .sortedBy { it.startMs }
            .toList()

    /**
     * Detects captions that will look wrong when burned in. The editor can
     * import them anyway; these are warnings, not hard blockers.
     */
    fun issues(cues: List<SubtitleCue>, projectDurationMs: Long): List<String> {
        if (cues.isEmpty()) return emptyList()
        return SubtitleIO.validate(cues, projectDurationMs.coerceAtLeast(0L))
    }
}
