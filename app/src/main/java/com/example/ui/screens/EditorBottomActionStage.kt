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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

    /** Cloud-only features are not part of the offline build. */
    val notAvailable: (String) -> Unit = { name ->
        Toast.makeText(
            context,
            "$name needs an online AI model - unavailable in this offline build",
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
            notify("Generating transcript")
            viewModel.addAutoCaptions()
        }
    }

    val onMainTabSelected: (EditorMainTab) -> Unit = { tab ->
        when (tab) {
            // Features that open their tool sheet directly.
            EditorMainTab.TEXT -> viewModel.setActiveSheet(ActiveToolSheet.TEXT_EDITOR)
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
                "video_quality" -> notAvailable("Video Quality")
                "remove_bg" -> notAvailable("Remove BG")
                "ai_remove" -> notAvailable("AI remove")
                "ai_expand" -> notAvailable("AI expand")
                "ai_remix" -> notAvailable("AI remix")
                "eye_contact" -> notAvailable("Eye contact")
                "relight" -> notAvailable("Relight")
                "motion_blur" -> notAvailable("Motion blur")
                "lip_sync" -> notAvailable("Lip sync")
                "auto_reframe" -> notAvailable("Auto reframe")
                "stabilize" -> notAvailable("Stabilize")
                "camera_tracking" -> notAvailable("Camera tracking")
                "isolate_voice" -> notAvailable("Isolate voice")
                "reduce_noise" -> notAvailable("Reduce noise")
                "audio_effects" -> notAvailable("Audio effects")
                "enhance_voice" -> notAvailable("Enhance voice")
                "video_translator" -> notAvailable("Video translator")
                "retouch" -> notAvailable("Retouch")
                "unlink" -> notify("Unlinking linked clips is not available yet")
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.AUDIO -> when (subFeature.id) {
                "extract" -> withClip { viewModel.extractAudioFromSelectedClip() }
                "sounds" -> onOpenAudioSheet(EditorAudioTabs.MUSIC)
                "sound_fx" -> onOpenAudioSheet(EditorAudioTabs.SFX)
                "record" -> onOpenAudioSheet(EditorAudioTabs.MIC)
                "text_to_audio" -> onShowTts()
                "ai_music" -> notAvailable("AI music")
                "brand_music" -> notAvailable("Brand music")
                "copyright" -> notify("Copyright-free music lives in the Sounds library")
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.EFFECT -> when (subFeature.id) {
                "video_effects" -> viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY)
                "photo_effects" -> viewModel.setActiveSheet(ActiveToolSheet.EFFECTS_LIBRARY)
                "ai_effects" -> viewModel.setActiveSheet(ActiveToolSheet.AI_TOOLS)
                "body_effects" -> notAvailable("Body effects")
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
                "auto_captions" -> viewModel.addAutoCaptions()
                "caption_template" -> viewModel.setActiveSheet(ActiveToolSheet.CAPTIONS)
                "import_captions" -> onImportCaptions()
                "ai_packaging" -> notAvailable("AI packaging")
                "auto_lyrics" -> notAvailable("Auto lyrics")
                "ai_emoji" -> notAvailable("AI emoji")
                else -> notify("${subFeature.label} is not available yet")
            }

            EditorMainTab.GENERATE_MEDIA -> when (subFeature.id) {
                "generated" -> notify("No generated media yet")
                "ai_image" -> notAvailable("AI image")
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
                "color" -> viewModel.setActiveSheet(ActiveToolSheet.BACKGROUND)
                "image" -> notify("Image backgrounds are not available yet")
                "blur" -> notify("Blurred backgrounds are not available yet")
                "brand_background" -> notAvailable("Brand Background")
                else -> notify("${subFeature.label} is not available yet")
            }

            else -> notify("${subFeature.label} is not available yet")
        }
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
