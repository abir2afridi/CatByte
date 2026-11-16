package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bottom navigation of the editor has two levels: the main row with the
 * 15 workspace features and the sub-feature row of the selected feature.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorBottomNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private fun renderBar() {
        compose.setContent {
            var activeTab by remember { mutableStateOf<EditorMainTab?>(null) }
            CapCutBottomActionBar(
                activeTab = activeTab,
                onMainTabSelected = { tab -> activeTab = tab },
                onSubFeatureSelected = { _, _ -> },
                onBackToMain = { activeTab = null }
            )
        }
    }

    @Test
    fun mainRow_showsTheFifteenFeatures() {
        renderBar()

        listOf(
            "edit", "audio", "text", "effect", "overlay", "captions", "filter", "adjust",
            "sticker", "generate_media", "ai_avatar", "transcript", "aspect_ratio",
            "background", "template"
        ).forEach { tag ->
            compose.onNodeWithTag("bottom_action_$tag").assertExists()
        }
    }

    @Test
    fun tappingEdit_showsTheSubFeatureRowWithABackAction() {
        renderBar()

        compose.onNodeWithTag("bottom_sub_row_edit").assertDoesNotExist()
        compose.onNodeWithTag("bottom_action_edit").performClick()

        compose.onNodeWithTag("bottom_sub_row_edit").assertIsDisplayed()
        compose.onNodeWithTag("bottom_sub_edit_split").assertExists()
        compose.onNodeWithTag("bottom_sub_edit_transcript").assertExists()
    }

    @Test
    fun tappingTheBackAction_returnsToTheMainRow() {
        renderBar()

        compose.onNodeWithTag("bottom_action_audio").performClick()
        compose.onNodeWithTag("bottom_sub_row_audio").assertIsDisplayed()

        compose.onNodeWithTag("bottom_nav_back").performClick()
        compose.onNodeWithTag("bottom_sub_row_audio").assertDoesNotExist()
        compose.onNodeWithTag("bottom_action_audio").assertExists()
    }
}
