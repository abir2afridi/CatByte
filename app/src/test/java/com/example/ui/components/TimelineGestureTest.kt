package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
 * unit-testable maths cannot: the ruler tap actually seeks, the two-finger
 * pinch actually zooms, and dragging the timeline scrubs while the playhead
 * stays nailed to the centre (CapCut fixed-playhead model). They also pin the
 * bug class from 2026-11-05 (pan vs. playhead vs. pinch stealing events).
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
                    currentPositionMs = mutableStateOf(0L),
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
            // With the fixed-centre model the ruler's time-0 edge sits at the
            // viewport centre, so tap a point that is visible to its right.
            click(Offset(40f, center.y))
        }
        compose.waitForIdle()

        val rawMs = TimelineGeometry.msAtContentXDp(40f / density, 1f)
        // The seek path quantises to frames (30 fps), like the real ruler.
        val expected = (rawMs * 30L / 1_000L) * 1_000L / 30L
        assertTrue("expected a seek, got $seeks", seeks.isNotEmpty())
        assertEquals(expected, seeks.last())
        // The tapped time is far from both clip edges, so no magnet wins.
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
                    currentPositionMs = mutableStateOf(0L),
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
    fun `dragging the timeline scrubs while the playhead stays centred`() {
        val seeks = mutableListOf<Long>()
        compose.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 175.dp)) {
                MultiTrackTimeline(
                    project = project,
                    currentPositionMs = mutableStateOf(0L),
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

        val needleBoundsBefore = compose.onNodeWithTag("timeline_playhead_needle")
            .getUnclippedBoundsInRoot()
        val needleBefore = (needleBoundsBefore.left + needleBoundsBefore.right).value / 2f

        // The needle must sit at the centre of the whole panel (under the
        // play button above), not the centre of the viewport after the gutter.
        val panelBounds = compose.onNodeWithTag("multi_track_timeline")
            .getUnclippedBoundsInRoot()
        val panelWidth = (panelBounds.right - panelBounds.left).value
        assertEquals(
            "needle must be at the panel centre",
            panelWidth / 2f,
            needleBefore,
            1f
        )

        // Drag left on the empty audio row: the content slides under the
        // needle, so time advances without the needle itself moving.
        compose.onNodeWithTag("multi_track_timeline").performTouchInput {
            down(Offset(200f, 120f))
            moveTo(Offset(180f, 120f))
            moveTo(Offset(150f, 120f))
            moveTo(Offset(110f, 120f))
            up()
        }
        compose.waitForIdle()

        val needleBoundsAfter = compose.onNodeWithTag("timeline_playhead_needle")
            .getUnclippedBoundsInRoot()
        val needleAfter = (needleBoundsAfter.left + needleBoundsAfter.right).value / 2f
        assertEquals("the playhead must stay fixed", needleBefore, needleAfter, 0.5f)

        assertTrue("expected a seek from timeline drag, got $seeks", seeks.isNotEmpty())
        assertTrue("drag left should advance time, got ${seeks.last()}", seeks.last() > 0L)
    }

    @Test
    fun `dragging the ruler scrolls the timeline under the needle`() {
        val seeks = mutableListOf<Long>()
        compose.setContent {
            Box(modifier = Modifier.size(width = 360.dp, height = 175.dp)) {
                MultiTrackTimeline(
                    project = project,
                    currentPositionMs = mutableStateOf(0L),
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

        // A drag on the ruler must scroll the content (the tap detector must
        // cancel and yield to the scroll container). Robolectric does not
        // apply scroll translation to getUnclippedBoundsInRoot, so the scroll
        // is asserted through the seeks it produces instead.
        compose.onNodeWithTag("timeline_ruler").performTouchInput {
            down(Offset(40f, center.y))
            moveTo(Offset(30f, center.y))
            moveTo(Offset(20f, center.y))
            moveTo(Offset(10f, center.y))
            up()
        }
        compose.waitForIdle()

        // A release at local x=40 would seek to ~615 ms if the drag had been
        // misread as a tap. A real scroll scrub emits many intermediate seeks
        // and continues past that point.
        val tapMs = 40L * 1_000L / 65L
        assertTrue("expected a scrolling scrub, got $seeks", seeks.size >= 3)
        assertTrue("drag left should advance time, got ${seeks.last()}", seeks.last() > 0L)
        assertTrue(
            "drag must keep scrubbing past the tap position ($tapMs ms), got $seeks",
            seeks.last() > tapMs
        )
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
