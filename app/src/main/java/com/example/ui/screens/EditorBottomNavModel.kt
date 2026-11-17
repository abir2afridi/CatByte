package com.example.ui.screens

import com.example.model.AspectRatioType

/**
 * Main entries of the editor bottom navigation.
 *
 * The bar is shown while no clip is selected. Tapping a tab either opens the
 * matching tool sheet straight away (Filter, Adjust, Sticker, Transcript,
 * Template) or swaps the bar for the tab's sub-feature row (Edit, Audio,
 * Effect, Overlay, Captions, Generate media, AI Avatar, Aspect ratio and
 * Background).
 */
internal enum class EditorMainTab(val label: String, val tag: String) {
    EDIT("Edit", "edit"),
    AUDIO("Audio", "audio"),
    TEXT("Text", "text"),
    EFFECT("Effect", "effect"),
    OVERLAY("Overlay", "overlay"),
    CAPTIONS("Captions", "captions"),
    FILTER("Filter", "filter"),
    ADJUST("Adjust", "adjust"),
    STICKER("Sticker", "sticker"),
    GENERATE_MEDIA("Generate media", "generate_media"),
    AI_AVATAR("AI Avatar", "ai_avatar"),
    TRANSCRIPT("Transcript", "transcript"),
    ASPECT_RATIO("Aspect ratio", "aspect_ratio"),
    BACKGROUND("Background", "background"),
    TEMPLATE("Template", "template")
}

/**
 * One chip of the sub-feature row.
 *
 * @param id stable identifier used for the test tag and for the action table.
 * @param label text shown in the chip.
 * @param ratio canvas ratio applied by the chip (`null` for the "Original"
 *   chip and for every feature that is not a ratio).
 * @param audioTab Audio & Foley studio tab opened by the chip (`null` when
 *   the chip does not open the audio sheet).
 */
internal data class EditorSubFeature(
    val id: String,
    val label: String,
    val ratio: AspectRatioType? = null,
    val audioTab: Int? = null
)

/** Tabs of the Audio & Foley studio, in the order the sheet shows them. */
internal object EditorAudioTabs {
    const val FOLEY = 0
    const val MUSIC = 1
    const val SFX = 2
    const val MIC = 3
}

/**
 * The sub-features of every main tab, in the order they are shown in the
 * bottom navigation. Tabs without a sub-feature row return an empty list and
 * open their tool sheet directly.
 */
