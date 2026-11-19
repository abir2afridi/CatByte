package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppLog
import com.example.data.AppLanguage
import com.example.data.AutoCaptionManager
import com.example.data.BeatDetector
import com.example.data.CacheManager
import com.example.data.ClipTiming
import com.example.data.ExportServiceScheduler
import com.example.data.MediaDurationFallback
import com.example.data.MediaMetadata
import com.example.data.MediaVideoInfo
import com.example.data.ProjectRepository
import com.example.export.ExportSettings
import com.example.export.ExportUiState
import com.example.data.PlayerRecoveryPolicy
import com.example.data.MissingMediaRef
import com.example.data.ScrubFrameAccuracy
import com.example.data.ScrubFrameLoader
import com.example.data.ScrubFramePolicy
import com.example.data.ScrubFrameResolver
import com.example.data.ScrubPreviewFrame
import com.example.data.SettingsRepository
import com.example.data.SpeechSynthesizer
import com.example.data.VoiceRecorder
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.AudioCompressor
import com.example.model.AudioEffectSettings
import com.example.model.AudioEqualizer
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.HslAdjustment
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.StickerOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineTrackIds
import com.example.model.TimelineEffect
import com.example.model.TimelineMarker
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import com.example.model.ViralTemplate
import com.example.data.KeyframeInterpolator
import com.example.data.db.ProjectJson
import com.example.engine.AspectMath
import com.example.engine.AudioLoudness
import com.example.engine.BeatGrid
import com.example.engine.CaptionBridge
import com.example.engine.ClipSplit
import com.example.engine.ClipTrimHandle
import com.example.engine.withMagneticMainTrack
import com.example.engine.FileJournalStorage
import com.example.engine.SnapEngine
import com.example.engine.SubtitleIO
import com.example.engine.TimelineEdits
import com.example.engine.UndoJournal
import com.example.viewmodel.features.AudioFeature
import com.example.viewmodel.features.AudioMixerFeature
import com.example.viewmodel.features.TextFeature
import com.example.viewmodel.features.StickerFeature
import com.example.viewmodel.features.StrokeFeature
import com.example.viewmodel.features.TimelineEffectFeature
import com.example.viewmodel.features.CanvasFeature
import com.example.viewmodel.features.MarkerFeature
import com.example.viewmodel.features.TrackControlFeature
import com.example.viewmodel.features.ClipAttributeClipboard
import com.example.viewmodel.features.ClipAttributeFeature
import com.example.viewmodel.features.ExportFeature
import com.example.viewmodel.features.KeyframeFeature
import com.example.viewmodel.features.OverlayClipFeature
import com.example.viewmodel.features.ClipEffectFeature
import com.example.viewmodel.features.BeatSyncFeature
import com.example.viewmodel.features.ManualBeatsFeature
import com.example.viewmodel.features.ClipPropertyFeature
import com.example.viewmodel.features.ClipTransformFeature
import com.example.viewmodel.features.SettingsFeature
import com.example.viewmodel.features.ClipEditFeature
import com.example.viewmodel.features.ClipMoveFeature
import com.example.viewmodel.features.ClipTrimFeature
import com.example.viewmodel.features.ImportFeature
import com.example.viewmodel.features.RelinkFeature
import com.example.viewmodel.features.HistoryFeature
import com.example.viewmodel.features.PlaybackFeature
import com.example.viewmodel.features.ScrubPreviewFeature
import com.example.viewmodel.features.ProjectLifecycleFeature
import com.example.viewmodel.features.ProjectBackupFeature
import com.example.viewmodel.features.SelectionFeature
import com.example.viewmodel.features.SelectionFeature.ClipboardEntry
import com.example.viewmodel.features.TemplateFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

enum class ActiveToolSheet {
    NONE,
    EDIT_CLIP,
    SPEED,
    AUDIO_LIBRARY,
    AUDIO_MIXER,
    /** Per-clip / per-track audio effects (voice presets, EQ, gate). */
    AUDIO_EFFECTS,
    VOICE_RECORDER,
    TEXT_EDITOR,
    EFFECTS_LIBRARY,
    FILTERS_LIBRARY,
    ADJUSTMENTS,
    CANVAS_RATIO,
    STICKERS_PICKER,
    TRANSITIONS_PICKER,
    EXPORT_MODAL,
    KEYFRAME,
    BLEND_MODE,
    MASK,
    TTS_VOICEOVER,
    CHROMA_KEY,
    VOLUME_BOOST,
    CAPTIONS,
    ANIMATIONS,
    CROP,
    OPACITY,
    AI_TOOLS,
    TIMELINE_TOOLS,
    /** Detected + hand-pinned beat grid management and cut-to-beats. */
    BEAT_GRID,
    BACKGROUND,
    COVER_EDITOR,
    TEMPLATES,
    /** Timeline marker management (rename / recolour / delete). */
    MARKERS,
    /** Freehand drawing sheet (Text → Draw). */
    DRAW,
    /** Body effects library (Effect → Body effects). */
    BODY_EFFECTS,
    /** Custom .cube LUT import sheet (Filters → Import .cube LUT). */
    LUT_IMPORT
}

/**
 * Snapshot of the audio clip that drives beat detection. Comparing this value
 * (instead of the whole project) avoids re-running detection when unrelated
 * timeline edits happen.
 */
private data class BeatSource(
    val uri: String,
    val startTimelineMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val speed: Float,
    val durationMs: Long
)

