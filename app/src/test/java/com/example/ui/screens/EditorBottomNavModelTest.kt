package com.example.ui.screens

import com.example.model.AspectRatioType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two-level bottom navigation must keep the feature list of the CapCut
 * workspace: 15 main features, each with its own sub-feature row.
 */
class EditorBottomNavModelTest {

    @Test
    fun `main row has the fifteen workspace features in order`() {
        assertEquals(
            listOf(
                "Edit",
                "Audio",
                "Text",
                "Effect",
                "Overlay",
                "Captions",
                "Filter",
                "Adjust",
                "Sticker",
                "Generate media",
                "AI Avatar",
                "Transcript",
                "Aspect ratio",
                "Background",
                "Template"
            ),
            EditorMainTab.entries.map { it.label }
        )
    }

    @Test
    fun `edit row lists every clip tool`() {
        val labels = EditorMainTab.EDIT.subFeatures().map { it.label }
        assertEquals(
            listOf(
                "Split", "Volume", "Animation", "Effect", "Delete", "Speed", "Beats", "Crop",
                "Duplicate", "Replace", "Overlay", "Adjust", "Filters", "Retouch", "Video Quality",
                "Remove BG", "AI remove", "AI expand", "AI remix", "Eye contact", "Relight",
                "Opacity", "Motion blur", "Lip sync", "Transform", "Auto reframe", "Stabilize",
                "Camera tracking", "Extract Audio", "Isolate voice", "Reduce noise",
                "Audio effects", "Enhance voice", "Video translator", "Freeze", "Reverse", "Mask",
                "Unlink", "Transcript"
            ),
            labels
        )
    }

    @Test
    fun `audio row opens the foley studio tabs`() {
        val features = EditorMainTab.AUDIO.subFeatures()
        assertEquals(
            listOf("Mixer", "Effects", "Extract", "Sounds", "Sound FX", "Record", "Text to audio", "AI music", "Brand music", "Copyright"),
            features.map { it.label }
        )
        assertEquals(EditorAudioTabs.MUSIC, features.first { it.id == "sounds" }.audioTab)
        assertEquals(EditorAudioTabs.SFX, features.first { it.id == "sound_fx" }.audioTab)
        assertEquals(EditorAudioTabs.MIC, features.first { it.id == "record" }.audioTab)
    }

    @Test
    fun `effect overlay captions generate media and avatar rows are complete`() {
        assertEquals(
            listOf("Video effects", "Body effects", "Photo effects", "AI effects"),
            EditorMainTab.EFFECT.subFeatures().map { it.label }
        )
        assertEquals(listOf("Add Overlay"), EditorMainTab.OVERLAY.subFeatures().map { it.label })
        assertEquals(
            listOf(
                "Enter captions", "Auto Captions", "Caption template", "AI packaging",
                "Auto lyrics", "AI emoji", "Import captions"
            ),
            EditorMainTab.CAPTIONS.subFeatures().map { it.label }
        )
        assertEquals(
            listOf("AI image", "AI video", "AI dialogue scene", "Generated"),
            EditorMainTab.GENERATE_MEDIA.subFeatures().map { it.label }
        )
        assertEquals(
            listOf(
                "Avatar library", "Video translator", "AI dialogue scene", "Mimic motion",
                "Fashion model video", "Lip sync", "Change expressions", "Photo to avatar",
                "Video to avatar"
            ),
            EditorMainTab.AI_AVATAR.subFeatures().map { it.label }
        )
    }

    @Test
    fun `aspect ratio row starts with original and carries the fixed ratios`() {
        val features = EditorMainTab.ASPECT_RATIO.subFeatures()
        assertEquals("Original", features.first().label)
        assertEquals(null, features.first().ratio)
        assertEquals(
            listOf(
                "Original", "9:16", "16:9", "1:1", "4:3", "3:4", "5:8", "128:27", "2:1",
                "2.35:1", "1.85:1"
            ),
            features.map { it.label }
        )
        assertEquals(AspectRatioType.RATIO_128_27, features.first { it.id == "ratio_128_27" }.ratio)
        assertEquals(AspectRatioType.RATIO_2_35_1, features.first { it.id == "ratio_2_35_1" }.ratio)
    }

    @Test
    fun `background row lists the four backgrounds`() {
        assertEquals(
            listOf("Color", "Image", "Blur", "Brand Background"),
            EditorMainTab.BACKGROUND.subFeatures().map { it.label }
        )
    }

    @Test
    fun `tabs that open a sheet directly have no sub-feature row`() {
        // Text now owns a sub-feature row (Add Text ... AI Packaging); the
        // tabs below are the ones that still open their sheet directly.
        listOf(
            EditorMainTab.FILTER,
            EditorMainTab.ADJUST,
            EditorMainTab.STICKER,
            EditorMainTab.TRANSCRIPT,
            EditorMainTab.TEMPLATE
        ).forEach { tab ->
            assertTrue("${tab.label} should open its sheet directly", tab.subFeatures().isEmpty())
        }
    }

    @Test
    fun `text row lists every text tool`() {
        assertEquals(
            listOf(
                "Add Text", "Auto Caption", "Stickers", "Draw", "Text template",
                "Text to audio", "Auto lyric", "AI writer", "AI Packaging"
            ),
            EditorMainTab.TEXT.subFeatures().map { it.label }
        )
    }

    @Test
    fun `every tab exposes the agreed sub-feature count`() {
        // Guards the whole workspace map: a tab that silently loses or gains a
        // chip fails here instead of shipping an empty row.
        val expected = mapOf(
            EditorMainTab.EDIT to 39,
            EditorMainTab.AUDIO to 10,
            EditorMainTab.TEXT to 9,
            EditorMainTab.EFFECT to 4,
            EditorMainTab.OVERLAY to 1,
            EditorMainTab.CAPTIONS to 7,
            EditorMainTab.GENERATE_MEDIA to 4,
            EditorMainTab.AI_AVATAR to 9,
            EditorMainTab.ASPECT_RATIO to 11,
            EditorMainTab.BACKGROUND to 4
        )
        expected.forEach { (tab, count) ->
            assertEquals("${tab.label} sub-feature count", count, tab.subFeatures().size)
        }
    }

    @Test
    fun `tabs that open their sheet directly have no sub-feature row`() {
        listOf(
            EditorMainTab.FILTER,
            EditorMainTab.ADJUST,
            EditorMainTab.STICKER,
            EditorMainTab.TRANSCRIPT,
            EditorMainTab.TEMPLATE
        ).forEach { tab ->
            assertTrue("${tab.label} must open its sheet directly", tab.subFeatures().isEmpty())
        }
    }
}
