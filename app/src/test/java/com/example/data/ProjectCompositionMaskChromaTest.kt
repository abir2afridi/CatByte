package com.example.data

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.example.data.export.ClipChromaKeyEffect
import com.example.data.export.ClipMaskEffect
import com.example.model.ChromaKeyData
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mask and the chroma key used to be stored on the clip and then
 * ignored by the renderer (the export even warned about it). These tests
 * pin that both now reach the shared effect list, which is what the
 * CompositionPlayer preview and the Transformer export consume.
 */
@UnstableApi
class ProjectCompositionMaskChromaTest {

    private fun clip(
        id: String,
        mask: MaskData = MaskData(),
        chroma: ChromaKeyData = ChromaKeyData()
    ) = MediaClip(
        id = id,
        title = id,
        uriString = "content://clip/$id",
        durationMs = 2_000L,
        originalDurationMs = 2_000L,
        mask = mask,
        chromaKey = chroma
    )

    private fun project(clips: List<MediaClip>, overlayTracks: List<VideoTrack> = emptyList()) =
        VideoProject(
            id = "project",
            title = "Project",
            clips = clips,
            overlayTracks = overlayTracks,
            fps = 30,
            resolution = "1080P"
        )

    private fun videoEffectsOf(
        project: VideoProject,
        index: Int = 0
    ): List<androidx.media3.common.Effect> {
        val result = ProjectCompositionFactory.buildPreview(project, watermarkEnabled = false)
        val videoSequence = result.composition.sequences.first { sequence ->
            sequence.trackTypes.contains(C.TRACK_TYPE_VIDEO)
        }
        return videoSequence.editedMediaItems[index].effects.videoEffects
    }

    @Test
    fun `a masked clip burns the mask into its pixels`() {
        val source = project(
            listOf(
                clip(
                    "a",
                    mask = MaskData(type = MaskType.CIRCLE, width = 0.5f, height = 0.5f)
                )
            )
        )

        val effects = videoEffectsOf(source)

        assertTrue(
            "the mask effect must be part of the clip's effect list",
            effects.any { it is ClipMaskEffect }
        )
    }

    @Test
    fun `a keyed clip burns the chroma key into its pixels`() {
        val source = project(
            listOf(clip("a", chroma = ChromaKeyData(isEnabled = true, keyColorHex = 0xFF00FF00)))
        )

        val effects = videoEffectsOf(source)

        assertTrue(
            "the chroma key effect must be part of the clip's effect list",
            effects.any { it is ClipChromaKeyEffect }
        )
    }

    @Test
    fun `a plain clip gets neither effect`() {
        val source = project(listOf(clip("a")))

        val effects = videoEffectsOf(source)

        assertFalse(effects.any { it is ClipMaskEffect })
        assertFalse(effects.any { it is ClipChromaKeyEffect })
    }

    @Test
    fun `the mask of an overlay clip is burned in as well`() {
        val overlay = VideoTrack(
            name = "Overlay",
            isMainTrack = false,
            clips = listOf(
                clip(
                    "o",
                    mask = MaskData(type = MaskType.RECTANGLE, width = 0.5f, height = 0.5f)
                )
            )
        )
        val source = project(listOf(clip("a")), overlayTracks = listOf(overlay))

        val result = ProjectCompositionFactory.buildPreview(source, watermarkEnabled = false)
        val videoSequences = result.composition.sequences.filter { sequence ->
            sequence.trackTypes.contains(C.TRACK_TYPE_VIDEO)
        }
        // The main track is the first video sequence, the overlay the second.
        val overlayItem = videoSequences.last().editedMediaItems.last()

        assertTrue(overlayItem.effects.videoEffects.any { it is ClipMaskEffect })
    }

    @Test
    fun `the plan no longer warns about the mask`() {
        val source = project(
            listOf(
                clip(
                    "a",
                    mask = MaskData(type = MaskType.CIRCLE, width = 0.5f, height = 0.5f)
                )
            )
        )

        val plan = ProjectCompositionFactory.plan(source, 1080, 1920, 30)

        assertFalse(plan.warnings.any { it.contains("Mask") })
    }
}
