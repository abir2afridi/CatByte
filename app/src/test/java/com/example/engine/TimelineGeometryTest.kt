package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineGeometryTest {

    @Test
    fun `default scale maps one second to 65 dp`() {
        assertEquals(0.065f, TimelineGeometry.dpPerMs(1f), 0.0001f)
        assertEquals(65f, TimelineGeometry.contentXDp(1_000L, 1f), 0.01f)
    }

    @Test
    fun `content width includes end padding and minimum duration`() {
        assertEquals(520f, TimelineGeometry.contentWidthDp(4_000L, 1f), 0.01f)
        assertEquals(
            TimelineGeometry.contentWidthDp(TimelineGeometry.MIN_TIMELINE_MS, 1f),
            TimelineGeometry.contentWidthDp(1_000L, 1f),
            0.01f
        )
    }

    @Test
    fun `position and x round trip at every zoom`() {
        listOf(0.5f, 1f, 2f, 4f).forEach { zoom ->
            val x = TimelineGeometry.contentXDp(2_500L, zoom)
            assertEquals(2_500L, TimelineGeometry.msAtContentXDp(x, zoom))
        }
    }

    @Test
    fun `drag delta is applied from a fixed anchor`() {
        // 65 px = exactly one second at the default scale.
        assertEquals(
            2_000L,
            TimelineGeometry.positionMsAfterDrag(
                anchorMs = 1_000L,
                dragPx = 65f,
                density = 1f,
                zoom = 1f
            )
        )
        assertEquals(
            500L,
            TimelineGeometry.positionMsAfterDrag(
                anchorMs = 1_500L,
                dragPx = -65f,
                density = 1f,
                zoom = 1f
            )
        )
    }

    @Test
    fun `screen x subtracts the current scroll offset`() {
        assertEquals(
            0f,
            TimelineGeometry.screenXDp(
                positionMs = 1_000L,
                zoom = 1f,
                scrollPx = 65,
                density = 1f
            ),
            0.01f
        )
    }

    @Test
    fun `density is respected when converting drag pixels to dp`() {
        // 130 physical px at density 2 = 65 dp = one second.
        assertEquals(
            2_000L,
            TimelineGeometry.positionMsAfterDrag(
                anchorMs = 1_000L,
                dragPx = 130f,
                density = 2f,
                zoom = 1f
            )
        )
    }

    @Test
    fun `trim drag maps one-to-one between finger pixels and handle pixels`() {
        // The trim handle converts the finger's drag to ms with the SAME scale
        // the clip width is drawn with, so the handle must track the finger
        // exactly: dragPx -> ms -> clipDp -> px must return the original px.
        listOf(0.5f, 1f, 2f, 3f).forEach { zoom ->
            val dpPerMs = TimelineGeometry.dpPerMs(zoom)
            val density = 2.75f
            val dragPx = 110f
            val deltaMs = TimelineGeometry.dragPxToMs(dragPx, density, dpPerMs)
            val handleDeltaPx = deltaMs * dpPerMs * density
            assertEquals(dragPx, handleDeltaPx, 0.6f)
        }
    }

    @Test
    fun `trim drag conversion does not amplify the finger movement`() {
        // Regression: callers pass dpPerMs (NOT the zoom) into dragPxToMs.
        // Converting it again shrank the scale 1000/65x and made every trim
        // run ~15x faster than the finger.
        val dpPerMs = TimelineGeometry.dpPerMs(1f)
        assertEquals(
            1_000L,
            TimelineGeometry.dragPxToMs(dragPx = dpPerMs * 1_000f, density = 1f, dpPerMs = dpPerMs)
        )
    }
}
