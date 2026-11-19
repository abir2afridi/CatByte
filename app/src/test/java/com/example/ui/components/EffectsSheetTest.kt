package com.example.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.model.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The effect panel's "None" card used to call `onAddEffect(null)`, which
 * every caller ignored - a button that looked alive and removed nothing.
 * It now removes the effect under the playhead and is disabled when there
 * is none.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EffectsSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the none card removes the effect`() {
        val removed = mutableListOf<Unit>()
        compose.setContent {
            EffectsSheet(
                canRemoveEffect = true,
                onAddEffect = {},
                onRemoveEffect = { removed += Unit },
                onClose = {}
            )
        }

        compose.onNodeWithTag("effect_none").performClick()
        compose.waitForIdle()

        assertEquals(1, removed.size)
    }

    @Test
    fun `the none card is disabled when there is nothing to remove`() {
        val removed = mutableListOf<Unit>()
        compose.setContent {
            EffectsSheet(
                canRemoveEffect = false,
                onAddEffect = {},
                onRemoveEffect = { removed += Unit },
                onClose = {}
            )
        }

        compose.onNodeWithTag("effect_none").assertIsNotEnabled()
        compose.onNodeWithTag("effect_none").performClick()
        compose.waitForIdle()

        assertTrue("a disabled card must not remove anything", removed.isEmpty())
    }

    @Test
    fun `picking a preset adds that effect`() {
        val added = mutableListOf<EffectType>()
        compose.setContent {
            EffectsSheet(
                canRemoveEffect = false,
                onAddEffect = { added += it },
                onRemoveEffect = {},
                onClose = {}
            )
        }

        val firstPreset = EffectType.entries.first { it.category == VideoEffectTabs.first().second }
        compose.onNodeWithTag("effect_${firstPreset.name.lowercase()}", useUnmergedTree = true)
            .performClick()
        compose.waitForIdle()

        assertEquals(listOf(firstPreset), added)
    }
}
