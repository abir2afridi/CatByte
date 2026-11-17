package com.example.viewmodel.features

import android.net.Uri
import com.example.data.AppLog
import com.example.data.ProjectBackupManager
import com.example.data.ProjectRepository
import com.example.viewmodel.EditorViewModel
import java.util.UUID

/**
 * Project backup/restore actions used by the home screen.
 *
 * Both operations are blocking file/ZIP work, so they suspend on Dispatchers.IO
 * inside the manager. The returned strings are shown as a toast and always
 * describe the real outcome (media copied, media skipped, or the failure).
 */
internal object ProjectBackupFeature {

    suspend fun backupProject(
        vm: EditorViewModel,
        projectId: String,
        destination: Uri,
        includeMedia: Boolean
    ): String {
        val context = vm.context ?: return "Backup needs the app context"
        val project = vm.repository.getProject(projectId) ?: return "Project not found"
        return runCatching {
            val summary = ProjectBackupManager(context).export(destination, project, includeMedia)
            val skipped = if (summary.mediaSkipped > 0) {
                ", ${summary.mediaSkipped} unreadable file(s) skipped"
            } else {
                ""
            }
            if (!includeMedia) {
                "Backup saved (project only — media can be relinked after restore)"
            } else {
                "Backup saved: ${summary.mediaWritten} media file(s)$skipped"
            }
        }.getOrElse { error ->
            AppLog.error("ProjectBackup", "Could not back up project $projectId", error)
            "Backup failed: ${error.message ?: "unknown error"}"
        }
    }

    suspend fun restoreBackup(vm: EditorViewModel, source: Uri): String {
        val context = vm.context ?: return "Restore needs the app context"
        return runCatching {
            val result = ProjectBackupManager(context).import(source)
            var restored = result.project
            if (restored.id.isBlank()) {
                restored = restored.copy(id = newProjectId())
            }
            // Importing the same backup twice must not overwrite the project
            // that is already in the library.
            if (vm.repository.getProject(restored.id) != null) {
                restored = restored.copy(
                    id = newProjectId(),
                    title = uniqueTitle(restored.title, vm.repository)
                )
            }
            val title = restored.title.trim().ifBlank { "Restored project" }
                .take(ProjectRepository.MAX_TITLE_LENGTH)
            val saved = restored.copy(title = title)
            vm.repository.saveProject(saved)
            AppLog.info(
                "ProjectBackup",
                "Restored project ${saved.id} with ${result.media.size} embedded media file(s)"
            )
            if (result.media.isEmpty()) {
                "Restored \"$title\" — relink media with the missing-media sheet"
            } else {
                "Restored \"$title\" with ${result.media.size} media file(s)"
            }
        }.getOrElse { error ->
            AppLog.error("ProjectBackup", "Could not restore the backup", error)
            "Restore failed: ${error.message ?: "unknown error"}"
        }
    }

    private fun newProjectId(): String =
        "proj_${UUID.randomUUID().toString().take(8)}"

    /** Adds a numeric suffix until the title is free (bounded, deterministic). */
    private fun uniqueTitle(title: String, repository: ProjectRepository): String {
        val base = title.trim().ifBlank { "Restored project" }
        val cap = ProjectRepository.MAX_TITLE_LENGTH
        val suffixLimit = 2..99
        for (suffix in suffixLimit) {
            val candidate = "${base.take(cap - 5)} ($suffix)"
            if (repository.projects.value.none { it.title == candidate }) return candidate
        }
        return "${base.take(cap - 15)} (Imported ${System.currentTimeMillis() % 1000})"
    }
}
