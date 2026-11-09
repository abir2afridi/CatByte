package com.example.viewmodel

import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the preview rebuild policy: debounced, never restarted mid-window,
 * fully suppressed during a live trim, one immediate rebuild at drag end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreviewRefreshSchedulerTest {

    private fun project(id: String) = VideoProject(
        id = id,
        title = id,
        clips = listOf(MediaClip(id = "clip_$id", title = id, durationMs = 1_000L))
    )

    @Test
    fun `pending refresh is not restarted and fires once with the latest project`() = runTest {
        var current: VideoProject? = project("a")
        val loaded = mutableListOf<VideoProject>()
        val scheduler = PreviewRefreshScheduler(this, { current }, { loaded += it })

        val c = project("c")
        scheduler.schedule()
        current = project("b")
        scheduler.schedule() // must NOT restart the pending job
        current = c
        advanceTimeBy(PreviewRefreshScheduler.DEBOUNCE_MS + 50)

        assertEquals(listOf(c), loaded)
    }

    @Test
    fun `live trim suppresses rebuilds until the drag ends`() = runTest {
        var current: VideoProject? = project("a")
        val loaded = mutableListOf<VideoProject>()
        val scheduler = PreviewRefreshScheduler(this, { current }, { loaded += it })

        val final = project("trimmed")
        scheduler.schedule()
        scheduler.beginSuppression()
        advanceTimeBy(500)
        assertTrue("no rebuild may land mid-drag", loaded.isEmpty())

        current = final
        scheduler.endSuppression()
        assertEquals(listOf(final), loaded)

        // The suppression is over: normal debounced refreshes resume.
        current = project("after")
        scheduler.schedule()
        advanceTimeBy(PreviewRefreshScheduler.DEBOUNCE_MS + 50)
        assertEquals(2, loaded.size)
    }

    @Test
    fun `refreshNow cancels pending work and rebuilds immediately`() = runTest {
        var current: VideoProject? = project("a")
        val loaded = mutableListOf<VideoProject>()
        val scheduler = PreviewRefreshScheduler(this, { current }, { loaded += it })

        val immediate = project("now")
        scheduler.schedule()
        current = immediate
        scheduler.refreshNow()

        assertEquals(listOf(immediate), loaded)
        advanceTimeBy(500)
        assertEquals(1, loaded.size)
    }
}
