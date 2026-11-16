package com.example.data

import com.example.model.MediaClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cover/thumbnail selection is pure data, so it can be checked on the JVM. */
class ProjectThumbnailLoaderTest {

    private fun project(clips: List<MediaClip>, frameMs: Long = 0L, imageUri: String = ""): VideoProject =
        VideoProject(
            id = "p",
            title = "P",
            clips = clips,
            coverFrameMs = frameMs,
            coverImageUri = imageUri
        )

    @Test
    fun `mainClipAt maps a global position into the right clip`() {
        val project = project(
            listOf(
                MediaClip(id = "a", title = "A", durationMs = 2_000L),
                MediaClip(id = "b", title = "B", durationMs = 3_000L)
            )
        )
        val (first, localFirst) = ProjectThumbnailLoader.mainClipAt(project, 500L)!!
        assertEquals("a", first.id)
        assertEquals(500L, localFirst)

        val (second, localSecond) = ProjectThumbnailLoader.mainClipAt(project, 3_500L)!!
        assertEquals("b", second.id)
        assertEquals(1_500L, localSecond)
    }

    @Test
    fun `mainClipAt clamps to the last clip after the end`() {
        val project = project(listOf(MediaClip(id = "a", title = "A", durationMs = 1_000L)))
        val (clip, local) = ProjectThumbnailLoader.mainClipAt(project, 99_000L)!!
        assertEquals("a", clip.id)
        assertEquals(1_000L, local)
    }

    @Test
    fun `mainClipAt returns null for an empty timeline`() {
        assertNull(ProjectThumbnailLoader.mainClipAt(project(emptyList()), 0L))
    }

    @Test
    fun `cache key changes with the cover frame and image`() {
        val base = project(emptyList(), frameMs = 0L)
        assertNotEquals(
            ProjectThumbnailLoader.cacheKey(base),
            ProjectThumbnailLoader.cacheKey(base.copy(coverFrameMs = 1_000L))
        )
        assertNotEquals(
            ProjectThumbnailLoader.cacheKey(base),
            ProjectThumbnailLoader.cacheKey(base.copy(coverImageUri = "content://cover/1"))
        )
    }
}
