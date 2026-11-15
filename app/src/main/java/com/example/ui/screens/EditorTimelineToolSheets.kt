package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.model.VideoProject
import com.example.ui.components.AiToolsSheet
import com.example.ui.components.CaptionsSheet
import com.example.ui.components.CoverEditorSheet
import com.example.ui.components.TimelineToolsSheet
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * The project-level tool sheets of the editor: the timeline tools (markers,
 * beat sync, multi-select, zoom), the caption tools, the AI tools and the
 * cover editor.
 */

@Composable
internal fun TimelineToolsToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject,
    isMultiSelect: Boolean,
    magneticSnap: Boolean,
    timelineZoom: Float,
    selectedClipId: String?,
    onMultiSelectChange: (Boolean) -> Unit
) {
    TimelineToolsSheet(
        isMultiSelect = isMultiSelect,
        magneticSnap = magneticSnap,
        zoom = timelineZoom,
        markerCount = project?.markers?.size ?: 0,
        hasSelectedClip = selectedClipId != null,
        onBeatSync = { viewModel.autoCutToBeat() },
        onAddMarker = { viewModel.addMarker() },
        onPreviousMarker = { viewModel.jumpToMarker(-1) },
        onNextMarker = { viewModel.jumpToMarker(1) },
        onToggleMultiSelect = {
            val next = !isMultiSelect
            onMultiSelectChange(next)
            if (!next) viewModel.clearSelection()
        },
        onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
        onZoomChanged = { viewModel.setTimelineZoom(it) },
        onToggleKeyframe = { viewModel.toggleKeyframeAtPlayhead() },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun CaptionsToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject,
    onLaunchCaptionImport: () -> Unit
) {
    val context = LocalContext.current
    CaptionsSheet(
        onAutoCaptions = { viewModel.addAutoCaptions() },
        onImportCaptions = { onLaunchCaptionImport() },
        onExportSrt = {
            val content = viewModel.exportSubtitles(toVtt = false)
            if (content == null) {
                Toast.makeText(context, "No captions to export yet", Toast.LENGTH_LONG).show()
            } else {
                shareSubtitleExport(context, project.id, content)
            }
        },
        onExportVtt = {
            val content = viewModel.exportSubtitles(toVtt = true)
            if (content == null) {
                Toast.makeText(context, "No captions to export yet", Toast.LENGTH_LONG).show()
            } else {
                shareSubtitleExport(context, project.id, content)
            }
        },
        onAddText = {
            viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
        },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun AiToolsToolSheet(viewModel: EditorViewModel) {
    val context = LocalContext.current
    AiToolsSheet(
        onSelect = { name ->
            Toast.makeText(
                context,
                "$name needs an online AI model — unavailable in this offline build",
                Toast.LENGTH_LONG
            ).show()
        },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun CoverEditorToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject
) {
    val context = LocalContext.current
    CoverEditorSheet(
        project = project,
        onSaveCover = { _, _ ->
            Toast.makeText(context, "Cover saved successfully", Toast.LENGTH_SHORT).show()
        },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}
