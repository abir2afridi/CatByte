package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testHomeLaunchAndSettingsNavigation() {
        // Verify settings button exists and is clickable
        composeTestRule.onNodeWithTag("home_settings_button").assertExists()
            .performClick()
    }

    @Test
    fun testNewProjectHeroButtonExists() {
        // Verify hero new project button exists
        composeTestRule.onNodeWithTag("hero_new_project_button").assertExists()
    }
}
