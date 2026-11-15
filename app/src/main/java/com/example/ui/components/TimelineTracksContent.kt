package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.engine.ClipTrimHandle
import com.example.model.VideoProject

/** The stacked main / overlay / audio / text / effects track rows. */
@Composable
internal fun TimelineTracksContent(
    project: VideoProject,
    selectedClipId: String?,
    selectedTextId: String?,
    msToDp: Float,
    fps: Int,
    isMultiSelect: Boolean,
    selectedClipIds: Set<String>,
    onSelectClip: (String) -> Unit,
    onTransitionClick: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit,
    onMoveOverlayClip: (clipId: String, newStartMs: Long) -> Unit,
    onMoveOverlayClipToTrack: (clipId: String, targetTrackId: String, newStartMs: Long) -> Unit,
    onTrimStart: (clipId: String, edge: ClipTrimHandle.Edge) -> Unit,
    onTrimPreview: (clipId: String, edge: ClipTrimHandle.Edge, deltaMs: Long) -> Unit,
    onTrimEnd: (clipId: String, edge: ClipTrimHandle.Edge) -> Unit,
    onMoveAudioClip: (audioId: String, newStartMs: Long) -> Unit,
    onToggleClipSelection: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onAddAudioClick: () -> Unit,
    onAddTextClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Main Video Track
        TimelineVideoTrack(
            clips = project.clips,
            selectedClipId = selectedClipId,
            msToDp = msToDp,
            onSelectClip = onSelectClip,
            onTransitionClick = onTransitionClick,
            onAddMediaClick = onAddMediaClick,
            onMoveClip = onMoveClip,
            onTrimStart = onTrimStart,
            onTrimPreview = onTrimPreview,
            onTrimEnd = onTrimEnd,
            fps = fps,
            isLocked = project.isMainTrackLocked,
            isMultiSelect = isMultiSelect,
            selectedClipIds = selectedClipIds,
            onToggleClipSelection = onToggleClipSelection
        )

        Spacer(modifier = Modifier.height(5.dp))

        // 2. Overlay Tracks
        project.overlayTracks.forEachIndexed { trackIndex, track ->
            TimelineOverlayTrack(
                clips = track.clips,
                selectedClipId = selectedClipId,
                msToDp = msToDp,
                onSelectClip = onSelectClip,
                onMoveClip = { clipId, startMs, trackShift ->
                    if (trackShift == 0) {
                        onMoveOverlayClip(clipId, startMs)
                    } else {
                        val targetIndex = (trackIndex + trackShift).coerceIn(
                            0,
                            project.overlayTracks.lastIndex
                        )
                        onMoveOverlayClipToTrack(
                            clipId,
                            project.overlayTracks[targetIndex].id,
                            startMs
                        )
                    }
                },
                onTrimStart = onTrimStart,
                onTrimPreview = onTrimPreview,
                onTrimEnd = onTrimEnd,
                fps = fps,
                isLocked = track.isLocked,
                trackIndex = trackIndex,
                trackCount = project.overlayTracks.size,
                isMultiSelect = isMultiSelect,
                selectedClipIds = selectedClipIds,
                onToggleClipSelection = onToggleClipSelection
            )
            Spacer(modifier = Modifier.height(5.dp))
        }

        // 3. Audio Track (+ Add audio)
        TimelineAudioTrack(
            tracks = project.audioTracks,
            msToDp = msToDp,
            onMoveClip = onMoveAudioClip,
            selectedAudioId = null,
            onAddAudioClick = onAddAudioClick
        )

        Spacer(modifier = Modifier.height(5.dp))

        // 4. Text Track (+ Add text)
        TimelineTextTrack(
            texts = project.textOverlays,
            selectedTextId = selectedTextId,
            msToDp = msToDp,
            onSelectText = onSelectText,
            onAddTextClick = onAddTextClick
        )

        if (project.effects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(5.dp))
            TimelineEffectsTrack(
                effects = project.effects,
                msToDp = msToDp
            )
        }
    }
}