@androidx.media3.common.util.UnstableApi
class EditorViewModel(
    internal val repository: ProjectRepository = ProjectRepository(),
    internal val settingsRepository: SettingsRepository? = null,
    internal val cacheManager: CacheManager? = null,
    internal val playerManager: com.example.data.PlayerManager? = null,
    internal val exportManager: com.example.data.ExportManager? = null,
    internal val context: Context? = null
) : ViewModel() {

    private val _projects = repository.projects
    val projects: StateFlow<List<VideoProject>> = _projects

    internal val _currentProject = MutableStateFlow<VideoProject?>(null)
    val currentProject: StateFlow<VideoProject?> = _currentProject.asStateFlow()

    // `totalDurationMs` walks every track and allocates lists. seekTo() runs on
    // every scroll event, so cache the value for the current immutable project.
    private var durationCacheProject: VideoProject? = null
    private var durationCacheMs = 0L

    internal fun totalDurationMsOf(project: VideoProject): Long {
        if (durationCacheProject === project) return durationCacheMs
        val total = project.totalDurationMs
        durationCacheProject = project
        durationCacheMs = total
        return total
    }

    internal val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    internal val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    internal val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    /** Ids currently in a multi-selection (single selection is a set of one). */
    internal val _selectedClipIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedClipIds: StateFlow<Set<String>> = _selectedClipIds.asStateFlow()

    internal val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    /** The independent audio clip currently selected in the timeline/mixer. */
    internal val _selectedAudioId = MutableStateFlow<String?>(null)
    val selectedAudioId: StateFlow<String?> = _selectedAudioId.asStateFlow()

    internal val _activeSheet = MutableStateFlow(ActiveToolSheet.NONE)
    val activeSheet: StateFlow<ActiveToolSheet> = _activeSheet.asStateFlow()

    internal val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    /**
     * True while the preview shows the selected clip without its colour
     * adjustments (the "Before / After" comparison of the Adjust panel).
     */
    internal val _compareOriginal = MutableStateFlow(false)
    val compareOriginal: StateFlow<Boolean> = _compareOriginal.asStateFlow()

    /** Detected beat positions (ms) used as magnetic snap targets on the timeline. */
    internal val _snapBeats = MutableStateFlow<List<Long>>(emptyList())
    val snapBeats: StateFlow<List<Long>> = _snapBeats.asStateFlow()

    /** Detection result only; hand-pinned [VideoProject.manualBeatsMs] are merged separately. */
    internal val _detectedBeats = MutableStateFlow<List<Long>>(emptyList())
    val detectedBeats: StateFlow<List<Long>> = _detectedBeats.asStateFlow()

    fun setEyedropperActive(active: Boolean) = SettingsFeature.setEyedropperActive(this, active)

    internal val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    internal val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    internal val _appThemeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.LIGHT)
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _appThemeMode.asStateFlow()

    internal val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    internal val _defaultResolution = MutableStateFlow("1080P")
    val defaultResolution: StateFlow<String> = _defaultResolution.asStateFlow()

    internal val _defaultFps = MutableStateFlow(30)
    val defaultFps: StateFlow<Int> = _defaultFps.asStateFlow()

    internal val _showPaperGrid = MutableStateFlow(true)
    val showPaperGrid: StateFlow<Boolean> = _showPaperGrid.asStateFlow()

    internal val _watermarkEnabled = MutableStateFlow(false)
    val watermarkEnabled: StateFlow<Boolean> = _watermarkEnabled.asStateFlow()

    /** Preview-only safe-area / rule-of-thirds guides (never exported). */
    internal val _showSafeAreaGuides = MutableStateFlow(false)
    val showSafeAreaGuides: StateFlow<Boolean> = _showSafeAreaGuides.asStateFlow()

    /** Media referenced by the open project that can no longer be opened. */
    internal val _missingMedia = MutableStateFlow<List<MissingMediaRef>>(emptyList())
    val missingMedia: StateFlow<List<MissingMediaRef>> = _missingMedia.asStateFlow()

    internal val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    /**
     * OpenAI-compatible endpoint configured in Settings → AI. Empty config
     * means the editor AI chips explain that no endpoint is set instead of
     * pretending to work.
     */
    internal val _aiConfig = MutableStateFlow(com.example.data.ai.AiConfig())
    val aiConfig: StateFlow<com.example.data.ai.AiConfig> = _aiConfig.asStateFlow()

    /** Reusable brand background (app-storage path, empty = none). */
    private val _brandBackgroundPath = MutableStateFlow("")
    val brandBackgroundPath: StateFlow<String> = _brandBackgroundPath.asStateFlow()

    /** Reusable brand music (app-storage path, empty = none). */
    private val _brandMusicPath = MutableStateFlow("")
    val brandMusicPath: StateFlow<String> = _brandMusicPath.asStateFlow()

    internal val _cacheSizeMb = MutableStateFlow(0)
    val cacheSizeMb: StateFlow<Int> = _cacheSizeMb.asStateFlow()

    internal val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    internal val voiceRecorder = context?.let { VoiceRecorder(it) }
    internal val speechSynthesizer = context?.let { SpeechSynthesizer(it) }

    fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) =
        SettingsFeature.setAppThemeMode(this, mode)

    fun setAppLanguage(language: AppLanguage) =
        SettingsFeature.setAppLanguage(this, language)

    fun openSettings() = SettingsFeature.openSettings(this)

    fun closeSettings() = SettingsFeature.closeSettings(this)

    fun setDefaultResolution(res: String) = SettingsFeature.setDefaultResolution(this, res)

    /** Stores the AI endpoint used by the editor AI features (Settings → AI). */
    fun saveAiConfig(
        baseUrl: String,
        apiKey: String,
        model: String,
        imageModel: String,
        speechModel: String = "whisper-1"
    ) = SettingsFeature.saveAiConfig(this, baseUrl, apiKey, model, imageModel, speechModel)

    fun setDefaultFps(fps: Int) = SettingsFeature.setDefaultFps(this, fps)

    fun togglePaperGrid(enabled: Boolean) = SettingsFeature.togglePaperGrid(this, enabled)

    fun toggleWatermark(enabled: Boolean) = SettingsFeature.toggleWatermark(this, enabled)

    fun toggleSafeAreaGuides(enabled: Boolean) =
        SettingsFeature.toggleSafeAreaGuides(this, enabled)

    fun refreshCacheSize() = SettingsFeature.refreshCacheSize(this)

    fun clearCache() = SettingsFeature.clearCache(this)

    // Voice recording with real MediaRecorder
    internal val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()
    internal val _voiceRecordingDurationMs = MutableStateFlow(0L)
    val voiceRecordingDurationMs: StateFlow<Long> = _voiceRecordingDurationMs.asStateFlow()
    /** Live microphone level (0..1) while a voice-over is being recorded. */
    internal val _voiceRecordingLevel = MutableStateFlow(0f)
    val voiceRecordingLevel: StateFlow<Float> = _voiceRecordingLevel.asStateFlow()

    // Undo / Redo history stacks
    internal val undoStack = mutableListOf<EditorHistoryEntry>()
    internal val redoStack = mutableListOf<VideoProject>()

    /**
     * Durable copy of the undo history. The in-memory stacks die with the
     * process; this journal is written atomically to disk so a crash no longer
     * destroys every undo step of the session.
     */
    internal val undoJournal: UndoJournal? by lazy {
        val directory = context?.filesDir?.let { File(it, "undo") }
        if (directory == null) null else UndoJournal(FileJournalStorage(File(directory, "history.json")))
    }

    /** One undoable state: the project to revert to, plus gesture metadata. */
    internal data class EditorHistoryEntry(
        val project: VideoProject,
        /** Non-null while a drag/slider gesture streams; equal keys collapse. */
        val key: String? = null,
        val atMs: Long = System.currentTimeMillis()
    )

    internal val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    internal val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    internal val _magneticSnap = MutableStateFlow(true)
    val magneticSnap: StateFlow<Boolean> = _magneticSnap.asStateFlow()

    fun toggleMagneticSnap() = SettingsFeature.toggleMagneticSnap(this)

    /**
     * Live scrub preview (source-frame extraction).
     *
     * The decoder lanes, settle watcher and preview hand-over live in their
     * own file so the still preview can be fixed without touching the
     * playback engine.
     */
    internal val scrubPreview = ScrubPreviewFeature(this)

    /** Source frame drawn over the preview while the timeline is scrubbed. */
    val scrubPreviewFrame: StateFlow<ScrubPreviewFrame?> = scrubPreview.scrubPreviewFrame

    /** Playback engine: play/pause, playhead polling, frozen-preview recovery. */
    internal val playback = PlaybackFeature(this)

    /** Inline trim handles (per-editor drag session). */
    internal val clipTrim = ClipTrimFeature(this)

    /** Media import (per-editor batch import job). */
    internal val importer = ImportFeature(this)
    internal var voiceRecordJob: Job? = null
    internal var exportJob: Job? = null

    /** Preview rebuild policy lives in its own file (PreviewRefreshScheduler). */
    internal val previewScheduler = PreviewRefreshScheduler(
        scope = viewModelScope,
        projectProvider = { _currentProject.value },
        refresh = { project -> refreshPreviewComposition(project) }
    )

    /**
     * Rebuilds the render graph after the user stops dragging/sliding. The
     * composition is identical to the export graph, so a short debounce keeps
     * sliders smooth without allowing preview/export drift.
     */
    internal fun schedulePreviewRefresh() {
        previewScheduler.schedule()
    }

    /**
     * Applies an edit to the preview.
     *
     * Normally the player already sits on the playhead, so loading the project
     * is enough. After a scrub it does NOT: the player was deliberately never
     * seeked (every seek blacks the surface out), and the still on screen is
     * the visible playhead. Preparing the refreshed composition at the stale
     * player position would hide the edit behind the still; instead load it at
     * the visible playhead and hand the still over once the new player is
     * really ready. If it never becomes ready the still stays up — the preview
     * must never fall back to black or an error pill.
     */
    private fun refreshPreviewComposition(project: VideoProject) {
        val manager = playerManager ?: return
        // The preview player is process-scoped: keep its comparison state in
        // sync with the editor before the composition is rebuilt, otherwise a
        // recreated player would show the graded frame again.
        manager.setCompareOriginal(
            enabled = _compareOriginal.value,
            clipId = if (_compareOriginal.value) _selectedClipId.value else null,
            rebuild = false
        )
        val scrubVisible = scrubPreview.isScrubVisible()
        if (!scrubVisible) {
            manager.loadProject(project)
            return
        }
        val at = _currentPositionMs.value
        // A fresh player at the visible playhead is the proven path: seeking or
        // re-preparing the paused CompositionPlayer is exactly what wedged it
        // during the scrub. The still hides the rebuild, so nothing flashes.
        manager.loadProject(project, startPositionMs = at, forceRebuild = true)
        scrubPreview.awaitEditHandoff(manager, at)
    }

    internal fun refreshPreviewNow() {
        previewScheduler.refreshNow()
    }

    init {
        val initialList = repository.projects.value
        if (initialList.isNotEmpty()) {
            val firstProject = initialList.first().withMagneticMainTrack()
            _currentProject.value = firstProject
            // CapCut opens with nothing selected: the user taps a clip to
            // select it. Auto-selecting the first clip kept the timeline in a
            // permanently "selected" state (white border + trim handles).
            clearSelection()
        }

        if (settingsRepository != null) {
            viewModelScope.launch {
                settingsRepository.settings.collect { settings ->
                    _appThemeMode.value = settings.themeMode
                    _defaultResolution.value = settings.defaultResolution
                    _defaultFps.value = settings.defaultFps
                    _showPaperGrid.value = settings.showPaperGrid
                    _watermarkEnabled.value = settings.watermarkEnabled
                    _showSafeAreaGuides.value = settings.showSafeAreaGuides
                    _appLanguage.value = settings.language
                    _aiConfig.value = com.example.data.ai.AiConfig(
                        baseUrl = settings.aiBaseUrl,
                        apiKey = settings.aiApiKey,
                        model = settings.aiModel,
                        imageModel = settings.aiImageModel,
                        speechModel = settings.aiSpeechModel
                    )
                    _brandBackgroundPath.value = settings.brandBackgroundPath
                    _brandMusicPath.value = settings.brandMusicPath
                }
            }
        }
        refreshCacheSize()
        speechSynthesizer?.initialise { /* ready */ }

        // Beat magnets for the timeline: re-detected whenever the first audio
        // source or the project duration changes. collectLatest cancels a stale
        // detection pass as soon as the audio changes again.
        viewModelScope.launch {
            _currentProject
                .map { project ->
                    val audio = project?.audioTracks
                        ?.firstOrNull { !it.uriString.isNullOrBlank() }
                    audio?.let {
                        BeatSource(
                            uri = it.uriString.orEmpty(),
                            startTimelineMs = it.startTimelineMs,
                            trimStartMs = it.trimStartMs,
                            trimEndMs = it.trimEndMs,
                            speed = it.speed,
                            durationMs = it.durationMs
                        )
                    }
                }
                .distinctUntilChanged()
                .collectLatest { source ->
                    val ctx = context
                    if (ctx == null || source == null) {
                        _detectedBeats.value = emptyList()
                        return@collectLatest
                    }
                    val sourceBeats = BeatDetector.detectBeats(ctx, source.uri)
                    _detectedBeats.value = BeatGrid.timelineBeats(
                        sourceBeats = sourceBeats,
                        startTimelineMs = source.startTimelineMs,
                        trimStartMs = source.trimStartMs,
                        speed = source.speed,
                        durationMs = source.durationMs
                    )
                }
        }

        // The timeline magnets are the detected beats plus the beats the user
        // pinned by hand. Pinning must not trigger another detection pass, so
        // the two sources are merged here instead of re-running the decoder.
        viewModelScope.launch {
            combine(_detectedBeats, _currentProject) { detected, project ->
                BeatGrid.merge(detected, project?.manualBeatsMs.orEmpty())
            }.collect { beats -> _snapBeats.value = beats }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scrubPreview.cancel()
        playback.cancel()
        voiceRecordJob?.cancel()
        exportJob?.cancel()
        importer.cancel()
        previewScheduler.cancel()
        voiceRecorder?.cancel()
        speechSynthesizer?.release()
        // Release the decoders on their own threads AFTER any in-flight decode.
        scrubPreview.shutdown()
        // PlayerManager is application-scoped (the editor UI reads it directly).
        // Releasing it here would leave a dead player when the activity is
        // recreated after finishing; just stop playback instead.
        playerManager?.pause()
    }

    fun openProject(project: VideoProject) = ProjectLifecycleFeature.openProject(this, project)

    /** Re-scans the open project for media files that no longer resolve. */
    fun refreshMissingMedia() = RelinkFeature.refresh(this)

    /**
     * Points a clip at a replacement file, keeping its trims, speed, filters
     * and keyframes, then clears the missing-media warning for that clip.
     */
    fun relinkClipMedia(clipId: String, uriString: String) =
        RelinkFeature.relinkClip(this, clipId, uriString)

    fun forgetMissingMedia(clipId: String) = RelinkFeature.forget(this, clipId)

    fun toggleProjectFavorite(id: String) = ProjectLifecycleFeature.toggleProjectFavorite(this, id)

    fun createNewProjectWithParams(
        title: String,
        ratio: AspectRatioType = AspectRatioType.RATIO_9_16,
        paperStyle: String = "Standard"
    ) = ProjectLifecycleFeature.createNewProjectWithParams(this, title, ratio, paperStyle)

    fun createNewProject() = ProjectLifecycleFeature.createNewProject(this)

    /** Renames a project (home list long-press menu / project settings). */
    fun renameProject(id: String, title: String) =
        ProjectLifecycleFeature.renameProject(this, id, title)

    fun duplicateProject(id: String) = ProjectLifecycleFeature.duplicateProject(this, id)

    fun deleteProject(id: String) = ProjectLifecycleFeature.deleteProject(this, id)

    /** Restores a draft removed from the home screen (undo delete). */
    fun restoreProject(project: VideoProject) = ProjectLifecycleFeature.restoreProject(this, project)

    /** Writes a project (optionally with its media) to a `.catbyte.zip` file. */
    suspend fun backupProject(
        projectId: String,
        destination: Uri,
        includeMedia: Boolean
    ): String = ProjectBackupFeature.backupProject(this, projectId, destination, includeMedia)

    /** Imports a `.catbyte.zip` backup into the home library. */
    suspend fun restoreProjectBackup(source: Uri): String =
        ProjectBackupFeature.restoreBackup(this, source)

    /** Clears ALL projects from the repository (database + memory). */
    fun clearAllProjects() = ProjectLifecycleFeature.clearAllProjects(this)

    fun setActiveSheet(sheet: ActiveToolSheet) = ProjectLifecycleFeature.setActiveSheet(this, sheet)

    fun selectClip(clipId: String?) = SelectionFeature.selectClip(this, clipId)

    /** Toggles a clip in the multi-selection. */
    fun toggleClipSelection(clipId: String) = SelectionFeature.toggleClipSelection(this, clipId)

    fun clearSelection() = SelectionFeature.clearSelection(this)

    fun selectAllClips() = SelectionFeature.selectAllClips(this)

    /** True when a group operation should apply instead of the single-clip one. */
    fun hasMultiSelection(): Boolean = SelectionFeature.hasMultiSelection(this)

    /**
     * Deletes every selected clip. Main-track members ripple (the gap closes,
     * list order defines time) while overlay members lift (the remaining
     * overlays keep their absolute positions).
     */
    fun deleteSelectedClips() = SelectionFeature.deleteSelectedClips(this)

    /**
     * Moves the selected overlay clips by [deltaMs] as one group, keeping their
     * relative offsets and clamping inside the project.
     */
    fun moveSelectedOverlays(deltaMs: Long) = SelectionFeature.moveSelectedOverlays(this, deltaMs)

    /** Copies selected overlay clips so they can be pasted at the playhead. */
    internal val _overlayClipboard = MutableStateFlow<List<MediaClip>>(emptyList())
    val overlayClipboard: StateFlow<List<MediaClip>> = _overlayClipboard.asStateFlow()

    /** Unified clipboard containing selected main-track and overlay clips. */
    internal val _clipboard = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    private val _clipboardCount = MutableStateFlow(0)
    val clipboardCount: StateFlow<Int> = _clipboardCount.asStateFlow()

    internal fun updateClipboard(entries: List<ClipboardEntry>) {
        _clipboard.value = entries
        _clipboardCount.value = entries.size
    }

    fun copySelectedOverlays() = SelectionFeature.copySelectedOverlays(this)

    /** Pastes the clipboard at the playhead, keeping the copied relative offsets. */
    fun pasteOverlays() = SelectionFeature.pasteOverlays(this)

    /** Copies the current selection (main track included). */
    fun copySelectedClips() {
        SelectionFeature.copySelectedClips(this)
    }

    /** Cuts the current selection as one undoable edit. */
    fun cutSelectedClips() = SelectionFeature.cutSelectedClips(this)

    /** Pastes main-track and overlay clips at the playhead as one undoable edit. */
    fun pasteClips() = SelectionFeature.pasteClips(this)

    fun selectText(textId: String?) = SelectionFeature.selectText(this, textId)

    fun setTimelineZoom(zoom: Float) = SelectionFeature.setTimelineZoom(this, zoom)

    // ==========================================
    // Playback Engine
    // ==========================================

    fun togglePlayback() = playback.togglePlayback()

    fun startPlayback() = playback.startPlayback()

    fun pausePlayback() = playback.pausePlayback()

    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)

    fun stepForward() = playback.stepForward()

    fun stepBackward() = playback.stepBackward()

    // ==========================================
    // Editing Actions (Undo/Redo Supported)
    // ==========================================

    /**
     * Bounded snapshot history (plan §12: immutable snapshots with structural
     * sharing). Data-class `copy` shares unchanged lists, so snapshots stay
     * cheap even at a deep history.
     */
    internal fun pushHistory(project: VideoProject, coalesceKey: String? = null) =
        HistoryFeature.pushHistory(this, project, coalesceKey)

    fun undo() = HistoryFeature.undo(this)

    fun redo() = HistoryFeature.redo(this)

    /**
     * Central mutation helper: snapshots the pre-edit project for undo, applies
     * [transform] and autosaves. [coalesceKey] groups a drag/slider stream so
     * one gesture becomes exactly one undo step (plan §12 coalescing rule).
     */
    internal fun updateProject(
        coalesceKey: String? = null,
        transform: (VideoProject) -> VideoProject
    ) = HistoryFeature.updateProject(this, coalesceKey, transform)

    // ==========================================
    // Clip operations
    // ==========================================

    /**
     * Splits the clip under the playhead and keeps the playhead exactly on the
     * cut. Returns false when the playhead is outside a clip or too close to an
     * edge, so the UI can tell the user instead of silently doing nothing.
     *
     * A selected overlay is split first (it is on top of the main track); then
     * the main-track clip under the playhead. Playback is paused before editing
     * so the cut lands on the frame the user is looking at.
     */
    fun splitClipAtPlayhead(): Boolean = ClipEditFeature.splitClipAtPlayhead(this)

    /**
     * Deletes the selected clip through [TimelineEdits.delete]: the main track
     * ripples (the gap closes, list order defines time) while overlay tracks
     * lift (absolute positions of the remaining overlays stay put).
     */
    fun deleteSelectedClip() = ClipEditFeature.deleteSelectedClip(this)

    fun duplicateSelectedClip() = ClipEditFeature.duplicateSelectedClip(this)

    /**
     * Inserts a 3-second freeze frame at the current playhead position.
     * Splits the active clip and creates a held frame with preserved styling.
     */
    fun freezeFrameAtPlayhead() = ClipEditFeature.freezeFrameAtPlayhead(this)

    /** Toggles reverse playback on the selected clip. */
    fun reverseSelectedClip() = ClipEditFeature.reverseSelectedClip(this)

    /** Updates chroma key (green/blue screen removal) for a clip. */
    fun setClipChromaKey(clipId: String, chromaData: com.example.model.ChromaKeyData) =
        ClipTransformFeature.setClipChromaKey(this, clipId, chromaData)

    /** Updates the selected clip's transform directly from canvas gestures. */
    fun updateSelectedClipTransform(
        scaleMultiplier: Float,
        rotationDelta: Float,
        panDeltaX: Float,
        panDeltaY: Float
    ) = ClipTransformFeature.updateSelectedClipTransform(
        this, scaleMultiplier, rotationDelta, panDeltaX, panDeltaY
    )

    /** Resets the selected clip's scale, position, and rotation to default. */
    fun resetSelectedClipTransform() = ClipTransformFeature.resetSelectedClipTransform(this)

    /** Samples video frame pixel color at normalized coordinates for Chroma Key. */
    fun sampleChromaColorAt(context: Context, normX: Float, normY: Float) =
        ClipTransformFeature.sampleChromaColorAt(this, context, normX, normY)

    /** Moves a clip to a new position in the timeline (drag-reorder). */
    fun moveClip(fromIndex: Int, toIndex: Int) = ClipMoveFeature.moveClip(this, fromIndex, toIndex)

    /**
     * Moves an overlay clip to a new start time. The drop snaps to neighbouring
     * overlay edges ([SnapEngine.magneticStart]) and clamps inside the project,
     * so a fling can never park a clip outside the timeline.
     */
    fun moveOverlayClip(clipId: String, desiredStartMs: Long) =
        ClipMoveFeature.moveOverlayClip(this, clipId, desiredStartMs)

    /**
     * Moves a clip to another overlay track at the requested start time.
     *
     * Cross-track drops are the CapCut "free overlay" model: the target track is
     * chosen by the vertical part of the drag, then [SnapEngine] resolves the
     * exact start inside that track. Locked source/target tracks refuse the move.
     */
    fun moveClipToTrack(clipId: String, targetTrackId: String, desiredStartMs: Long) =
        ClipMoveFeature.moveClipToTrack(this, clipId, targetTrackId, desiredStartMs)

    /** Track-level mute. Passes [TimelineTrackIds.MAIN] for the main video track. */
    fun setTrackMuted(trackId: String, muted: Boolean) =
        TrackControlFeature.setTrackMuted(this, trackId, muted)

    fun setTrackLocked(trackId: String, locked: Boolean) =
        TrackControlFeature.setTrackLocked(this, trackId, locked)

    /** Solo silences every other video track's audio, like an audio desk. */
    fun setTrackSolo(trackId: String, solo: Boolean) =
        TrackControlFeature.setTrackSolo(this, trackId, solo)

    /** Mutes or unmutes a single clip (audio gain only; the video stays visible). */
    fun setClipMuted(clipId: String, muted: Boolean) =
        TrackControlFeature.setClipMuted(this, clipId, muted)

    /** Mutes or unmutes one audio clip (the per-audio-row header toggle). */
    fun setAudioClipMuted(audioId: String, muted: Boolean) =
        TrackControlFeature.setAudioClipMuted(this, audioId, muted)

    /** Mutes or unmutes every audio clip at once (the "Audio" row header). */
    fun setAllAudioMuted(muted: Boolean) = TrackControlFeature.setAllAudioMuted(this, muted)

    /**
     * Soloes one audio track: while it is soloed every other audio track drops
     * out of the mix, in the preview and in the exported file alike.
     */
    fun setAudioSolo(audioId: String, soloed: Boolean) =
        AudioMixerFeature.setAudioSolo(this, audioId, soloed)

    /** Adds a marker at the playhead. */
    fun addMarker(label: String = "") = MarkerFeature.addMarker(this, label)

    fun removeMarker(markerId: String) = MarkerFeature.removeMarker(this, markerId)

    /** Renames a timeline marker. */
    fun renameMarker(markerId: String, label: String) =
        MarkerFeature.renameMarker(this, markerId, label)

    /** Recolours a timeline marker pin. */
    fun setMarkerColor(markerId: String, colorArgb: Long) =
        MarkerFeature.setMarkerColor(this, markerId, colorArgb)

    /** Adds a freehand drawing stroke at the playhead (Text → Draw). */
    fun addStroke(points: List<Float>, colorArgb: Long, widthFraction: Float): Boolean =
        StrokeFeature.addStroke(this, points, colorArgb, widthFraction)

    /** Removes every drawing stroke. */
    fun clearStrokes() = StrokeFeature.clearStrokes(this)

    fun moveMarkerToPlayhead(markerId: String) =
        MarkerFeature.moveMarkerToPlayhead(this, markerId)

    fun seekToMarker(markerId: String) = MarkerFeature.seekToMarker(this, markerId)

    /** Jumps to the marker before/after the playhead. Returns the new position. */
    fun jumpToMarker(direction: Int): Long? = MarkerFeature.jumpToMarker(this, direction)

    /** Reorders overlay tracks (top row = first layer rendered). */
    fun moveOverlayTrack(fromIndex: Int, toIndex: Int) =
        TrackControlFeature.moveOverlayTrack(this, fromIndex, toIndex)

    /**
     * Moves an audio clip to a new start time. The drop snaps to neighbouring
     * audio clips and clamps inside the project.
     */
    fun moveAudioClip(audioId: String, desiredStartMs: Long) =
        TrackControlFeature.moveAudioClip(this, audioId, desiredStartMs)

    /**
     * Moves the selected main-track clips as one contiguous block by [delta]
     * positions. A group keeps its internal order; neighbours close the gap.
     */
    fun moveSelectedMainClips(delta: Int) =
        TrackControlFeature.moveSelectedMainClips(this, delta)

    /** Snapshot of the visual/audio attributes that "paste attributes" copies. */
    internal var attributeClipboard: ClipAttributeClipboard? = null

    /** Copies the selected clip's look/audio attributes for paste-attributes. */
    fun copySelectedClipAttributes() = ClipAttributeFeature.copySelectedClipAttributes(this)

    /** Applies the copied look/audio attributes to every selected clip. */
    fun pasteClipAttributes() = ClipAttributeFeature.pasteClipAttributes(this)

    /**
     * Trims the selected clip from an inline timeline handle.
     *
     * [ClipTrimHandle] owns the source maths; this only applies the result, so the
     * sheet trim and the handles can never disagree. Locked tracks refuse it.
     */
    fun trimSelectedClipEdge(edge: ClipTrimHandle.Edge, outputDeltaMs: Long, fps: Int) =
        clipTrim.trimSelectedClipEdge(edge, outputDeltaMs, fps)

    /** Snapshots the clip before a handle drag so every live update is absolute. */
    fun beginTrimClip(clipId: String, edge: ClipTrimHandle.Edge) =
        clipTrim.beginTrimClip(clipId, edge)

    /**
     * Live trim while the handle moves. All the rules (in/out only, magnetic
     * reflow, overlay end pinning, idempotent absolute application) live in
     * [ClipTrimSession]; this only wires state/history/preview/save.
     */
    fun updateTrimClip(cumulativeDeltaMs: Long, fps: Int) =
        clipTrim.updateTrimClip(cumulativeDeltaMs, fps)

    /** Ends the handle drag; autosaves once and refreshes the preview once. */
    fun endTrimClip() = clipTrim.endTrimClip()

    /** Adds a clip by looking up its real duration and waveform from the file. */
    suspend fun addClipFromUri(uriString: String, title: String) =
        importer.addClipFromUri(uriString, title)

    /** Imports multiple URIs sequentially, then calls [onComplete] when all are done. */
    fun importMultipleUris(uris: List<String>, titles: List<String>, onComplete: () -> Unit) =
        importer.importMultipleUris(uris, titles, onComplete)

    /** Launch import of multiple URIs from Composable (non-suspend context). */
    fun launchImportUris(uris: List<android.net.Uri>) = importer.launchImportUris(uris)

    /** Updates the trim of the selected clip. Streams collapse into one undo. */
    fun updateClipTrim(trimStartMs: Long, trimEndMs: Long) =
        ClipPropertyFeature.updateClipTrim(this, trimStartMs, trimEndMs)

    /**
     * Sets a constant speed. Picking a constant speed clears the velocity curve
     * (the two modes are mutually exclusive, like CapCut), and the timeline
     * duration is recomputed through the shared ClipTiming implementation.
     */
    fun setClipSpeed(speed: Float) = ClipPropertyFeature.setClipSpeed(this, speed)

    /** Applies a velocity curve to the selected clip and retimes the timeline. */
    fun setClipSpeedCurve(curve: SpeedCurve) = ClipPropertyFeature.setClipSpeedCurve(this, curve)

    fun setClipFilter(filter: VideoFilter) = ClipPropertyFeature.setClipFilter(this, filter)

    /** Custom `.cube` LUTs imported from storage (Filters → Import .cube LUT). */
    internal val _importedLuts = MutableStateFlow<List<com.example.ui.components.ImportedLut>>(emptyList())
    val importedLuts: StateFlow<List<com.example.ui.components.ImportedLut>> = _importedLuts.asStateFlow()

    /** The currently applied imported LUT on the selected clip, if any. */
    internal val _activeLutId = MutableStateFlow<String?>(null)
    val activeLutId: StateFlow<String?> = _activeLutId.asStateFlow()

    fun addImportedLut(lut: com.example.ui.components.ImportedLut) {
        _importedLuts.value = _importedLuts.value + lut
        _activeLutId.value = lut.id
        setClipFilter(VideoFilter.NONE)
    }

    fun removeImportedLut(id: String) {
        _importedLuts.value = _importedLuts.value.filterNot { it.id == id }
        if (_activeLutId.value == id) _activeLutId.value = null
    }

    fun setActiveLut(lut: com.example.ui.components.ImportedLut?) {
        _activeLutId.value = lut?.id
        if (lut != null) setClipFilter(VideoFilter.NONE)
    }

    /** Per-clip audio effects (voice preset, enhance voice, noise gate). */
    /** Applies or removes GPU motion blur on the selected clip ("Motion blur"). */
    fun toggleMotionBlurOnSelectedClip(enabled: Boolean): Boolean {
        val clipId = _selectedClipId.value ?: return false
        val project = _currentProject.value ?: return false
        val clip = project.allVideoClips.firstOrNull { it.id == clipId } ?: return false
        if (clip.motionBlur == enabled) return true
        updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { c ->
                    if (c.id == clipId) c.copy(motionBlur = enabled) else c
                }
            )
        }
        return true
    }

    /**
     * Applies or removes vocal isolation on the selected video clip. Used by
     * the "Isolate voice" bottom-bar chip; returns false when nothing is selected.
     */
    fun toggleIsolateVoiceOnSelectedClip(enabled: Boolean): Boolean {
        val clipId = _selectedClipId.value ?: return false
        val project = _currentProject.value ?: return false
        val clip = project.allVideoClips.firstOrNull { it.id == clipId } ?: return false
        setClipAudioEffects(clip.audioEffects.copy(isolateVoice = enabled))
        return true
    }

    fun setClipAudioEffects(settings: AudioEffectSettings) =
        ClipPropertyFeature.setClipAudioEffects(this, settings)

    /** Fade in/out of the selected clip's own audio (output milliseconds). */
    fun setClipAudioFades(fadeInMs: Long, fadeOutMs: Long) =
        ClipPropertyFeature.setClipAudioFades(this, fadeInMs, fadeOutMs)

    fun setClipTransition(transition: TransitionType) =
        ClipPropertyFeature.setClipTransition(this, transition)

    /**
     * Applies a transition to one specific clip. The transition picker can
     * be opened for a clip that is not the selected one, so the transition
     * must never fall back to the selection.
     */
    fun setClipTransitionForClip(clipId: String, transition: TransitionType) =
        ClipPropertyFeature.setClipTransitionForClip(this, clipId, transition)

    /**
     * Length of one clip's transition. The render graph draws the
     * transition with this value, so the slider really changes it.
     */
    fun setClipTransitionDuration(clipId: String, durationMs: Long) =
        ClipPropertyFeature.setClipTransitionDuration(this, clipId, durationMs)

    fun setClipAnimation(animation: ClipAnimation) =
        ClipPropertyFeature.setClipAnimation(this, animation)

    /**
     * Length of the selected clip's entry animation. The render graph samples
     * the animation curve against this value, so the slider really changes
     * the animation.
     */
    fun setClipAnimationDuration(durationMs: Long) =
        ClipPropertyFeature.setClipAnimationDuration(this, durationMs)

    /** Keeps (default) or drops the source pitch when the clip is retimed. */
    fun setClipMaintainPitch(maintain: Boolean) =
        ClipPropertyFeature.setClipMaintainPitch(this, maintain)

    fun rotateSelectedClip() = ClipPropertyFeature.rotateSelectedClip(this)

    fun mirrorSelectedClip() = ClipPropertyFeature.mirrorSelectedClip(this)

    fun updateClipAdjustments(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        vignette: Float
    ) = ClipPropertyFeature.updateClipAdjustments(this, brightness, contrast, saturation, vignette)

    /**
     * Per-channel HSL grade of one clip (hue, saturation, luminance).
     *
     * The values live on the clip and are applied by the shared composition,
     * so the preview and the exported file are graded identically.
     */
    fun setClipHslGrade(clipId: String, adjustments: List<HslAdjustment>) =
        ClipEffectFeature.setClipHslAdjustments(this, clipId, adjustments)

    /**
     * Preview-only "compare with the original".
     *
     * While it is on, the preview shows the selected clip without its colour
     * adjustments. The project keeps the grade, so the export is unaffected.
     */
    fun setCompareOriginal(enabled: Boolean) {
        if (_compareOriginal.value == enabled) return
        _compareOriginal.value = enabled
        playerManager?.setCompareOriginal(enabled, if (enabled) _selectedClipId.value else null)
    }

    /**
     * Ends the comparison without rebuilding the preview.
     *
     * Used when the whole project changes: the next load of the project
     * already uses the real grade.
     */
    internal fun clearCompareOriginal() {
        if (!_compareOriginal.value) return
        _compareOriginal.value = false
        playerManager?.setCompareOriginal(false, null, rebuild = false)
    }

    /** Volume slider streams collapse into one undo step. */
    fun setClipVolume(volume: Float) = ClipPropertyFeature.setClipVolume(this, volume)

    /** Absolute opacity for the selected clip; keyframes still override it. */
    fun setClipOpacity(opacity: Float) = ClipPropertyFeature.setClipOpacity(this, opacity)

    /**
     * Absolute crop/zoom transform. Crop presets compute these values with
     * CropMath; storing the absolute scale (rather than a multiplier) keeps
     * repeated preset taps idempotent.
     */
    fun setSelectedClipCrop(scale: Float, posX: Float, posY: Float) =
        ClipPropertyFeature.setSelectedClipCrop(this, scale, posX, posY)

    fun resetSelectedClipCrop() = setSelectedClipCrop(1f, 0.5f, 0.5f)

    /**
     * Absolute rotation of the selected clip, in degrees. The rotation is
     * part of the clip's transform, so preview and export agree.
     */
    fun setSelectedClipRotation(rotationDeg: Float) =
        ClipPropertyFeature.setSelectedClipRotation(this, rotationDeg)

    /**
     * Mirrors the selected clip horizontally. The mirror flag is part of the
     * clip's transform, so preview and export agree.
     */
    fun setSelectedClipMirrored(mirrored: Boolean) =
        ClipPropertyFeature.setSelectedClipMirrored(this, mirrored)

    /**
     * Real loudness normalisation driven by the clip's decoded waveform. The
     * resulting gain is written to MediaClip.volume, which preview playback
     * and the export GainProcessor both read, so the switch is not cosmetic.
     */
    fun setClipLoudnessAdjustment(enabled: Boolean) =
        ClipPropertyFeature.setClipLoudnessAdjustment(this, enabled)

    /**
     * Probes media dimensions for projects imported before source-size
     * persistence existed, so Crop's aspect presets use the real frame.
     */
    fun ensureSelectedClipSourceSize() = ClipPropertyFeature.ensureSelectedClipSourceSize(this)

    /**
     * Lazily decodes the selected clip's waveform. Imported video clips start
     * with an empty envelope for speed, so the Volume panel calls this before
     * offering loudness normalisation.
     */
    fun ensureSelectedClipWaveform() = ClipPropertyFeature.ensureSelectedClipWaveform(this)

    /**
     * Replaces the selected clip's source file while keeping its position on
     * the timeline. Duration, dimensions and the waveform are re-probed so the
     * new media lines up in preview and export instead of keeping stale values.
     */
    fun replaceSelectedClipUri(uriString: String, title: String) =
        ClipPropertyFeature.replaceSelectedClipUri(this, uriString, title)

    // ==========================================
    // Audio Tracks
    // ==========================================

    /**
     * Adds an audio track from a URI (real duration + waveform).
     *
     * [onComplete] receives true only after a durable source was stored and the
     * track was added to the project.
     */
    fun addAudioTrackFromUri(
        uriString: String,
        title: String,
        artist: String = "Unknown",
        onComplete: (Boolean) -> Unit = {}
    ) = AudioFeature.addAudioTrackFromUri(
        this,
        uriString,
        title,
        artist,
        onComplete
    )

    /**
     * Generates a built-in library sample as a durable WAV before adding it.
     * [onComplete] is true only when a renderable track was actually added.
     */
    fun addSampleAudioTrack(
        sample: AudioClip,
        onComplete: (Boolean) -> Unit = {}
    ) = AudioFeature.addSampleAudioTrack(this, sample, onComplete)

    fun addAudioTrack(audioClip: AudioClip) = AudioFeature.addAudioTrack(this, audioClip)

    /**
     * "AI music": composes a track for the whole timeline on-device
     * ([com.example.engine.MusicGenerator], procedural synthesis, no network)
     * and adds it at the playhead like any imported track.
     */
    fun generateAiMusic(mood: com.example.engine.MusicGenerator.Mood, onComplete: (Boolean) -> Unit = {}) {
        val project = _currentProject.value
        if (project == null) {
            onComplete(false)
            return
        }
        val duration = totalDurationMsOf(project).coerceIn(2_000L, 60_000L)
        val seed = System.nanoTime()
        AudioFeature.addSampleAudioTrack(
            this,
            AudioClip(
                id = "ai_music_${mood.name.lowercase()}",
                title = "AI music · ${mood.displayName}",
                artist = "Generated on device",
                durationMs = duration
            ),
            onComplete
        ) { _, renderDurationMs ->
            com.example.engine.MusicGenerator.render(mood, renderDurationMs, seed)
        }
    }

    fun deleteAudioTrack(id: String) = AudioFeature.deleteAudioTrack(this, id)

    /** Selects an independent audio clip and clears video/text selection. */
    fun selectAudioClip(audioId: String?) = SelectionFeature.selectAudioClip(this, audioId)

    /**
     * Opens the per-track mixer. When no track is selected the first track is
     * selected automatically. Returns false when the project has no audio.
     */
    fun openAudioMixer(): Boolean {
        val tracks = _currentProject.value?.audioTracks.orEmpty()
        if (tracks.isEmpty()) return false
        val selected = _selectedAudioId.value?.takeIf { id -> tracks.any { it.id == id } }
            ?: tracks.first().id
        // Audio, video and text are mutually exclusive. Route through the
        // selection feature so opening the mixer cannot leave a stale text or
        // video selection behind.
        SelectionFeature.selectAudioClip(this, selected)
        _activeSheet.value = ActiveToolSheet.AUDIO_MIXER
        return true
    }

    /** Sets one audio track's linear gain (0..2 = 0..200%). */
    fun setAudioVolume(audioId: String, volume: Float) =
        AudioMixerFeature.setAudioVolume(this, audioId, volume)

    /**
     * Stereo balance of one audio track (-1 hard left, 0 centre, +1 hard
     * right). Preview and export share the same composition, so the track is
     * placed in the mix in exactly the same way.
     */
    fun setAudioPan(audioId: String, pan: Float) =
        AudioMixerFeature.setAudioPan(this, audioId, pan)

    /**
     * Three-band EQ of one audio track. The filters run in the shared
     * composition chain, so preview and export are equalised identically.
     */
    fun setAudioEqualizer(audioId: String, equalizer: AudioEqualizer) =
        AudioMixerFeature.setAudioEqualizer(this, audioId, equalizer)

    /**
     * Dynamics compressor of one audio track. The envelope follower runs in
     * the shared composition chain, so preview and export compress
     * identically.
     */
    fun setAudioCompressor(audioId: String, compressor: AudioCompressor) =
        AudioMixerFeature.setAudioCompressor(this, audioId, compressor)

    /**
     * Normalises one audio track's gain to the target loudness from its decoded
     * waveform. False means the track has no analysable waveform yet.
     */
    fun normaliseAudioVolume(audioId: String): Boolean =
        AudioMixerFeature.normaliseAudioVolume(this, audioId)

    /** Sets fade-in/out durations on the output timeline. */
    fun setAudioFades(audioId: String, fadeInMs: Long, fadeOutMs: Long) =
        AudioMixerFeature.setAudioFades(this, audioId, fadeInMs, fadeOutMs)

    /** Changes an audio clip's speed while preserving its source trim. */
    fun setAudioSpeed(audioId: String, speed: Float) =
        AudioMixerFeature.setAudioSpeed(this, audioId, speed)

    /** Changes absolute source in/out points and retimes the clip. */
    fun setAudioTrim(audioId: String, trimStartMs: Long, trimEndMs: Long) =
        AudioMixerFeature.setAudioTrim(this, audioId, trimStartMs, trimEndMs)

    /** Moves an audio clip on the project timeline. */
    fun setAudioTimelineStart(audioId: String, startMs: Long) =
        AudioMixerFeature.setAudioTimelineStart(this, audioId, startMs)

    /** Restores gain, mute, fades, speed and trim for one audio clip. */
    fun resetAudioMixer(audioId: String) = AudioMixerFeature.resetAudioMixer(this, audioId)

    /** Sets one track's audio-effect chain (voice preset, EQ, gate). */
    fun setAudioEffects(audioId: String, settings: AudioEffectSettings) =
        AudioMixerFeature.setAudioEffects(this, audioId, settings)

    /** Sets how often the track's source window repeats (1 = play it once). */
    fun setAudioLoopCount(audioId: String, count: Int) =
        AudioMixerFeature.setAudioLoopCount(this, audioId, count)

    /** Turns auto-ducking of music under a voice-over on or off. */
    fun setAutoDuckEnabled(enabled: Boolean) =
        AudioMixerFeature.setAutoDuckEnabled(this, enabled)

    /** Sets how deep the music dips under a voice-over (0 = no dip, 1 = off). */
    fun setAutoDuckStrength(strength: Float) =
        AudioMixerFeature.setAutoDuckStrength(this, strength)

    /**
     * Splits the selected independent audio track at the playhead. Returns
     * false when the playhead is outside the track or too close to an edge, so
     * the UI can tell the user instead of silently doing nothing.
     */
    fun splitAudioAtPlayhead(): Boolean = AudioMixerFeature.splitAudioAtPlayhead(this)

    /** Extracts the selected clip's audio as a real timeline audio track. */
    fun extractAudioFromSelectedClip() = AudioFeature.extractAudioFromSelectedClip(this)

    /**
     * Unlinks the selected clip's audio into its own audio track (CapCut
     * "Unlink"). Returns false when there is nothing to detach.
     */
    fun detachAudioFromSelectedClip(): Boolean = AudioFeature.detachAudioFromSelectedClip(this)

    /** Starts a real microphone recording. */
    fun startVoiceRecording() = AudioFeature.startVoiceRecording(this)

    /** Stops the recording and adds the clip to the timeline. */
    fun stopAndSaveVoiceRecording() = AudioFeature.stopAndSaveVoiceRecording(this)

    fun cancelVoiceRecording() = AudioFeature.cancelVoiceRecording(this)

    /** Generates a voice-over from text using the device TTS engine. */
    fun startTtsVoiceover(text: String) = AudioFeature.startTtsVoiceover(this, text)

    // ==========================================
    // Text & Subtitles
    // ==========================================

    data class SubtitleImportResult(
        val imported: Int,
        val issues: List<String> = emptyList()
    )

    fun importSubtitles(raw: String) = TextFeature.importSubtitles(this, raw)

    fun exportSubtitles(toVtt: Boolean = false) = TextFeature.exportSubtitles(this, toVtt)

    /**
     * AI auto captions from the user's configured endpoint. Suspends while the
     * clip's audio is extracted and transcribed; the returned string is shown
     * to the user as a toast.
     */
    suspend fun addAutoCaptions(): String = TextFeature.addAutoCaptions(this)

    fun addTextOverlay(
        text: String = "Double tap to edit",
        fontStyle: TextFontStyle = TextFontStyle.CAPCUT_BOLD,
        color: Long = 0xFFFFFFFF,
        animation: TextAnimationType = TextAnimationType.POP_UP
    ) = TextFeature.addTextOverlay(this, text, fontStyle, color, animation)

    fun updateSelectedText(
        text: String,
        fontStyle: TextFontStyle,
        color: Long,
        animation: TextAnimationType
    ) = TextFeature.updateSelectedText(this, text, fontStyle, color, animation)

    /** Bold / italic / letter + line spacing for the selected text overlay. */
    fun setSelectedTextStyle(
        bold: Boolean,
        italic: Boolean,
        letterSpacingEm: Float,
        lineSpacingMultiplier: Float
    ) = TextFeature.setSelectedTextStyle(this, bold, italic, letterSpacingEm, lineSpacingMultiplier)

    /** Adds/replaces a keyframe on the selected text at the playhead. */
    fun setTextKeyframe(
        property: KeyframeProperty,
        value: Float,
        easing: EasingCurve = EasingCurve.EASE_IN_OUT
    ): Boolean = TextFeature.setTextKeyframe(this, property, value, easing)

    fun removeTextKeyframe(keyframeId: String) =
        TextFeature.removeTextKeyframe(this, keyframeId)

    fun clearTextKeyframes() = TextFeature.clearTextKeyframes(this)

    fun deleteSelectedText() = TextFeature.deleteSelectedText(this)

    // ==========================================
    // Stickers & Effects
    // ==========================================

    fun addSticker(symbol: String) = StickerFeature.addSticker(this, symbol)

    fun deleteSticker(id: String) = StickerFeature.deleteSticker(this, id)

    fun addTimelineEffect(effectType: EffectType) =
        TimelineEffectFeature.addTimelineEffect(this, effectType)

    fun deleteTimelineEffect(id: String) =
        TimelineEffectFeature.deleteTimelineEffect(this, id)

    /** Edits a placed effect: duration and intensity (undoable, one update). */
    fun updateTimelineEffect(id: String, durationMs: Long, intensityScale: Float) =
        updateProject { proj ->
            proj.copy(
                effects = proj.effects.map { fx ->
                    if (fx.id == id) {
                        fx.copy(
                            durationMs = durationMs.coerceIn(300L, 30_000L),
                            intensityScale = intensityScale.coerceIn(0.2f, 2f)
                        )
                    } else fx
                }
            )
        }

    fun setAspectRatio(ratio: AspectRatioType) = CanvasFeature.setAspectRatio(this, ratio)

    fun setPaperStyle(style: String) = CanvasFeature.setPaperStyle(this, style)

    /** Sets (or, with an empty string, clears) the canvas background image. */
    fun setBackgroundImage(uriString: String) =
        CanvasFeature.setBackgroundImage(this, uriString)

    /** Sets what fills the letterbox bars: "color", "image" or "blur". */
    fun setBackgroundMode(mode: String) = CanvasFeature.setBackgroundMode(this, mode)

    /** Copies a picked gallery image into app storage and applies it. */
    fun importBackgroundImage(picked: Uri): String? =
        context?.let { CanvasFeature.importBackgroundImage(this, it, picked) }

    /** Copies a picked audio file into app storage. Returns the stored path. */
    fun importBrandMusic(picked: Uri): String? =
        context?.let { AudioFeature.importBrandAudio(it, picked) }

    /** Stores (or clears) the reusable brand music path. */
    fun saveBrandMusic(path: String) {
        _brandMusicPath.value = path
        viewModelScope.launch { settingsRepository?.setBrandMusic(path) }
    }

    /**
     * Brand background: stores [path] as the reusable brand image and applies
     * it to the current project. An empty path clears the brand only.
     */
    fun saveBrandBackground(path: String) {
        _brandBackgroundPath.value = path
        viewModelScope.launch { settingsRepository?.setBrandBackground(path) }
        if (path.isNotBlank()) {
            setBackgroundImage(path)
            setBackgroundMode("image")
        }
    }

    /**
     * Stores the project cover: a timeline frame and/or a custom image. Both the
     * home thumbnail and the cover editor read it back.
     */
    fun setProjectCover(frameMs: Long, imageUri: String) {
        com.example.data.ProjectThumbnailLoader.clear()
        updateProject { proj ->
            proj.copy(
                coverFrameMs = frameMs.coerceAtLeast(0L),
                coverImageUri = imageUri
            )
        }
    }

    /**
     * Applies the "Original" canvas ratio (the ratio of the imported footage).
     * Returns the applied ratio, or `null` when the source size still has to
     * be probed from the media file.
     */
    fun applyOriginalAspectRatio(): AspectRatioType? =
        CanvasFeature.applyOriginalAspectRatio(this)

    // ==========================================
    // Templates
    // ==========================================

    /**
     * Applies a built-in template: a new draft is created from the template
     * and opened, so the current project is kept untouched.
     */
    fun applyTemplate(template: ViralTemplate) = TemplateFeature.applyTemplate(this, template)

    // ==========================================
    // Export & Rendering Pipeline
    // ==========================================

    // ==========================================
    // Export settings (AI UHD dropdown)
    // ==========================================
    // The settings model lives in com.example.export.ExportSettings.

    internal val _exportSettings = MutableStateFlow(ExportSettings())
    val exportSettings: StateFlow<ExportSettings> = _exportSettings.asStateFlow()

    fun updateExportSettings(settings: ExportSettings) =
        ExportFeature.updateExportSettings(this, settings)

    /**
     * One-click export with the pre-selected settings. The GIF tab renders
     * through the video pipeline at the matching resolution until a dedicated
     * GIF encoder lands in ExportManager.
     */
    fun startSelectedExport() = ExportFeature.startSelectedExport(this)

    fun startExport(
        resolution: String = _defaultResolution.value,
        fps: Int = _defaultFps.value,
        bitrateMbps: Int = 10
    ) = ExportFeature.startExport(this, resolution, fps, bitrateMbps)

    /** Renders the audio only (.m4a) with the current mix. */
    fun startAudioOnlyExport() = ExportFeature.startAudioOnlyExport(this)

    /** Renders a single audio track (.m4a) without the video or other tracks. */
    fun exportAudioTrack(audioId: String) = ExportFeature.startAudioTrackExport(this, audioId)

    fun dismissExport() = ExportFeature.dismissExport(this)

    /** Starts a background export with explicit context (called from UI). */
    fun startBackgroundExportWithContext(
        context: Context,
        resolution: String = _defaultResolution.value,
        fps: Int = _defaultFps.value,
        bitrateMbps: Int = 10
    ) = ExportFeature.startBackgroundExportWithContext(
        this, context, resolution, fps, bitrateMbps
    )

    /** Stops an in-flight render and closes the export sheet. */
    fun cancelExport() = ExportFeature.cancelExport(this)

    // ==========================================
    // Phase 1 Actions: Keyframes, PIP, Blend & Mask
    // ==========================================

    fun addKeyframe(
        clipId: String,
        property: KeyframeProperty,
        value: Float,
        easing: EasingCurve = EasingCurve.EASE_IN_OUT
    ) = KeyframeFeature.addKeyframe(this, clipId, property, value, easing)

    fun removeKeyframe(clipId: String, keyframeId: String) =
        KeyframeFeature.removeKeyframe(this, clipId, keyframeId)

    /**
     * The keyframe before/after the playhead of one clip, as a timeline
     * position. A missing neighbour is null.
     */
    fun keyframeNeighbours(clipId: String, timelinePositionMs: Long): Pair<Long?, Long?> =
        KeyframeFeature.keyframeNeighbours(this, clipId, timelinePositionMs)

    /** Toggles a keyframe at the current playhead position on the selected clip. */
    fun toggleKeyframeAtPlayhead() = KeyframeFeature.toggleKeyframeAtPlayhead(this)

    /**
     * Toggles a slow push-in (Ken Burns) on the selected clip. Returns a short
     * status for the toast so a blocked toggle is explained, not silent.
     */
    fun toggleKenBurns(): String = KeyframeFeature.toggleKenBurns(this)

    /**
     * Adds a PIP/overlay clip. The source duration is probed from the real
     * media file instead of assuming four seconds, otherwise the overlay would
     * be cut short (or stretched into black) in both preview and export.
     */
    fun addOverlayClipFromUri(uriString: String, title: String) =
        OverlayClipFeature.addOverlayClipFromUri(this, uriString, title)

    /** Updates the PIP transform used by the compositor and keyframe sheet. */
    fun updateOverlayTransform(
        clipId: String,
        scale: Float? = null,
        posX: Float? = null,
        posY: Float? = null,
        opacity: Float? = null
    ) = OverlayClipFeature.updateOverlayTransform(this, clipId, scale, posX, posY, opacity)

    fun setClipBlendMode(clipId: String, mode: BlendMode) =
        ClipEffectFeature.setClipBlendMode(this, clipId, mode)

    fun setClipMask(clipId: String, maskData: MaskData) =
        ClipEffectFeature.setClipMask(this, clipId, maskData)

    fun setClipHslAdjustments(clipId: String, adjustments: List<HslAdjustment>) =
        ClipEffectFeature.setClipHslAdjustments(this, clipId, adjustments)

    /**
     * Beat-sync: splits clips at musical beats measured from the decoded audio.
     * Returns the result for the calling screen's toast; an unreadable file or
     * a beatless track is reported honestly instead of cutting at made-up beats.
     */
    suspend fun autoCutToBeat(): String = BeatSyncFeature.autoCutToBeat(this)

    /**
     * Pins a beat by hand at [atMs]. Pinned beats appear on the ruler and act as
     * snap magnets next to the detected ones. Returns the result for the toast.
     */
    fun pinBeatAt(atMs: Long): String = ManualBeatsFeature.pin(this, atMs)

    /** Removes the pinned beat nearest to [atMs]. Returns the result for the toast. */
    fun unpinBeatAt(atMs: Long): String = ManualBeatsFeature.unpin(this, atMs)

    /** Removes every hand-pinned beat. Returns the result for the toast. */
    fun clearManualBeats(): String = ManualBeatsFeature.clear(this)

    /** Exports a real MP4 and reports where it can be shared. */
    fun shareExportMessage(platform: String): String =
        ExportFeature.shareExportMessage(this, platform)
}