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
import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
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
     * Auto captions for the whole timeline.
     *
     * Two real engines, tried in order:
     *  1. the offline Vosk model (Settings → AI → Speech models) - no network,
     *     no account, works everywhere; preferred when installed;
     *  2. the configured AI endpoint (Settings → AI) for users who prefer a
     *     cloud recogniser.
     *
     * Every main-track clip with media is transcribed in order (or just the
     * selected overlay clip), and each result is shifted onto the timeline using
     * the clip's start, trim and speed. Clips without an audio track are skipped,
     * and an engine failure stops the run with the failing clip's name instead
     * of leaving half the timeline captioned silently.
     */
    suspend fun addAutoCaptions(vm: EditorViewModel): String {
        val project = vm._currentProject.value ?: return "Open a project first"
        val context = vm.context ?: return "AI captions need the app context"

        val language = com.example.data.captions.TranscriptionModels.Language
            .forTag(vm._appLanguage.value.tag)
        val offlineModel = com.example.data.captions.TranscriptionModels
            .installedFolder(context, language)
        val config = vm.aiConfig.value
        if (offlineModel == null && !config.isConfigured) {
            return "Auto captions need the offline ${language.displayName} speech model " +
                "(Settings → AI → Speech models) or an AI endpoint in Settings → AI."
        }

        val selectedId = vm._selectedClipId.value
        val selected = project.allVideoClips.firstOrNull { it.id == selectedId }
        val isOverlaySelected = selected != null && project.clips.none { it.id == selected.id }
        val targets = if (isOverlaySelected && selected != null) {
            listOf(selected)
        } else {
            project.clips.filter { !it.uriString.isNullOrBlank() }
        }
        if (targets.isEmpty()) return "Add a video clip with audio first"

        val capped = targets.take(MAX_AI_CLIPS)
        val bengali = vm._appLanguage.value.tag?.startsWith("bn") == true
        val created = mutableListOf<TextOverlay>()
        var silentClips = 0
        var captionedClips = 0

        // Offline first: the on-device Vosk model needs no endpoint, no key
        // and no network, so it always wins when it is installed.
        if (offlineModel != null) {
            return offlineCaptions(
                vm = vm,
                project = project,
                context = context,
                modelPath = offlineModel.path,
                clips = capped,
                totalTargets = targets.size
            )
        }

        for (clip in capped) {
            if (created.size >= MAX_AI_CAPTIONS) break
            val uriString = clip.uriString ?: continue
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
                is AiAudioExtractor.Result.Failure -> {
                    if (extracted.noAudio) {
                        silentClips++
                        continue
                    }
                    return "Captioning stopped at '${clip.title}': ${extracted.message}"
                }
                is AiAudioExtractor.Result.Ok -> extracted
            }

            val transcript = try {
                val bytes = audio.file.readBytes()
                when (
                    val result = AiClient.transcribe(
                        config = config,
                        audio = bytes,
                        fileName = audio.file.name,
                        language = if (bengali) "bn" else null,
                        fallbackDurationMs = audio.durationMs
                    )
                ) {
                    is AiResult.Failure -> return "Captioning stopped at '${clip.title}': ${result.message}"
                    is AiResult.Ok -> result.value
                }
            } catch (error: Throwable) {
                return "Could not read '${clip.title}' audio: ${error.message ?: "unknown error"}"
            } finally {
                runCatching { audio.file.delete() }
            }

            val overlays = AutoCaptionManager.overlaysFromTranscript(
                segments = transcript.segments,
                sourceOffsetMs = audio.sourceStartMs,
                clipStartMs = timelineStartOf(project, clip.id),
                trimStartMs = clip.trimStartMs,
                speed = clip.speed,
                clipDurationMs = clip.durationMs
            ).take(MAX_AI_CAPTIONS - created.size)
            if (overlays.isNotEmpty()) {
                created += overlays
                captionedClips++
            } else {
                silentClips++
            }
        }

        if (created.isEmpty()) {
            return "The AI endpoint heard no speech in ${capped.size} clip(s)"
        }
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + created)
        }
        vm._selectedTextId.value = created.first().id
        AppLog.info(
            "EditorViewModel",
            "Added ${created.size} AI captions from $captionedClips/${capped.size} clip(s) via ${config.speechModel}"
        )
        val skipped = when {
            silentClips > 0 -> ", $silentClips silent clip(s) skipped"
            else -> ""
        }
        val truncated = if (targets.size > capped.size) {
            " (first ${capped.size} clips)"
        } else {
            ""
        }
        return "Added ${created.size} AI caption${if (created.size == 1) "" else "s"}" +
            " from $captionedClips clip${if (captionedClips == 1) "" else "s"}$skipped$truncated"
    }

    /**
     * The offline path: decode each clip's audio to 16 kHz mono, recognise it
     * with the downloaded Vosk model and place the timed words on the timeline
     * with the same trim/speed mapping the cloud path uses.
     */
    private suspend fun offlineCaptions(
        vm: EditorViewModel,
        project: com.example.model.VideoProject,
        context: android.content.Context,
        modelPath: String,
        clips: List<com.example.model.MediaClip>,
        totalTargets: Int
    ): String {
        val created = mutableListOf<TextOverlay>()
        var silentClips = 0
        var captionedClips = 0

        for (clip in clips) {
            if (created.size >= MAX_AI_CAPTIONS) break
            val uriString = clip.uriString ?: continue
            val uri = android.net.Uri.parse(uriString)
            val sourceSpanMs = (clip.durationMs * clip.speed.coerceAtLeast(0.01f)).toLong()
                .takeIf { it > 0L }
                ?: ClipTiming.sourceLengthMs(clip)

            val segments = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val pcm = com.example.data.captions.PcmExtractor.extract(
                    context = context,
                    uri = uri,
                    startMs = clip.trimStartMs,
                    maxDurationMs = sourceSpanMs.coerceAtMost(MAX_OFFLINE_SPAN_MS)
                )
                if (pcm.samples.isEmpty()) {
                    emptyList()
                } else {
                    val words = com.example.data.captions.VoskTranscriber(modelPath)
                        .recognize(pcm.samples, pcm.sampleRate)
                    AutoCaptionManager.segmentsFromWords(words)
                }
            }
            if (segments.isEmpty()) {
                silentClips++
                continue
            }
            val overlays = AutoCaptionManager.overlaysFromTranscript(
                segments = segments,
                sourceOffsetMs = clip.trimStartMs,
                clipStartMs = timelineStartOf(project, clip.id),
                trimStartMs = clip.trimStartMs,
                speed = clip.speed,
                clipDurationMs = clip.durationMs
            ).take(MAX_AI_CAPTIONS - created.size)
            if (overlays.isNotEmpty()) {
                created += overlays
                captionedClips++
            } else {
                silentClips++
            }
        }

        if (created.isEmpty()) {
            return "The offline model heard no speech in ${clips.size} clip(s)"
        }
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + created)
        }
        vm._selectedTextId.value = created.first().id
        AppLog.info(
            "EditorViewModel",
            "Added ${created.size} offline captions from $captionedClips/${clips.size} clip(s)"
        )
        val skipped = if (silentClips > 0) ", $silentClips silent clip(s) skipped" else ""
        val truncated = if (totalTargets > clips.size) " (first ${clips.size} clips)" else ""
        return "Added ${created.size} offline caption${if (created.size == 1) "" else "s"}" +
            " from $captionedClips clip${if (captionedClips == 1) "" else "s"}$skipped$truncated"
    }

    /** Offline decode/refuse span per clip (Vosk small models are fast but not free). */
    private const val MAX_OFFLINE_SPAN_MS = 15 * 60_000L

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
    private const val MAX_AI_CAPTIONS = 500

    /** Upper bound on API calls per tap (each clip is one upload). */
    private const val MAX_AI_CLIPS = 20

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

    /**
     * Adds (or replaces) a keyframe on the selected text overlay at the
     * playhead. The renderers already evaluate these keyframes in both the
     * preview and the exported file; this is the missing way to author them.
     *
     * Only properties the text renderer actually reads are accepted. Returns
     * false when nothing is selected or the playhead is outside the overlay.
     */
    fun setTextKeyframe(
        vm: EditorViewModel,
        property: KeyframeProperty,
        value: Float,
        easing: EasingCurve = EasingCurve.EASE_IN_OUT
    ): Boolean {
        if (property !in TEXT_KEYFRAME_PROPERTIES) return false
        val id = vm._selectedTextId.value ?: return false
        val project = vm._currentProject.value ?: return false
        val text = project.textOverlays.firstOrNull { it.id == id } ?: return false
        val localMs = (vm._currentPositionMs.value - text.startTimelineMs)
            .coerceIn(0L, text.durationMs)
        if (vm._currentPositionMs.value < text.startTimelineMs ||
            vm._currentPositionMs.value > text.startTimelineMs + text.durationMs
        ) {
            return false
        }
        val safeValue = when (property) {
            KeyframeProperty.POSITION_X, KeyframeProperty.POSITION_Y -> value.coerceIn(0f, 1f)
            KeyframeProperty.SCALE -> value.coerceIn(0.2f, 4f)
            KeyframeProperty.OPACITY -> value.coerceIn(0f, 1f)
            else -> value
        }
        vm.updateProject("text-keyframe:$id:$property") { proj ->
            proj.copy(
                textOverlays = proj.textOverlays.map { item ->
                    if (item.id != id) {
                        item
                    } else {
                        val kept = item.keyframes.filterNot {
                            it.property == property && kotlin.math.abs(it.timestampMs - localMs) < 100L
                        }
                        val keyframe = Keyframe(
                            timestampMs = localMs,
                            property = property,
                            value = safeValue,
                            easing = easing
                        )
                        item.copy(keyframes = (kept + keyframe).sortedBy { it.timestampMs })
                    }
                }
            )
        }
        return true
    }

    fun removeTextKeyframe(vm: EditorViewModel, keyframeId: String) {
        val id = vm._selectedTextId.value ?: return
        vm.updateProject { proj ->
            proj.copy(
                textOverlays = proj.textOverlays.map { item ->
                    if (item.id == id) {
                        item.copy(keyframes = item.keyframes.filterNot { it.id == keyframeId })
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun clearTextKeyframes(vm: EditorViewModel) {
        val id = vm._selectedTextId.value ?: return
        vm.updateProject { proj ->
            proj.copy(
                textOverlays = proj.textOverlays.map { item ->
                    if (item.id == id) item.copy(keyframes = emptyList()) else item
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

    /** Properties the text renderer evaluates (volume/rotation are clip-only). */
    private val TEXT_KEYFRAME_PROPERTIES = setOf(
        KeyframeProperty.POSITION_X,
        KeyframeProperty.POSITION_Y,
        KeyframeProperty.SCALE,
        KeyframeProperty.OPACITY
    )
}