internal fun EditorMainTab.subFeatures(): List<EditorSubFeature> = when (this) {
    EditorMainTab.EDIT -> listOf(
        EditorSubFeature("split", "Split"),
        EditorSubFeature("volume", "Volume"),
        EditorSubFeature("animation", "Animation"),
        EditorSubFeature("effect", "Effect"),
        EditorSubFeature("delete", "Delete"),
        EditorSubFeature("speed", "Speed"),
        EditorSubFeature("beats", "Beats"),
        EditorSubFeature("crop", "Crop"),
        EditorSubFeature("duplicate", "Duplicate"),
        EditorSubFeature("replace", "Replace"),
        EditorSubFeature("overlay", "Overlay"),
        EditorSubFeature("adjust", "Adjust"),
        EditorSubFeature("filters", "Filters"),
        EditorSubFeature("retouch", "Retouch"),
        EditorSubFeature("video_quality", "Video Quality"),
        EditorSubFeature("remove_bg", "Remove BG"),
        EditorSubFeature("ai_remove", "AI remove"),
        EditorSubFeature("ai_expand", "AI expand"),
        EditorSubFeature("ai_remix", "AI remix"),
        EditorSubFeature("eye_contact", "Eye contact"),
        EditorSubFeature("relight", "Relight"),
        EditorSubFeature("opacity", "Opacity"),
        EditorSubFeature("motion_blur", "Motion blur"),
        EditorSubFeature("lip_sync", "Lip sync"),
        EditorSubFeature("transform", "Transform"),
        EditorSubFeature("auto_reframe", "Auto reframe"),
        EditorSubFeature("stabilize", "Stabilize"),
        EditorSubFeature("camera_tracking", "Camera tracking"),
        EditorSubFeature("extract_audio", "Extract Audio"),
        EditorSubFeature("isolate_voice", "Isolate voice"),
        EditorSubFeature("reduce_noise", "Reduce noise"),
        EditorSubFeature("audio_effects", "Audio effects"),
        EditorSubFeature("enhance_voice", "Enhance voice"),
        EditorSubFeature("video_translator", "Video translator"),
        EditorSubFeature("freeze", "Freeze"),
        EditorSubFeature("reverse", "Reverse"),
        EditorSubFeature("mask", "Mask"),
        EditorSubFeature("unlink", "Unlink"),
        EditorSubFeature("transcript", "Transcript")
    )

    EditorMainTab.TEXT -> listOf(
        EditorSubFeature("add_text", "Add Text"),
        EditorSubFeature("auto_caption", "Auto Caption"),
        EditorSubFeature("stickers", "Stickers"),
        EditorSubFeature("draw", "Draw"),
        EditorSubFeature("text_template", "Text template"),
        EditorSubFeature("text_to_audio", "Text to audio"),
        EditorSubFeature("auto_lyric", "Auto lyric"),
        EditorSubFeature("ai_writer", "AI writer"),
        EditorSubFeature("ai_packaging", "AI Packaging")
    )

    EditorMainTab.AUDIO -> listOf(
        EditorSubFeature("mixer", "Mixer"),
        EditorSubFeature("extract", "Extract"),
        EditorSubFeature("sounds", "Sounds", audioTab = EditorAudioTabs.MUSIC),
        EditorSubFeature("sound_fx", "Sound FX", audioTab = EditorAudioTabs.SFX),
        EditorSubFeature("record", "Record", audioTab = EditorAudioTabs.MIC),
        EditorSubFeature("text_to_audio", "Text to audio"),
        EditorSubFeature("ai_music", "AI music"),
        EditorSubFeature("brand_music", "Brand music"),
        EditorSubFeature("copyright", "Copyright")
    )

    EditorMainTab.EFFECT -> listOf(
        EditorSubFeature("video_effects", "Video effects"),
        EditorSubFeature("body_effects", "Body effects"),
        EditorSubFeature("photo_effects", "Photo effects"),
        EditorSubFeature("ai_effects", "AI effects")
    )

    EditorMainTab.OVERLAY -> listOf(
        EditorSubFeature("add_overlay", "Add Overlay")
    )

    EditorMainTab.CAPTIONS -> listOf(
        EditorSubFeature("enter_captions", "Enter captions"),
        EditorSubFeature("auto_captions", "Auto Captions"),
        EditorSubFeature("caption_template", "Caption template"),
        EditorSubFeature("ai_packaging", "AI packaging"),
        EditorSubFeature("auto_lyrics", "Auto lyrics"),
        EditorSubFeature("ai_emoji", "AI emoji"),
        EditorSubFeature("import_captions", "Import captions")
    )

    EditorMainTab.GENERATE_MEDIA -> listOf(
        EditorSubFeature("ai_image", "AI image"),
        EditorSubFeature("ai_video", "AI video"),
        EditorSubFeature("ai_dialogue_scene", "AI dialogue scene"),
        EditorSubFeature("generated", "Generated")
    )

    EditorMainTab.AI_AVATAR -> listOf(
        EditorSubFeature("avatar_library", "Avatar library"),
        EditorSubFeature("video_translator", "Video translator"),
        EditorSubFeature("ai_dialogue_scene", "AI dialogue scene"),
        EditorSubFeature("mimic_motion", "Mimic motion"),
        EditorSubFeature("fashion_model_video", "Fashion model video"),
        EditorSubFeature("lip_sync", "Lip sync"),
        EditorSubFeature("change_expressions", "Change expressions"),
        EditorSubFeature("photo_to_avatar", "Photo to avatar"),
        EditorSubFeature("video_to_avatar", "Video to avatar")
    )

    EditorMainTab.ASPECT_RATIO -> listOf(
        EditorSubFeature("original", "Original"),
        EditorSubFeature("ratio_9_16", "9:16", ratio = AspectRatioType.RATIO_9_16),
        EditorSubFeature("ratio_16_9", "16:9", ratio = AspectRatioType.RATIO_16_9),
        EditorSubFeature("ratio_1_1", "1:1", ratio = AspectRatioType.RATIO_1_1),
        EditorSubFeature("ratio_4_3", "4:3", ratio = AspectRatioType.RATIO_4_3),
        EditorSubFeature("ratio_3_4", "3:4", ratio = AspectRatioType.RATIO_3_4),
        EditorSubFeature("ratio_5_8", "5:8", ratio = AspectRatioType.RATIO_5_8),
        EditorSubFeature("ratio_128_27", "128:27", ratio = AspectRatioType.RATIO_128_27),
        EditorSubFeature("ratio_2_1", "2:1", ratio = AspectRatioType.RATIO_2_1),
        EditorSubFeature("ratio_2_35_1", "2.35:1", ratio = AspectRatioType.RATIO_2_35_1),
        EditorSubFeature("ratio_1_85_1", "1.85:1", ratio = AspectRatioType.RATIO_1_85_1)
    )

    EditorMainTab.BACKGROUND -> listOf(
        EditorSubFeature("color", "Color"),
        EditorSubFeature("image", "Image"),
        EditorSubFeature("blur", "Blur"),
        EditorSubFeature("brand_background", "Brand Background")
    )

    EditorMainTab.FILTER,
    EditorMainTab.ADJUST,
    EditorMainTab.STICKER,
    EditorMainTab.TRANSCRIPT,
    EditorMainTab.TEMPLATE -> emptyList()
}
