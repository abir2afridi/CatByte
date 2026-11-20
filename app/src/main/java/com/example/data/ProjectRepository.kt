package com.example.data

import android.content.Context
import com.example.data.db.CatByteDatabase
import com.example.data.db.ProjectDao
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectJson
import com.example.model.AspectRatioType
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Stores the user projects. When a database is available the projects are
 * persisted with Room so they survive an app restart, otherwise the repository
 * works purely in memory (used by the unit tests).
 */
class ProjectRepository(context: Context? = null) {

    private val _projects = MutableStateFlow<List<VideoProject>>(SampleDataProvider.getInitialProjects())
    val projects: StateFlow<List<VideoProject>> = _projects.asStateFlow()

    private val dao: ProjectDao? = context?.let {
        runCatching { CatByteDatabase.getInstance(it).projectDao() }.getOrNull()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        if (dao != null) {
            scope.launch { loadFromDatabase() }
        }
    }

    private suspend fun loadFromDatabase() {
        val dao = dao ?: return
        val rows = runCatching { dao.getAll() }.getOrElse {
            AppLog.error("ProjectRepository", "Could not read the project database", it)
            emptyList()
        }
        val restored = rows.mapNotNull { ProjectJson.fromJson(it.payload) }
        if (restored.isNotEmpty()) {
            _projects.value = restored
            AppLog.info("ProjectRepository", "Restored ${restored.size} projects from the database")
        }
    }

    fun getProject(id: String): VideoProject? {
        return _projects.value.find { it.id == id }
    }

    fun saveProject(project: VideoProject) {
        val stamped = project.copy(lastModified = System.currentTimeMillis())
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = stamped
        } else {
            current.add(0, stamped)
        }
        _projects.value = current
        persist(stamped)
    }

    /**
     * Renames a project and persists it.
     *
     * The title is trimmed and capped at [MAX_TITLE_LENGTH] so the home list and
     * the editor title bar stay readable. Blank input and a no-op rename leave
     * the stored project untouched. Returns the updated project, or null when
     * the project does not exist / the title was unusable.
     */
    fun renameProject(id: String, title: String): VideoProject? {
        val normalized = normalizeTitle(title) ?: return null
        val current = getProject(id) ?: return null
        if (current.title == normalized) return current
        val renamed = current.copy(title = normalized)
        saveProject(renamed)
        return getProject(id)
    }

    fun toggleFavorite(id: String) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(isFavorite = !current[index].isFavorite)
            _projects.value = current
            persist(current[index])
        }
    }

    fun createNewProject(
        title: String = "New Project",
        aspectRatio: AspectRatioType = AspectRatioType.RATIO_9_16,
        paperStyle: String = "Drafting Grid",
        initialClips: List<MediaClip> = emptyList(),
        resolution: String = "1080P",
        fps: Int = 30
    ): VideoProject {
        // A new project starts with an empty timeline. Seeding it with a demo
        // video used to point every fresh project at a desktop-only file path,
        // so on a phone the first clip was unreadable and the export failed.
        val project = VideoProject(
            id = "proj_${UUID.randomUUID().toString().take(8)}",
            title = title,
            aspectRatio = aspectRatio,
            paperStyle = paperStyle,
            clips = initialClips,
            resolution = resolution,
            fps = fps,
            lastModified = System.currentTimeMillis()
        )
        saveProject(project)
        return project
    }

    fun duplicateProject(id: String) {
        val original = getProject(id) ?: return
        val duplicate = original.copy(
            id = "proj_${UUID.randomUUID().toString().take(8)}",
            title = "${original.title} (Copy)",
            lastModified = System.currentTimeMillis()
        )
        saveProject(duplicate)
    }

    fun deleteProject(id: String) {
        _projects.value = _projects.value.filterNot { it.id == id }
        scope.launch {
            runCatching { dao?.deleteById(id) }
                .onFailure { AppLog.error("ProjectRepository", "Could not delete the project", it) }
        }
    }

    /** Re-inserts a previously deleted project, preserving its id and content. */
    fun restoreProject(project: VideoProject) {
        val current = _projects.value.toMutableList()
        if (current.none { it.id == project.id }) {
            current.add(0, project)
            _projects.value = current
        }
        persist(project)
    }

    /** Deletes every stored project (used by the "clear drafts" action). */
    fun deleteAllProjects() {
        _projects.value = emptyList()
        scope.launch { runCatching { dao?.deleteAll() } }
    }

    /**
     * Debounced autosave: a trailing 400 ms window per project so a slider
     * drag (which can emit dozens of edits a second) serialises the project
     * JSON once, not once per tick. The in-memory list stays instant; only
     * the Room write is delayed, and the latest state always wins.
     */
    private val pendingSaves = java.util.concurrent.ConcurrentHashMap<String, Job>()

    private fun persist(project: VideoProject) {
        val dao = dao ?: return
        val previous = pendingSaves.put(project.id, scope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            pendingSaves.remove(project.id)
            runCatching { dao.upsert(project.toEntity()) }
                .onFailure { AppLog.error("ProjectRepository", "Could not store the project", it) }
        })
        previous?.cancel()
    }

    /** Saves any pending debounced state for [projectId] right now. */
    fun flushPendingSave(project: VideoProject) {
        val dao = dao ?: return
        pendingSaves.remove(project.id)?.cancel()
        scope.launch {
            runCatching { dao.upsert(project.toEntity()) }
                .onFailure { AppLog.error("ProjectRepository", "Could not store the project", it) }
        }
    }

    companion object {
        /** Longest project title the home list and editor bar are designed for. */
        const val MAX_TITLE_LENGTH = 80

        /** Trailing autosave window (see [persist]). */
        const val AUTOSAVE_DEBOUNCE_MS = 400L

        /** Trimmed title, or null when the input is empty after trimming. */
        fun normalizeTitle(raw: String): String? =
            raw.trim().take(MAX_TITLE_LENGTH).takeIf { it.isNotEmpty() }
    }

    private fun VideoProject.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        title = title,
        payload = ProjectJson.toJson(this),
        aspectRatio = aspectRatio.name,
        resolution = resolution,
        fps = fps,
        paperStyle = paperStyle,
        isFavorite = isFavorite,
        lastModified = lastModified
    )
}
