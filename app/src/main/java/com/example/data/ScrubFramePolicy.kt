package com.example.data

import kotlin.math.abs

/**
 * Decides whether a freshly decoded scrub frame may replace the one on screen.
 *
 * Own file on purpose — this is the tuned rule that keeps the scrub preview
 * truthful while two decoder lanes race (drag lane + settle lane):
 *
 * A decode result can arrive AFTER a newer frame was already published (the
 * other lane decoded a later playhead first). Publishing it as-is would drag
 * the picture backwards mid-drag, which reads as "the scene jumps back" while
 * the user is trying to pick a split frame. A frame is therefore only
 * published when it is at least as close to the CURRENT playhead as the frame
 * already on screen — this stays correct in both drag directions because the
 * distance is measured against the live target, not against the previous
 * frame's position.
 */
object ScrubFramePolicy {

    /** Sentinel for "nothing published in this gesture yet": always publish. */
    const val NOTHING_PUBLISHED = Long.MAX_VALUE

    fun shouldPublish(
        requestedMs: Long,
        targetMs: Long,
        lastPublishedMs: Long
    ): Boolean {
        if (lastPublishedMs == NOTHING_PUBLISHED) return true
        return abs(requestedMs - targetMs) <= abs(lastPublishedMs - targetMs)
    }
}
