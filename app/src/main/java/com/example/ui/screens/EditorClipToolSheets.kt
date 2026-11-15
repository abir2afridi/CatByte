package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.ClipAnimation
import com.example.model.MediaClip
import com.example.ui.components.AdjustmentsSheet
import com.example.ui.components.AnimationSheet
import com.example.ui.components.CropSheet
import com.example.ui.components.EditClipSheet
import com.example.ui.components.OpacitySheet
import com.example.ui.components.SpeedSheet
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * The clip-level tool sheets of the editor: the clip action sheet, speed,
 * animations, adjustments, crop and opacity.
 *
 * Every sheet lives in its own component file under `ui/components/`; this
 * file only wires them to the ViewModel.
 */

/** Closes the sheet and leaves the eyedropper mode. */
internal fun EditorViewModel.closeToolSheet() {
    setEyedropperActive(false)
    setActiveSheet(ActiveToolSheet.NONE)
}

@Composable
internal fun EditClipToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?,
    splitAtPlayhead: () -> Unit
) {
    EditClipSheet(
        clip = currentClip,
        onSplit = splitAtPlayhead,
        onDelete = { viewModel.deleteSelectedClip() },
        onDuplicate = { viewModel.duplicateSelectedClip() },
        onRotate = { viewModel.rotateSelectedClip() },
        onMirror = { viewModel.mirrorSelectedClip() },
        onOpenSpeed = { viewModel.setActiveSheet(ActiveToolSheet.SPEED) },
        onOpenAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
        onOpenKeyframe = { viewModel.setActiveSheet(ActiveToolSheet.KEYFRAME) },
        onOpenBlendMode = { viewModel.setActiveSheet(ActiveToolSheet.BLEND_MODE) },
        onOpenMask = { viewModel.setActiveSheet(ActiveToolSheet.MASK) },
        onExtractAudio = { viewModel.extractAudioFromSelectedClip() },
        onFreeze = { viewModel.freezeFrameAtPlayhead() },
        onReverse = { viewModel.reverseSelectedClip() },
        onOpenVolume = { viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST) },
        onOpenChroma = { viewModel.setActiveSheet(ActiveToolSheet.CHROMA_KEY) },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun SpeedToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    SpeedSheet(
        currentSpeed = currentClip?.speed ?: 1.0f,
        clipDurationMs = currentClip?.durationMs ?: 0L,
        isChangePitch = currentClip?.maintainPitch == false,
        onSpeedSelected = { viewModel.setClipSpeed(it) },
        onSelectSpeedCurve = { viewModel.setClipSpeedCurve(it) },
        onChangePitch = { changePitch -> viewModel.setClipMaintainPitch(!changePitch) },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun AnimationToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    AnimationSheet(
        current = currentClip?.animationIn ?: ClipAnimation.NONE,
        onSelect = { viewModel.setClipAnimation(it) },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun AdjustmentsToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    AdjustmentsSheet(
        clip = currentClip,
        onUpdateAdjustments = { b, c, s, v ->
            viewModel.updateClipAdjustments(b, c, s, v)
        },
        onClose = { viewModel.closeToolSheet() }
    )
}

@Composable
internal fun CropToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    CropSheet(
        sourceWidth = currentClip?.sourceWidth ?: 0,
        sourceHeight = currentClip?.sourceHeight ?: 0,
        currentScale = currentClip?.scale ?: 1.0f,
        currentPosX = currentClip?.posX ?: 0.5f,
        currentPosY = currentClip?.posY ?: 0.5f,
        onApply = { scale, posX, posY ->
            viewModel.setSelectedClipCrop(scale, posX, posY)
        },
        onReset = { viewModel.resetSelectedClipCrop() },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun OpacityToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    OpacitySheet(
        currentOpacity = currentClip?.opacity ?: 1.0f,
        onOpacityChange = { viewModel.setClipOpacity(it) },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}
