package com.example.data

import com.example.model.AspectRatioType
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProjectRepository {
    private val _projects = MutableStateFlow<List<VideoProject>>(SampleDataProvider.getInitialProjects())
    val projects: StateFlow<List<VideoProject>> = _projects.asStateFlow()

    fun getProject(id: String): VideoProject? {
        return _projects.value.find { it.id == id }
    }

    fun saveProject(project: VideoProject) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = project.copy(lastModified = System.currentTimeMillis())
        } else {
            current.add(0, project.copy(lastModified = System.currentTimeMillis()))
        }
        _projects.value = current
    }

    fun toggleFavorite(id: String) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(isFavorite = !current[index].isFavorite)
            _projects.value = current
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
    }

    /** Re-inserts a previously deleted project, preserving its id and content. */
    fun restoreProject(project: VideoProject) {
        val current = _projects.value.toMutableList()
        if (current.none { it.id == project.id }) {
            current.add(0, project)
            _projects.value = current
        }
    }
}
