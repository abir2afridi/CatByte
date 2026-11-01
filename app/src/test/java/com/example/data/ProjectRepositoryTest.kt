package com.example.data

import com.example.model.AspectRatioType
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Repository behaviour is intentionally seed-free since DEC-008: the editor
 * no longer auto-creates demo projects. Every test therefore builds its own
 * data through the public API.
 */
class ProjectRepositoryTest {

    @Test
    fun `fresh repository starts empty`() {
        val repo = ProjectRepository()
        assertTrue(repo.projects.value.isEmpty())
    }

    @Test
    fun `createNewProject stores and returns the project`() {
        val repo = ProjectRepository()
        val project = repo.createNewProject(
            title = "Test Project",
            aspectRatio = AspectRatioType.RATIO_16_9,
            paperStyle = "Standard"
        )
        assertNotNull(repo.projects.value.find { it.id == project.id })
        assertEquals("Test Project", project.title)
        assertEquals(AspectRatioType.RATIO_16_9, project.aspectRatio)
        assertEquals("Standard", project.paperStyle)
    }

    @Test
    fun `toggleFavorite flips the flag`() {
        val repo = ProjectRepository()
        val project = repo.createNewProject("Favorites")
        val initial = project.isFavorite
        repo.toggleFavorite(project.id)
        val updated = repo.projects.value.find { it.id == project.id }
        assertEquals(!initial, updated?.isFavorite)
        repo.toggleFavorite(project.id)
        val restored = repo.projects.value.find { it.id == project.id }
        assertEquals(initial, restored?.isFavorite)
    }

    @Test
    fun `duplicateProject copies fields with a new id`() {
        val repo = ProjectRepository()
        val original = repo.createNewProject("To Copy")
        repo.duplicateProject(original.id)
        val copies = repo.projects.value.filter { it.title == "To Copy (Copy)" }
        assertEquals(1, copies.size)
        val copy = copies.first()
        assertFalse(copy.id == original.id)
        assertEquals(original.aspectRatio, copy.aspectRatio)
    }

    @Test
    fun `deleteProject removes it from the list`() {
        val repo = ProjectRepository()
        val project = repo.createNewProject("Doomed")
        repo.deleteProject(project.id)
        assertTrue(repo.projects.value.none { it.id == project.id })
    }

    @Test
    fun `saveProject updates an existing project by id`() {
        val repo = ProjectRepository()
        val project = repo.createNewProject("Original Title")
        repo.saveProject(project.copy(title = "Renamed"))
        val updated = repo.projects.value.find { it.id == project.id }
        assertEquals("Renamed", updated?.title)
        assertEquals(1, repo.projects.value.size)
    }

    @Test
    fun `deleteAllProjects clears everything`() {
        val repo = ProjectRepository()
        repo.createNewProject("A")
        repo.createNewProject("B")
        repo.deleteAllProjects()
        assertEquals(0, repo.projects.value.size)
    }

    @Test
    fun `totalDuration counts clip spans`() {
        val repo = ProjectRepository()
        val clip = com.example.model.MediaClip(id = "c1", title = "Clip", durationMs = 2500L)
        val project: VideoProject = repo.createNewProject("Timeline", initialClips = listOf(clip))
        assertEquals(2500L, project.totalDurationMs)
    }
}
