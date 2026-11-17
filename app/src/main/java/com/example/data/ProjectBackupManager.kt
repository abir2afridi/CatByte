package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Android side of project backup/restore: opens SAF destinations, reads the
 * project's media (content or file URIs) and copies restored media into
 * app-private storage so the project keeps working after the ZIP is gone.
 */
class ProjectBackupManager(private val context: Context) {

    /**
     * Writes [project] to [destination]. When [includeMedia] is false only the
     * project + manifest are stored (small file, relink later on restore).
     */
    suspend fun export(
        destination: Uri,
        project: com.example.model.VideoProject,
        includeMedia: Boolean
    ): ProjectBackupCodec.WriteSummary = withContext(Dispatchers.IO) {
        val assets = if (!includeMedia) {
            emptyList()
        } else {
            ProjectBackupCodec.mediaUris(project).map { uriString ->
                ProjectBackupCodec.Asset(
                    originalUri = uriString,
                    extension = extensionFor(uriString),
                    open = { openSource(uriString) }
                )
            }
        }
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: throw IOException("Could not open the backup destination")
        output.use { stream ->
            ProjectBackupCodec.write(project, stream, assets)
        }
    }

    /**
     * Reads a backup and remaps embedded media to extracted local files.
     * The returned project is not saved; [com.example.viewmodel.features.ProjectBackupFeature]
     * decides how to insert it into the library.
     */
    suspend fun import(source: Uri): ProjectBackupCodec.ReadResult = withContext(Dispatchers.IO) {
        val importDir = File(
            context.filesDir,
            "project_media/restore_${System.currentTimeMillis()}_${(0..0xFFFFF).random().toString(16)}"
        )
        if (!importDir.mkdirs()) {
            throw IOException("Could not create the restore folder")
        }
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IOException("Could not open the backup file")
            val result = input.use { stream ->
                ProjectBackupCodec.read(stream) { entryName ->
                    val fileName = entryName.substringAfterLast('/')
                    if (!ProjectBackupCodec.isSafeMediaFileName(fileName)) {
                        null
                    } else {
                        File(importDir, fileName).outputStream()
                    }
                }
            }
            val mapping = result.media.mapNotNull { entry ->
                val file = File(importDir, entry.entryName.substringAfterLast('/'))
                if (file.isFile && file.length() > 0L) entry.originalUri to file.absolutePath else null
            }.toMap()
            if (mapping.isEmpty()) {
                importDir.deleteRecursively()
            }
            result.copy(project = with(ProjectBackupCodec) { result.project.remapMediaUris(mapping) })
        } catch (t: Throwable) {
            // Never leave half-extracted media behind when the archive is bad.
            importDir.deleteRecursively()
            throw t
        }
    }

    /** Best-effort extension for the ZIP entry name (falls back to the MIME type). */
    private fun extensionFor(uriString: String): String {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
        val displayName = runCatching { uri?.lastPathSegment }.getOrNull().orEmpty()
        val fromName = displayName.substringAfterLast('.', "")
            .lowercase()
            .filter { it.isLetterOrDigit() }
        if (fromName.isNotEmpty()) return fromName.take(8)
        val mime = runCatching { uri?.let { context.contentResolver.getType(it) } }.getOrNull()
        return mime?.substringAfterLast('/')
            ?.lowercase()
            ?.filter { it.isLetterOrDigit() }
            ?.take(8)
            ?.ifEmpty { null }
            ?: "bin"
    }

    private fun openSource(uriString: String): InputStream? = runCatching {
        val uri = Uri.parse(uriString)
        when (uri.scheme) {
            null, "file" -> {
                val path = uri.path ?: uriString
                FileInputStream(File(path))
            }
            else -> context.contentResolver.openInputStream(uri)
        }
    }.getOrNull()
}
