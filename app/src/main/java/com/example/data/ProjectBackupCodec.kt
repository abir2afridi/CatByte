package com.example.data

import com.example.data.db.ProjectJson
import com.example.model.MediaClip
import com.example.model.VideoProject
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Read/write engine for `.catbyte.zip` project backups.
 *
 * A backup is a plain ZIP that any file manager can inspect:
 *
 * ```
 * project.json     the complete editing project (ProjectJson)
 * manifest.json    format version + which original URI each media file was
 * media/0001.mp4   optional copies of the project's media
 * ```
 *
 * The codec is pure JVM (no Android classes) so round trips, zip-slip names
 * and zip-bomb limits are unit-testable. The Android-specific file/URI work
 * lives in [ProjectBackupManager].
 *
 * Backwards/forwards compatibility: an older app ignores unknown ZIP entries,
 * and a backup from a newer format is refused instead of being half-restored.
 */
object ProjectBackupCodec {

    const val FORMAT_VERSION = 1
    const val PROJECT_ENTRY = "project.json"
    const val MANIFEST_ENTRY = "manifest.json"

    private const val MEDIA_PREFIX = "media/"
    private const val MAX_JSON_BYTES = 8L * 1024 * 1024
    private const val MAX_MEDIA_ENTRY_BYTES = 4L * 1024 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 16L * 1024 * 1024 * 1024
    private const val MAX_ENTRIES = 4000

    /** One media source to embed. [open] is called at most once. */
    data class Asset(
        val originalUri: String,
        val extension: String,
        val open: () -> InputStream?
    )

    data class ManifestEntry(val originalUri: String, val entryName: String)

    data class WriteSummary(val mediaWritten: Int, val mediaSkipped: Int)

    data class ReadResult(val project: VideoProject, val media: List<ManifestEntry>)

    // ------------------------------------------------------------------
    // Write
    // ------------------------------------------------------------------

