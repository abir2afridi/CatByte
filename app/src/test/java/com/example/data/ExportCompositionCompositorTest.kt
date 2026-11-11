package com.example.data

import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.UnstableApi
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Media3 only builds a VideoCompositor when MORE THAN ONE sequence carries a
 * video track. With a single video track it uses `SingleInputVideoGraph`,
 * which rejects any custom `VideoCompositorSettings`:
 *
 *   "SingleInputVideoGraph does not use VideoCompositor, and therefore cannot
 *    apply VideoCompositorSettings"
 *
 * That killed every export of a single-clip project before a single frame was
 * rendered. The custom settings only describe the overlay (PIP) inputs, so
 * they are only attached when overlays actually exist.
 */
@UnstableApi
class ExportCompositionCompositorTest {

    private fun clip(id: String, durationMs: Long = 1_000L, startMs: Long = 0L) = MediaClip(
        id = id,
        title = id,
        uriString = "content://clip/$id",
        durationMs = durationMs,
        originalDurationMs = durationMs,
        startTimelineMs = startMs
    )

    private fun project(
        clips: List<MediaClip>,
        overlayTracks: List<VideoTrack> = emptyList()
    ) = VideoProject(
        id = "project",
        title = "Project",
        clips = clips,
        overlayTracks = overlayTracks,
        fps = 30,
        resolution = "1080P"
    )

    @Test
    fun `single clip export uses the default compositor settings`() {
        val result = ProjectCompositionFactory.buildExport(
            project(listOf(clip("a", durationMs = 15_000L))),
            resolution = "1080P",
            fps = 30,
            watermarkEnabled = false
        )

        assertEquals(
            VideoCompositorSettings.DEFAULT,
            result.composition.videoCompositorSettings
        )
    }

    @Test
    fun `overlay export keeps the custom compositor settings`() {
        val overlay = VideoTrack(
            name = "Overlay",
            isMainTrack = false,
            clips = listOf(clip("o", startMs = 200L))
        )
        val result = ProjectCompositionFactory.buildExport(
            project(listOf(clip("a", durationMs = 15_000L)), overlayTracks = listOf(overlay)),
            resolution = "1080P",
            fps = 30,
            watermarkEnabled = false
        )

        assertNotEquals(
            VideoCompositorSettings.DEFAULT,
            result.composition.videoCompositorSettings
        )
        assertEquals(1, result.overlayInputs.size)
    }
}
