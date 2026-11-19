package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MediaClip
import com.example.model.TextOverlay
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.ui.components.BodyEffectTabs
import com.example.ui.components.EffectsSheet
import com.example.ui.components.FiltersSheet
import com.example.ui.components.StickersSheet
import com.example.ui.components.TransitionsPickerSheet
import com.example.ui.text.TextEditorSheet
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

/**
 * The overlay tool sheets of the editor: text, stickers, timeline effects,
 * filters and transitions.
 */

@Composable
internal fun TextEditorToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject,
    currentText: TextOverlay?,
    captionExportCount: Int,
    onLaunchCaptionImport: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playheadMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    TextEditorSheet(
        textOverlay = currentText,
        onAddOrUpdateText = { text, font, color, anim ->
            if (currentText != null) {
                viewModel.updateSelectedText(text, font, color, anim)
            } else {
                viewModel.addTextOverlay(text, font, color, anim)
            }
        },
        onDeleteText = {
            viewModel.deleteSelectedText()
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onImportCaptions = { onLaunchCaptionImport() },
        onExportCaptions = {
            val content = viewModel.exportSubtitles(toVtt = false)
            if (content == null) {
                Toast.makeText(context, "No text overlays to export yet", Toast.LENGTH_LONG).show()
            } else {
                shareSubtitleExport(context, project.id, content)
            }
        },
        captionExportCount = captionExportCount,
        onUpdateTextStyle = { bold, italic, letterSpacing, lineSpacing ->
            viewModel.setSelectedTextStyle(bold, italic, letterSpacing, lineSpacing)
        },
        onAutoCaptions = {
            scope.launch { Toast.makeText(context, viewModel.addAutoCaptions(), Toast.LENGTH_LONG).show() }
        },
        playheadMs = playheadMs,
        onSetTextKeyframe = { property, value -> viewModel.setTextKeyframe(property, value) },
        onRemoveTextKeyframe = { keyframeId -> viewModel.removeTextKeyframe(keyframeId) },
        onClearTextKeyframes = { viewModel.clearTextKeyframes() },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun StickersToolSheet(viewModel: EditorViewModel) {
    StickersSheet(
        onAddSticker = { emoji ->
            viewModel.addSticker(emoji)
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun EffectsToolSheet(viewModel: EditorViewModel) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val position by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    // The "None" card removes the effect the playhead is on, so it is only
    // enabled when there is one.
    val canRemove = project?.effects.orEmpty().any { effect ->
        position >= effect.startTimelineMs &&
            position < effect.startTimelineMs + effect.durationMs
    }
    EffectsSheet(
        canRemoveEffect = canRemove,
        onAddEffect = { fx ->
            viewModel.addTimelineEffect(fx)
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onRemoveEffect = {
            viewModel.removeEffectAtPlayhead()
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun BodyEffectsToolSheet(viewModel: EditorViewModel) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val position by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val canRemove = project?.effects.orEmpty().any { effect ->
        position >= effect.startTimelineMs &&
            position < effect.startTimelineMs + effect.durationMs
    }
    EffectsSheet(
        canRemoveEffect = canRemove,
        onAddEffect = { fx ->
            viewModel.addTimelineEffect(fx)
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onRemoveEffect = {
            viewModel.removeEffectAtPlayhead()
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onClose = { viewModel.closeToolSheet() },
        tabs = BodyEffectTabs,
        title = "Body effects"
    )
}

@Composable
internal fun FiltersToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    val importedLuts by viewModel.importedLuts.collectAsStateWithLifecycle()
    val activeLutId by viewModel.activeLutId.collectAsStateWithLifecycle()
    FiltersSheet(
        activeFilter = currentClip?.filter ?: VideoFilter.NONE,
        importedLuts = importedLuts,
        activeLutId = activeLutId,
        onSelectFilter = { filter ->
            viewModel.setClipFilter(filter)
        },
        onSelectLut = { lut -> viewModel.setActiveLut(lut) },
        onOpenLutImport = { viewModel.setActiveSheet(ActiveToolSheet.LUT_IMPORT) },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun LutImportToolSheet(viewModel: EditorViewModel) {
    val importedLuts by viewModel.importedLuts.collectAsStateWithLifecycle()
    com.example.ui.components.LutImportSheet(
        importedLuts = importedLuts,
        onImportLut = { viewModel.addImportedLut(it) },
        onRemoveLut = { viewModel.removeImportedLut(it) },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY) }
    )
}

@Composable
internal fun TransitionsToolSheet(
    viewModel: EditorViewModel,
    activeTransitionClipId: String?
) {
    val clipId = activeTransitionClipId ?: return
    val clip = viewModel.currentProject.value?.allVideoClips?.firstOrNull { it.id == clipId }
    TransitionsPickerSheet(
        clipId = clipId,
        currentTransition = clip?.transition ?: TransitionType.NONE,
        currentDurationMs = clip?.transitionDurationMs ?: 400L,
        onSelectTransition = { targetClipId, transition ->
            // The transition belongs to the clip the picker was opened for,
            // not to whatever clip happens to be selected.
            viewModel.setClipTransitionForClip(targetClipId, transition)
        },
        onDurationChanged = { targetClipId, durationMs ->
            viewModel.setClipTransitionDuration(targetClipId, durationMs)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}
