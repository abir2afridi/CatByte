package com.example.ui.screens

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * The editor's bottom action bar.
 *
 * A selected clip shows the clip action bar; without a selection the CapCut
 * bottom bar (media, audio, text, effects, ...) is shown.
 */
@Composable
internal fun EditorBottomActionStage(
    viewModel: EditorViewModel,
    selectedClipId: String?,
    isMultiSelect: Boolean,
    splitAtPlayhead: () -> Unit,
    replaceMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    addOverlayLauncher: ActivityResultLauncher<PickVisualMediaRequest>
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121215))
            .navigationBarsPadding()
            .padding(top = 6.dp)
    ) {
        if (selectedClipId != null && !isMultiSelect) {
            ClipActionBar(
                onBack = { viewModel.clearSelection() },
                onSplit = splitAtPlayhead,
                onVolume = { viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST) },
                onAnimations = { viewModel.setActiveSheet(ActiveToolSheet.ANIMATIONS) },
                onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                onDelete = { viewModel.deleteSelectedClip() },
                onSpeed = { viewModel.setActiveSheet(ActiveToolSheet.SPEED) },
                onBeats = { viewModel.autoCutToBeat() },
                onCrop = { viewModel.setActiveSheet(ActiveToolSheet.CROP) },
                onDuplicate = { viewModel.duplicateSelectedClip() },
                onReplace = {
                    replaceMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onOverlay = {
                    addOverlayLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) },
                onAspectRatio = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                onBackground = { viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND) },
                onOpacity = { viewModel.setActiveSheet(ActiveToolSheet.OPACITY) },
                onFreeze = { viewModel.freezeFrameAtPlayhead() },
                onReverse = { viewModel.reverseSelectedClip() },
                onExtractAudio = { viewModel.extractAudioFromSelectedClip() },
                onMore = { viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP) },
                onAiTools = { viewModel.setActiveSheet(ActiveToolSheet.AI_TOOLS) }
            )
        } else {
            CapCutBottomActionBar(
                onEdit = {
                    if (selectedClipId != null) {
                        viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP)
                    } else {
                        Toast.makeText(context, "Select a clip to edit", Toast.LENGTH_SHORT).show()
                    }
                },
                onAudio = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
                onText = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
                onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                onOverlay = {
                    addOverlayLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onCaptions = { viewModel.setActiveSheet(ActiveToolSheet.CAPTIONS) },
                onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) },
                onFilters = { viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY) },
                onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                onRatio = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                onBackground = { viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND) },
                onTools = { viewModel.setActiveSheet(ActiveToolSheet.TIMELINE_TOOLS) }
            )
        }
    }
}
