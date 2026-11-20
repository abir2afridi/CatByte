package com.example.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.State
import com.example.engine.Shortcuts
import com.example.viewmodel.EditorViewModel

/**
 * The editor's keyboard shortcuts.
 *
 * Tablets, Chromebooks and DeX get a real editing keyboard: play/pause,
 * split, delete, undo/redo, zoom and frame stepping all work without
 * touching the canvas.
 */
internal fun Modifier.editorKeyboardShortcuts(
    focusRequester: FocusRequester,
    viewModel: EditorViewModel,
    timelineZoom: Float,
    positionState: State<Long>,
    splitAtPlayhead: () -> Unit
): Modifier = this
    .focusRequester(focusRequester)
    .focusable()
    .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val command = Shortcuts.resolve(
            key = event.key.toEditorKey(),
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed,
        ) ?: return@onPreviewKeyEvent false
        when (command) {
            Shortcuts.Command.PLAY_PAUSE -> viewModel.togglePlayback()
            Shortcuts.Command.SPLIT -> splitAtPlayhead()
            Shortcuts.Command.DELETE -> viewModel.deleteSelectedClip()
            Shortcuts.Command.UNDO -> viewModel.undo()
            Shortcuts.Command.REDO -> viewModel.redo()
            Shortcuts.Command.ZOOM_IN -> viewModel.setTimelineZoom(timelineZoom + 0.25f)
            Shortcuts.Command.ZOOM_OUT -> viewModel.setTimelineZoom(timelineZoom - 0.25f)
            Shortcuts.Command.PREV_FRAME ->
                viewModel.seekTo((positionState.value - FRAME_STEP_MS).coerceAtLeast(0L))
            Shortcuts.Command.NEXT_FRAME -> viewModel.seekTo(positionState.value + FRAME_STEP_MS)
            // M flips the main track's audio, like a console channel mute.
            Shortcuts.Command.TOGGLE_MUTE -> viewModel.toggleMainTrackMute()
        }
        true
    }
