package com.example.viewmodel.features

import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.AutoCaptionManager
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
     * Places offline sample captions on the timeline. Cues come from
     * [AutoCaptionManager], so a three-second phrase becomes two or three short
     * subtitle cards instead of one wall of text.
     */
    fun addAutoCaptions(vm: EditorViewModel) {
        val project = vm._currentProject.value ?: return
        vm.viewModelScope.launch {
            val bengali = vm._appLanguage.value.tag?.startsWith("bn") == true
            val overlays = AutoCaptionManager.generateCaptions(
                durationMs = project.totalDurationMs,
                language = if (bengali) "Bengali" else "English",
            )
            if (overlays.isEmpty()) return@launch
            vm.updateProject { proj ->
                proj.copy(textOverlays = proj.textOverlays + overlays)
            }
            vm._selectedTextId.value = overlays.first().id
            AppLog.info("EditorViewModel", "Added ${overlays.size} auto captions")
        }
    }

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

    fun deleteSelectedText(vm: EditorViewModel) {
        val id = vm._selectedTextId.value ?: return
        vm.updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays.filterNot { it.id == id })
        }
        vm._selectedTextId.value = null
    }
}