    /**
     * Streams [project] and [assets] as a backup ZIP. An asset whose stream
     * cannot be opened (or fails mid-copy) is skipped and counted instead of
     * aborting the whole backup; the project itself is always written last,
     * after the media, so an interrupted media copy cannot hide the project.
     */
    fun write(
        project: VideoProject,
        output: OutputStream,
        assets: List<Asset>
    ): WriteSummary {
        var written = 0
        var skipped = 0
        val manifestEntries = ArrayList<ManifestEntry>()

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            // Media is already compressed; fastest level keeps backups quick.
            zip.setLevel(Deflater.BEST_SPEED)

            assets.forEachIndexed { index, asset ->
                val entryName = mediaEntryName(index, asset.extension)
                val input = runCatching { asset.open() }.getOrNull()
                if (input == null) {
                    skipped++
                    return@forEachIndexed
                }
                try {
                    input.use { source ->
                        zip.putNextEntry(ZipEntry(entryName))
                        source.copyTo(zip, DEFAULT_BUFFER_SIZE)
                        zip.closeEntry()
                    }
                    manifestEntries += ManifestEntry(asset.originalUri, entryName)
                    written++
                } catch (e: Exception) {
                    // Providers can throw SecurityException/IllegalStateException
                    // for a vanished file, not just IOException. One unreadable
                    // asset must not fail the whole backup.
                    skipped++
                    AppLog.warn(
                        "ProjectBackup",
                        "Skipping media ${asset.originalUri}: ${e.message ?: "read failed"}"
                    )
                    // Close whatever part of the entry was written so the next
                    // entry starts cleanly; if even that fails the stream is
                    // broken and the caller reports a failed backup.
                    runCatching { zip.closeEntry() }
                }
            }

            zip.putNextEntry(ZipEntry(PROJECT_ENTRY))
            zip.write(ProjectJson.toJson(project).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestJson(project, manifestEntries).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return WriteSummary(written, skipped)
    }

    private fun manifestJson(project: VideoProject, media: List<ManifestEntry>): String =
        JSONObject().apply {
            put("format", FORMAT_VERSION)
            put("app", "CatByte")
            put("createdAt", System.currentTimeMillis())
            put("projectId", project.id)
            put("projectTitle", project.title)
            put("includesMedia", media.isNotEmpty())
            put(
                "media",
                JSONArray().also { array ->
                    media.forEach { entry ->
                        array.put(
                            JSONObject().apply {
                                put("uri", entry.originalUri)
                                put("entry", entry.entryName)
                            }
                        )
                    }
                }
            )
        }.toString()

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * Reads a backup. Media entries are streamed into the sink returned by
     * [openMediaOutput] (called with the ZIP entry name); the returned
     * [ReadResult.media] only lists entries that were actually extracted.
     *
     * Unknown entries are ignored, unsafe names (`../`, absolute paths, nested
     * folders) are never passed to the sink, and size/count limits stop a
     * malicious archive before it can fill the disk.
     *
     * @throws IOException when the input is not a backup, is truncated, or
     *   comes from a newer format.
     */
    fun read(
        input: InputStream,
        openMediaOutput: (String) -> OutputStream?
    ): ReadResult {
        var project: VideoProject? = null
        var manifest: JSONObject? = null
        val extracted = LinkedHashSet<String>()
        var entryCount = 0
        var remainingBytes = MAX_TOTAL_BYTES

        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_ENTRIES) {
                    throw IOException("Backup contains too many files")
                }
                val name = entry.name ?: ""
                when {
                    name == PROJECT_ENTRY || name == MANIFEST_ENTRY -> {
                        val limit = minOf(MAX_JSON_BYTES, remainingBytes)
                        if (limit <= 0L) throw IOException("Backup is too large")
                        val bytes = readLimited(zip, limit, name)
                        remainingBytes -= bytes.size
                        if (name == PROJECT_ENTRY) {
                            project = ProjectJson.fromJson(String(bytes, Charsets.UTF_8))
                                ?: throw IOException("Backup project data is unreadable")
                        } else {
                            manifest = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }
                                .getOrElse { throw IOException("Backup manifest is unreadable") }
                        }
                    }

                    isSafeMediaEntry(name) -> {
                        if (!extracted.add(name)) {
                            // Duplicate entry: keep the first copy only.
                        } else {
                            val limit = minOf(MAX_MEDIA_ENTRY_BYTES, remainingBytes)
                            if (limit <= 0L) throw IOException("Backup is too large")
                            val sink = runCatching { openMediaOutput(name) }.getOrNull()
                            if (sink != null) {
                                sink.use { out ->
                                    remainingBytes -= copyLimited(zip, out, limit, name)
                                }
                            } else {
                                extracted.remove(name)
                            }
                        }
                    }
                    // Everything else is ignored.
                }
                zip.closeEntry()
            }
        }

        val loaded = project ?: throw IOException("Not a CatByte backup (project.json is missing)")
        val entries = manifestEntries(manifest, extracted)
        return ReadResult(loaded, entries)
    }

    private fun manifestEntries(
        manifest: JSONObject?,
        extracted: Set<String>
    ): List<ManifestEntry> {
        if (manifest == null) return emptyList()
        val version = manifest.optInt("format", 0)
        if (version > FORMAT_VERSION) {
            throw IOException("This backup was made by a newer CatByte version")
        }
        val array = manifest.optJSONArray("media") ?: return emptyList()
        val result = ArrayList<ManifestEntry>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val uri = item.optString("uri")
            val entry = item.optString("entry")
            if (uri.isBlank() || entry !in extracted) continue
            result += ManifestEntry(uri, entry)
        }
        return result
    }

    // ------------------------------------------------------------------
    // Helpers (also used by the tests and the manager)
    // ------------------------------------------------------------------

    fun isSafeMediaEntry(name: String): Boolean {
        if (!name.startsWith(MEDIA_PREFIX)) return false
        return isSafeMediaFileName(name.removePrefix(MEDIA_PREFIX))
    }

    fun isSafeMediaFileName(fileName: String): Boolean {
        if (fileName.isEmpty() || fileName.length > 100) return false
        if (fileName.startsWith(".")) return false
        if (fileName.contains('/') || fileName.contains('\\') || fileName.contains("..")) return false
        return fileName.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
    }

    /** Every URI that a full backup should embed, de-duplicated and ordered. */
    fun mediaUris(project: VideoProject): List<String> = buildList {
        project.clips.forEach { clip -> clip.uriString?.takeIf { it.isNotBlank() }?.let(::add) }
        project.overlayTracks.forEach { track ->
            track.clips.forEach { clip -> clip.uriString?.takeIf { it.isNotBlank() }?.let(::add) }
        }
        project.audioTracks.forEach { audio -> audio.uriString?.takeIf { it.isNotBlank() }?.let(::add) }
        project.backgroundImageUri.takeIf { it.isNotBlank() }?.let(::add)
        project.coverImageUri.takeIf { it.isNotBlank() }?.let(::add)
    }.distinct()

    private fun mediaEntryName(index: Int, extension: String): String {
        val safeExtension = extension.lowercase()
            .filter { it.isLetterOrDigit() }
            .take(8)
            .ifEmpty { "bin" }
        return MEDIA_PREFIX + index.toString().padStart(4, '0') + "." + safeExtension
    }

    private fun readLimited(zip: ZipInputStream, limit: Long, name: String): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw IOException("Backup entry $name is too large")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun copyLimited(
        zip: ZipInputStream,
        output: OutputStream,
        limit: Long,
        name: String
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw IOException("Backup entry $name is too large")
            output.write(buffer, 0, read)
        }
        return total
    }
}

/**
 * Points every media reference at its restored local copy. URIs without a
 * mapping are left untouched, so a project-only backup still opens and the
 * normal missing-media flow can relink them.
 */
fun VideoProject.remapMediaUris(mapping: Map<String, String>): VideoProject {
    if (mapping.isEmpty()) return this

    fun remapClip(clip: MediaClip): MediaClip {
        val original = clip.uriString ?: return clip
        val restored = mapping[original] ?: return clip
        return clip.copy(uriString = restored)
    }

    return copy(
        clips = clips.map(::remapClip),
        overlayTracks = overlayTracks.map { track ->
            track.copy(clips = track.clips.map(::remapClip))
        },
        audioTracks = audioTracks.map { audio ->
            val original = audio.uriString ?: return@map audio
            val restored = mapping[original] ?: return@map audio
            audio.copy(uriString = restored)
        },
        backgroundImageUri = mapping[backgroundImageUri] ?: backgroundImageUri,
        coverImageUri = mapping[coverImageUri] ?: coverImageUri
    )
}
