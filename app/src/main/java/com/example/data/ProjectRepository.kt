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
import kotlinx.coroutines.SupervisorJob
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
        if (rows.isEmpty()) {
            // First launch: store the starter drafts so the home screen is not empty.
            val seeds = SampleDataProvider.getInitialProjects()
            runCatching { dao.upsertAll(seeds.map { it.toEntity() }) }
                .onFailure { AppLog.error("ProjectRepository", "Could not seed the project database", it) }
            _projects.value = seeds
            return
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

    private fun persist(project: VideoProject) {
        val dao = dao ?: return
        scope.launch {
            runCatching { dao.upsert(project.toEntity()) }
                .onFailure { AppLog.error("ProjectRepository", "Could not store the project", it) }
        }
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
