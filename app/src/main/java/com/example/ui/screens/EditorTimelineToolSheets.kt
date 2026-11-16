package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.TemplateEngine
import com.example.model.VideoProject
import com.example.ui.components.AiImagePromptDialog
import com.example.ui.components.AiToolsSheet
import com.example.ui.components.CaptionsSheet
import com.example.ui.components.CoverEditorSheet
import com.example.ui.components.TemplatePickerSheet
import com.example.ui.components.TimelineToolsSheet
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.features.AiFeature
import kotlinx.coroutines.launch

/**
 * The project-level tool sheets of the editor: the timeline tools (markers,
 * beat sync, multi-select, zoom), the caption tools, the AI tools, the
 * template library and the cover editor.
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
    val safeAreaGuides by viewModel.showSafeAreaGuides.collectAsState()
    TimelineToolsSheet(
        isMultiSelect = isMultiSelect,
        magneticSnap = magneticSnap,
        zoom = timelineZoom,
        markerCount = project?.markers?.size ?: 0,
        hasSelectedClip = selectedClipId != null,
        safeAreaGuides = safeAreaGuides,
        onBeatSync = { viewModel.autoCutToBeat() },
        onAddMarker = { viewModel.addMarker() },
        onPreviousMarker = { viewModel.jumpToMarker(-1) },
        onNextMarker = { viewModel.jumpToMarker(1) },
        onOpenMarkers = { viewModel.setActiveSheet(ActiveToolSheet.MARKERS) },
        onToggleMultiSelect = {
            val next = !isMultiSelect
            onMultiSelectChange(next)
            if (!next) viewModel.clearSelection()
        },
        onToggleMagneticSnap = { viewModel.toggleMagneticSnap() },
        onToggleSafeAreaGuides = { viewModel.toggleSafeAreaGuides(it) },
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
    val scope = rememberCoroutineScope()
    CaptionsSheet(
        onAutoCaptions = {
            scope.launch { Toast.makeText(context, viewModel.addAutoCaptions(), Toast.LENGTH_LONG).show() }
        },
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
internal fun AiToolsToolSheet(
    viewModel: EditorViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val project by viewModel.currentProject.collectAsState()
    var showImagePrompt by remember { mutableStateOf(false) }

    fun notify(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    AiToolsSheet(
        configured = aiConfig.isConfigured,
        endpointLabel = android.net.Uri.parse(aiConfig.baseUrl).host ?: aiConfig.baseUrl,
        onRunTool = { id ->
            when (id) {
                "remix" -> scope.launch { notify(AiFeature.remixScript(viewModel)) }
                "translate" -> scope.launch { notify(AiFeature.translateCaptions(viewModel)) }
                "packaging" -> scope.launch { notify(AiFeature.packagingTitles(viewModel)) }
                "captions" -> scope.launch { notify(viewModel.addAutoCaptions()) }
                "image" -> showImagePrompt = true
                else -> notify("This tool needs a specialised model that the configured endpoint does not provide")
            }
        },
        onOpenSettings = onOpenSettings,
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )

    if (showImagePrompt) {
        AiImagePromptDialog(
            defaultPrompt = project?.title?.ifBlank { "cinematic background" }
                ?.plus(", vertical 9:16 background, no text")
                ?: "cinematic 9:16 background, no text",
            onGenerate = { prompt ->
                showImagePrompt = false
                scope.launch {
                    notify("Generating image with ${aiConfig.imageModel}…")
                    notify(AiFeature.generateBackgroundImage(viewModel, prompt))
                }
            },
            onDismiss = { showImagePrompt = false }
        )
    }
}

@Composable
internal fun CoverEditorToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject
) {
    val context = LocalContext.current
    CoverEditorSheet(
        project = project,
        onSaveCover = { frameMs, customUri ->
            viewModel.setProjectCover(frameMs, customUri?.toString().orEmpty())
            Toast.makeText(context, "Cover saved", Toast.LENGTH_SHORT).show()
        },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun TemplateToolSheet(viewModel: EditorViewModel) {
    TemplatePickerSheet(
        templates = TemplateEngine.getViralTemplates(),
        onApplyTemplate = { template -> viewModel.applyTemplate(template) },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}
