package com.example.viewmodel

import com.example.model.VideoProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The preview player's rebuild policy, as its own file.
 *
 * Own file on purpose — these are the tuned rules that keep the preview both
 * live and smooth; unrelated edits to `EditorViewModel` must not dilute them:
 *
 *  1. Edits rebuild the preview after a short [DEBOUNCE_MS] debounce, so
 *     sliders stay smooth without preview/export drift.
 *  2. A pending refresh is NEVER restarted. Restarting it per drag frame meant
 *     the 120ms window kept sliding and the preview never updated during a
 *     drag. A pending job picks up the latest project when it fires.
 *  3. While an inline trim session is live, rebuilds are SUPPRESSED entirely:
 *     rebuilding the whole Media3 composition mid-drag is far too heavy and
 *     made the trim handle stutter ("atkee atkee"). The trim session calls
 *     [endSuppression] at finger lift, which fires exactly one rebuild.
 *
 * Pure coroutine code (no Android types), so the policy runs in JVM tests.
 */
internal class PreviewRefreshScheduler(
    private val scope: CoroutineScope,
    /** Reads the latest project when a pending refresh actually fires. */
    private val projectProvider: () -> VideoProject?,
    private val refresh: (VideoProject) -> Unit
) {
    private var job: Job? = null
    private var suppressed = false

    fun schedule() {
        if (suppressed) return
        if (job?.isActive == true) return
        job = scope.launch {
            delay(DEBOUNCE_MS)
            projectProvider()?.let(refresh)
        }
    }

    /** Begin a live trim: cancel pending work and refuse rebuilds until the drag ends. */
    fun beginSuppression() {
        suppressed = true
        job?.cancel()
        job = null
    }

    /** Drag ended: fire exactly one immediate rebuild with the final state. */
    fun endSuppression() {
        suppressed = false
        refreshNow()
    }

    fun refreshNow() {
        job?.cancel()
        job = null
        projectProvider()?.let(refresh)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEBOUNCE_MS = 120L
    }
}
