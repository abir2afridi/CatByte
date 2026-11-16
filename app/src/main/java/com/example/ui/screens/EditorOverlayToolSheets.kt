package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.model.MediaClip
import com.example.model.TextOverlay
import com.example.model.VideoFilter
import com.example.model.VideoProject
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
    EffectsSheet(
        onAddEffect = { fx ->
            if (fx != null) {
                viewModel.addTimelineEffect(fx)
            }
            viewModel.setActiveSheet(ActiveToolSheet.NONE)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun FiltersToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    FiltersSheet(
        activeFilter = currentClip?.filter ?: VideoFilter.NONE,
        onSelectFilter = { filter ->
            viewModel.setClipFilter(filter)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun TransitionsToolSheet(
    viewModel: EditorViewModel,
    activeTransitionClipId: String?
) {
    TransitionsPickerSheet(
        clipId = activeTransitionClipId ?: "",
        onSelectTransition = { trans ->
            viewModel.setClipTransition(trans)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}
