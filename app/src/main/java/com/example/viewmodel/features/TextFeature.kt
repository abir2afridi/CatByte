package com.example.viewmodel.features

import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.AiAudioExtractor
import com.example.data.AutoCaptionManager
import com.example.data.ClipTiming
import com.example.data.ai.AiClient
import com.example.data.ai.AiResult
import com.example.engine.CaptionBridge
import com.example.engine.SubtitleIO
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import java.util.UUID

/** Text overlays, subtitle import/export and automatic captions. */
internal object TextFeature {

    /** Refuses to parse caption files larger than 2 MB. */
    private const val MAX_SUBTITLE_BYTES = 2 * 1024 * 1024L

    /**
     * Parses an SRT/VTT file's contents and places every cue as a bottom-centred
     * text overlay. Existing overlays are kept; the first imported cue is
     * selected so the sheet opens on something real.
     */
    fun importSubtitles(vm: EditorViewModel, raw: String): EditorViewModel.SubtitleImportResult {
        val project = vm._currentProject.value
            ?: return EditorViewModel.SubtitleImportResult(0, listOf("No project is open"))
        if (raw.length > MAX_SUBTITLE_BYTES) {
            return EditorViewModel.SubtitleImportResult(0, listOf("File is too large (max 2 MB)"))
        }
        val cues = SubtitleIO.parse(raw)
        if (cues.isEmpty()) {
            return EditorViewModel.SubtitleImportResult(0, listOf("No valid timecodes found in the file"))
        }
        val issues = CaptionBridge.issues(cues, project.totalDurationMs)
        val overlays = CaptionBridge.overlaysFromCues(cues)
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + overlays)
        }
        // Select the first imported cue so the sheet opens on something real.
        vm._selectedTextId.value = overlays.firstOrNull()?.id
        return EditorViewModel.SubtitleImportResult(overlays.size, issues)
    }

    /**
     * Serialises the timeline's text overlays as an SRT file, or a VTT file when
     * [toVtt] is set. Returns null when there is nothing worth exporting.
     */
    fun exportSubtitles(vm: EditorViewModel, toVtt: Boolean = false): String? {
        val project = vm._currentProject.value ?: return null
        val cues = CaptionBridge.cuesFromOverlays(project.textOverlays)
        if (cues.isEmpty()) return null
        return if (toVtt) SubtitleIO.toVtt(cues) else SubtitleIO.toSrt(cues)
    }

    /**
     * AI auto captions: extracts the selected clip's audio, sends it to the
     * speech-to-text endpoint configured in Settings → AI, and places the timed
     * result as subtitle overlays. Nothing is inserted when no endpoint is
     * configured (the old demo phrases were misleading).
     */
    suspend fun addAutoCaptions(vm: EditorViewModel): String {
        val project = vm._currentProject.value ?: return "Open a project first"
        val context = vm.context ?: return "AI captions need the app context"
        val config = vm.aiConfig.value
        if (!config.isConfigured) {
            return "Auto captions need an AI endpoint. Add a base URL in Settings → AI."
        }

        val selectedId = vm._selectedClipId.value
        val clip = project.allVideoClips.firstOrNull { it.id == selectedId }
            ?: project.allVideoClips.firstOrNull { !it.uriString.isNullOrBlank() }
            ?: return "Add a video clip with audio first"
        val uriString = clip.uriString
        if (uriString.isNullOrBlank()) return "This clip has no media file to transcribe"

        val sourceSpanMs = (clip.durationMs * clip.speed.coerceAtLeast(0.01f)).toLong()
            .takeIf { it > 0L }
            ?: ClipTiming.sourceLengthMs(clip)
        val extracted = AiAudioExtractor.extract(
            context = context,
            uriString = uriString,
            startMs = clip.trimStartMs,
            durationMs = sourceSpanMs.coerceAtMost(ClipTiming.sourceLengthMs(clip))
        )
        val audio = when (extracted) {
            is AiAudioExtractor.Result.Failure -> return extracted.message
            is AiAudioExtractor.Result.Ok -> extracted
        }

        val bytes = runCatching { audio.file.readBytes() }.getOrElse {
            return "Could not read the extracted audio: ${it.message}"
        }
        val bengali = vm._appLanguage.value.tag?.startsWith("bn") == true
        val result = try {
            AiClient.transcribe(
                config = config,
                audio = bytes,
                fileName = audio.file.name,
                language = if (bengali) "bn" else null,
                fallbackDurationMs = audio.durationMs
            )
        } finally {
            runCatching { audio.file.delete() }
        }

        val transcript = when (result) {
            is AiResult.Failure -> return result.message
            is AiResult.Ok -> result.value
        }

        val overlays = AutoCaptionManager.overlaysFromTranscript(
            segments = transcript.segments,
            sourceOffsetMs = audio.sourceStartMs,
            clipStartMs = timelineStartOf(project, clip.id),
            trimStartMs = clip.trimStartMs,
            speed = clip.speed,
            clipDurationMs = clip.durationMs
        ).take(MAX_AI_CAPTIONS)

        if (overlays.isEmpty()) {
            return "The AI endpoint heard no speech in this clip"
        }
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + overlays)
        }
        vm._selectedTextId.value = overlays.first().id
        AppLog.info("EditorViewModel", "Added ${overlays.size} AI captions from ${config.speechModel}")
        return "Added ${overlays.size} AI caption${if (overlays.size == 1) "" else "s"} from the configured endpoint"
    }

    /** Position of a clip on the timeline (main clips are cumulative). */
    private fun timelineStartOf(project: com.example.model.VideoProject, clipId: String): Long {
        var cursor = 0L
        for (clip in project.clips) {
            if (clip.id == clipId) return cursor
            cursor += clip.durationMs
        }
        return project.overlayTracks
            .flatMap { it.clips }
            .firstOrNull { it.id == clipId }
            ?.startTimelineMs
            ?: 0L
    }

    /** Upper bound so a long monologue cannot flood the timeline in one tap. */
    private const val MAX_AI_CAPTIONS = 400

    fun addTextOverlay(
        vm: EditorViewModel,
        text: String = "Double tap to edit",
        fontStyle: TextFontStyle = TextFontStyle.CAPCUT_BOLD,
        color: Long = 0xFFFFFFFF,
        animation: TextAnimationType = TextAnimationType.POP_UP
    ) {
        val newText = TextOverlay(
            id = UUID.randomUUID().toString(),
            text = text,
            startTimelineMs = vm._currentPositionMs.value,
            durationMs = 3000L,
            fontStyle = fontStyle,
            color = color,
            animation = animation,
            posX = 0.5f,
            posY = 0.5f
        )
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + newText)
        }
        vm._selectedTextId.value = newText.id
    }

    fun updateSelectedText(
        vm: EditorViewModel,
        text: String,
        fontStyle: TextFontStyle,
        color: Long,
        animation: TextAnimationType
    ) {
        val id = vm._selectedTextId.value ?: return
        vm.updateProject { proj ->
            val updated = proj.textOverlays.map { item ->
                if (item.id == id) {
                    item.copy(text = text, fontStyle = fontStyle, color = color, animation = animation)
                } else item
            }
            proj.copy(textOverlays = updated)
        }
    }

    /**
     * Bold / italic / letter and line spacing for the selected text overlay.
     * The preview and the exported file both read these fields.
     */
    fun setSelectedTextStyle(
        vm: EditorViewModel,
        bold: Boolean,
        italic: Boolean,
        letterSpacingEm: Float,
        lineSpacingMultiplier: Float
    ) {
        val id = vm._selectedTextId.value ?: return
        vm.updateProject { proj ->
            proj.copy(
                textOverlays = proj.textOverlays.map { item ->
                    if (item.id == id) {
                        item.copy(
                            isBold = bold,
                            isItalic = italic,
                            letterSpacingEm = letterSpacingEm.coerceIn(0f, 0.4f),
                            lineSpacingMultiplier = lineSpacingMultiplier.coerceIn(0.7f, 2.5f)
                        )
                    } else item
                }
            )
        }
    }

    fun deleteSelectedText(vm: EditorViewModel) {
        val id = vm._selectedTextId.value ?: return
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays.filterNot { it.id == id })
        }
        vm._selectedTextId.value = null
    }
}
