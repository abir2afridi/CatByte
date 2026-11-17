package com.example.ui.screens

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.features.AiFeature
import kotlinx.coroutines.launch

/**
 * The editor's bottom action bar.
 *
 * A selected clip shows the clip action bar; without a selection the CapCut
 * bottom bar (media, audio, text, effects, ...) is shown.
 *
 * The CapCut bar has two levels: the main row with the 15 workspace features
 * and, for the features that own a sub-menu, a second row with those
 * sub-features. Tapping a sub-feature runs the matching editor action or
 * opens the matching tool sheet.
 */
@Composable
internal fun EditorBottomActionStage(
    viewModel: EditorViewModel,
    project: VideoProject,
    selectedClipId: String?,
    isMultiSelect: Boolean,
    splitAtPlayhead: () -> Unit,
    replaceMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    addOverlayLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onOpenAudioSheet: (Int) -> Unit,
    onShowTts: () -> Unit,
    onImportCaptions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val brandBackgroundPath by viewModel.brandBackgroundPath.collectAsState()

    // The active main feature of the two-level navigation. It is stored by
    // name so the state survives a configuration change.
    var activeTabName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeTab = activeTabName?.let { name ->
        EditorMainTab.entries.firstOrNull { it.name == name }
    }

    // Selecting a clip swaps the bar for the clip action bar, so the
    // sub-feature row is closed and the main row is shown again afterwards.
    LaunchedEffect(selectedClipId) {
        if (selectedClipId != null) activeTabName = null
    }

    /** Short status message, e.g. "Select a clip first". */
    val notify: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    var showAiMusicDialog by rememberSaveable { mutableStateOf(false) }

    // Brand music: first pick imports + saves it; later taps re-add it.
    val brandMusicPath by viewModel.brandMusicPath.collectAsState()
    val brandMusicPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { picked ->
        if (picked != null) {
            val stored = viewModel.importBrandMusic(picked)
            if (stored != null) {
                viewModel.saveBrandMusic(stored)
                viewModel.addAudioTrackFromUri(stored, "Brand music", "Your brand") { ok ->
                    notify(if (ok) "Brand music saved and added" else "Could not add the brand music")
                }
            } else {
                notify("Could not import the brand music")
            }
        }
    }

    // Brand background: first pick imports + saves it as the reusable brand
    // image; afterwards the chip reapplies the saved one with a single tap.
    val brandBackgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { picked ->
        if (picked != null) {
            val stored = viewModel.importBackgroundImage(picked)
            if (stored != null) {
                viewModel.saveBrandBackground(stored)
                notify("Brand background saved - tap again on any project to reuse it")
            } else {
                notify("Could not import the brand image")
            }
        }
    }


    /**
     * Features that need a specialised AI provider (background removal, lip
     * sync, avatar generation, ...). Text and image AI DO work when the user
     * configures an endpoint in Settings → AI.
     */
    val notAvailable: (String) -> Unit = { name ->
        Toast.makeText(
            context,
            "$name needs a specialised AI provider. Text and image AI work through Settings → AI.",
            Toast.LENGTH_LONG
        ).show()
    }

    /** The clip under the playhead, used when a tool needs a target clip. */
    val clipAtPlayhead: () -> MediaClip? = {
        val position = viewModel.currentPositionMs.value
        project.clips.firstOrNull { position >= it.startTimelineMs && position < it.startTimelineMs + it.durationMs }
            ?: project.overlayTracks
                .flatMap { it.clips }
                .firstOrNull { position >= it.startTimelineMs && position < it.startTimelineMs + it.durationMs }
    }

    /**
     * Runs a clip-level tool. The bottom bar is shown while no clip is
     * selected, so the clip under the playhead is selected first - that is the
     * clip CapCut would edit.
     */
    val withClip: (() -> Unit) -> Unit = { action ->
        if (selectedClipId != null) {
            action()
        } else {
            val target = clipAtPlayhead() ?: project.clips.firstOrNull()
            if (target == null) {
                notify("Add a clip to the timeline first")
            } else {
                viewModel.selectClip(target.id)
                action()
            }
        }
    }

    /** Generates the transcript/captions when the video actually has audio. */
    val generateTranscript: () -> Unit = {
        val hasVoice = project.clips.any { !it.isMuted && it.volume > 0f } ||
            project.audioTracks.any { !it.isMuted }
        if (!hasVoice) {
            notify("This video has no voice to transcribe")
        } else {
            scope.launch {
                notify("Transcribing audio with AI…")
                notify(viewModel.addAutoCaptions())
            }
        }
    }

    val onMainTabSelected: (EditorMainTab) -> Unit = { tab ->
        when (tab) {
            // Features that open their tool sheet directly.
            EditorMainTab.FILTER -> withClip {
                viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY)
            }
            EditorMainTab.ADJUST -> withClip {
                viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS)
            }
            EditorMainTab.STICKER -> viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER)
            EditorMainTab.TRANSCRIPT -> generateTranscript()
            EditorMainTab.TEMPLATE -> viewModel.setActiveSheet(ActiveToolSheet.TEMPLATES)

            // Features that own a sub-feature row. Tapping the same tab
            // again returns to the main row.
            else -> activeTabName = if (activeTabName == tab.name) null else tab.name
        }
    }

    val onSubFeatureSelected: (EditorMainTab, EditorSubFeature) -> Unit = { tab, subFeature ->
        when (tab) {
            EditorMainTab.EDIT -> when (subFeature.id) {
                "split" -> withClip { splitAtPlayhead() }
                "volume" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST) }
                "animation" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.ANIMATIONS) }
                "effect" -> viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY)
                "delete" -> withClip { viewModel.deleteSelectedClip() }
                "speed" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.SPEED) }
                "beats" -> viewModel.autoCutToBeat()
                "crop" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.CROP) }
                "duplicate" -> withClip { viewModel.duplicateSelectedClip() }
                "replace" -> withClip {
                    replaceMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
                "overlay" -> addOverlayLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
                "adjust" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) }
                "filters" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.FILTERS_LIBRARY) }
                "opacity" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.OPACITY) }
                "transform" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP) }
                "mask" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.MASK) }
                "extract_audio" -> withClip { viewModel.extractAudioFromSelectedClip() }
                "freeze" -> withClip { viewModel.freezeFrameAtPlayhead() }
                "reverse" -> withClip { viewModel.reverseSelectedClip() }
                "transcript" -> generateTranscript()
                "video_quality" -> viewModel.setActiveSheet(ActiveToolSheet.EXPORT_MODAL)
                "remove_bg" -> notAvailable("Remove BG")
                "ai_remove" -> notAvailable("AI remove")
                "ai_remix" -> scope.launch {
                    notify("AI remix: thinking…")
                    notify(AiFeature.remixScript(viewModel))
                }
                "ai_expand" -> notAvailable("AI expand")
                "eye_contact" -> notAvailable("Eye contact")
                "relight" -> notAvailable("Relight")
                "motion_blur" -> withClip {
                    val clip = project.allVideoClips.firstOrNull { it.id == viewModel.selectedClipId.value }
                    val enabling = clip?.motionBlur != true
                    viewModel.toggleMotionBlurOnSelectedClip(enabling)
                    notify(if (enabling) "Motion blur on for this clip" else "Motion blur off")
                }
                "lip_sync" -> notAvailable("Lip sync")
                "auto_reframe" -> notAvailable("Auto reframe")
                "stabilize" -> notAvailable("Stabilize")
                "camera_tracking" -> notAvailable("Camera tracking")
                "isolate_voice" -> withClip {
                    val applied = viewModel.toggleIsolateVoiceOnSelectedClip(true)
                    if (applied) {
                        notify("Voice isolated - speech band kept, music suppressed")
                    } else {
                        notify("Could not apply voice isolation")
                    }
                }
                // The audio DSP (voice presets, enhance voice, noise gate) is
                // real: the sheet drives AudioEffectSettings into the shared
                // composition, so preview and export both run the kernels.
                "reduce_noise" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_EFFECTS) }
                "audio_effects" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_EFFECTS) }
                "enhance_voice" -> withClip { viewModel.setActiveSheet(ActiveToolSheet.AUDIO_EFFECTS) }
                "video_translator" -> scope.launch {
                    notify("AI translate: working...")
                    notify(AiFeature.translateCaptions(viewModel))
                }
                "retouch" -> notAvailable("Retouch")
                "unlink" -> withClip {
                    if (!viewModel.detachAudioFromSelectedClip()) {
                        notify("This clip's audio is already detached")
                    } else {
                        notify("Audio detached - it now has its own track")
                    }
                }
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.TEXT -> when (subFeature.id) {
                "add_text" -> viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
                "auto_caption" -> scope.launch {
                    notify("Generating captions...")
                    notify(viewModel.addAutoCaptions())
                }
                "stickers" -> viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER)
                "draw" -> viewModel.setActiveSheet(ActiveToolSheet.DRAW)
                "text_template" -> viewModel.setActiveSheet(ActiveToolSheet.TEMPLATES)
                "text_to_audio" -> onShowTts()
                "auto_lyric" -> scope.launch {
                    notify("AI lyrics: writing...")
                    notify(AiFeature.generateLyrics(viewModel))
                }
                "ai_writer" -> scope.launch {
                    notify("AI writer: writing...")
                    notify(AiFeature.writeScript(viewModel))
                }
                "ai_packaging" -> scope.launch {
                    notify("AI packaging: thinking...")
                    notify(AiFeature.packagingTitles(viewModel))
                }
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.AUDIO -> when (subFeature.id) {
                "mixer" -> {
                    if (!viewModel.openAudioMixer()) {
                        notify("Import or record an audio track first")
                    }
                }
                // The effects sheet edits the selected track. Without one the
                // mixer is the better entry point, so the user can pick a
                // track first instead of hitting a dead end.
                "effects" -> {
                    val selectedTrack = viewModel.selectedAudioId.value
                    if (selectedTrack != null) {
                        viewModel.setActiveSheet(ActiveToolSheet.AUDIO_EFFECTS)
                    } else if (!viewModel.openAudioMixer()) {
                        notify("Import or record an audio track first")
                    }
                }
                "extract" -> withClip { viewModel.extractAudioFromSelectedClip() }
                "sounds" -> onOpenAudioSheet(EditorAudioTabs.MUSIC)
                "sound_fx" -> onOpenAudioSheet(EditorAudioTabs.SFX)
                "record" -> onOpenAudioSheet(EditorAudioTabs.MIC)
                "text_to_audio" -> onShowTts()
                "ai_music" -> showAiMusicDialog = true
                "brand_music" -> {
                    if (brandMusicPath.isNotBlank()) {
                        viewModel.addAudioTrackFromUri(brandMusicPath, "Brand music", "Your brand") { ok ->
                            notify(if (ok) "Brand music added at the playhead" else "Could not add the brand music")
                        }
                    } else {
                        brandMusicPicker.launch("audio/*")
                    }
                }
                "copyright" -> notify("Copyright-free music lives in the Sounds library")
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.EFFECT -> when (subFeature.id) {
                "video_effects" -> viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY)
                "photo_effects" -> viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY)
                "ai_effects" -> viewModel.setActiveSheet(ActiveToolSheet.AI_TOOLS)
                "body_effects" -> viewModel.setActiveSheet(ActiveToolSheet.BODY_EFFECTS)
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.OVERLAY -> when (subFeature.id) {
                "add_overlay" -> addOverlayLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.CAPTIONS -> when (subFeature.id) {
                "enter_captions" -> viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
                "auto_captions" -> scope.launch {
                    notify("Transcribing audio with AI…")
                    notify(viewModel.addAutoCaptions())
                }
                "caption_template" -> viewModel.setActiveSheet(ActiveToolSheet.CAPTIONS)
                "import_captions" -> onImportCaptions()
                "ai_packaging" -> scope.launch {
                    notify("AI packaging: thinking...")
                    notify(AiFeature.packagingTitles(viewModel))
                }
                "auto_lyrics" -> scope.launch {
                    notify("AI lyrics: writing...")
                    notify(AiFeature.generateLyrics(viewModel))
                }
                "ai_emoji" -> scope.launch {
                    notify("AI emoji: thinking...")
                    notify(AiFeature.emojiSuggestions(viewModel))
                }
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.GENERATE_MEDIA -> when (subFeature.id) {
                "generated" -> notify("No generated media yet")
                "ai_image" -> scope.launch {
                    notify("AI image: generating...")
                    val prompt = project.title.ifBlank { "cinematic background" } +
                        ", vertical 9:16 background, no text"
                    notify(AiFeature.generateBackgroundImage(viewModel, prompt))
                }
                "ai_video" -> notAvailable("AI video")
                "ai_dialogue_scene" -> notAvailable("AI dialogue scene")
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.AI_AVATAR -> when (subFeature.id) {
                "avatar_library" -> notAvailable("Avatar library")
                "video_translator" -> notAvailable("Video translator")
                "ai_dialogue_scene" -> notAvailable("AI dialogue scene")
                "mimic_motion" -> notAvailable("Mimic motion")
                "fashion_model_video" -> notAvailable("Fashion model video")
                "lip_sync" -> notAvailable("Lip sync")
                "change_expressions" -> notAvailable("Change expressions")
                "photo_to_avatar" -> notAvailable("Photo to avatar")
                "video_to_avatar" -> notAvailable("Video to avatar")
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.ASPECT_RATIO -> {
                val ratio = subFeature.ratio
                if (ratio != null) {
                    viewModel.setAspectRatio(ratio)
                } else {
                    // "Original": copy the ratio of the imported footage.
                    val hasSource = project.allVideoClips.any { !it.uriString.isNullOrBlank() }
                    if (!hasSource) {
                        notify("No source video to copy the ratio from")
                    } else {
                        val original = viewModel.applyOriginalAspectRatio()
                        notify(
                            if (original != null) {
                                "Original ratio: ${original.label}"
                            } else {
                                "Matching the source video ratio"
                            }
                        )
                    }
                }
            }

            EditorMainTab.BACKGROUND -> when (subFeature.id) {
                "color" -> {
                    viewModel.setBackgroundMode("color")
                    viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND)
                }
                "image" -> {
                    viewModel.setBackgroundMode("image")
                    viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND)
                }
                "blur" -> {
                    viewModel.setBackgroundMode("blur")
                    notify("Blur on - the video fills and blurs behind the frame")
                }
                "brand_background" -> {
                    if (brandBackgroundPath.isNotBlank()) {
                        viewModel.saveBrandBackground(brandBackgroundPath)
                        notify("Brand background applied")
                    } else {
                        brandBackgroundPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
                else -> notify("${subFeature.label} is not available yet")
            }

            else -> notify("${subFeature.label} is not available yet")
        }
    }

    if (showAiMusicDialog) {
        com.example.ui.components.AiMusicDialog(
            onGenerate = { mood ->
                showAiMusicDialog = false
                notify("Composing ${mood.displayName.lowercase()} music...")
                viewModel.generateAiMusic(mood) { ok ->
                    notify(if (ok) "AI music added at the playhead" else "Could not generate the music")
                }
            },
            onDismiss = { showAiMusicDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121215))
            .navigationBarsPadding()
            .padding(top = 6.dp)
    ) {
        if (selectedClipId != null && !isMultiSelect) {
            ClipActionBar(
                onBack = { viewModel.clearSelection() },
                onSplit = splitAtPlayhead,
                onVolume = { viewModel.setActiveSheet(ActiveToolSheet.VOLUME_BOOST) },
                onAnimations = { viewModel.setActiveSheet(ActiveToolSheet.ANIMATIONS) },
                onEffects = { viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY) },
                onDelete = { viewModel.deleteSelectedClip() },
                onSpeed = { viewModel.setActiveSheet(ActiveToolSheet.SPEED) },
                onBeats = { viewModel.autoCutToBeat() },
                onCrop = { viewModel.setActiveSheet(ActiveToolSheet.CROP) },
                onDuplicate = { viewModel.duplicateSelectedClip() },
                onReplace = {
                    replaceMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onOverlay = {
                    addOverlayLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onAdjust = { viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS) },
                onStickers = { viewModel.setActiveSheet(ActiveToolSheet.STICKERS_PICKER) },
                onAspectRatio = { viewModel.setActiveSheet(ActiveToolSheet.CANVAS_RATIO) },
                onBackground = { viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND) },
                onOpacity = { viewModel.setActiveSheet(ActiveToolSheet.OPACITY) },
                onFreeze = { viewModel.freezeFrameAtPlayhead() },
                onReverse = { viewModel.reverseSelectedClip() },
                onExtractAudio = { viewModel.extractAudioFromSelectedClip() },
                onMore = { viewModel.setActiveSheet(ActiveToolSheet.EDIT_CLIP) },
                onAiTools = { viewModel.setActiveSheet(ActiveToolSheet.AI_TOOLS) }
            )
        } else {
            CapCutBottomActionBar(
                activeTab = activeTab,
                onMainTabSelected = onMainTabSelected,
                onSubFeatureSelected = onSubFeatureSelected,
                onBackToMain = { activeTabName = null }
            )
        }
    }
}
