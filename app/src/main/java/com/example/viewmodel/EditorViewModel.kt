package com.example.viewmodel

import android.content.Context
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
import com.example.data.ProjectRepository
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
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import com.example.data.KeyframeInterpolator
import com.example.data.db.ProjectJson
import com.example.engine.BeatGrid
import com.example.engine.CaptionBridge
import com.example.engine.ClipSplit
import com.example.engine.ClipTrimHandle
import com.example.engine.FileJournalStorage
import com.example.engine.SnapEngine
import com.example.engine.SubtitleIO
import com.example.engine.TimelineEdits
import com.example.engine.UndoJournal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    VOLUME_BOOST
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val stage: String = "",
    val isCompleted: Boolean = false,
    val resolution: String = "1080P",
    val fps: Int = 30,
    val videoPath: String = "",
    val galleryUri: String = "",
    /** Features that were approximated or skipped by the renderer. */
    val warnings: List<String> = emptyList()
)

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
    private val repository: ProjectRepository = ProjectRepository(),
    private val settingsRepository: SettingsRepository? = null,
    private val cacheManager: CacheManager? = null,
    private val playerManager: com.example.data.PlayerManager? = null,
    private val exportManager: com.example.data.ExportManager? = null,
    private val context: Context? = null
) : ViewModel() {

    private val _projects = repository.projects
    val projects: StateFlow<List<VideoProject>> = _projects

    private val _currentProject = MutableStateFlow<VideoProject?>(null)
    val currentProject: StateFlow<VideoProject?> = _currentProject.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    private val _activeSheet = MutableStateFlow(ActiveToolSheet.NONE)
    val activeSheet: StateFlow<ActiveToolSheet> = _activeSheet.asStateFlow()

    private val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    /** Detected beat positions (ms) used as magnetic snap targets on the timeline. */
    private val _snapBeats = MutableStateFlow<List<Long>>(emptyList())
    val snapBeats: StateFlow<List<Long>> = _snapBeats.asStateFlow()

    fun setEyedropperActive(active: Boolean) {
        _isEyedropperActive.value = active
    }

    private val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _appThemeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.LIGHT)
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _appThemeMode.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _defaultResolution = MutableStateFlow("1080P")
    val defaultResolution: StateFlow<String> = _defaultResolution.asStateFlow()

    private val _defaultFps = MutableStateFlow(30)
    val defaultFps: StateFlow<Int> = _defaultFps.asStateFlow()

    private val _showPaperGrid = MutableStateFlow(true)
    val showPaperGrid: StateFlow<Boolean> = _showPaperGrid.asStateFlow()

    private val _watermarkEnabled = MutableStateFlow(false)
    val watermarkEnabled: StateFlow<Boolean> = _watermarkEnabled.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow(0)
    val cacheSizeMb: StateFlow<Int> = _cacheSizeMb.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val voiceRecorder = context?.let { VoiceRecorder(it) }
    private val speechSynthesizer = context?.let { SpeechSynthesizer(it) }

    fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _appThemeMode.value = mode
        viewModelScope.launch { settingsRepository?.setThemeMode(mode) }
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        viewModelScope.launch { settingsRepository?.setLanguage(language) }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
        refreshCacheSize()
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setDefaultResolution(res: String) {
        _defaultResolution.value = res
        viewModelScope.launch { settingsRepository?.setDefaultResolution(res) }
    }

    fun setDefaultFps(fps: Int) {
        _defaultFps.value = fps
        viewModelScope.launch { settingsRepository?.setDefaultFps(fps) }
    }

    fun togglePaperGrid(enabled: Boolean) {
        _showPaperGrid.value = enabled
        viewModelScope.launch { settingsRepository?.setShowPaperGrid(enabled) }
    }

    fun toggleWatermark(enabled: Boolean) {
        _watermarkEnabled.value = enabled
        viewModelScope.launch { settingsRepository?.setWatermarkEnabled(enabled) }
        // The watermark is burned into the same preview composition as export.
        playerManager?.setWatermarkEnabled(enabled)
    }

    fun refreshCacheSize() {
        val manager = cacheManager ?: return
        viewModelScope.launch {
            _cacheSizeMb.value = manager.cacheSizeMb()
        }
    }

    fun clearCache() {
        val manager = cacheManager
        if (manager == null) {
            _cacheSizeMb.value = 0
            return
        }
        viewModelScope.launch {
            _cacheSizeMb.value = manager.clearCache()
        }
    }

    // Voice recording with real MediaRecorder
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()
    private val _voiceRecordingDurationMs = MutableStateFlow(0L)
    val voiceRecordingDurationMs: StateFlow<Long> = _voiceRecordingDurationMs.asStateFlow()

    // Undo / Redo history stacks
    private val undoStack = mutableListOf<EditorHistoryEntry>()
    private val redoStack = mutableListOf<VideoProject>()

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
    private data class EditorHistoryEntry(
        val project: VideoProject,
        /** Non-null while a drag/slider gesture streams; equal keys collapse. */
        val key: String? = null,
        val atMs: Long = System.currentTimeMillis()
    )

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _magneticSnap = MutableStateFlow(true)
    val magneticSnap: StateFlow<Boolean> = _magneticSnap.asStateFlow()

    fun toggleMagneticSnap() {
        _magneticSnap.value = !_magneticSnap.value
    }

    private var playbackJob: Job? = null
    private var voiceRecordJob: Job? = null
    private var exportJob: Job? = null
    private var importJob: Job? = null
    private var previewRefreshJob: Job? = null

    /**
     * Rebuilds the render graph after the user stops dragging/sliding. The
     * composition is identical to the export graph, so a short debounce keeps
     * sliders smooth without allowing preview/export drift.
     */
    private fun schedulePreviewRefresh() {
        val project = _currentProject.value ?: return
        previewRefreshJob?.cancel()
        previewRefreshJob = viewModelScope.launch {
            delay(120L)
            playerManager?.loadProject(project)
        }
    }

    private fun refreshPreviewNow() {
        previewRefreshJob?.cancel()
        previewRefreshJob = null
        _currentProject.value?.let { playerManager?.loadProject(it) }
    }

    init {
        val initialList = repository.projects.value
        if (initialList.isNotEmpty()) {
            val firstProject = initialList.first()
            _currentProject.value = firstProject
            _selectedClipId.value = firstProject.clips.firstOrNull()?.id
                ?: firstProject.overlayTracks.firstOrNull()?.clips?.firstOrNull()?.id
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
        playbackJob?.cancel()
        voiceRecordJob?.cancel()
        exportJob?.cancel()
        importJob?.cancel()
        previewRefreshJob?.cancel()
        voiceRecorder?.cancel()
        speechSynthesizer?.release()
        // PlayerManager is application-scoped (the editor UI reads it directly).
        // Releasing it here would leave a dead player when the activity is
        // recreated after finishing; just stop playback instead.
        playerManager?.pause()
    }

    fun openProject(project: VideoProject) {
        AppLog.info("EditorViewModel", "openProject() called: projectId=${project.id}, title=${project.title}, clips=${project.clips.size}, totalDuration=${project.totalDurationMs}ms")
        pausePlayback()
        playerManager?.loadProject(project)
        _currentProject.value = project
        _currentPositionMs.value = 0L
        _selectedClipId.value = project.clips.firstOrNull()?.id
            ?: project.overlayTracks.firstOrNull()?.clips?.firstOrNull()?.id
        _selectedTextId.value = null
        _activeSheet.value = ActiveToolSheet.NONE
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        AppLog.debug("EditorViewModel", "openProject() completed, currentProject set")
    }

    fun toggleProjectFavorite(id: String) {
        repository.toggleFavorite(id)
        if (_currentProject.value?.id == id) {
            _currentProject.value = _currentProject.value?.copy(
                isFavorite = !(_currentProject.value?.isFavorite ?: false)
            )
        }
    }

    fun createNewProjectWithParams(
        title: String,
        ratio: AspectRatioType = AspectRatioType.RATIO_9_16,
        paperStyle: String = "Standard"
    ) {
        pausePlayback()
        val proj = repository.createNewProject(
            title = title.ifBlank { "New Project #${System.currentTimeMillis() % 1000}" },
            aspectRatio = ratio,
            paperStyle = paperStyle,
            initialClips = emptyList(),
            resolution = _defaultResolution.value,
            fps = _defaultFps.value
        )
        openProject(proj)
    }

    fun createNewProject() {
        createNewProjectWithParams("New Project #${System.currentTimeMillis() % 1000}")
    }

    fun duplicateProject(id: String) {
        repository.duplicateProject(id)
        _currentProject.value?.let { playerManager?.loadProject(it) }
    }

    fun deleteProject(id: String) {
        if (_currentProject.value?.id == id) {
            _currentProject.value = null
        }
        repository.deleteProject(id)
        _currentProject.value?.let { playerManager?.loadProject(it) } ?: playerManager?.pause()
    }

    /** Restores a draft removed from the home screen (undo delete). */
    fun restoreProject(project: VideoProject) {
        repository.saveProject(project)
        _currentProject.value = project
        playerManager?.loadProject(project)
    }

    /** Clears ALL projects from the repository (database + memory). */
    fun clearAllProjects() {
        _currentProject.value = null
        repository.deleteAllProjects()
        playerManager?.pause()
    }

    fun setActiveSheet(sheet: ActiveToolSheet) {
        _activeSheet.value = sheet
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
        if (clipId != null) {
            _selectedTextId.value = null
        }
    }

    fun selectText(textId: String?) {
        _selectedTextId.value = textId
        if (textId != null) {
            _selectedClipId.value = null
        }
    }

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.5f, 3.0f)
    }

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
        val totalMs = project.totalDurationMs
        if (_currentPositionMs.value >= totalMs) {
            _currentPositionMs.value = 0L
            playerManager?.seekTo(0L)
        }
        _isPlaying.value = true

        if (playerManager != null) {
            // loadProject() is a no-op when the same project is already prepared.
            playerManager.loadProject(project)
            playerManager.seekTo(_currentPositionMs.value)
            playerManager.play()
            playbackJob?.cancel()
            playbackJob = viewModelScope.launch {
                while (isActive && _isPlaying.value) {
                    delay(50L)
                    playerManager.syncPosition()
                    _currentPositionMs.value = playerManager.currentPositionMs.value
                    val state = playerManager.playbackState.value
                    if (state == androidx.media3.common.Player.STATE_ENDED ||
                        (state == androidx.media3.common.Player.STATE_IDLE &&
                            playerManager.lastError.value != null)
                    ) {
                        _isPlaying.value = false
                        break
                    }
                }
            }
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

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        playerManager?.pause()
        // Snap to the frame the player actually stopped on; the 50 ms poll can
        // be one frame behind, and a split must land on the visible frame.
        playerManager?.syncPosition()
        playerManager?.currentPositionMs?.value?.let { _currentPositionMs.value = it }
    }

    fun seekTo(positionMs: Long) {
        val project = _currentProject.value ?: return
        val clamped = positionMs.coerceIn(0L, project.totalDurationMs)
        _currentPositionMs.value = clamped
        playerManager?.seekTo(clamped)
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
            return
        }
        undoStack.add(EditorHistoryEntry(project, coalesceKey, now))
        if (undoStack.size > HISTORY_MAX) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        undoJournal?.record(ProjectJson.toJson(project), coalesceKey)
    }

    private fun applyHistoryState(project: VideoProject) {
        _currentProject.value = project
        repository.saveProject(project)
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
    private fun updateProject(
        coalesceKey: String? = null,
        transform: (VideoProject) -> VideoProject
    ) {
        val curr = _currentProject.value ?: return
        pushHistory(curr, coalesceKey)
        val updated = transform(curr)
        _currentProject.value = updated
        repository.saveProject(updated)
        schedulePreviewRefresh()
    }

    // ==========================================
    // Clip operations
    // ==========================================

    /** Applies [transform] to a clip on the main track or any overlay track. */
    private fun VideoProject.withUpdatedClip(
        clipId: String,
        transform: (MediaClip) -> MediaClip
    ): VideoProject {
        var changed = false
        val updatedMain = clips.map { clip ->
            if (clip.id == clipId) {
                changed = true
                transform(clip)
            } else clip
        }
        val updatedOverlays = overlayTracks.map { track ->
            val updatedClips = track.clips.map { clip ->
                if (clip.id == clipId) {
                    changed = true
                    transform(clip)
                } else clip
            }
            if (updatedClips == track.clips) track else track.copy(clips = updatedClips)
        }
        return if (changed) copy(clips = updatedMain, overlayTracks = updatedOverlays) else this
    }

    /**
     * Clip-local time. Main-track clips are laid out cumulatively (their stored
     * startTimelineMs is not authoritative), overlay clips use their explicit
     * startTimelineMs. Keyframes are always stored in clip-local time.
     */
    private fun VideoProject.localTimeForClip(clipId: String, timelineMs: Long): Long? {
        var cursor = 0L
        for (clip in clips) {
            if (clip.id == clipId) {
                return (timelineMs - cursor).coerceIn(0L, clip.durationMs)
            }
            cursor += clip.durationMs
        }
        overlayTracks.forEach { track ->
            track.clips.firstOrNull { it.id == clipId }?.let { clip ->
                return (timelineMs - clip.startTimelineMs).coerceIn(0L, clip.durationMs)
            }
        }
        return null
    }

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
            // Pull the player's exact position before cutting; the 50 ms sync
            // loop may be one frame behind the paused frame.
            playerManager?.syncPosition()
            playerManager?.currentPositionMs?.value?.let { _currentPositionMs.value = it }
        }
        val playhead = _currentPositionMs.value
        val selectedId = _selectedClipId.value
        val min = ClipSplit.MIN_SPLIT_MS

        // Locked tracks refuse the cut. A selected overlay splits even when the
        // main track is locked, and vice versa.
        if (selectedId != null && project.isTrackLockedForClip(selectedId)) return false
        val selectedIsMain = selectedId != null && project.clips.any { it.id == selectedId }
        if (project.isMainTrackLocked && (selectedId == null || selectedIsMain)) return false

        // 1. A selected overlay clip under the playhead wins over the main track.
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

        // 2. Main-track clip under the playhead (list order defines the time).
        var cursorMs = 0L
        project.clips.forEachIndexed { index, clip ->
            val localMs = playhead - cursorMs
            if (localMs in min..(clip.durationMs - min)) {
                val split = ClipSplit.split(clip, localMs) { UUID.randomUUID().toString() }
                    ?: return false
                val clips = project.clips.toMutableList()
                clips.removeAt(index)
                clips.add(index, split.second)
                clips.add(index, split.first)
                updateProject { it.copy(clips = clips) }
                _selectedClipId.value = split.second.id
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
    fun setClipChromaKey(clipId: String, chromaData: com.example.model.ChromaKeyData) {
        updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { if (it.id == clipId) it.copy(chromaKey = chromaData) else it },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(chromaKey = chromaData) else it })
                }
            )
        }
    }

    /** Updates the selected clip's transform directly from canvas gestures. */
    fun updateSelectedClipTransform(
        scaleMultiplier: Float,
        rotationDelta: Float,
        panDeltaX: Float,
        panDeltaY: Float
    ) {
        val clipId = _selectedClipId.value ?: return
        updateProject(coalesceKey = "clipTransform:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val newScale = (clip.scale * scaleMultiplier).coerceIn(0.1f, 5.0f)
                val newRotation = (clip.rotation + rotationDelta) % 360f
                val newPosX = (clip.posX + panDeltaX).coerceIn(-0.5f, 1.5f)
                val newPosY = (clip.posY + panDeltaY).coerceIn(-0.5f, 1.5f)
                clip.copy(
                    scale = newScale,
                    rotation = if (newRotation < 0f) newRotation + 360f else newRotation,
                    posX = newPosX,
                    posY = newPosY
                )
            }
        }
    }

    /** Resets the selected clip's scale, position, and rotation to default. */
    fun resetSelectedClipTransform() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    scale = 1.0f,
                    rotation = 0f,
                    posX = 0.5f,
                    posY = 0.5f
                )
            }
        }
    }

    /** Samples video frame pixel color at normalized coordinates for Chroma Key. */
    fun sampleChromaColorAt(context: Context, normX: Float, normY: Float) {
        val clipId = _selectedClipId.value ?: return
        val clip = _currentProject.value?.allVideoClips?.find { it.id == clipId } ?: return
        val uriStr = clip.uriString
        if (uriStr != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, Uri.parse(uriStr))
                    val timeUs = (_currentPositionMs.value * 1000L).coerceAtLeast(0L)
                    val frameBitmap = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (frameBitmap != null) {
                        val px = (normX.coerceIn(0f, 1f) * (frameBitmap.width - 1)).toInt()
                        val py = (normY.coerceIn(0f, 1f) * (frameBitmap.height - 1)).toInt()
                        val pixel = frameBitmap.getPixel(px, py)
                        val colorHex = (pixel.toLong() and 0xFFFFFFFFL) or 0xFF000000L
                        frameBitmap.recycle()
                        retriever.release()
                        withContext(Dispatchers.Main) {
                            val curr = clip.chromaKey
                            setClipChromaKey(clipId, curr.copy(isEnabled = true, keyColorHex = colorHex))
                        }
                    } else {
                        retriever.release()
                    }
                } catch (e: Exception) {
                    AppLog.error("EditorViewModel", "Failed to sample chroma color at $normX, $normY", e)
                }
            }
        }
    }

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
    fun setTrackMuted(trackId: String, muted: Boolean) {
        updateProject { proj ->
            if (trackId == TimelineTrackIds.MAIN) {
                return@updateProject proj.copy(isMainTrackMuted = muted)
            }
            proj.copy(
                overlayTracks = proj.overlayTracks.map { track ->
                    if (track.id == trackId) track.copy(isMuted = muted) else track
                }
            )
        }
    }

    fun setTrackLocked(trackId: String, locked: Boolean) {
        updateProject { proj ->
            if (trackId == TimelineTrackIds.MAIN) {
                return@updateProject proj.copy(isMainTrackLocked = locked)
            }
            proj.copy(
                overlayTracks = proj.overlayTracks.map { track ->
                    if (track.id == trackId) track.copy(isLocked = locked) else track
                }
            )
        }
    }

    /** Solo silences every other video track's audio, like an audio desk. */
    fun setTrackSolo(trackId: String, solo: Boolean) {
        updateProject { proj ->
            if (trackId == TimelineTrackIds.MAIN) {
                return@updateProject proj.copy(isMainTrackSolo = solo)
            }
            val cleared = if (solo) proj.copy(
                isMainTrackSolo = false,
                overlayTracks = proj.overlayTracks.map { it.copy(isSolo = false) }
            ) else {
                proj
            }
            if (solo) {
                cleared.copy(
                    overlayTracks = cleared.overlayTracks.map { track ->
                        if (track.id == trackId) track.copy(isSolo = true) else track
                    }
                )
            } else {
                cleared
            }
        }
    }

    /** Mutes or unmutes a single clip (audio gain only; the video stays visible). */
    fun setClipMuted(clipId: String, muted: Boolean) {
        updateProject { proj ->
            proj.copy(
                clips = proj.clips.map {                    if (it.id == clipId) it.copy(isMuted = muted) else it
                },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map {
                            if (it.id == clipId) it.copy(isMuted = muted) else it
                        }
                    )
                }
            )
        }
    }

    /** Mutes or unmutes every audio clip at once (the "Audio" row header). */
    fun setAllAudioMuted(muted: Boolean) {
        val project = _currentProject.value ?: return
        if (project.audioTracks.isEmpty()) return
        updateProject { proj ->
            proj.copy(audioTracks = proj.audioTracks.map { it.copy(isMuted = muted) })
        }
    }

    /**
     * Trims the selected clip from an inline timeline handle.
     *
     * [ClipTrimHandle] owns the source maths; this only applies the result, so the
     * sheet trim and the handles can never disagree. Locked tracks refuse it.
     */
    fun trimSelectedClipEdge(edge: ClipTrimHandle.Edge, outputDeltaMs: Long, fps: Int) {
        val clipId = _selectedClipId.value ?: return
        updateProject(
            coalesceKey = "trim:$clipId:$edge"
        ) { proj ->
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val clip = (proj.clips + proj.overlayTracks.flatMap { it.clips })
                .firstOrNull { it.id == clipId }
                ?: return@updateProject proj
            val result = ClipTrimHandle.trim(clip, edge, outputDeltaMs, fps)
                ?: return@updateProject proj
            proj.withUpdatedClip(clipId) { updated ->
                updated.copy(
                    trimStartMs = result.trimStartMs,
                    trimEndMs = result.trimEndMs,
                    durationMs = result.durationMs
                )
            }
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
            AppLog.debug("EditorViewModel", "Fetching waveform for uri=$uriString")
            val waveform = MediaMetadata.waveform(ctx, uri, 48) ?: emptyList()
            val realTitle = title.takeIf { it.isNotBlank() }
                ?: MediaMetadata.displayName(ctx, uri)
            // Place clip after existing clips on the timeline
            val currentProject = _currentProject.value
            val lastEndMs = currentProject?.clips?.maxOfOrNull { it.startTimelineMs + it.durationMs } ?: 0L
            AppLog.debug("EditorViewModel", "Creating clip: title=$realTitle, startTimelineMs=$lastEndMs, durationMs=$durationMs, waveformSize=${waveform.size}")
            val clip = MediaClip(
                id = UUID.randomUUID().toString(),
                title = realTitle,
                uriString = uriString,
                startTimelineMs = lastEndMs,
                durationMs = if (durationMs > 0) durationMs else 4000L,
                originalDurationMs = if (durationMs > 0) durationMs else 4000L,
                waveform = waveform.ifEmpty { emptyList() },
                filter = VideoFilter.NONE
            )
            _isImporting.value = false
            updateProject { proj ->
                proj.copy(clips = proj.clips + clip)
            }
            _selectedClipId.value = clip.id
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
            uris.forEachIndexed { index, uriString ->
                AppLog.info("EditorViewModel", "Importing URI ${index + 1}/${uris.size}: $uriString")
                addClipFromUri(uriString, titles.getOrElse(index) { "Clip ${index + 1}" })
            }
            AppLog.info("EditorViewModel", "importMultipleUris() completed all imports")
            onComplete()
        }
    }

    /** Launch import of multiple URIs from Composable (non-suspend context). */
    fun launchImportUris(uris: List<android.net.Uri>) {
        val uriStrings = uris.map { it.toString() }
        val titles = uriStrings.indices.map { "Clip ${it + 1}" }
        AppLog.info("EditorViewModel", "launchImportUris() called: ${uris.size} URIs")
        importMultipleUris(uriStrings, titles) { }
    }

    /** Updates the trim of the selected clip. Streams collapse into one undo. */
    fun updateClipTrim(trimStartMs: Long, trimEndMs: Long) {
        val clipId = _selectedClipId.value ?: return
        if (_currentProject.value?.isTrackLockedForClip(clipId) == true) return
        updateProject(coalesceKey = "trim:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val newTrimStart = trimStartMs.coerceIn(
                    0L,
                    clip.originalDurationMs.coerceAtLeast(300L)
                )
                val newTrimEnd = trimEndMs.coerceIn(
                    newTrimStart + 300L,
                    clip.originalDurationMs.coerceAtLeast(newTrimStart + 300L)
                )
                val trimmed = clip.copy(
                    trimStartMs = newTrimStart,
                    trimEndMs = newTrimEnd
                )
                trimmed.copy(durationMs = ClipTiming.presentationDurationMs(trimmed))
            }
        }
    }

    /**
     * Sets a constant speed. Picking a constant speed clears the velocity curve
     * (the two modes are mutually exclusive, like CapCut), and the timeline
     * duration is recomputed through the shared [ClipTiming] implementation.
     */
    fun setClipSpeed(speed: Float) {
        val clipId = _selectedClipId.value ?: return
        updateProject(coalesceKey = "speed:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val updatedClip = clip.copy(
                    speed = speed.coerceIn(0.1f, 16f),
                    speedCurve = SpeedCurve()
                )
                updatedClip.copy(durationMs = ClipTiming.presentationDurationMs(updatedClip))
            }
        }
    }

    /** Applies a velocity curve to the selected clip and retimes the timeline. */
    fun setClipSpeedCurve(curve: SpeedCurve) {
        val clipId = _selectedClipId.value ?: return
        updateProject(coalesceKey = "speedCurve:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val updatedClip = clip.copy(speedCurve = curve)
                updatedClip.copy(durationMs = ClipTiming.presentationDurationMs(updatedClip))
            }
        }
    }

    fun setClipFilter(filter: VideoFilter) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(filter = filter) }
        }
    }

    fun setClipTransition(transition: TransitionType) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(transition = transition) }
        }
    }

    fun setClipAnimation(animation: ClipAnimation) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(animationIn = animation) }
        }
    }

    fun rotateSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(rotation = (clip.rotation + 90f) % 360f)
            }
        }
    }

    fun mirrorSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(isMirrored = !clip.isMirrored) }
        }
    }

    fun updateClipAdjustments(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        vignette: Float
    ) {
        val clipId = _selectedClipId.value ?: return
        updateProject(coalesceKey = "adjust:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    brightness = brightness.coerceIn(-1f, 1f),
                    contrast = contrast.coerceIn(0.1f, 2f),
                    saturation = saturation.coerceIn(0f, 2f),
                    vignette = vignette.coerceIn(0f, 1f)
                )
            }
        }
    }

    /** Volume slider streams collapse into one undo step. */
    fun setClipVolume(volume: Float) {
        val clipId = _selectedClipId.value ?: return
        updateProject(coalesceKey = "volume:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(volume = volume.coerceIn(0f, 10f)) }
        }
    }

    // ==========================================
    // Audio Tracks
    // ==========================================

    /** Adds an audio track from a URI (real duration + waveform). */
    fun addAudioTrackFromUri(uriString: String, title: String, artist: String = "Unknown") {
        val ctx = context ?: return
        val uri = Uri.parse(uriString)
        viewModelScope.launch {
            val durationMs = MediaMetadata.durationMs(ctx, uri)
            val waveform = MediaMetadata.waveform(ctx, uri, 48) ?: emptyList()
            val realTitle = title.takeIf { it.isNotBlank() }
                ?: MediaMetadata.displayName(ctx, uri)
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = realTitle,
                artist = artist,
                uriString = uriString,
                startTimelineMs = _currentPositionMs.value,
                durationMs = if (durationMs > 0) durationMs else 6000L,
                waveform = waveform.ifEmpty { emptyList() }
            )
            updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
        }
    }

    fun addAudioTrack(audioClip: AudioClip) {
        updateProject { proj ->
            val track = audioClip.copy(
                id = UUID.randomUUID().toString(),
                startTimelineMs = _currentPositionMs.value
            )
            proj.copy(audioTracks = proj.audioTracks + track)
        }
    }

    fun deleteAudioTrack(id: String) {
        updateProject { proj ->
            proj.copy(audioTracks = proj.audioTracks.filterNot { it.id == id })
        }
    }

    /**
     * Extracts the selected clip's audio as a real timeline audio track. The
     * source trim and speed are carried over, so the extracted track lines up
     * with the picture in both preview and export instead of starting from the
     * beginning of the original recording.
     */
    fun extractAudioFromSelectedClip() {
        val ctx = context ?: return
        val project = _currentProject.value ?: return
        val selectedId = _selectedClipId.value

        var found: MediaClip? = null
        var startMs = 0L
        var cursor = 0L
        for (clip in project.clips) {
            if (clip.id == selectedId) {
                found = clip
                startMs = cursor
                break
            }
            cursor += clip.durationMs
        }
        if (found == null) {
            project.overlayTracks.forEach { track ->
                track.clips.firstOrNull { it.id == selectedId }?.let { clip ->
                    found = clip
                    startMs = clip.startTimelineMs
                }
            }
        }

        val clip = found ?: return
        val uriString = clip.uriString
        if (uriString.isNullOrBlank()) {
            AppLog.warn(
                "EditorViewModel",
                "Cannot extract audio: '${clip.title}' has no media source"
            )
            return
        }

        val sourceLength = ClipTiming.sourceLengthMs(clip)
        val trimStart = clip.trimStartMs.coerceAtLeast(0L)
        val trimEnd = if (clip.trimEndMs > trimStart) clip.trimEndMs else trimStart + sourceLength
        val effectiveSpeed = if (ClipTiming.hasSpeedCurve(clip)) {
            (sourceLength.toFloat() / clip.durationMs.coerceAtLeast(1L)).coerceIn(0.1f, 16f)
        } else {
            clip.speed.coerceIn(0.1f, 16f)
        }

        viewModelScope.launch {
            val waveform = withContext(Dispatchers.IO) {
                MediaMetadata.waveform(ctx, Uri.parse(uriString), 48)
            } ?: emptyList()
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "${clip.title} (Audio)",
                artist = "Extracted",
                uriString = uriString,
                startTimelineMs = startMs,
                durationMs = clip.durationMs.coerceAtLeast(300L),
                trimStartMs = trimStart,
                trimEndMs = trimEnd,
                speed = effectiveSpeed,
                volume = clip.volume.coerceIn(0f, 2f),
                waveform = waveform
            )
            updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
            AppLog.info(
                "EditorViewModel",
                "Extracted audio from '${clip.title}': start=${startMs}ms, duration=${audio.durationMs}ms"
            )
        }
    }

    /** Starts a real microphone recording. */
    fun startVoiceRecording() {
        val recorder = voiceRecorder ?: return
        val ok = recorder.start()
        if (ok) {
            _isRecordingVoice.value = true
            _voiceRecordingDurationMs.value = 0L
            voiceRecordJob?.cancel()
            voiceRecordJob = viewModelScope.launch {
                while (isActive && _isRecordingVoice.value) {
                    delay(100L)
                    _voiceRecordingDurationMs.value += 100L
                }
            }
        }
    }

    /** Stops the recording and adds the clip to the timeline. */
    fun stopAndSaveVoiceRecording() {
        val recorder = voiceRecorder ?: return
        val file = recorder.stop()
        _isRecordingVoice.value = false
        voiceRecordJob?.cancel()
        voiceRecordJob = null

        if (file != null && file.exists()) {
            val duration = _voiceRecordingDurationMs.value
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Voiceover",
                artist = "You",
                uriString = file.absolutePath,
                startTimelineMs = _currentPositionMs.value,
                durationMs = duration.coerceAtLeast(500L),
                isVoiceover = true,
                waveform = emptyList() // could compute later
            )
            updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorder?.cancel()
        _isRecordingVoice.value = false
        _voiceRecordingDurationMs.value = 0L
        voiceRecordJob?.cancel()
        voiceRecordJob = null
    }

    /** Generates a voice-over from text using the device TTS engine. */
    fun startTtsVoiceover(text: String) {
        val tts = speechSynthesizer ?: return
        _activeSheet.value = ActiveToolSheet.TTS_VOICEOVER
        tts.synthesize(text) { file, error ->
            if (file != null) {
                val audio = AudioClip(
                    id = UUID.randomUUID().toString(),
                    title = "AI Voiceover",
                    artist = "CatByte TTS",
                    uriString = file.absolutePath,
                    startTimelineMs = _currentPositionMs.value,
                    durationMs = MediaDurationFallback.durationMs(context!!, Uri.fromFile(file)),
                    isVoiceover = true,
                    waveform = emptyList()
                )
                updateProject { proj ->
                    proj.copy(audioTracks = proj.audioTracks + audio)
                }
                }
            // The UI can show error if needed
        }
    }

    // ==========================================
    // Text & Subtitles
    // ==========================================

    data class SubtitleImportResult(
        val imported: Int,
        val issues: List<String> = emptyList()
    )

    /** Refuses to parse caption files larger than 2 MB. */
    private val MAX_SUBTITLE_BYTES = 2 * 1024 * 1024L

    /**
     * Parses an SRT/VTT file's contents and places every cue as a bottom-centred
     * text overlay. Existing overlays are kept; the first imported cue is
     * selected so the sheet opens on something real.
     */
    fun importSubtitles(raw: String): SubtitleImportResult {
        val project = _currentProject.value ?: return SubtitleImportResult(0, listOf("No project is open"))
        if (raw.length > MAX_SUBTITLE_BYTES) {
            return SubtitleImportResult(0, listOf("File is too large (max 2 MB)"))
        }
        val cues = SubtitleIO.parse(raw)
        if (cues.isEmpty()) {
            return SubtitleImportResult(0, listOf("No valid timecodes found in the file"))
        }
        val issues = CaptionBridge.issues(cues, project.totalDurationMs)
        val overlays = CaptionBridge.overlaysFromCues(cues)
        updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + overlays)
        }
        // Select the first imported cue so the sheet opens on something real.
        _selectedTextId.value = overlays.firstOrNull()?.id
        return SubtitleImportResult(overlays.size, issues)
    }

    /**
     * Serialises the timeline's text overlays as an SRT file, or a VTT file when
     * [toVtt] is set. Returns null when there is nothing worth exporting.
     */
    fun exportSubtitles(toVtt: Boolean = false): String? {
        val project = _currentProject.value ?: return null
        val cues = CaptionBridge.cuesFromOverlays(project.textOverlays)
        if (cues.isEmpty()) return null
        return if (toVtt) SubtitleIO.toVtt(cues) else SubtitleIO.toSrt(cues)
    }

    /**
     * Places offline sample captions on the timeline. Cues come from
     * [AutoCaptionManager], so a three-second phrase becomes two or three short
     * subtitle cards instead of one wall of text.
     */
    fun addAutoCaptions() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val bengali = _appLanguage.value.tag?.startsWith("bn") == true
            val overlays = AutoCaptionManager.generateCaptions(
                durationMs = project.totalDurationMs,
                language = if (bengali) "Bengali" else "English",
            )
            if (overlays.isEmpty()) return@launch
            updateProject { proj ->
                proj.copy(textOverlays = proj.textOverlays + overlays)
            }
            _selectedTextId.value = overlays.first().id
            AppLog.info("EditorViewModel", "Added ${overlays.size} auto captions")
        }
    }

    fun addTextOverlay(
        text: String = "Double tap to edit",
        fontStyle: TextFontStyle = TextFontStyle.CAPCUT_BOLD,
        color: Long = 0xFFFFFFFF,
        animation: TextAnimationType = TextAnimationType.POP_UP
    ) {
        val newText = TextOverlay(
            id = UUID.randomUUID().toString(),
            text = text,
            startTimelineMs = _currentPositionMs.value,
            durationMs = 3000L,
            fontStyle = fontStyle,
            color = color,
            animation = animation,
            posX = 0.5f,
            posY = 0.5f
        )
        updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays + newText)
        }
        _selectedTextId.value = newText.id
    }

    fun updateSelectedText(
        text: String,
        fontStyle: TextFontStyle,
        color: Long,
        animation: TextAnimationType
    ) {
        val id = _selectedTextId.value ?: return
        updateProject { proj ->
            val updated = proj.textOverlays.map { item ->
                if (item.id == id) {
                    item.copy(text = text, fontStyle = fontStyle, color = color, animation = animation)
                } else item
            }
            proj.copy(textOverlays = updated)
        }
    }

    fun deleteSelectedText() {
        val id = _selectedTextId.value ?: return
        updateProject { proj ->
            proj.copy(textOverlays = proj.textOverlays.filterNot { it.id == id })
        }
        _selectedTextId.value = null
    }

    // ==========================================
    // Stickers & Effects
    // ==========================================

    fun addSticker(symbol: String) {
        val sticker = StickerOverlay(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            startTimelineMs = _currentPositionMs.value,
            durationMs = 2500L,
            posX = 0.5f,
            posY = 0.4f,
            scale = 1.0f
        )
        updateProject { proj ->
            proj.copy(stickers = proj.stickers + sticker)
        }
    }

    fun deleteSticker(id: String) {
        updateProject { proj ->
            proj.copy(stickers = proj.stickers.filterNot { it.id == id })
        }
    }

    fun addTimelineEffect(effectType: EffectType) {
        val effect = TimelineEffect(
            id = UUID.randomUUID().toString(),
            name = effectType.displayName,
            effectType = effectType,
            startTimelineMs = _currentPositionMs.value,
            durationMs = 2500L
        )
        updateProject { proj ->
            proj.copy(effects = proj.effects + effect)
        }
    }

    fun deleteTimelineEffect(id: String) {
        updateProject { proj ->
            proj.copy(effects = proj.effects.filterNot { it.id == id })
        }
    }

    // ==========================================
    // Canvas & Aspect Ratio
    // ==========================================

    fun setAspectRatio(ratio: AspectRatioType) {
        updateProject { proj ->
            proj.copy(aspectRatio = ratio)
        }
    }

    fun setPaperStyle(style: String) {
        updateProject { proj ->
            proj.copy(paperStyle = style)
        }
    }

    // ==========================================
    // Export & Rendering Pipeline
    // ==========================================

    fun startExport(
        resolution: String = _defaultResolution.value,
        fps: Int = _defaultFps.value
    ) {
        val project = _currentProject.value ?: return
        pausePlayback()
        exportJob?.cancel()
        _exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Preparing timeline...",
            resolution = resolution,
            fps = fps
        )

        exportJob = viewModelScope.launch {
            if (exportManager != null) {
                // Mirror the renderer's live progress (including compatibility
                // warnings) into the export sheet instead of showing a frozen
                // 0% until the MP4 is finished.
                val progressJob = launch {
                    exportManager.progress.collect { renderProgress ->
                        if (_exportState.value.isExporting) {
                            _exportState.value = _exportState.value.copy(
                                progress = renderProgress.progress.coerceIn(0f, 1f),
                                stage = renderProgress.stage.ifBlank { _exportState.value.stage },
                                warnings = renderProgress.warnings
                            )
                        }
                    }
                }
                val result = exportManager.export(project, resolution, fps, _watermarkEnabled.value)
                progressJob.cancel()
                val renderWarnings = exportManager.progress.value.warnings
                if (result != null) {
                    _exportState.value = _exportState.value.copy(
                        isExporting = false,
                        isCompleted = true,
                        progress = 1.0f,
                        stage = "Export complete!",
                        videoPath = result.file.absolutePath,
                        galleryUri = result.galleryUri?.toString() ?: "",
                        warnings = renderWarnings
                    )
                } else {
                    _exportState.value = _exportState.value.copy(
                        isExporting = false,
                        stage = exportManager.progress.value.error ?: "Export failed",
                        warnings = renderWarnings
                    )
                }
            } else {
                // Fallback simulated export (kept for compatibility)
                val stages = listOf(
                    "Analyzing clips & keyframes..." to 0.20f,
                    "Rendering transitions & color grading..." to 0.50f,
                    "Synthesizing audio tracks & voiceover..." to 0.75f,
                    "Encoding MP4 (${resolution} ${fps}fps)..." to 0.95f,
                    "Finalizing video file..." to 1.0f
                )

                for ((stageText, targetProgress) in stages) {
                    val startProgress = _exportState.value.progress
                    val steps = 8
                    for (s in 1..steps) {
                        delay(80L)
                        val p = startProgress + (targetProgress - startProgress) * (s.toFloat() / steps)
                        _exportState.value = _exportState.value.copy(
                            progress = p,
                            stage = stageText
                        )
                    }
                }

                _exportState.value = _exportState.value.copy(
                    isExporting = false,
                    isCompleted = true,
                    progress = 1.0f,
                    stage = "Export complete!",
                    videoPath = "/storage/emulated/0/Movies/CatByte/Export_${System.currentTimeMillis()}.mp4"
                )
            }
        }
    }

    fun dismissExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportUiState()
        _activeSheet.value = ActiveToolSheet.NONE
    }

    /** Starts a background export with explicit context (called from UI). */
    fun startBackgroundExportWithContext(
        context: Context,
        resolution: String = _defaultResolution.value,
        fps: Int = _defaultFps.value
    ) {
        val project = _currentProject.value ?: return
        pausePlayback()

        _exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            stage = "Queued for background export...",
            resolution = resolution,
            fps = fps
        )

        ExportServiceScheduler.startExport(context, project, resolution, fps, _watermarkEnabled.value)
    }

    /** Stops an in-flight render and closes the export sheet. */
    fun cancelExport() {
        exportManager?.cancel()
        dismissExport()
    }

    // ==========================================
    // Phase 1 Actions: Keyframes, PIP, Blend & Mask
    // ==========================================

    fun addKeyframe(
        clipId: String,
        property: KeyframeProperty,
        value: Float,
        easing: EasingCurve = EasingCurve.EASE_IN_OUT
    ) {
        val currentPos = _currentPositionMs.value
        updateProject(coalesceKey = "keyframe:$clipId:$property") { proj ->
            val clipLocalMs = proj.localTimeForClip(clipId, currentPos) ?: return@updateProject proj
            proj.withUpdatedClip(clipId) { clip ->
                val existing = clip.keyframes.filterNot {
                    it.property == property &&
                        kotlin.math.abs(it.timestampMs - clipLocalMs) < 100L
                }
                val newKf = Keyframe(
                    timestampMs = clipLocalMs,
                    property = property,
                    value = value,
                    easing = easing
                )
                clip.copy(keyframes = (existing + newKf).sortedBy { it.timestampMs })
            }
        }
    }

    fun removeKeyframe(clipId: String, keyframeId: String) {
        updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(keyframes = clip.keyframes.filterNot { it.id == keyframeId })
            }
        }
    }

    /** Toggles a keyframe at the current playhead position on the selected clip. */
    fun toggleKeyframeAtPlayhead() {
        val clipId = _selectedClipId.value ?: return
        val currentPos = _currentPositionMs.value
        val project = _currentProject.value ?: return
        val clipLocalMs = project.localTimeForClip(clipId, currentPos) ?: return

        var existingKfId: String? = null
        project.clips.find { it.id == clipId }?.let { clip ->
            existingKfId = clip.keyframes.firstOrNull { kotlin.math.abs(it.timestampMs - clipLocalMs) < 150L }?.id
        }
        if (existingKfId == null) {
            project.overlayTracks.flatMap { it.clips }.find { it.id == clipId }?.let { clip ->
                existingKfId = clip.keyframes.firstOrNull { kotlin.math.abs(it.timestampMs - clipLocalMs) < 150L }?.id
            }
        }

        if (existingKfId != null) {
            removeKeyframe(clipId, existingKfId!!)
        } else {
            addKeyframe(clipId, KeyframeProperty.SCALE, 1.0f)
        }
    }

    /**
     * Adds a PIP/overlay clip. The source duration is probed from the real
     * media file instead of assuming four seconds, otherwise the overlay would
     * be cut short (or stretched into black) in both preview and export.
     */
    fun addOverlayClipFromUri(uriString: String, title: String) {
        val currentPos = _currentPositionMs.value
        viewModelScope.launch {
            val ctx = context
            val probedMs = if (ctx != null) {
                withContext(Dispatchers.IO) {
                    MediaMetadata.durationMs(ctx, Uri.parse(uriString))
                }
            } else {
                0L
            }
            val durationMs = if (probedMs > 0L) probedMs else 4000L
            val newClip = MediaClip(
                id = "pip_${UUID.randomUUID().toString().take(8)}",
                title = title,
                uriString = uriString,
                startTimelineMs = currentPos,
                durationMs = durationMs,
                originalDurationMs = durationMs,
                scale = 0.5f,
                posX = 0.5f,
                posY = 0.5f
            )
            updateProject { proj ->
                val currentTracks = proj.overlayTracks.toMutableList()
                if (currentTracks.isEmpty()) {
                    currentTracks.add(
                        VideoTrack(
                            name = "PIP Track 1",
                            isMainTrack = false,
                            clips = listOf(newClip)
                        )
                    )
                } else {
                    val track = currentTracks.first()
                    currentTracks[0] = track.copy(clips = track.clips + newClip)
                }
                proj.copy(overlayTracks = currentTracks)
            }
            _selectedClipId.value = newClip.id
            refreshPreviewNow()
        }
    }

    /** Updates the PIP transform used by the compositor and keyframe sheet. */
    fun updateOverlayTransform(
        clipId: String,
        scale: Float? = null,
        posX: Float? = null,
        posY: Float? = null,
        opacity: Float? = null
    ) {
        updateProject(coalesceKey = "pipTransform:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    scale = scale?.coerceIn(0.05f, 4f) ?: clip.scale,
                    posX = posX?.coerceIn(0f, 1f) ?: clip.posX,
                    posY = posY?.coerceIn(0f, 1f) ?: clip.posY,
                    opacity = opacity?.coerceIn(0f, 1f) ?: clip.opacity
                )
            }
        }
    }

    fun setClipBlendMode(clipId: String, mode: BlendMode) {
        updateProject { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(blendMode = mode) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(blendMode = mode) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }

    fun setClipMask(clipId: String, maskData: MaskData) {
        updateProject { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(mask = maskData) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(mask = maskData) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }

    fun setClipHslAdjustments(clipId: String, adjustments: List<HslAdjustment>) {
        updateProject { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(hslAdjustments = adjustments) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(hslAdjustments = adjustments) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }

    /**
     * Beat-sync: splits clips at detected musical beats (energy onset detection
     * with a BPM-based synthetic fallback).
     */
    /**
     * Cuts every main-track clip at the musical beats of the audio bed.
     *
     * Beats come from the same detection path the timeline magnets use; the
     * split itself goes through [ClipSplit] so trims, speed, keyframes and
     * transition ownership survive the cut.
     */
    fun autoCutToBeat(beatsPerMinute: Int = 120) {
        val proj = _currentProject.value ?: return
        val ctx = context ?: return
        if (proj.isMainTrackLocked) return
        viewModelScope.launch {
            val beats = if (_snapBeats.value.isNotEmpty()) {
                _snapBeats.value
            } else {
                val firstUri = proj.audioTracks.firstOrNull { !it.uriString.isNullOrBlank() }?.uriString
                    ?: proj.clips.firstOrNull { !it.uriString.isNullOrBlank() }?.uriString
                com.example.data.BeatDetector.detectBeats(
                    ctx, firstUri, proj.totalDurationMs, beatsPerMinute
                )
            }
            if (beats.isEmpty()) return@launch

            updateProject { current ->
                val newClips = mutableListOf<MediaClip>()
                var acc = 0L
                for (clip in current.clips) {
                    val clipEnd = acc + clip.durationMs
                    val localBeats = BeatGrid.cutOffsets(
                        durationMs = clip.durationMs,
                        beatOffsetsMs = beats
                            .filter { it in acc until clipEnd }
                            .map { it - acc }
                    )
                    if (localBeats.isEmpty()) {
                        newClips.add(clip.copy(startTimelineMs = acc))
                    } else {
                        var remaining = clip
                        var consumed = 0L
                        for (beat in localBeats) {
                            val offset = beat - consumed
                            val split = ClipSplit.split(remaining, offset) {
                                "beat_${UUID.randomUUID().toString().take(8)}"
                            } ?: continue
                            newClips.add(split.first.copy(startTimelineMs = acc + consumed))
                            remaining = split.second
                            consumed = beat
                        }
                        newClips.add(remaining.copy(startTimelineMs = acc + consumed))
                    }
                    acc = clipEnd
                }
                current.copy(clips = newClips)
            }
        }
    }

    /**
     * Exports a real MP4 and reports where it can be shared.
     */
    fun shareExportMessage(platform: String): String {
        val path = _exportState.value.videoPath
        return when {
            path.isBlank() -> "Export the project first."
            !File(path).exists() -> "Export failed — no MP4 was produced."
            else -> "Video ready at $path — shared to $platform."
        }
    }
}