package com.example.data.player

import com.example.data.AppLog
import com.example.data.PlayerManager
import com.example.data.PlayerManager.Companion.TAG
import com.example.data.PlayerRecoveryPolicy

/**
 * Host lifecycle recovery.
 *
 * The preview manager is a process-scoped singleton: it outlives the activity.
 * When the app is only backgrounded (not killed), a player whose surface died
 * can end up errored/idle, and on reopen nothing would play — not even
 * freshly imported media — until the user cleared the app from recents. The
 * editor UI calls these two hooks around foreground events; on resume we
 * rebuild a broken player at the same playhead.
 */

/** Remembers that the host went to the background. */
internal fun PlayerManager.handleHostPause() {
    hostBackgrounded = true
}

/** Rebuilds a broken player when the host comes back. */
internal fun PlayerManager.handleHostResume() {
    if (!hostBackgrounded) return
    hostBackgrounded = false
    val project = currentProject ?: return
    if (PlayerRecoveryPolicy.shouldRebuildOnHostResume(
            backgrounded = true,
            lastError = _lastError.value,
            playbackState = _playbackState.value
        )
    ) {
        AppLog.warn(TAG, "Preview player is idle after the host returned; rebuilding it")
        loadProjectInternal(project, _currentPositionMs.value, forceRebuild = true)
    }
}
