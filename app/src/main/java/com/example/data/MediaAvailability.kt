package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** One media reference stored in a project that the app cannot open. */
data class MissingMediaRef(
    val id: String,
    val title: String,
    val uriString: String,
    /** True for audio-track clips, false for main/overlay video clips. */
    val isAudio: Boolean
)

/**
 * Checks whether the media a project points at still exists.
 *
 * Content URIs granted with `takePersistableUriPermission` normally survive an
 * app restart, but they stop resolving when the user clears the gallery's data,
 * deletes the file, or revokes the grant. A persisted project that references a
 * dead URI used to open to a silent black preview with no explanation; the
 * editor now asks the user to relink the file.
 */
object MediaAvailability {

    /** Every clip that points at real media (placeholders and drawables excluded). */
    fun mediaRefs(project: VideoProject): List<MissingMediaRef> {
        val video = project.allVideoClips.mapNotNull { clip ->
            val uri = clip.uriString
            if (uri.isNullOrBlank() || clip.drawableResId != null) {
                null
            } else {
                MissingMediaRef(id = clip.id, title = clip.title, uriString = uri, isAudio = false)
            }
        }
        val audio = project.audioTracks.mapNotNull { clip ->
            val uri = clip.uriString
            if (uri.isNullOrBlank()) {
                null
            } else {
                MissingMediaRef(id = clip.id, title = clip.title, uriString = uri, isAudio = true)
            }
        }
        return video + audio
    }

    /** The subset of [project]'s media that cannot be opened right now. */
    suspend fun findMissing(context: Context, project: VideoProject): List<MissingMediaRef> {
        val refs = mediaRefs(project)
        if (refs.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            refs.filterNot { isReadable(context, it.uriString) }
        }
    }

    /**
     * True when [uriString] can still be opened. Only local schemes are probed:
     * remote URLs (`http`, `rtsp`, custom providers) are assumed good so a
     * network hiccup never turns into a false "missing media" warning.
     */
    suspend fun isReadable(context: Context, uriString: String): Boolean =
        withContext(Dispatchers.IO) {
            val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext false
            when (uri.scheme?.lowercase()) {
                "content", "android.resource" -> runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                }.getOrDefault(false)

                // A path without a scheme (e.g. a stored absolute path) is a file.
                "file", null -> runCatching {
                    val file = if (uri.scheme == null) File(uriString) else File(uri.path ?: return@runCatching false)
                    file.exists() && file.canRead()
                }.getOrDefault(false)

                // asset://, http(s)://, rtsp://, custom provider schemes.
                else -> true
            }
        }
}
