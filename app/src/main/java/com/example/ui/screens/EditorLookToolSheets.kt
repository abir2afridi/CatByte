package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.BlendMode
import com.example.model.ChromaKeyData
import com.example.model.MaskData
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.ui.components.BackgroundSheet
import com.example.ui.components.BlendModeSheet
import com.example.ui.components.CanvasRatioSheet
import com.example.ui.components.ChromaKeySheet
import com.example.ui.components.DrawSheet
import com.example.ui.components.MarkerSheet
import com.example.ui.components.KeyframeSheet
import com.example.ui.components.MaskSheet
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * The look tool sheets of the editor: canvas ratio and background, plus the
 * per-clip keyframes, blend mode, mask and chroma key.
 */

@Composable
internal fun CanvasRatioToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject
) {
    CanvasRatioSheet(
        activeRatio = project.aspectRatio,
        currentPaperStyle = project.paperStyle,
        onSelectRatio = { viewModel.setAspectRatio(it) },
        onSelectPaperStyle = { viewModel.setPaperStyle(it) },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun BackgroundToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject
) {
    BackgroundSheet(
        currentPaperStyle = project.paperStyle,
        onSelectBackground = { viewModel.setPaperStyle(it) },
        currentBackgroundImageUri = project.backgroundImageUri,
        // Imported into app storage: photo-picker grants do not survive a reboot.
        onPickBackgroundImage = { picked -> viewModel.importBackgroundImage(picked) },
        onClearBackgroundImage = { viewModel.setBackgroundImage("") },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun KeyframeToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?,
    selectedClipId: String?
) {
    KeyframeSheet(
        clip = currentClip,
        onAddKeyframe = { prop, val1, ease ->
            if (selectedClipId != null) {
                viewModel.addKeyframe(selectedClipId, prop, val1, ease)
            }
        },
        onRemoveKeyframe = { kfId ->
            if (selectedClipId != null) {
                viewModel.removeKeyframe(selectedClipId, kfId)
            }
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun BlendModeToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?,
    selectedClipId: String?
) {
    BlendModeSheet(
        selectedMode = currentClip?.blendMode ?: BlendMode.NORMAL,
        onSelectBlendMode = { mode ->
            if (selectedClipId != null) {
                viewModel.setClipBlendMode(selectedClipId, mode)
            }
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun MaskToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?,
    selectedClipId: String?
) {
    MaskSheet(
        maskData = currentClip?.mask ?: MaskData(),
        onUpdateMask = { mask ->
            if (selectedClipId != null) {
                viewModel.setClipMask(selectedClipId, mask)
            }
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun ChromaKeyToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?,
    selectedClipId: String?,
    isEyedropperActive: Boolean
) {
    ChromaKeySheet(
        chromaData = currentClip?.chromaKey ?: ChromaKeyData(),
        isEyedropperActive = isEyedropperActive,
        onToggleEyedropper = { viewModel.setEyedropperActive(!isEyedropperActive) },
        onUpdateChroma = { data ->
            if (selectedClipId != null) {
                viewModel.setClipChromaKey(selectedClipId, data)
            }
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun MarkerToolSheet(
    viewModel: EditorViewModel,
    project: VideoProject
) {
    MarkerSheet(
        markers = project.markers,
        onAddAtPlayhead = { viewModel.addMarker() },
        onJump = { viewModel.seekToMarker(it) },
        onMoveToPlayhead = { viewModel.moveMarkerToPlayhead(it) },
        onRename = { id, label -> viewModel.renameMarker(id, label) },
        onColor = { id, color -> viewModel.setMarkerColor(id, color) },
        onDelete = { viewModel.removeMarker(it) },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun DrawToolSheet(viewModel: EditorViewModel) {
    DrawSheet(
        onApply = { points, color, width -> viewModel.addStroke(points, color, width) },
        onClearAll = { viewModel.clearStrokes() },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}
