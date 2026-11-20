package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.engine.ClipAnimationMath
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
    val context = LocalContext.current
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
        onKenBurns = {
            Toast.makeText(context, viewModel.toggleKenBurns(), Toast.LENGTH_SHORT).show()
        },
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
        currentDurationMs = currentClip?.animationDurationMs
            ?: ClipAnimationMath.DURATION_MS,
        onSelect = { viewModel.setClipAnimation(it) },
        onDurationChanged = { viewModel.setClipAnimationDuration(it) },
        onClose = { viewModel.setActiveSheet(ActiveToolSheet.NONE) }
    )
}

@Composable
internal fun AdjustmentsToolSheet(
    viewModel: EditorViewModel,
    currentClip: MediaClip?
) {
    val compareOriginal by viewModel.compareOriginal.collectAsState()
    AdjustmentsSheet(
        clip = currentClip,
        onUpdateAdjustments = { b, c, s, v ->
            viewModel.updateClipAdjustments(b, c, s, v)
        },
        onUpdateGradeAdjustments = { exposure, temperature, tint, vibrance ->
            viewModel.updateGradeAdjustments(exposure, temperature, tint, vibrance)
        },
        onUpdateShaderAdjustments = { highlights, shadows, sharpen, grain, blurAmount ->
            viewModel.updateShaderAdjustments(highlights, shadows, sharpen, grain, blurAmount)
        },
        onToggleRetouch = { enabled ->
            viewModel.setClipRetouch(enabled)
        },
        onToggleRelight = { strength ->
            viewModel.setClipRelight(strength)
        },
        onUpdateHsl = { adjustments ->
            // The HSL grade belongs to the clip that is being graded.
            currentClip?.id?.let { viewModel.setClipHslGrade(it, adjustments) }
        },
        isComparing = compareOriginal,
        onCompareChanged = { enabled -> viewModel.setCompareOriginal(enabled) },
        onClose = {
            // Leaving the sheet always restores the graded preview.
            viewModel.setCompareOriginal(false)
            viewModel.closeToolSheet()
        }
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
        currentRotation = currentClip?.rotation ?: 0f,
        onRotationChanged = { viewModel.setSelectedClipRotation(it) },
        currentMirrored = currentClip?.isMirrored ?: false,
        onMirroredChanged = { viewModel.setSelectedClipMirrored(it) },
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
