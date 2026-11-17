package com.example.viewmodel.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

/** Durable audio import, captured timeline position and selection behaviour. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioImportTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `content source is copied into app private storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://audio.test/private-copy")
        val bytes = "private audio bytes".toByteArray()
        Shadows.shadowOf(context.contentResolver)
            .registerInputStream(uri, ByteArrayInputStream(bytes))

        val source = AudioFeature.resolveDurableAudioSource(
            context = context,
            uri = uri,
            persistableReadGranted = false
        )

        assertNotNull(source)
        val storedFile = File(source!!.uriString)
        assertNotEquals(uri.toString(), source.uriString)
        assertTrue(storedFile.absolutePath.startsWith(context.filesDir.absolutePath))
        assertTrue(storedFile.isFile)
        assertArrayEquals(bytes, storedFile.readBytes())
        storedFile.delete()
    }

    @Test
    fun `copy failure without a persisted read grant is rejected`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://audio.test/no-persisted-grant")

        val source = AudioFeature.resolveDurableAudioSource(
            context = context,
            uri = uri,
            persistableReadGranted = false
        )

        assertNull(source)
    }

    @Test
    fun `copy failure keeps the original uri only after a persisted read grant`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://audio.test/persisted-grant")
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        assertTrue(AudioFeature.hasPersistedReadPermission(context, uri))

        val source = AudioFeature.resolveDurableAudioSource(
            context = context,
            uri = uri,
            persistableReadGranted = true
        )

        assertNotNull(source)
        assertEquals(uri.toString(), source!!.uriString)
        assertTrue(source.requiresPersistedReadPermission)
    }

    @Test
    fun `import captures the playhead and completed import selects the audio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ProjectRepository()
        val project = repository.createNewProject(
            title = "Audio import",
            initialClips = listOf(
                MediaClip(
                    id = "video",
                    title = "Video",
                    durationMs = 5_000L,
                    originalDurationMs = 5_000L
                )
            )
        )
        val viewModel = EditorViewModel(repository = repository, context = context)
        viewModel.openProject(project)
        viewModel.toggleClipSelection("video")
        viewModel.selectText("title")
        viewModel._currentPositionMs.value = 2_500L

        val uri = Uri.parse("content://audio.test/captured-start")
        val bytes = "captured audio".toByteArray()
        Shadows.shadowOf(context.contentResolver)
            .registerInputStream(uri, ByteArrayInputStream(bytes))
        val probeStarted = CompletableDeferred<Unit>()
        val releaseProbe = CompletableDeferred<Unit>()
        var importSucceeded = false

        AudioFeature.addAudioTrackFromUri(
            vm = viewModel,
            uriString = uri.toString(),
            title = "Imported",
            artist = "Test",
            onComplete = { importSucceeded = it },
            probe = AudioFeature.AudioImportProbe { _, _ ->
                probeStarted.complete(Unit)
                releaseProbe.await()
                AudioFeature.AudioImportMedia(durationMs = 1_500L, waveform = listOf(0.5f))
            },
            ioDispatcher = dispatcher
        )

        dispatcher.scheduler.runCurrent()
        assertTrue("the metadata probe should be running", probeStarted.isCompleted)

        // Moving the playhead while the import is pending must not move the
        // imported clip away from the position captured at initiation.
        viewModel._currentPositionMs.value = 9_000L
        releaseProbe.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(importSucceeded)
        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(2_500L, audio.startTimelineMs)
        assertNotEquals(uri.toString(), audio.uriString)
        assertTrue(File(audio.uriString!!).isFile)

        assertEquals(audio.id, viewModel.selectedAudioId.value)
        assertNull(viewModel.selectedClipId.value)
        assertTrue(viewModel.selectedClipIds.value.isEmpty())
        assertNull(viewModel.selectedTextId.value)
        assertFalse(audio.uriString!!.startsWith("content://"))
    }
}
