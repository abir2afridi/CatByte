package com.example.data

import androidx.media3.transformer.EditedMediaItem
import com.example.data.export.ClipGradeShaderEffect
import com.example.model.MediaClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-pixel grade effect must reach the shared effect list exactly like
 * the mask/chroma effects do, so the CompositionPlayer preview and the
 * Transformer export both consume it.
 */
class ProjectCompositionGradeShaderTest {

    private fun projectWith(vararg clips: MediaClip): VideoProject =
        VideoProject(id = "p1", title = "Grade test", clips = clips.toList())

    @Test
    fun `graded clip carries the shader effect in the composition`() {
        val project = projectWith(
            MediaClip(id = "a", title = "Graded", uriString = "content://x/1", sharpen = 0.5f)
        )
        val result = ProjectCompositionFactory.buildPreview(project, watermarkEnabled = false)
        val item = result.composition.sequences.first().editedMediaItems.first() as EditedMediaItem
        assertTrue(
            "expected ClipGradeShaderEffect in the effect list",
            item.effects.videoEffects.any { it is ClipGradeShaderEffect }
        )
    }

    @Test
    fun `retouched clip carries the shader effect`() {
        val project = projectWith(
            MediaClip(id = "a", title = "Retouched", uriString = "content://x/1", retouch = true)
        )
        val result = ProjectCompositionFactory.buildPreview(project, watermarkEnabled = false)
        val item = result.composition.sequences.first().editedMediaItems.first() as EditedMediaItem
        assertTrue(
            item.effects.videoEffects.any { it is ClipGradeShaderEffect }
        )
    }

    @Test
    fun `plain clip pays nothing for the shader pass`() {
        val project = projectWith(
            MediaClip(id = "a", title = "Plain", uriString = "content://x/1")
        )
        val result = ProjectCompositionFactory.buildPreview(project, watermarkEnabled = false)
        val item = result.composition.sequences.first().editedMediaItems.first() as EditedMediaItem
        assertEquals(
            0,
            item.effects.videoEffects.count { it is ClipGradeShaderEffect }
        )
    }
}
