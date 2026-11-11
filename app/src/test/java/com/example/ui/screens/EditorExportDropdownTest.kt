package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.model.VideoProject
import com.example.viewmodel.EditorViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The AI UHD chip must open the export settings panel.
 *
 * The panel used to live inside the "a bottom tool sheet is open" branch of
 * the editor screen, so tapping "AI UHD" only flipped the chip into its open
 * state (video | GIF tabs) while the settings themselves were never composed.
 * Robolectric renders the real screen, so this test fails on that regression.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorExportDropdownTest {

    @get:Rule
    val compose = createComposeRule()

    private fun renderEditor() {
        val viewModel = EditorViewModel()
        viewModel.openProject(VideoProject(id = "p1", title = "Project"))
        compose.setContent {
            EditorScreen(
                viewModel = viewModel,
                onBackToHome = {},
                onOpenSettings = {}
            )
        }
    }

    @Test
    fun tappingAiUhdChip_opensTheExportSettingsPanel() {
        renderEditor()

        compose.onNodeWithTag("export_dropdown_panel").assertDoesNotExist()
        compose.onNodeWithTag("editor_ai_uhd_chip").performClick()
        compose.onNodeWithTag("export_dropdown_panel").assertIsDisplayed()
    }

    @Test
    fun tappingAiUhdChipTwice_closesTheExportSettingsPanel() {
        renderEditor()

        compose.onNodeWithTag("editor_ai_uhd_chip").performClick()
        compose.onNodeWithTag("export_dropdown_panel").assertIsDisplayed()
        compose.onNodeWithTag("editor_ai_uhd_chip").performClick()
        compose.onNodeWithTag("export_dropdown_panel").assertDoesNotExist()
    }
}
