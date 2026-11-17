package com.example.viewmodel

import com.example.data.ProjectRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Background → Color / Image / Blur / Brand": the canvas background mode is
 * validated, applied, persisted in the project JSON and reusable as a brand
 * image.
 */
class BackgroundModeTest {

    private fun viewModel(): EditorViewModel {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val project = ProjectRepository().createNewProject("Background")
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `blur mode is applied`() {
        val viewModel = viewModel()

        viewModel.setBackgroundMode("blur")

        assertEquals("blur", viewModel.currentProject.value!!.backgroundMode)
    }

    @Test
    fun `switching back to color clears the blur`() {
        val viewModel = viewModel()
        viewModel.setBackgroundMode("blur")

        viewModel.setBackgroundMode("color")

        assertEquals("color", viewModel.currentProject.value!!.backgroundMode)
    }

    @Test
    fun `unknown modes fall back to color`() {
        val viewModel = viewModel()

        viewModel.setBackgroundMode("neon")

        assertEquals("color", viewModel.currentProject.value!!.backgroundMode)
    }

    @Test
    fun `saving a brand background applies it as an image background`() {
        val viewModel = viewModel()

        viewModel.saveBrandBackground("/data/brand.jpg")

        val project = viewModel.currentProject.value!!
        assertEquals("/data/brand.jpg", project.backgroundImageUri)
        assertEquals("image", project.backgroundMode)
        assertEquals("/data/brand.jpg", viewModel.brandBackgroundPath.value)
    }

    @Test
    fun `clearing the brand keeps the current background`() {
        val viewModel = viewModel()
        viewModel.saveBrandBackground("/data/brand.jpg")

        viewModel.saveBrandBackground("")

        // The brand slot is empty again, but the project keeps its background.
        assertEquals("", viewModel.brandBackgroundPath.value)
        assertTrue(viewModel.currentProject.value!!.backgroundImageUri.isNotBlank())
    }

    @Test
    fun `setting an image background does not change the mode by itself`() {
        val viewModel = viewModel()

        // Raw setter (used by the AI image path before it switches the mode).
        viewModel.setBackgroundImage("/data/bg.jpg")

        assertEquals("color", viewModel.currentProject.value!!.backgroundMode)
    }
}
