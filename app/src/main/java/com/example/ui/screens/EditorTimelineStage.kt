package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.VideoProject
import com.example.ui.components.MultiTrackTimeline
import com.example.ui.components.SelectionControlsBar
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/**
 * The editor's timeline stage: the multi-select controls bar and the
 * multi-track timeline itself.
 */
@Composable
internal fun EditorTimelineStage(
    viewModel: EditorViewModel,
    project: VideoProject,
    positionState: State<Long>,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedTextId: String?,
    selectedClipIds: Set<String>,
    hasOverlayClipboard: Boolean,
    timelineZoom: Float,
    magneticSnap: Boolean,
    snapBeats: List<Long>,
    isMultiSelect: Boolean,
    onMultiSelectChange: (Boolean) -> Unit,
    addMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onTransitionClick: (String) -> Unit
) {
    // Group actions for multi-selection.
    val selectedOverlayClips = remember(project, selectedClipIds) {
        project.overlayTracks.flatMap { it.clips }
            .filter { it.id in selectedClipIds }
    }
    val selectedMainClips = remember(project, selectedClipIds) {
        project.clips.filter { it.id in selectedClipIds }
    }
    val canMoveMainBlock = selectedMainClips.size > 1
    if (selectedClipIds.isNotEmpty()) {
        SelectionControlsBar(
            selectedCount = selectedClipIds.size,
            isMultiSelect = isMultiSelect,
            canMoveOverlays = selectedOverlayClips.isNotEmpty(),
            canMoveMainBlock = canMoveMainBlock,
            canPaste = hasOverlayClipboard,
            onMoveLeft = { viewModel.moveSelectedOverlays(-100L) },
            onMoveRight = { viewModel.moveSelectedOverlays(100L) },
            onMoveBlockLeft = { viewModel.moveSelectedMainClips(-1) },
            onMoveBlockRight = { viewModel.moveSelectedMainClips(1) },
            onDelete = { viewModel.deleteSelectedClips() },
            onCopy = { viewModel.copySelectedClips() },
            onCut = { viewModel.cutSelectedClips() },
            onPaste = { viewModel.pasteClips() },
            onCopyAttributes = { viewModel.copySelectedClipAttributes() },
            onPasteAttributes = { viewModel.pasteClipAttributes() },
            onClear = {
                viewModel.clearSelection()
                onMultiSelectChange(false)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Multi-Track Timeline (integrated CapCut left gutter and timecode)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 220.dp)
    ) {
        MultiTrackTimeline(
            project = project,
            currentPositionMs = positionState,
            isPlaying = isPlaying,
            selectedClipId = selectedClipId,
            selectedTextId = selectedTextId,
            zoom = timelineZoom,
            magneticSnap = magneticSnap,
            onSeek = { viewModel.seekTo(it) },
            onSelectClip = {
                viewModel.selectClip(it)
            },
            onSelectText = {
                viewModel.selectText(it)
                viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
            },
            onAddMediaClick = {
                addMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onTransitionClick = { clipId ->
                onTransitionClick(clipId)
            },
            onMoveClip = { from, to -> viewModel.moveClip(from, to) },
            onMoveOverlayClip = { clipId, startMs -> viewModel.moveOverlayClip(clipId, startMs) },
            onMoveOverlayClipToTrack = { clipId, trackId, startMs ->
                viewModel.moveClipToTrack(clipId, trackId, startMs)
            },
            onTrimStart = { clipId, edge ->
                viewModel.beginTrimClip(clipId, edge)
            },
            onTrimPreview = { _, _, deltaMs ->
                // Live: applies against the drag-start snapshot, idempotent.
                viewModel.updateTrimClip(deltaMs, project.fps)
            },
            onTrimEnd = { _, _ ->
                viewModel.endTrimClip()
            },
            onTrackMute = { trackId, muted -> viewModel.setTrackMuted(trackId, muted) },
            onTrackLock = { trackId, locked -> viewModel.setTrackLocked(trackId, locked) },
            onTrackSolo = { trackId, solo -> viewModel.setTrackSolo(trackId, solo) },
            onMoveOverlayTrack = { from, to -> viewModel.moveOverlayTrack(from, to) },
            onMoveAudioClip = { audioId, startMs -> viewModel.moveAudioClip(audioId, startMs) },
            onToggleAudioMute = { audioId, muted -> viewModel.setAudioClipMuted(audioId, muted) },
            onZoomChange = { viewModel.setTimelineZoom(it) },
            onToggleClipSelection = { viewModel.toggleClipSelection(it) },
            isMultiSelect = isMultiSelect,
            selectedClipIds = selectedClipIds,
            beats = snapBeats,
            onUserInteraction = { viewModel.pausePlayback() },
            onClearSelection = { viewModel.clearSelection() },
            onAddAudioClick = { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_LIBRARY) },
            onAddTextClick = { viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR) },
            onCoverClick = { viewModel.setActiveSheet(ActiveToolSheet.COVER_EDITOR) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
