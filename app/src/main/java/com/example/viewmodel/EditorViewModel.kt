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
import com.example.viewmodel.features.TextFeature
import com.example.viewmodel.features.StickerFeature
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
import com.example.viewmodel.features.ClipPropertyFeature
import com.example.viewmodel.features.ClipTransformFeature
import com.example.viewmodel.features.SettingsFeature
import com.example.viewmodel.features.ProjectLifecycleFeature
import com.example.viewmodel.features.SelectionFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    BACKGROUND,
    COVER_EDITOR
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
) {
    /** Source-media length the clip actually consumes. */
    val sourceWindowMs: Long
        get() = if (trimEndMs > trimStartMs) {
            (trimEndMs - trimStartMs).coerceAtLeast(1L)
        } else {
            (durationMs * speed.coerceAtLeast(0.01f)).toLong().coerceAtLeast(1L)
        }
}

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

    private fun totalDurationMsOf(project: VideoProject): Long {
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

    internal val _activeSheet = MutableStateFlow(ActiveToolSheet.NONE)
    val activeSheet: StateFlow<ActiveToolSheet> = _activeSheet.asStateFlow()

    internal val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    /** Detected beat positions (ms) used as magnetic snap targets on the timeline. */
    internal val _snapBeats = MutableStateFlow<List<Long>>(emptyList())
    val snapBeats: StateFlow<List<Long>> = _snapBeats.asStateFlow()

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

    internal val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    internal val _cacheSizeMb = MutableStateFlow(0)
    val cacheSizeMb: StateFlow<Int> = _cacheSizeMb.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
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

    fun setDefaultFps(fps: Int) = SettingsFeature.setDefaultFps(this, fps)

    fun togglePaperGrid(enabled: Boolean) = SettingsFeature.togglePaperGrid(this, enabled)

    fun toggleWatermark(enabled: Boolean) = SettingsFeature.toggleWatermark(this, enabled)

    fun refreshCacheSize() = SettingsFeature.refreshCacheSize(this)

    fun clearCache() = SettingsFeature.clearCache(this)

    // Voice recording with real MediaRecorder
    internal val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()
    internal val _voiceRecordingDurationMs = MutableStateFlow(0L)
    val voiceRecordingDurationMs: StateFlow<Long> = _voiceRecordingDurationMs.asStateFlow()

    // Undo / Redo history stacks
    internal val undoStack = mutableListOf<EditorHistoryEntry>()
    internal val redoStack = mutableListOf<VideoProject>()

    /**
     * Durable copy of the undo history. The in-memory stacks die with the
     * process; this journal is written atomically to disk so a crash no longer
     * destroys every undo step of the session.
     */
    private val undoJournal: UndoJournal? by lazy {
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

    private var playbackJob: Job? = null

    // ------------------------------------------------------------------
    // Live scrub preview (source-frame extraction)
    // ------------------------------------------------------------------
    // CompositionPlayer 1.11.1 stops its render loop for the whole duration of
    // every seek, so seeking with the timeline kept the preview black until the
    // drag stopped. The scrub preview therefore does NOT touch the player at
    // all: frames are decoded straight from the source media and drawn over the
    // preview. The player is only rebuilt/seeked when the user presses Play,
    // where a short prepare is hidden behind the still frame.
    /**
     * Two independent decoder lanes, both decoding frame-exact pictures.
     * Per-frame precision was explicitly requested for the split workflow: a
     * keyframe preview freezes inside a GOP and only jumps at GOP boundaries,
     * so the user cannot tell which frame a split will land on. The lanes
     * differ in purpose, not accuracy:
     *  - the drag lane follows the moving playhead;
     *  - the settle lane decodes the frame the gesture stopped on with its own
     *    decoder, so a long in-flight drag decode can never delay it.
     */
    private val scrubFrameExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "catbyte-scrub-frame").apply { isDaemon = true }
    }
    private val scrubFrameDispatcher = scrubFrameExecutor.asCoroutineDispatcher()
    private val scrubFrameLoader = ScrubFrameLoader()

    private val scrubExactExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "catbyte-scrub-exact").apply { isDaemon = true }
    }
    private val scrubExactDispatcher = scrubExactExecutor.asCoroutineDispatcher()
    /** Only ever touches the clip under the final playhead. */
    private val scrubExactLoader = ScrubFrameLoader(maxCachedRetrievers = 1)

    /**
     * One conflated slot per lane: while the finger moves only the newest
     * playhead is worth decoding. A fling overwrites the pending request instead
     * of allocating a coroutine per scroll event, which is what made the
     * timeline itself stutter.
     */
    private val scrubFrameRequests = Channel<ScrubFrameRequest>(Channel.CONFLATED)
    private val scrubExactRequests = Channel<ScrubFrameRequest>(Channel.CONFLATED)
    private var scrubFrameWorker: Job? = null
    private var scrubExactWorker: Job? = null
    /** Incremented when the overlay is cleared so stale decodes are dropped. */
    private var scrubFrameGeneration = 0
    private var scrubFrameHandoffJob: Job? = null
    /** Watches a preview reloaded by an edit made while a scrub still is up. */
    private var scrubEditHandoffJob: Job? = null
    private var lastScrubRequestedMs = Long.MIN_VALUE
    /**
     * Position of the frame currently on screen ([ScrubFramePolicy.NOTHING_PUBLISHED]
     * until the first publish of a gesture). Feeds the staleness rule that
     * keeps late decode results from dragging the picture backwards.
     */
    private var lastPublishedScrubMs = ScrubFramePolicy.NOTHING_PUBLISHED
    private val _scrubPreviewFrame = MutableStateFlow<ScrubPreviewFrame?>(null)
    /** Source frame drawn over the preview while the timeline is scrubbed. */
    val scrubPreviewFrame: StateFlow<ScrubPreviewFrame?> = _scrubPreviewFrame.asStateFlow()

    /**
     * One frame decode request. [final] selects the settle lane (the frame the
     * gesture stopped on); both lanes decode frame-exact pictures.
     */
    private data class ScrubFrameRequest(
        val project: VideoProject,
        val positionMs: Long,
        val final: Boolean,
        val generation: Int
    )

    /** Debounced "gesture over" job that decodes the frame-exact still. */
    private var scrubSettleJob: Job? = null
    /** Latest playhead the scrub preview should show. */
    private var scrubTargetMs = 0L
    /**
     * True while the player has not landed on the scrub playhead yet. Playback
     * must seek the player to the UI playhead instead of trusting its position.
     */
    private var scrubSettlePending = false
    /** Bounded self-healing attempts for a frozen preview. */
    private companion object {
        const val MAX_READY_REBUILDS = 2
        const val RECOVERY_RESET_MS = 1_000L

        /**
         * How long the timeline must stay quiet before the settle lane decodes
         * the stopped-at frame with its dedicated decoder. The player is not
         * touched at all: the decoded frame simply sharpens the overlay.
         */
        const val SCRUB_SETTLE_MS = 180L

        /**
         * Small playhead moves reuse the frame already on screen. Without this,
         * a slow drag decoded a new frame every few milliseconds of movement,
         * which wasted the decoder and competed with the UI thread.
         */
        const val SCRUB_FRAME_MIN_DELTA_MS = 32L

        /** Poll interval for the playback hand-over watcher. */
        const val SCRUB_POLL_MS = 24L

        /**
         * The extracted frame stays up briefly after the player reports READY so
         * the surface has time to draw the first playback frame before the
         * hand-over.
         */
        const val SCRUB_FRAME_HANDOFF_MS = 120L

        /**
         * The hand-over waits until playback has actually advanced by this much,
         * i.e. frames are really being rendered. A READY state alone can still
         * belong to the old position while a seek is in flight.
         */
        const val SCRUB_HANDOFF_MIN_ADVANCE_MS = 32L

        /** Never leave the extracted frame covering the preview forever. */
        const val SCRUB_FRAME_HANDOFF_TIMEOUT_MS = 1_500L

        /**
         * Playback only re-syncs the player to the UI playhead when the two
         * really diverged (a project switch). A scrub legitimately leaves the
         * player a few frames ahead of the playhead, and seeking back there is
         * what parked the preview in BUFFERING.
         */
        const val PLAYHEAD_RESYNC_TOLERANCE_MS = 750L
    }
    internal var voiceRecordJob: Job? = null
    internal var exportJob: Job? = null
    private var importJob: Job? = null

    /** Preview rebuild policy lives in its own file (PreviewRefreshScheduler). */
    private val previewScheduler = PreviewRefreshScheduler(
        scope = viewModelScope,
        projectProvider = { _currentProject.value },
        refresh = { project -> refreshPreviewComposition(project) }
    )

    /**
     * Rebuilds the render graph after the user stops dragging/sliding. The
     * composition is identical to the export graph, so a short debounce keeps
     * sliders smooth without allowing preview/export drift.
     */
    private fun schedulePreviewRefresh() {
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
        val scrubVisible = _scrubPreviewFrame.value != null || scrubSettlePending
        if (!scrubVisible) {
            manager.loadProject(project)
            return
        }
        val at = _currentPositionMs.value
        // A fresh player at the visible playhead is the proven path: seeking or
        // re-preparing the paused CompositionPlayer is exactly what wedged it
        // during the scrub. The still hides the rebuild, so nothing flashes.
        manager.loadProject(project, startPositionMs = at, forceRebuild = true)
        awaitScrubEditHandoff(manager, at)
    }

    /**
     * Waits for the composition reloaded by an edit to reach READY at the
     * visible playhead, then removes the scrub still.
     *
     * Deliberately conservative: it never pulls the still out from under a
     * live drag, and on timeout it keeps the still (a correct picture) rather
     * than exposing a player that may still be buffering. Pressing Play then
     * takes the proven rebuild path.
     */
    private fun awaitScrubEditHandoff(
        manager: com.example.data.PlayerManager,
        positionMs: Long
    ) {
        scrubEditHandoffJob?.cancel()
        val reloadedPlayer = manager.activePlayer.value ?: return
        scrubEditHandoffJob = viewModelScope.launch {
            var waited = 0L
            while (waited < SCRUB_FRAME_HANDOFF_TIMEOUT_MS) {
                if (_isPlaying.value) return@launch
                val ready = manager.activePlayer.value === reloadedPlayer &&
                    reloadedPlayer.playbackState == androidx.media3.common.Player.STATE_READY
                val quiet = scrubSettleJob?.isActive != true &&
                    _currentPositionMs.value == positionMs
                if (ready && quiet) {
                    // Give the surface time to draw the refreshed composition
                    // before the still disappears.
                    delay(SCRUB_FRAME_HANDOFF_MS)
                    if (!_isPlaying.value &&
                        scrubSettleJob?.isActive != true &&
                        _currentPositionMs.value == positionMs
                    ) {
                        scrubSettlePending = false
                        clearScrubFrame()
                    }
                    return@launch
                }
                delay(SCRUB_POLL_MS)
                waited += SCRUB_POLL_MS
            }
            AppLog.warn(
                "EditorViewModel",
                "Refreshed preview not ready at ${positionMs}ms; keeping the scrub still"
            )
        }
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
                    _appLanguage.value = settings.language
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
                        _snapBeats.value = emptyList()
                        return@collectLatest
                    }
                    val sourceBeats = BeatDetector.detectBeats(
                        ctx,
                        source.uri,
                        source.sourceWindowMs
                    )
                    _snapBeats.value = BeatGrid.timelineBeats(
                        sourceBeats = sourceBeats,
                        startTimelineMs = source.startTimelineMs,
                        trimStartMs = source.trimStartMs,
                        speed = source.speed,
                        durationMs = source.durationMs
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelScrubPreview()
        playbackJob?.cancel()
        voiceRecordJob?.cancel()
        exportJob?.cancel()
        importJob?.cancel()
        previewScheduler.cancel()
        voiceRecorder?.cancel()
        speechSynthesizer?.release()
        scrubFrameWorker?.cancel()
        scrubFrameWorker = null
        scrubExactWorker?.cancel()
        scrubExactWorker = null
        scrubFrameRequests.close()
        scrubExactRequests.close()
        // Release the decoders on their own threads AFTER any in-flight decode;
        // shutdown() drains the queue and then stops the thread.
        scrubFrameExecutor.execute { scrubFrameLoader.clear() }
        scrubFrameExecutor.shutdown()
        scrubExactExecutor.execute { scrubExactLoader.clear() }
        scrubExactExecutor.shutdown()
        // PlayerManager is application-scoped (the editor UI reads it directly).
        // Releasing it here would leave a dead player when the activity is
        // recreated after finishing; just stop playback instead.
        playerManager?.pause()
    }

    fun openProject(project: VideoProject) = ProjectLifecycleFeature.openProject(this, project)

    fun toggleProjectFavorite(id: String) = ProjectLifecycleFeature.toggleProjectFavorite(this, id)

    fun createNewProjectWithParams(
        title: String,
        ratio: AspectRatioType = AspectRatioType.RATIO_9_16,
        paperStyle: String = "Standard"
    ) = ProjectLifecycleFeature.createNewProjectWithParams(this, title, ratio, paperStyle)

    fun createNewProject() = ProjectLifecycleFeature.createNewProject(this)

    fun duplicateProject(id: String) = ProjectLifecycleFeature.duplicateProject(this, id)

    fun deleteProject(id: String) = ProjectLifecycleFeature.deleteProject(this, id)

    /** Restores a draft removed from the home screen (undo delete). */
    fun restoreProject(project: VideoProject) = ProjectLifecycleFeature.restoreProject(this, project)

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

    fun copySelectedOverlays() = SelectionFeature.copySelectedOverlays(this)

    /** Pastes the clipboard at the playhead, keeping the copied relative offsets. */
    fun pasteOverlays() = SelectionFeature.pasteOverlays(this)

    fun selectText(textId: String?) = SelectionFeature.selectText(this, textId)

    fun setTimelineZoom(zoom: Float) = SelectionFeature.setTimelineZoom(this, zoom)

    // ==========================================
    // Playback Engine
    // ==========================================

    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        val project = _currentProject.value ?: return
        val totalMs = totalDurationMsOf(project)
        var restartFromEnd = false
        if (_currentPositionMs.value >= totalMs) {
            _currentPositionMs.value = 0L
            restartFromEnd = true
        }
        _isPlaying.value = true
        AppLog.info("EditorViewModel", "startPlayback() at ${_currentPositionMs.value}ms")

        val manager = playerManager
        if (manager != null) {
            // The timeline may have been scrubbed without the player following
            // it: that is the whole point of the extracted-frame overlay, the
            // player is left alone until the gesture ends. Seek it to the
            // VISIBLE playhead before playing. syncPosition() must not run here
            // in that case — it would overwrite the playhead with the player's
            // stale position.
            val pendingScrub = scrubSettlePending ||
                scrubSettleJob?.isActive == true ||
                restartFromEnd
            scrubSettlePending = false
            scrubSettleJob?.cancel()
            scrubSettleJob = null

            // loadProject() is a no-op when the same project is already prepared.
            manager.loadProject(project)

            if (pendingScrub) {
                // CompositionPlayer 1.11.1 wedges on a paused seek of more than
                // ~100 ms (every larger scrub seek stayed BUFFERING forever).
                // The proven path is a FRESH player prepared at the visible
                // playhead. The extracted frame covers the preview while it
                // prepares, so there is no black flash — and, unlike the old
                // beginScrub()+seek path, no failed seek to raise a preview
                // error. The playhead follows the safe-start snap.
                if (manager.usesCompositionPreview.value) {
                    manager.rebuildPreviewAt(_currentPositionMs.value)
                    _currentPositionMs.value = manager.currentPositionMs.value
                } else {
                    // The ExoPlayer fallback seeks cheaply and never blacks out.
                    manager.beginScrub()
                    manager.seekTo(_currentPositionMs.value)
                    manager.endScrubKeepPlaying()
                }
            } else {
                manager.endScrubKeepPlaying()
                // Only re-sync for a REAL divergence (e.g. a project switch reset
                // the UI playhead): a seek to the SAME position while paused
                // flushes CompositionPlayer's audio graph and the following
                // play() starts silent (androidx/media#3482).
                manager.syncPosition()
                val drift = kotlin.math.abs(
                    manager.currentPositionMs.value - _currentPositionMs.value
                )
                if (drift > PLAYHEAD_RESYNC_TOLERANCE_MS) {
                    manager.beginScrub()
                    manager.seekTo(_currentPositionMs.value)
                    manager.endScrubKeepPlaying()
                }
            }
            // Keep the extracted frame up until the real surface shows the frame
            // playback starts from; otherwise the hand-over flashes black.
            scheduleScrubFrameHandoff(manager)
            // play() itself defers while the player is BUFFERING; the polling
            // loop below issues it as soon as the player reports READY.
            manager.play()
            startPlaybackPolling(totalMs, manager)
        } else {
            playbackJob?.cancel()
            playbackJob = viewModelScope.launch {
                val stepMs = 50L
                while (isActive && _isPlaying.value) {
                    delay(stepMs)
                    val newPos = _currentPositionMs.value + stepMs
                    if (newPos >= totalMs) {
                        _currentPositionMs.value = totalMs
                        _isPlaying.value = false
                        break
                    } else {
                        _currentPositionMs.value = newPos
                    }
                }
            }
        }
    }

    /**
     * Hands the preview from the extracted scrub frame back to the real player
     * surface.
     *
     * The player can report READY a frame or two before the surface has drawn
     * the new frame, so the overlay is held for [SCRUB_FRAME_HANDOFF_MS]. The
     * timeout guarantees a failed player can never leave a still frame covering
     * a live preview forever.
     */
    private fun scheduleScrubFrameHandoff(
        manager: com.example.data.PlayerManager
    ) {
        if (_scrubPreviewFrame.value == null) return
        scrubFrameHandoffJob?.cancel()
        scrubFrameHandoffJob = viewModelScope.launch {
            val startPositionMs = manager.activePlayer.value?.currentPosition ?: 0L
            var waited = 0L
            while (waited < SCRUB_FRAME_HANDOFF_TIMEOUT_MS && _isPlaying.value) {
                val player = manager.activePlayer.value
                val ready = player?.playbackState == androidx.media3.common.Player.STATE_READY
                val advanced = player != null &&
                    kotlin.math.abs(player.currentPosition - startPositionMs) >=
                    SCRUB_HANDOFF_MIN_ADVANCE_MS
                if (ready && advanced) break
                delay(SCRUB_POLL_MS)
                waited += SCRUB_POLL_MS
            }
            delay(SCRUB_FRAME_HANDOFF_MS)
            if (_isPlaying.value) clearScrubFrame()
        }
    }

    /**
     * Drives the playhead from the real player and self-heals a frozen preview.
     *
     * Two failure modes live here:
     *  1. `play()` landing while the player is still BUFFERING from a scrub
     *     seek freezes CompositionPlayer — the device log shows every
     *     BUFFERING-state play() frozen and every READY-state play() smooth.
     *     startPlayback() therefore hands over to phase 1 below, which issues
     *     the real `play()` only once the player reports STATE_READY.
     *  2. CompositionPlayer can freeze with a stuck playhead even after a
     *     READY start (androidx/media#3452). Nudge seeks never rendered a
     *     frame, so the recovery is a brand-new player rebuilt at the playhead
     *     ([PlayerManager.rebuildPreviewAt]) — the only path proven to reach
     *     READY again.
     */
    private fun startPlaybackPolling(totalMs: Long, manager: com.example.data.PlayerManager) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var tick = 0
            var readyTicks = 0
            var watchdogTicks = 0
            var playIssued = false
            var rebuilds = 0
            var recoveries = 0
            var lastRecoveryPosition = 0L
            var stalledTicks = 0
            var bufferingTicks = 0
            var lastPosition = _currentPositionMs.value
            while (isActive && _isPlaying.value) {
                // The playhead follows the player at display rate (~60 Hz).
                // With the old 50 ms poll the timeline moved in visible 20 Hz
                // steps, which read as "laggy" even when the playback itself
                // was perfectly smooth. The recovery watchdog keeps its old
                // ~50 ms cadence (it runs every WATCHDOG_STRIDE position
                // ticks), so all of its budgets mean exactly what they meant
                // before.
                delay(PlayerRecoveryPolicy.POSITION_TICK_MS)
                manager.syncPosition()
                val position = manager.currentPositionMs.value
                _currentPositionMs.value = position
                val state = manager.playbackState.value
                val error = manager.lastError.value

                if (PlayerRecoveryPolicy.hasReachedEnd(position, totalMs, state)) {
                    _isPlaying.value = false
                    clearScrubFrame()
                    break
                }

                // ---- Phase 1: reach STATE_READY at the playhead, THEN play ----
                if (!playIssued) {
                    readyTicks++
                    when {
                        state == androidx.media3.common.Player.STATE_READY -> {
                            // A deferred play (the LIVE state was still
                            // BUFFERING even though the cached state says
                            // READY) must not be marked as issued — otherwise
                            // the preview would stay silent forever.
                            if (manager.play()) {
                                playIssued = true
                                tick = 0
                                readyTicks = 0
                                stalledTicks = 0
                                bufferingTicks = 0
                                lastPosition = manager.currentPositionMs.value
                                _currentPositionMs.value = lastPosition
                            }
                        }

                        state == androidx.media3.common.Player.STATE_IDLE && error != null -> {
                            // Broken player: rebuild right away instead of
                            // waiting out the whole READY timeout.
                            rebuilds++
                            readyTicks = 0
                            AppLog.warn(
                                "EditorViewModel",
                                "Preview player broken; rebuilding at ${_currentPositionMs.value}ms"
                            )
                            manager.rebuildPreviewAt(_currentPositionMs.value)
                        }

                        readyTicks > PlayerRecoveryPolicy.WAIT_READY_POSITION_TICKS -> {
                            if (rebuilds < MAX_READY_REBUILDS) {
                                rebuilds++
                                readyTicks = 0
                                AppLog.warn(
                                    "EditorViewModel",
                                    "Preview never became READY after scrub; rebuilding at ${_currentPositionMs.value}ms"
                                )
                                manager.rebuildPreviewAt(_currentPositionMs.value)
                            } else {
                                AppLog.error(
                                    "EditorViewModel",
                                    "Preview player never became READY; stopping playback"
                                )
                                _isPlaying.value = false
                                clearScrubFrame()
                                break
                            }
                        }
                    }
                    continue
                }

                // ---- Phase 2: playing — the watchdog runs at the slow cadence ----
                watchdogTicks++
                if (watchdogTicks < PlayerRecoveryPolicy.WATCHDOG_STRIDE) {
                    // Fast path: only the playhead moves. The freeze watchdog
                    // below needs its own cadence, otherwise a 300 ms stall
                    // would be detected after 100 ms.
                    lastPosition = position
                    continue
                }
                watchdogTicks = 0
                tick++
                val graceOver = tick > PlayerRecoveryPolicy.STARTUP_GRACE_TICKS
                if (graceOver &&
                    state == androidx.media3.common.Player.STATE_IDLE &&
                    error != null
                ) {
                    _isPlaying.value = false
                    clearScrubFrame()
                    break
                }

                // A player that keeps buffering will never resume on its own:
                // rebuild it at the playhead instead of waiting forever.
                if (state == androidx.media3.common.Player.STATE_BUFFERING) {
                    bufferingTicks++
                    if (bufferingTicks >= PlayerRecoveryPolicy.WAIT_BUFFERING_WATCHDOG_TICKS &&
                        rebuilds < MAX_READY_REBUILDS
                    ) {
                        rebuilds++
                        bufferingTicks = 0
                        AppLog.warn(
                            "EditorViewModel",
                            "Preview stayed buffering for " +
                                "${PlayerRecoveryPolicy.WAIT_BUFFERING_WATCHDOG_TICKS} ticks; " +
                                "rebuilding at ${position}ms"
                        )
                        manager.rebuildPreviewAt(position)
                        playIssued = false
                        tick = 0
                        stalledTicks = 0
                        lastPosition = manager.currentPositionMs.value
                        continue
                    }
                } else {
                    bufferingTicks = 0
                }

                // `isPlaying` is deliberately NOT part of this test:
                // CompositionPlayer reports isPlaying=true while frames are
                // frozen (#3452). Only a non-advancing playhead counts.
                //
                // A BUFFERING player is not a freeze: at a clip cut the incoming
                // item is still loading, the playhead legitimately stops, and
                // CompositionPlayer resumes by itself once every item is READY
                // again. Counting that as a stall made the watchdog nudge-seek
                // at every transition — the playhead crawled and got stuck.
                val stallCandidate = PlayerRecoveryPolicy.countsAsStallCandidate(state)
                if (graceOver && stallCandidate && position <= lastPosition + 1L) {
                    stalledTicks++
                } else {
                    stalledTicks = 0
                }
                if (PlayerRecoveryPolicy.shouldRecoverStall(
                        startupGraceOver = graceOver,
                        stalledTicks = stalledTicks
                    )
                ) {
                    stalledTicks = 0
                    // A freeze the playhead escaped from long ago starts a new
                    // recovery budget.
                    if (position - lastRecoveryPosition > RECOVERY_RESET_MS) recoveries = 0
                    lastRecoveryPosition = position
                    recoveries++
                    when {
                        rebuilds < MAX_READY_REBUILDS -> {
                            rebuilds++
                            AppLog.warn(
                                "EditorViewModel",
                                "Playback froze at ${position}ms; rebuilding the preview player"
                            )
                            manager.rebuildPreviewAt(position)
                            playIssued = false
                        }

                        else -> {
                            AppLog.error(
                                "EditorViewModel",
                                "Preview keeps freezing at ${position}ms; stopping playback"
                            )
                            _isPlaying.value = false
                            clearScrubFrame()
                            break
                        }
                    }
                    // Recovery seeks are asynchronous: restart the grace window
                    // so stale positions from the seek cannot trigger another
                    // recovery immediately. PlayerManager applies the target to
                    // its own position flow synchronously.
                    tick = 0
                    lastPosition = manager.currentPositionMs.value
                    _currentPositionMs.value = lastPosition
                    continue
                }
                lastPosition = position
            }
        }
    }

    fun pausePlayback() {
        val wasPlaying = _isPlaying.value
        if (wasPlaying) {
            AppLog.info("EditorViewModel", "pausePlayback() at ${_currentPositionMs.value}ms")
        }
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        scrubFrameHandoffJob?.cancel()
        scrubFrameHandoffJob = null
        val manager = playerManager ?: return
        manager.pause()
        // Only adopt the player's position when it was actually the source of
        // truth (the user paused playback). Pressing the timeline while already
        // paused must NOT snap the playhead back to the player's pre-scrub
        // position: the extracted frame on screen is the visible playhead.
        if (!wasPlaying) {
            beginScrubFrameGesture()
            return
        }
        manager.syncPosition()
        _currentPositionMs.value = manager.currentPositionMs.value
        clearScrubFrame()
    }

    /**
     * Starts a new scrub gesture without removing the frame already on screen.
     * Decodes queued for the previous playhead are dropped so they cannot flash
     * back while the finger is already somewhere else.
     */
    private fun beginScrubFrameGesture() {
        scrubFrameGeneration++
        while (scrubFrameRequests.tryReceive().isSuccess) {
            // Drain the conflated slot.
        }
        while (scrubExactRequests.tryReceive().isSuccess) {
            // A pending settle still belongs to the previous gesture.
        }
        lastScrubRequestedMs = Long.MIN_VALUE
        lastPublishedScrubMs = ScrubFramePolicy.NOTHING_PUBLISHED
        // The preview is about to be driven by decoded source frames; a stale
        // player error from an earlier recovery must not sit over the scrub.
        playerManager?.clearPreviewError()
    }

    fun seekTo(positionMs: Long) {
        val project = _currentProject.value ?: return
        // Cached: this runs on every scroll event.
        val clamped = positionMs.coerceIn(0L, totalDurationMsOf(project))
        _currentPositionMs.value = clamped
        scrubTargetMs = clamped

        val manager = playerManager
        if (_isPlaying.value) {
            // Playback already keeps the render loop alive, so the composition
            // can follow the seek directly. The extracted-frame overlay exists
            // only for a PAUSED player, whose seek blacks the surface out.
            manager?.seekTo(clamped)
            return
        }

        scrubSettlePending = true
        requestScrubFrame(project, clamped)
        scheduleScrubSettle()
    }

    /**
     * Decodes the source frame under [positionMs] and draws it over the preview.
     *
     * This is what makes the timeline scroll show real frames in realtime: the
     * player is not seeked at all during the drag (every CompositionPlayer seek
     * stops rendering until it settles, which is the reported black screen).
     * Every frame is decoded frame-exact so the user can read WHICH frame a
     * split will land on while scrolling.
     */
    private fun requestScrubFrame(
        project: VideoProject,
        positionMs: Long,
        final: Boolean = false
    ) {
        val appContext = context?.applicationContext ?: return
        // Small moves reuse the frame already on screen: at 30 fps one source
        // frame is ~33 ms, so anything closer than that is the same picture.
        // The settle request bypasses this so the final playhead is always
        // decoded.
        if (!final &&
            lastScrubRequestedMs != Long.MIN_VALUE &&
            kotlin.math.abs(positionMs - lastScrubRequestedMs) < SCRUB_FRAME_MIN_DELTA_MS
        ) {
            return
        }
        lastScrubRequestedMs = positionMs
        ensureScrubWorkers(appContext)
        val request = ScrubFrameRequest(
            project = project,
            positionMs = positionMs,
            final = final,
            generation = scrubFrameGeneration
        )
        if (final) {
            scrubExactRequests.trySend(request)
        } else {
            scrubFrameRequests.trySend(request)
        }
    }

    /**
     * One worker per lane decodes whatever the newest request is; the conflated
     * channels drop intermediate playheads instead of queueing a decode per
     * scroll event. That is what keeps both the preview and the timeline smooth.
     */
    private fun ensureScrubWorkers(appContext: Context) {
        if (scrubFrameWorker?.isActive != true) {
            scrubFrameWorker = viewModelScope.launch {
                for (request in scrubFrameRequests) {
                    val bitmap = withContext(scrubFrameDispatcher) {
                        decodeScrubFrame(appContext, request, scrubFrameLoader)
                    }
                    publishScrubFrame(request, bitmap, requireCurrentTarget = false)
                }
            }
        }
        if (scrubExactWorker?.isActive != true) {
            scrubExactWorker = viewModelScope.launch {
                for (request in scrubExactRequests) {
                    val bitmap = withContext(scrubExactDispatcher) {
                        decodeScrubFrame(appContext, request, scrubExactLoader)
                    }
                    // The settle frame is only useful if the playhead is still
                    // there; a resumed drag already has its own lane.
                    publishScrubFrame(request, bitmap, requireCurrentTarget = true)
                }
            }
        }
    }

    private fun decodeScrubFrame(
        appContext: Context,
        request: ScrubFrameRequest,
        loader: ScrubFrameLoader
    ): Bitmap? {
        val source = ScrubFrameResolver.resolve(request.project, request.positionMs)
            ?: return null
        // Both lanes decode frame-exact: a keyframe preview freezes inside a
        // GOP, which is exactly what made "which frame do I split at" unreadable.
        // [ScrubFrameAccuracy.KEYFRAME] stays available in the loader for a
        // future adaptive mode on sources exact decoding cannot keep up with.
        return loader.load(appContext, source, ScrubFrameAccuracy.EXACT)
    }

    private fun publishScrubFrame(
        request: ScrubFrameRequest,
        bitmap: Bitmap?,
        requireCurrentTarget: Boolean
    ) {
        if (bitmap == null) return
        if (_isPlaying.value) return
        if (request.generation != scrubFrameGeneration) return
        if (requireCurrentTarget && request.positionMs != scrubTargetMs) return
        // A late decode result must not drag the picture backwards past the
        // frame already on screen (ScrubFramePolicy). A failed decode keeps the
        // previous frame instead of flashing black.
        if (!ScrubFramePolicy.shouldPublish(
                requestedMs = request.positionMs,
                targetMs = scrubTargetMs,
                lastPublishedMs = lastPublishedScrubMs
            )
        ) {
            return
        }
        lastPublishedScrubMs = request.positionMs
        _scrubPreviewFrame.value = ScrubPreviewFrame(
            bitmap = bitmap,
            positionMs = request.positionMs
        )
    }

    /**
     * Waits for the timeline to go quiet, then decodes the frame-exact still.
     *
     * One watcher per gesture: it re-arms itself when the user moves while the
     * decode is pending. The player is deliberately NOT seeked here — a seek on
     * the paused CompositionPlayer is what blacked the preview out and, when it
     * never settled, produced the preview error pill. The player is rebuilt at
     * the visible playhead only when the user presses Play.
     */
    private fun scheduleScrubSettle() {
        if (scrubSettleJob?.isActive == true) return
        if (_isPlaying.value) return
        scrubSettleJob = viewModelScope.launch {
            while (true) {
                val target = scrubTargetMs
                delay(SCRUB_SETTLE_MS)
                if (_isPlaying.value) return@launch
                if (scrubTargetMs != target) continue
                _currentProject.value?.let { requestScrubFrame(it, target, final = true) }
                return@launch
            }
        }
    }

    private fun clearScrubFrame() {
        // Drop queued decodes: their frames belong to the gesture that just
        // ended and must not flash back over the live player.
        scrubFrameGeneration++
        while (scrubFrameRequests.tryReceive().isSuccess) {
            // Drain the conflated slot.
        }
        while (scrubExactRequests.tryReceive().isSuccess) {
            // Drain the conflated slot.
        }
        _scrubPreviewFrame.value = null
        lastScrubRequestedMs = Long.MIN_VALUE
        lastPublishedScrubMs = ScrubFramePolicy.NOTHING_PUBLISHED
    }

    /** Cancels every scrub job and removes the overlay (project switch, destroy). */
    internal fun cancelScrubPreview() {
        scrubSettlePending = false
        scrubSettleJob?.cancel()
        scrubSettleJob = null
        scrubFrameHandoffJob?.cancel()
        scrubFrameHandoffJob = null
        scrubEditHandoffJob?.cancel()
        scrubEditHandoffJob = null
        playerManager?.cancelScrub()
        clearScrubFrame()
    }

    fun stepForward() {
        seekTo(_currentPositionMs.value + 1000L)
    }

    fun stepBackward() {
        seekTo(_currentPositionMs.value - 1000L)
    }

    // ==========================================
    // Editing Actions (Undo/Redo Supported)
    // ==========================================

    /**
     * Bounded snapshot history (plan §12: immutable snapshots with structural
     * sharing). Data-class `copy` shares unchanged lists, so snapshots stay
     * cheap even at a deep history.
     */
    private val HISTORY_MAX = 200

    /** Drag/slider streams inside this window collapse into ONE undo step. */
    private val COALESCE_WINDOW_MS = 500L

    private fun pushHistory(project: VideoProject, coalesceKey: String? = null) {
        val now = System.currentTimeMillis()
        val last = undoStack.lastOrNull()
        if (coalesceKey != null && last != null &&
            last.key == coalesceKey && now - last.atMs < COALESCE_WINDOW_MS
        ) {
            // Same gesture is still streaming: keep the pre-gesture snapshot
            // on top so ONE undo reverts the whole drag (CapCut behaviour).
            // Slide the timestamp so a long drag stays a single undo step.
            undoStack[undoStack.lastIndex] = last.copy(atMs = now)
        } else {
            undoStack.add(EditorHistoryEntry(project, coalesceKey, now))
            if (undoStack.size > HISTORY_MAX) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
        }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        undoJournal?.record(ProjectJson.toJson(project), coalesceKey)
    }

    private fun applyHistoryState(project: VideoProject) {
        val normalized = project.withMagneticMainTrack()
        _currentProject.value = normalized
        repository.saveProject(normalized)
        _canUndo.value = undoStack.isNotEmpty() || undoJournal?.canUndo == true
        _canRedo.value = redoStack.isNotEmpty() || undoJournal?.canRedo == true
        schedulePreviewRefresh()
    }

    fun undo() {
        val curr = _currentProject.value ?: return
        // Advance the durable journal alongside the in-memory stack so both stay
        // aligned; when the stack is empty after a restart, the journal answers.
        val journalState = undoJournal?.undo(ProjectJson.toJson(curr))?.let { ProjectJson.fromJson(it) }
        if (undoStack.isNotEmpty()) {
            redoStack.add(curr)
            applyHistoryState(undoStack.removeAt(undoStack.lastIndex).project)
        } else if (journalState != null && journalState.id == curr.id) {
            applyHistoryState(journalState)
        }
    }

    fun redo() {
        val curr = _currentProject.value ?: return
        val journalState = undoJournal?.redo(ProjectJson.toJson(curr))?.let { ProjectJson.fromJson(it) }
        if (redoStack.isNotEmpty()) {
            undoStack.add(EditorHistoryEntry(curr))
            applyHistoryState(redoStack.removeAt(redoStack.lastIndex))
        } else if (journalState != null && journalState.id == curr.id) {
            applyHistoryState(journalState)
        }
    }

    /**
     * Central mutation helper: snapshots the pre-edit project for undo, applies
     * [transform] and autosaves. [coalesceKey] groups a drag/slider stream so
     * one gesture becomes exactly one undo step (plan §12 coalescing rule).
     */
    internal fun updateProject(
        coalesceKey: String? = null,
        transform: (VideoProject) -> VideoProject
    ) {
        val curr = _currentProject.value ?: return
        pushHistory(curr, coalesceKey)
        val updated = transform(curr).withMagneticMainTrack()
        _currentProject.value = updated
        repository.saveProject(updated)
        // Trimming can make the timeline shorter than the playhead.
        val clampedPosition = _currentPositionMs.value.coerceIn(0L, updated.totalDurationMs)
        if (clampedPosition != _currentPositionMs.value) {
            _currentPositionMs.value = clampedPosition
            playerManager?.seekTo(clampedPosition)
        }
        schedulePreviewRefresh()
    }

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
    fun splitClipAtPlayhead(): Boolean {
        val project = _currentProject.value ?: return false
        if (_isPlaying.value) {
            pausePlayback()
            playerManager?.syncPosition()
            playerManager?.currentPositionMs?.value?.let { _currentPositionMs.value = it }
        }
        val playhead = _currentPositionMs.value
        val selectedId = _selectedClipId.value
        val min = ClipSplit.MIN_SPLIT_MS

        if (selectedId != null && project.isTrackLockedForClip(selectedId)) return false
        val selectedIsMain = selectedId != null && project.clips.any { it.id == selectedId }
        if (project.isMainTrackLocked && (selectedId == null || selectedIsMain)) return false

        if (selectedId != null) {
            project.overlayTracks.forEachIndexed { trackIndex, track ->
                val clip = track.clips.firstOrNull { it.id == selectedId } ?: return@forEachIndexed
                val localMs = playhead - clip.startTimelineMs
                if (localMs < min || localMs > clip.durationMs - min) return@forEachIndexed

                val split = ClipSplit.split(clip, localMs) { UUID.randomUUID().toString() }
                    ?: return@forEachIndexed
                val second = split.second.copy(
                    startTimelineMs = clip.startTimelineMs + localMs
                )
                val newClips = track.clips.flatMap { existing ->
                    if (existing.id == clip.id) listOf(split.first, second) else listOf(existing)
                }
                updateProject { current ->
                    current.copy(
                        overlayTracks = current.overlayTracks.mapIndexed { index, currentTrack ->
                            if (index == trackIndex) currentTrack.copy(clips = newClips) else currentTrack
                        }
                    )
                }
                _selectedClipId.value = second.id
                return true
            }
        }

        var cursorMs = 0L
        project.clips.forEachIndexed { index, clip ->
            val localMs = playhead - cursorMs
            if (localMs in min..(clip.durationMs - min)) {
                val split = ClipSplit.split(clip, localMs) { UUID.randomUUID().toString() }
                    ?: return false
                val first = split.first.copy(
                    startTimelineMs = clip.startTimelineMs
                )
                val second = split.second.copy(
                    startTimelineMs = clip.startTimelineMs + localMs
                )
                val clips = project.clips.toMutableList()
                clips.removeAt(index)
                clips.add(index, second)
                clips.add(index, first)
                updateProject { it.copy(clips = clips) }
                _selectedClipId.value = second.id
                return true
            }
            cursorMs += clip.durationMs
        }
        return false
    }

    /**
     * Deletes the selected clip through [TimelineEdits.delete]: the main track
     * ripples (the gap closes, list order defines time) while overlay tracks
     * lift (absolute positions of the remaining overlays stay put).
     */
    fun deleteSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val mainSpans = proj.clips.map {
                TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
            }
            val byId = proj.clips.associateBy { it.id }
            val newMainClips = TimelineEdits.delete(mainSpans, clipId, ripple = true)
                .mapNotNull { span -> byId[span.id] }

            val newOverlays = proj.overlayTracks.map { track ->
                if (track.clips.none { it.id == clipId }) {
                    track
                } else {
                    val spans = track.clips.map {
                        TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
                    }
                    val lookup = track.clips.associateBy { it.id }
                    track.copy(
                        clips = TimelineEdits.delete(spans, clipId, ripple = false)
                            .mapNotNull { span -> lookup[span.id] }
                    )
                }
            }
            proj.copy(clips = newMainClips, overlayTracks = newOverlays)
        }
        val updated = _currentProject.value
        _selectedClipId.value = updated?.clips?.firstOrNull()?.id
            ?: updated?.overlayTracks?.firstOrNull()?.clips?.firstOrNull()?.id
    }

    fun duplicateSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        var duplicatedOverlayId: String? = null
        updateProject { proj ->
            val index = proj.clips.indexOfFirst { it.id == clipId }
            if (index != -1) {
                val original = proj.clips[index]
                val copy = original.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${original.title} (Copy)"
                )
                val newClips = proj.clips.toMutableList()
                newClips.add(index + 1, copy)
                proj.copy(clips = newClips)
            } else {
                // Overlay clips are absolute on the timeline: place the copy
                // immediately after the original instead of stacking it on top.
                val updatedTracks = proj.overlayTracks.map { track ->
                    val overlayIndex = track.clips.indexOfFirst { it.id == clipId }
                    if (overlayIndex == -1) {
                        track
                    } else {
                        val original = track.clips[overlayIndex]
                        val copy = original.copy(
                            id = UUID.randomUUID().toString(),
                            title = "${original.title} (Copy)",
                            startTimelineMs = original.startTimelineMs + original.durationMs
                        )
                        duplicatedOverlayId = copy.id
                        track.copy(clips = track.clips + copy)
                    }
                }
                proj.copy(overlayTracks = updatedTracks)
            }
        }
        duplicatedOverlayId?.let { _selectedClipId.value = it }
    }

    /**
     * Inserts a 3-second freeze frame at the current playhead position.
     * Splits the active clip and creates a held frame with preserved styling.
     */
    fun freezeFrameAtPlayhead() {
        val project = _currentProject.value ?: return
        val playhead = _currentPositionMs.value
        val clips = project.clips.toMutableList()

        var accumulated = 0L
        var targetIndex = -1
        for (i in clips.indices) {
            val clipEnd = accumulated + clips[i].durationMs
            if (playhead in accumulated..clipEnd) {
                targetIndex = i
                break
            }
            accumulated = clipEnd
        }

        if (targetIndex != -1) {
            val target = clips[targetIndex]
            val splitOffsetMs = (playhead - accumulated).coerceIn(0L, target.durationMs)
            val remainingMs = target.durationMs - splitOffsetMs

            val firstHalf = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = splitOffsetMs.coerceAtLeast(100L),
                trimEndMs = target.trimStartMs + splitOffsetMs,
                title = target.title
            )
            val freezeClip = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = 3000L,
                trimStartMs = target.trimStartMs + splitOffsetMs,
                trimEndMs = target.trimStartMs + splitOffsetMs + 33L,
                speed = 0.011f,
                title = "${target.title} (Freeze)"
            )
            val secondHalf = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = remainingMs.coerceAtLeast(100L),
                trimStartMs = target.trimStartMs + splitOffsetMs,
                title = target.title
            )

            clips.removeAt(targetIndex)
            if (remainingMs > 100L) {
                clips.add(targetIndex, secondHalf)
            }
            clips.add(targetIndex, freezeClip)
            if (splitOffsetMs > 100L) {
                clips.add(targetIndex, firstHalf)
            }

            updateProject("freeze_${target.id}") { it.copy(clips = clips) }
            _selectedClipId.value = freezeClip.id
        }
    }

    /** Toggles reverse playback on the selected clip. */
    fun reverseSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { clip ->
                    if (clip.id == clipId) {
                        val rev = !clip.isReversed
                        clip.copy(
                            isReversed = rev,
                            title = if (rev) "${clip.title} (Reverse)" else clip.title.replace(" (Reverse)", "")
                        )
                    } else clip
                },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map { clip ->
                            if (clip.id == clipId) {
                                val rev = !clip.isReversed
                                clip.copy(
                                    isReversed = rev,
                                    title = if (rev) "${clip.title} (Reverse)" else clip.title.replace(" (Reverse)", "")
                                )
                            } else clip
                        }
                    )
                }
            )
        }
    }

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
    fun moveClip(fromIndex: Int, toIndex: Int) {
        val project = _currentProject.value ?: return
        if (project.isMainTrackLocked) return
        val clips = project.clips.toMutableList()
        if (fromIndex !in clips.indices || toIndex !in clips.indices) return
        val clip = clips.removeAt(fromIndex)
        clips.add(toIndex, clip)
        updateProject { it.copy(clips = clips) }
    }

    /**
     * Moves an overlay clip to a new start time. The drop snaps to neighbouring
     * overlay edges ([SnapEngine.magneticStart]) and clamps inside the project,
     * so a fling can never park a clip outside the timeline.
     */
    fun moveOverlayClip(clipId: String, desiredStartMs: Long) {
        updateProject { proj ->
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val trackIndex = proj.overlayTracks.indexOfFirst { track ->
                track.clips.any { it.id == clipId }
            }
            if (trackIndex == -1) return@updateProject proj
            val track = proj.overlayTracks[trackIndex]
            val clip = track.clips.first { it.id == clipId }
            val occupied = SnapEngine.occupiedRanges(
                track.clips
                    .filter { it.id != clipId }
                    .map { it.startTimelineMs to it.durationMs }
            )
            val snapped = SnapEngine.magneticStart(desiredStartMs, clip.durationMs, occupied)
            val maxStart = (proj.totalDurationMs - clip.durationMs).coerceAtLeast(0L)
            val start = snapped.coerceIn(0L, maxStart)
            val updatedTrack = track.copy(
                clips = track.clips.map { existing ->
                    if (existing.id == clipId) existing.copy(startTimelineMs = start) else existing
                }
            )
            proj.copy(
                overlayTracks = proj.overlayTracks.mapIndexed { index, current ->
                    if (index == trackIndex) updatedTrack else current
                }
            )
        }
    }

    /**
     * Moves a clip to another overlay track at the requested start time.
     *
     * Cross-track drops are the CapCut "free overlay" model: the target track is
     * chosen by the vertical part of the drag, then [SnapEngine] resolves the
     * exact start inside that track. Locked source/target tracks refuse the move.
     */
    fun moveClipToTrack(clipId: String, targetTrackId: String, desiredStartMs: Long) {
        updateProject { proj ->
            if (proj.isTrackLocked(targetTrackId) || proj.trackIdForClip(clipId) == targetTrackId) {
                return@updateProject proj
            }
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val sourceIndex = proj.overlayTracks.indexOfFirst { track ->
                track.clips.any { it.id == clipId }
            }
            val targetIndex = proj.overlayTracks.indexOfFirst { it.id == targetTrackId }
            if (sourceIndex == -1 || targetIndex == -1) return@updateProject proj

            val clip = proj.overlayTracks[sourceIndex].clips.first { it.id == clipId }
            val occupied = SnapEngine.occupiedRanges(
                proj.overlayTracks[targetIndex].clips.map { it.startTimelineMs to it.durationMs }
            )
            val snapped = SnapEngine.magneticStart(desiredStartMs, clip.durationMs, occupied)
            val maxStart = (proj.totalDurationMs - clip.durationMs).coerceAtLeast(0L)
            val start = snapped.coerceIn(0L, maxStart)
            val moved = clip.copy(startTimelineMs = start)

            proj.copy(
                overlayTracks = proj.overlayTracks.mapIndexed { index, track ->
                    when (index) {
                        sourceIndex -> track.copy(clips = track.clips.filterNot { it.id == clipId })
                        targetIndex -> track.copy(clips = track.clips + moved)
                        else -> track
                    }
                }
            )
        }
    }

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

    /** Adds a marker at the playhead. */
    fun addMarker(label: String = "") = MarkerFeature.addMarker(this, label)

    fun removeMarker(markerId: String) = MarkerFeature.removeMarker(this, markerId)

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
    fun trimSelectedClipEdge(edge: ClipTrimHandle.Edge, outputDeltaMs: Long, fps: Int) {
        val clipId = _selectedClipId.value ?: return
        beginTrimClip(clipId, edge)
        updateTrimClip(outputDeltaMs, fps)
        endTrimClip()
    }

    /** One inline handle drag, snapshotted when the finger goes down. */
    private var trimSession: ClipTrimSession? = null

    /** Snapshots the clip before a handle drag so every live update is absolute. */
    fun beginTrimClip(clipId: String, edge: ClipTrimHandle.Edge) {
        val proj = _currentProject.value
        if (proj == null || proj.isTrackLockedForClip(clipId)) {
            trimSession = null
            return
        }
        val clip = (proj.clips + proj.overlayTracks.flatMap { it.clips })
            .firstOrNull { it.id == clipId }
        trimSession = clip?.let { ClipTrimSession(clipId, edge, it, proj) }
        // No rebuilds while the handle moves (see PreviewRefreshScheduler);
        // endTrimClip fires exactly one rebuild at finger lift.
        if (trimSession != null) {
            previewScheduler.beginSuppression()
        }
    }

    /**
     * Live trim while the handle moves. All the rules (in/out only, magnetic
     * reflow, overlay end pinning, idempotent absolute application) live in
     * [ClipTrimSession]; this only wires state/history/preview/save.
     */
    fun updateTrimClip(cumulativeDeltaMs: Long, fps: Int) {
        val session = trimSession ?: return
        val curr = _currentProject.value ?: return
        val updated = session.applyTo(curr, cumulativeDeltaMs, fps) ?: return
        if (!session.historyPushed) {
            session.historyPushed = true
            // ONE undo entry per drag: the pre-drag project.
            pushHistory(session.baseProject, "trim:${session.clipId}:${session.edge}")
        }
        _currentProject.value = updated
        // Trimming can make the timeline shorter than the playhead.
        val clampedPosition = _currentPositionMs.value.coerceIn(0L, updated.totalDurationMs)
        if (clampedPosition != _currentPositionMs.value) {
            _currentPositionMs.value = clampedPosition
            playerManager?.seekTo(clampedPosition)
        }
        schedulePreviewRefresh()
    }

    /** Ends the handle drag; autosaves once and refreshes the preview once. */
    fun endTrimClip() {
        val hadSession = trimSession != null
        trimSession = null
        if (hadSession) {
            _currentProject.value?.let { repository.saveProject(it) }
            // One rebuild at finger lift: the preview shows the final trim
            // immediately, without any mid-drag composition rebuilds.
            previewScheduler.endSuppression()
        }
    }

    /** Adds a clip by looking up its real duration and waveform from the file. */
    suspend fun addClipFromUri(uriString: String, title: String) {
        val ctx = context ?: return
        val uri = Uri.parse(uriString)
        AppLog.info("EditorViewModel", "addClipFromUri() started: uri=$uriString, title=$title")
        _isImporting.value = true
        try {
            AppLog.debug("EditorViewModel", "Fetching duration for uri=$uriString")
            val durationMs = MediaMetadata.durationMs(ctx, uri)
            val info = runCatching { MediaMetadata.videoInfo(ctx, uri) }.getOrNull()
            AppLog.debug("EditorViewModel", "Fetching waveform for uri=$uriString")
            val waveform = MediaMetadata.waveform(ctx, uri, 48) ?: emptyList()
            val realTitle = title.takeIf { it.isNotBlank() }
                ?: MediaMetadata.displayName(ctx, uri)
            // Place clip after existing clips on the timeline. A fresh project
            // (or one holding only a legacy placeholder) starts at zero and
            // discards the placeholder so the imported clip is visible.
            val currentProject = _currentProject.value
            val isFreshProject = currentProject == null ||
                currentProject.clips.all { it.isPlaceholderClip }
            val lastEndMs = if (isFreshProject) {
                0L
            } else {
                currentProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            }
            AppLog.debug("EditorViewModel", "Creating clip: title=$realTitle, startTimelineMs=$lastEndMs, durationMs=$durationMs, waveformSize=${waveform.size}")
            val clip = MediaClip(
                id = UUID.randomUUID().toString(),
                title = realTitle,
                uriString = uriString,
                startTimelineMs = lastEndMs,
                durationMs = if (durationMs > 0) durationMs else 4000L,
                originalDurationMs = if (durationMs > 0) durationMs else 4000L,
                waveform = waveform.ifEmpty { emptyList() },
                sourceWidth = info?.width ?: 0,
                sourceHeight = info?.height ?: 0,
                filter = VideoFilter.NONE
            )
            _isImporting.value = false
            updateProject { proj ->
                val fresh = isFreshProject && proj.clips.all { it.isPlaceholderClip }
                val ratio = if (fresh) {
                    info?.let { AspectMath.fromVideo(it.width, it.height, it.rotation) }
                } else {
                    null
                }
                proj.copy(
                    aspectRatio = ratio ?: proj.aspectRatio,
                    clips = (if (fresh) emptyList() else proj.clips) + clip
                )
            }
            _selectedClipId.value = clip.id
            _currentPositionMs.value = clip.startTimelineMs
            playerManager?.seekTo(clip.startTimelineMs)
            AppLog.info("EditorViewModel", "Successfully imported clip: $realTitle (${durationMs}ms), clipId=${clip.id}")
            // Reload the player with the updated project
        } catch (e: Exception) {
            _isImporting.value = false
            AppLog.error("EditorViewModel", "Failed to import clip from $uriString", e)
            // Still create a clip with fallback values so the user can at least see it
            val currentProject = _currentProject.value
            val lastEndMs = currentProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            val fallbackClip = MediaClip(
                id = UUID.randomUUID().toString(),
                title = title.takeIf { it.isNotBlank() } ?: "Imported Clip",
                uriString = uriString,
                startTimelineMs = lastEndMs,
                durationMs = 4000L,
                originalDurationMs = 4000L,
                filter = VideoFilter.NONE
            )
            updateProject { proj ->
                proj.copy(clips = proj.clips + fallbackClip)
            }
            _selectedClipId.value = fallbackClip.id
        }
    }

    /** Imports multiple URIs sequentially, then calls [onComplete] when all are done. */
    fun importMultipleUris(uris: List<String>, titles: List<String>, onComplete: () -> Unit) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            AppLog.info("EditorViewModel", "importMultipleUris() started: ${uris.size} URIs to import")
            _isImporting.value = true
            val built = mutableListOf<MediaClip>()
            val startingProject = _currentProject.value
            // A fresh project (new, or one that only contains a legacy demo
            // placeholder) starts at zero and adopts the first imported clip's
            // ratio/title. Placeholder-only timelines are dropped entirely so
            // the imported footage is what the user sees.
            val isFreshProject = startingProject == null ||
                startingProject.clips.all { it.isPlaceholderClip }
            var cursor = if (isFreshProject) {
                0L
            } else {
                startingProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            }
            var firstInfo: MediaVideoInfo? = null

            uris.forEachIndexed { index, uriString ->
                val ctx = context
                AppLog.info("EditorViewModel", "Importing URI ${index + 1}/${uris.size}: $uriString")
                val info = if (ctx != null) {
                    runCatching { MediaMetadata.videoInfo(ctx, Uri.parse(uriString)) }.getOrNull()
                        ?: MediaVideoInfo(0)
                } else {
                    MediaVideoInfo(0)
                }
                if (firstInfo == null && info.width > 0 && info.height > 0) {
                    firstInfo = info
                }
                val fallbackTitle = titles.getOrNull(index)?.takeIf { it.isNotBlank() }
                val fileTitle = info.displayName.substringBeforeLast('.').takeIf { it.isNotBlank() }
                val realTitle = (fallbackTitle?.takeIf { !it.startsWith("Clip ") })
                    ?: fileTitle
                    ?: "Clip ${index + 1}"
                val durationMs = if (info.durationMs > 0L) info.durationMs else 4_000L
                built += MediaClip(
                    id = UUID.randomUUID().toString(),
                    title = realTitle,
                    uriString = uriString,
                    startTimelineMs = cursor,
                    durationMs = durationMs,
                    originalDurationMs = durationMs,
                    sourceWidth = info.width,
                    sourceHeight = info.height,
                    // Waveform decode is expensive; video clips use the default
                    // waveform until the user opens the Audio sheet.
                    waveform = emptyList(),
                    filter = VideoFilter.NONE
                )
                cursor += durationMs
            }

            updateProject { proj ->
                val fresh = isFreshProject && proj.clips.all { it.isPlaceholderClip }
                val existingClips = if (fresh) emptyList() else proj.clips
                val renamed = if (fresh && built.isNotEmpty() &&
                    (proj.title.isBlank() || proj.title == "New Project" ||
                        proj.title.startsWith("New Project") || proj.title.startsWith("Untitled"))
                ) {
                    built.first().title
                } else {
                    proj.title
                }
                val ratio = if (fresh) {
                    firstInfo?.let { AspectMath.fromVideo(it.width, it.height, it.rotation) }
                } else {
                    null
                }
                proj.copy(
                    title = renamed,
                    aspectRatio = ratio ?: proj.aspectRatio,
                    clips = existingClips + built
                )
            }
            // Show the imported footage immediately instead of leaving the
            // playhead parked on the (now removed) placeholder at 0.
            val firstImportedStart = built.firstOrNull()?.startTimelineMs ?: 0L
            _currentPositionMs.value = firstImportedStart
            playerManager?.seekTo(firstImportedStart)
            _selectedClipId.value = built.lastOrNull()?.id
            _isImporting.value = false
            AppLog.info("EditorViewModel", "importMultipleUris() completed ${built.size} clip(s)")
            onComplete()
        }
    }

    /** Launch import of multiple URIs from Composable (non-suspend context). */
    fun launchImportUris(uris: List<android.net.Uri>) {
        val uriStrings = uris.map { it.toString() }
        AppLog.info("EditorViewModel", "launchImportUris() called: ${uris.size} URIs")
        // Empty titles: the real file display names come from MediaMetadata.
        importMultipleUris(uriStrings, emptyList()) { }
    }

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

    fun setClipTransition(transition: TransitionType) =
        ClipPropertyFeature.setClipTransition(this, transition)

    fun setClipAnimation(animation: ClipAnimation) =
        ClipPropertyFeature.setClipAnimation(this, animation)

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

    /** Adds an audio track from a URI (real duration + waveform). */
    fun addAudioTrackFromUri(uriString: String, title: String, artist: String = "Unknown") =
        AudioFeature.addAudioTrackFromUri(this, uriString, title, artist)

    fun addAudioTrack(audioClip: AudioClip) = AudioFeature.addAudioTrack(this, audioClip)

    fun deleteAudioTrack(id: String) = AudioFeature.deleteAudioTrack(this, id)

    /** Extracts the selected clip's audio as a real timeline audio track. */
    fun extractAudioFromSelectedClip() = AudioFeature.extractAudioFromSelectedClip(this)

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

    fun addAutoCaptions() = TextFeature.addAutoCaptions(this)

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

    fun setAspectRatio(ratio: AspectRatioType) = CanvasFeature.setAspectRatio(this, ratio)

    fun setPaperStyle(style: String) = CanvasFeature.setPaperStyle(this, style)

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

    /** Toggles a keyframe at the current playhead position on the selected clip. */
    fun toggleKeyframeAtPlayhead() = KeyframeFeature.toggleKeyframeAtPlayhead(this)

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
     * Beat-sync: splits clips at detected musical beats (energy onset detection
     * with a BPM-based synthetic fallback).
     */
    fun autoCutToBeat(beatsPerMinute: Int = 120) =
        BeatSyncFeature.autoCutToBeat(this, beatsPerMinute)

    /** Exports a real MP4 and reports where it can be shared. */
    fun shareExportMessage(platform: String): String =
        ExportFeature.shareExportMessage(this, platform)
}