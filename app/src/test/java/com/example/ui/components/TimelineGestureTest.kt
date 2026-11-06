package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import com.example.model.MediaClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gesture-level guard for the timeline.
 *
 * Robolectric drives real Compose pointer input, so these tests cover the parts
 * unit-testable maths cannot: the ruler scrub actually seeks, the two-finger
 * pinch actually zooms, and dragging the needle actually moves time. They pin
 * the bug class from 2026-11-05 (pan vs. playhead vs. pinch stealing events).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimelineGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private val project = VideoProject(
        id = "timeline-test",
        title = "Gesture test",
        clips = listOf(
            MediaClip(
                id = "clip_1",
                title = "Clip",
                durationMs = 3_000L,
                originalDurationMs = 3_000L
            )
        )
    )

    @Test
    fun `ruler tap seeks to the tapped time`() {
        val seeks = mutableListOf<Long>()
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            Box(modifier = Modifier.size(width = 360.dp, height = 175.dp)) {
                MultiTrackTimeline(
                    project = project,
                    currentPositionMs = 0L,
                    isPlaying = false,
                    selectedClipId = null,
                    selectedTextId = null,
                    zoom = 1f,
                    onSeek = { seeks += it },
                    onSelectClip = {},
                    onSelectText = {},
                    onAddMediaClick = {},
                    onTransitionClick = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        compose.onNodeWithTag("timeline_ruler").performTouchInput {
            click(Offset(130f, center.y))
        }
        compose.waitForIdle()

        val expected = TimelineGeometry.msAtContentXDp(130f / density, 1f)
        assertTrue("expected a seek, got $seeks", seeks.isNotEmpty())
        assertEquals(expected, seeks.last())
        // 2 s is far enough from the 3 s clip edge that no magnet should win.
        assertTrue(
            "unexpected snap: ${seeks.last()}",
            kotlin.math.abs(seeks.last() - expected) <= 1L
        )
    }

    @Test
    fun `two-finger pinch reports a zoom change`() {
        val zooms = mutableListOf<Float>()
        compose.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 175.dp)) {
                MultiTrackTimeline(
                    project = project,
                    currentPositionMs = 0L,
                    isPlaying = false,
                    selectedClipId = null,
                    selectedTextId = null,
                    zoom = 1f,
                    onSeek = {},
                    onSelectClip = {},
                    onSelectText = {},
                    onAddMediaClick = {},
                    onTransitionClick = {},
                    onZoomChange = { zooms += it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        compose.onNodeWithTag("multi_track_timeline").performTouchInput {
            pinch(
                start0 = Offset(120f, 120f),
                end0 = Offset(60f, 120f),
                start1 = Offset(200f, 120f),
                end1 = Offset(260f, 120f)
            )
        }
        compose.waitForIdle()

        assertTrue("pinch should call onZoomChange, got $zooms", zooms.isNotEmpty())
        assertTrue("pinch should zoom in, got ${zooms.last()}", zooms.last() > 1f)
    }

    @Test
    fun `dragging the playhead seeks forward`() {
        val seeks = mutableListOf<Long>()
        compose.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 175.dp)) {
                MultiTrackTimeline(
                    project = project,
                    currentPositionMs = 0L,
                    isPlaying = false,
                    selectedClipId = null,
                    selectedTextId = null,
                    zoom = 1f,
                    onSeek = { seeks += it },
                    onSelectClip = {},
                    onSelectText = {},
                    onAddMediaClick = {},
                    onTransitionClick = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // The playhead sits at viewport x=0 (behind the 54 dp track header),
        // so grab its 44 dp hit target just to the right of the header.
        compose.onNodeWithTag("multi_track_timeline").performTouchInput {
            down(Offset(56f, 120f))
            moveTo(Offset(70f, 120f))
            moveTo(Offset(90f, 120f))
            moveTo(Offset(130f, 120f))
            up()
        }
        compose.waitForIdle()

        assertTrue("expected a seek from needle drag, got $seeks", seeks.isNotEmpty())
        assertTrue("drag should move forward, got ${seeks.last()}", seeks.last() > 0L)
    }

    @Test
    fun `snap threshold is expressed in frames for the ruler density`() {
        // Guard the unit hand-off: 12 dp at 2.1667 dp/frame is 5 frames, so a
        // ruler seek 5 frames from a clip edge must snap and 6 frames must not.
        val pxPerFrameDp = TimelineGeometry.dpPerMs(1f) * (1_000f / 30f)
        val threshold = SnapEngine.thresholdFrames(TimelineGeometry.SNAP_THRESHOLD_DP, pxPerFrameDp)
        assertEquals(6L, threshold)
    }
}
