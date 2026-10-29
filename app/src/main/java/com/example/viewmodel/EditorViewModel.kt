package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppLanguage
import com.example.data.CacheManager
import com.example.data.ProjectRepository
import com.example.data.SampleDataProvider
import com.example.data.SettingsRepository
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.ClipAnimation
import com.example.model.EffectType
import com.example.model.MediaClip
import com.example.model.StickerOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    EXPORT_MODAL
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val stage: String = "",
    val isCompleted: Boolean = false,
    val resolution: String = "1080P",
    val fps: Int = 30,
    val videoPath: String = ""
)

class EditorViewModel(
    private val repository: ProjectRepository = ProjectRepository(),
    private val settingsRepository: SettingsRepository? = null,
    private val cacheManager: CacheManager? = null
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

    private val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    // App Settings & Pencil Theme State
    private val _appThemeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.PENCIL_LIGHT)
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

    // Voice recording simulation
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()
    private val _voiceRecordingDurationMs = MutableStateFlow(0L)
    val voiceRecordingDurationMs: StateFlow<Long> = _voiceRecordingDurationMs.asStateFlow()

    // Undo / Redo history stacks
    private val undoStack = mutableListOf<VideoProject>()
    private val redoStack = mutableListOf<VideoProject>()

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

    init {
        // Select first project by default
        val initialList = repository.projects.value
        if (initialList.isNotEmpty()) {
            _currentProject.value = initialList.first()
            _selectedClipId.value = initialList.first().clips.firstOrNull()?.id
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
    }

    fun openProject(project: VideoProject) {
        pausePlayback()
        _currentProject.value = project
        _currentPositionMs.value = 0L
        _selectedClipId.value = project.clips.firstOrNull()?.id
        _selectedTextId.value = null
        _activeSheet.value = ActiveToolSheet.NONE
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
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
        paperStyle: String = "Drafting Grid"
    ) {
        pausePlayback()
        val defaultClips = listOf(
            MediaClip(
                id = UUID.randomUUID().toString(),
                title = "Scene 1 (Sketch)",
                gradientColors = listOf(0xFF1A1A1A, 0xFF2D2D2D, 0xFF3D3D3D),
                durationMs = 3500L,
                filter = VideoFilter.SKETCH_GRAPHITE
            ),
            MediaClip(
                id = UUID.randomUUID().toString(),
                title = "Scene 2 (Blueprint)",
                gradientColors = listOf(0xFF001F3F, 0xFF003366, 0xFF0074D9),
                durationMs = 4000L,
                filter = VideoFilter.BLUEPRINT
            )
        )
        val proj = repository.createNewProject(
            title = title.ifBlank { "Notebook Draft #${System.currentTimeMillis() % 1000}" },
            aspectRatio = ratio,
            paperStyle = paperStyle,
            initialClips = defaultClips,
            resolution = _defaultResolution.value,
            fps = _defaultFps.value
        )
        openProject(proj)
    }

    fun createNewProject() {
        createNewProjectWithParams("Notebook Draft #${System.currentTimeMillis() % 1000}")
    }

    fun createFromTemplate(templateId: String) {
        val tpl = SampleDataProvider.templates.find { it.id == templateId } ?: return
        val proj = repository.createFromTemplate(
            tpl,
            resolution = _defaultResolution.value,
            fps = _defaultFps.value
        )
        openProject(proj)
    }

    fun duplicateProject(id: String) {
        repository.duplicateProject(id)
    }

    fun deleteProject(id: String) {
        if (_currentProject.value?.id == id) {
            _currentProject.value = null
        }
        repository.deleteProject(id)
    }

    /** Restores a draft removed from the home screen (undo delete). */
    fun restoreProject(project: VideoProject) {
        repository.saveProject(project)
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
        }
        _isPlaying.value = true

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

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekTo(positionMs: Long) {
        val project = _currentProject.value ?: return
        _currentPositionMs.value = positionMs.coerceIn(0L, project.totalDurationMs)
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

    private fun pushHistory(project: VideoProject) {
        undoStack.add(project)
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        val curr = _currentProject.value ?: return
        if (undoStack.isNotEmpty()) {
            redoStack.add(curr)
            val prev = undoStack.removeAt(undoStack.lastIndex)
            _currentProject.value = prev
            repository.saveProject(prev)
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun redo() {
        val curr = _currentProject.value ?: return
        if (redoStack.isNotEmpty()) {
            undoStack.add(curr)
            val next = redoStack.removeAt(redoStack.lastIndex)
            _currentProject.value = next
            repository.saveProject(next)
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    private fun updateProject(transform: (VideoProject) -> VideoProject) {
        val curr = _currentProject.value ?: return
        pushHistory(curr)
        val updated = transform(curr)
        _currentProject.value = updated
        repository.saveProject(updated)
    }

    fun splitClipAtPlayhead() {
        val project = _currentProject.value ?: return
        val playhead = _currentPositionMs.value
        val clips = project.clips.toMutableList()

        // Find which clip intersects the playhead
        var accumulated = 0L
        var targetIndex = -1
        for (i in clips.indices) {
            val clipEnd = accumulated + clips[i].durationMs
            if (playhead > accumulated + 100L && playhead < clipEnd - 100L) {
                targetIndex = i
                break
            }
            accumulated = clipEnd
        }

        if (targetIndex != -1) {
            val target = clips[targetIndex]
            val splitOffsetMs = playhead - accumulated
            val remainingMs = target.durationMs - splitOffsetMs

            val firstHalf = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = splitOffsetMs,
                title = "${target.title} (Part 1)"
            )
            val secondHalf = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = remainingMs,
                title = "${target.title} (Part 2)"
            )

            clips.removeAt(targetIndex)
            clips.add(targetIndex, secondHalf)
            clips.add(targetIndex, firstHalf)

            updateProject { it.copy(clips = clips) }
            _selectedClipId.value = secondHalf.id
        }
    }

    fun deleteSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.filterNot { it.id == clipId }
            proj.copy(clips = updated)
        }
        _selectedClipId.value = _currentProject.value?.clips?.firstOrNull()?.id
    }

    fun duplicateSelectedClip() {
        val clipId = _selectedClipId.value ?: return
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
            } else proj
        }
    }

    fun addClipFromUri(uriString: String, title: String) {
        val newClip = MediaClip(
            id = UUID.randomUUID().toString(),
            title = title,
            uriString = uriString,
            durationMs = 4000L,
            filter = VideoFilter.NONE
        )
        updateProject { proj ->
            proj.copy(clips = proj.clips + newClip)
        }
        _selectedClipId.value = newClip.id
    }

    fun addSampleClip(title: String, resId: Int) {
        val newClip = MediaClip(
            id = UUID.randomUUID().toString(),
            title = title,
            drawableResId = resId,
            durationMs = 3500L,
            filter = VideoFilter.NONE
        )
        updateProject { proj ->
            proj.copy(clips = proj.clips + newClip)
        }
        _selectedClipId.value = newClip.id
    }

    fun setClipSpeed(speed: Float) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) {
                    val newDuration = ((clip.originalDurationMs / speed).toLong()).coerceAtLeast(300L)
                    clip.copy(speed = speed, durationMs = newDuration)
                } else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun setClipFilter(filter: VideoFilter) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) clip.copy(filter = filter) else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun setClipTransition(transition: TransitionType) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) clip.copy(transition = transition) else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun setClipAnimation(animation: ClipAnimation) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) clip.copy(animationIn = animation) else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun rotateSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) clip.copy(rotation = (clip.rotation + 90f) % 360f) else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun mirrorSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) clip.copy(isMirrored = !clip.isMirrored) else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun updateClipAdjustments(brightness: Float, contrast: Float, saturation: Float, vignette: Float) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) {
                    clip.copy(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        vignette = vignette
                    )
                } else clip
            }
            proj.copy(clips = updated)
        }
    }

    fun setClipVolume(volume: Float) {
        val clipId = _selectedClipId.value ?: return
        updateProject { proj ->
            val updated = proj.clips.map { clip ->
                if (clip.id == clipId) clip.copy(volume = volume) else clip
            }
            proj.copy(clips = updated)
        }
    }

    // ==========================================
    // Audio Tracks
    // ==========================================

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

    fun startVoiceRecording() {
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

    fun stopAndSaveVoiceRecording() {
        val duration = _voiceRecordingDurationMs.value
        _isRecordingVoice.value = false
        voiceRecordJob?.cancel()
        voiceRecordJob = null

        if (duration > 500L) {
            val voiceTrack = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Voiceover",
                artist = "You",
                startTimelineMs = _currentPositionMs.value,
                durationMs = duration,
                isVoiceover = true,
                waveform = listOf(0.3f, 0.6f, 0.8f, 0.9f, 0.7f, 0.85f, 0.4f, 0.6f)
            )
            updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + voiceTrack)
            }
        }
    }

    // ==========================================
    // Text & Subtitles
    // ==========================================

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
                videoPath = "/storage/emulated/0/Movies/CapCut/Export_${System.currentTimeMillis()}.mp4"
            )
        }
    }

    fun dismissExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportUiState()
        _activeSheet.value = ActiveToolSheet.NONE
    }

    /** Stops an in-flight render and closes the export sheet. */
    fun cancelExport() {
        dismissExport()
    }

    /**
     * Export is still a preview simulation; report honestly instead of pretending
     * that a shareable MP4 exists.
     */
    fun shareExportMessage(platform: String): String {
        val path = _exportState.value.videoPath
        return when {
            path.isBlank() -> "Export the project first."
            !File(path).exists() ->
                "This preview build simulates rendering, so there is no MP4 to share to $platform yet."
            else -> "Sharing to $platform is not available in this build."
        }
    }
}
