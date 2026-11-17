package com.example.data

import com.example.data.db.ProjectJson
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectBackupCodecTest {

    private fun sampleProject() = VideoProject(
        id = "proj_test",
        title = "Backup test",
        aspectRatio = AspectRatioType.RATIO_16_9,
        clips = listOf(
            MediaClip(
                id = "clip_1",
                title = "Main",
                uriString = "content://media/video/1",
                durationMs = 3000L
            )
        ),
        overlayTracks = listOf(
            VideoTrack(
                id = "track_1",
                clips = listOf(
                    MediaClip(
                        id = "clip_2",
                        title = "Overlay",
                        uriString = "file:///tmp/overlay.mp4",
                        durationMs = 1500L
                    )
                )
            )
        ),
        audioTracks = listOf(
            AudioClip(
                id = "audio_1",
                title = "Music",
                artist = "Test",
                uriString = "content://media/audio/2",
                durationMs = 4000L
            )
        ),
        backgroundImageUri = "content://media/image/bg",
        coverImageUri = "content://media/image/cover"
    )

    private fun backup(project: VideoProject, assets: List<ProjectBackupCodec.Asset>): ByteArray {
        val out = ByteArrayOutputStream()
        ProjectBackupCodec.write(project, out, assets)
        return out.toByteArray()
    }

    private fun readWithSink(bytes: ByteArray): Pair<ProjectBackupCodec.ReadResult, Map<String, ByteArray>> {
        val extracted = LinkedHashMap<String, ByteArray>()
        val result = ProjectBackupCodec.read(ByteArrayInputStream(bytes)) { name ->
            object : ByteArrayOutputStream() {
                override fun close() {
                    extracted[name] = toByteArray()
                    super.close()
                }
            }
        }
        return result to extracted
    }

    @Test
    fun roundTripKeepsProjectAndMedia() {
        val videoBytes = byteArrayOf(1, 2, 3, 4, 5)
        val audioBytes = ByteArray(2048) { (it % 251).toByte() }
        val bytes = backup(
            sampleProject(),
            listOf(
                ProjectBackupCodec.Asset("content://media/video/1", "mp4") {
                    ByteArrayInputStream(videoBytes)
                },
                ProjectBackupCodec.Asset("content://media/audio/2", "M4A") {
                    ByteArrayInputStream(audioBytes)
                }
            )
        )

        val (result, extracted) = readWithSink(bytes)

        assertEquals("Backup test", result.project.title)
        assertEquals(AspectRatioType.RATIO_16_9, result.project.aspectRatio)
        assertEquals(1, result.project.clips.size)
        assertEquals(1, result.project.overlayTracks.single().clips.size)
        assertEquals(2, result.media.size)
        assertEquals(2, extracted.size)
        assertEquals("media/0000.mp4", result.media.first().entryName)
        assertTrue(extracted.getValue("media/0000.mp4").contentEquals(videoBytes))
        assertTrue(extracted.getValue("media/0001.m4a").contentEquals(audioBytes))
    }

    @Test
    fun restoreRemapsEveryMediaReference() {
        val bytes = backup(
            sampleProject(),
            listOf(
                ProjectBackupCodec.Asset("content://media/video/1", "mp4") { ByteArrayInputStream(byteArrayOf(1)) },
                ProjectBackupCodec.Asset("file:///tmp/overlay.mp4", "mp4") { ByteArrayInputStream(byteArrayOf(2)) },
                ProjectBackupCodec.Asset("content://media/audio/2", "m4a") { ByteArrayInputStream(byteArrayOf(3)) },
                ProjectBackupCodec.Asset("content://media/image/bg", "jpg") { ByteArrayInputStream(byteArrayOf(4)) },
                ProjectBackupCodec.Asset("content://media/image/cover", "png") { ByteArrayInputStream(byteArrayOf(5)) }
            )
        )
        val result = readWithSink(bytes).first
        val mapping = result.media.associate { it.originalUri to "/restored/${it.entryName}" }
        val restored = with(ProjectBackupCodec) { result.project.remapMediaUris(mapping) }

        assertEquals("/restored/media/0000.mp4", restored.clips.single().uriString)
        assertEquals("/restored/media/0001.mp4", restored.overlayTracks.single().clips.single().uriString)
        assertEquals("/restored/media/0002.m4a", restored.audioTracks.single().uriString)
        assertEquals("/restored/media/0003.jpg", restored.backgroundImageUri)
        assertEquals("/restored/media/0004.png", restored.coverImageUri)
    }

    @Test
    fun projectOnlyBackupHasNoMediaButStillRestores() {
        val bytes = backup(sampleProject(), emptyList())
        val (result, extracted) = readWithSink(bytes)

        assertEquals("Backup test", result.project.title)
        assertTrue(result.media.isEmpty())
        assertTrue(extracted.isEmpty())
        // References stay as they were: the normal relink flow can find them.
        assertEquals("content://media/video/1", result.project.clips.single().uriString)
    }

    @Test
    fun unreadableAssetIsSkippedInsteadOfFailingTheBackup() {
        val out = ByteArrayOutputStream()
        val summary = ProjectBackupCodec.write(
            sampleProject(),
            out,
            listOf(
                ProjectBackupCodec.Asset("content://missing", "mp4") { null },
                ProjectBackupCodec.Asset("content://media/video/1", "mp4") {
                    ByteArrayInputStream(byteArrayOf(9))
                }
            )
        )

        assertEquals(1, summary.mediaWritten)
        assertEquals(1, summary.mediaSkipped)
        val result = readWithSink(out.toByteArray()).first
        assertEquals("Backup test", result.project.title)
        assertEquals(1, result.media.size)
        assertEquals("content://media/video/1", result.media.single().originalUri)
    }

    @Test
    fun manifestIsWrittenLastAndReadable() {
        val entries = mutableListOf<String>()
        val bytes = backup(sampleProject(), emptyList())
        java.util.zip.ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries += entry.name
            }
        }
        assertEquals(listOf(ProjectBackupCodec.PROJECT_ENTRY, ProjectBackupCodec.MANIFEST_ENTRY), entries)
    }

    @Test
    fun unsafeEntriesAreNeverExtracted() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ProjectBackupCodec.PROJECT_ENTRY))
            zip.write(ProjectJson.toJson(sampleProject()).toByteArray())
            zip.closeEntry()
            listOf("../evil.txt", "media/../../evil.bin", "media/nested/evil.bin", "/abs.bin").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(byteArrayOf(7))
                zip.closeEntry()
            }
        }
        val (result, extracted) = readWithSink(out.toByteArray())

        assertTrue(extracted.isEmpty())
        assertTrue(result.media.isEmpty())
        assertEquals("proj_test", result.project.id)
    }

    @Test
    fun newerFormatIsRefused() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ProjectBackupCodec.PROJECT_ENTRY))
            zip.write(ProjectJson.toJson(sampleProject()).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ProjectBackupCodec.MANIFEST_ENTRY))
            zip.write(JSONObject().put("format", ProjectBackupCodec.FORMAT_VERSION + 1).toString().toByteArray())
            zip.closeEntry()
        }
        val error = runCatching { readWithSink(out.toByteArray()) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is IOException)
    }

    @Test
    fun missingProjectEntryIsRejected() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("notes.txt"))
            zip.write("hello".toByteArray())
            zip.closeEntry()
        }
        val error = runCatching { readWithSink(out.toByteArray()) }.exceptionOrNull()
        assertTrue(error is IOException)
    }

    @Test
    fun mediaUrisAreDeduplicatedAndOrdered() {
        val project = sampleProject().copy(
            clips = listOf(
                MediaClip(id = "a", title = "a", uriString = "content://one"),
                MediaClip(id = "b", title = "b", uriString = "content://one"),
                MediaClip(id = "c", title = "c", uriString = null)
            )
        )
        val uris = ProjectBackupCodec.mediaUris(project)

        assertEquals(
            listOf(
                "content://one",
                "file:///tmp/overlay.mp4",
                "content://media/audio/2",
                "content://media/image/bg",
                "content://media/image/cover"
            ),
            uris
        )
    }

    @Test
    fun safeEntryNameRules() {
        assertTrue(ProjectBackupCodec.isSafeMediaEntry("media/0001.mp4"))
        assertTrue(ProjectBackupCodec.isSafeMediaEntry("media/clip_1-final.MP4"))
        assertTrue(!ProjectBackupCodec.isSafeMediaEntry("media/../clip.mp4"))
        assertTrue(!ProjectBackupCodec.isSafeMediaEntry("media/a/b.mp4"))
        assertTrue(!ProjectBackupCodec.isSafeMediaEntry("media/.hidden"))
        assertTrue(!ProjectBackupCodec.isSafeMediaEntry("other/0001.mp4"))
    }
}
