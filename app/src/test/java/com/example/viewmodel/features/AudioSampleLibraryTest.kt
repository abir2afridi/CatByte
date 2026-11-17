package com.example.viewmodel.features

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectRepository
import com.example.data.SampleAudioGenerator
import com.example.data.SampleDataProvider
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Built-in library metadata must become a durable, renderable audio source. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioSampleLibraryTest {

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
    fun `sample is generated at the captured playhead and added with a real file`() = runTest(dispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ProjectRepository()
        val project = repository.createNewProject("Sample library")
        val viewModel = EditorViewModel(repository = repository, context = context)
        viewModel.openProject(project)
        viewModel._currentPositionMs.value = 2_750L
        var completed = false

        AudioFeature.addSampleAudioTrack(
            vm = viewModel,
            sample = SampleDataProvider.trendingAudioList.first(),
            onComplete = { completed = it },
            ioDispatcher = dispatcher
        )
        advanceUntilIdle()

        assertTrue(completed)
        val audio = viewModel.currentProject.value!!.audioTracks.single()
        assertEquals(2_750L, audio.startTimelineMs)
        assertNotNull(audio.uriString)
        val source = File(audio.uriString!!)
        assertTrue(source.isFile)
        assertTrue(source.length() > 44L)
        assertTrue(source.absolutePath.startsWith(context.filesDir.absolutePath))
        assertTrue(audio.waveform.isNotEmpty())
        assertEquals(audio.durationMs, audio.originalDurationMs)
        assertEquals(audio.id, viewModel.selectedAudioId.value)
    }

    @Test
    fun `generation failure reports false and does not add a null uri track`() = runTest(dispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ProjectRepository()
        val project = repository.createNewProject("Sample failure")
        val viewModel = EditorViewModel(repository = repository, context = context)
        viewModel.openProject(project)
        var completed: Boolean? = null

        AudioFeature.addSampleAudioTrack(
            vm = viewModel,
            sample = SampleDataProvider.soundEffectsList.first(),
            onComplete = { completed = it },
            ioDispatcher = dispatcher,
            generator = { _, _ -> throw IllegalStateException("disk full") }
        )
        advanceUntilIdle()

        assertFalse(completed == true)
        assertTrue(viewModel.currentProject.value!!.audioTracks.isEmpty())
        assertTrue(viewModel.selectedAudioId.value == null)
    }

    @Test
    fun `persisted generated file is a wav and keeps its actual duration`() = runTest(dispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generated = SampleAudioGenerator.generate("sfx_ding", 1_800L)

        val persisted = AudioFeature.persistGeneratedSample(context, generated)

        assertTrue(persisted.file.isFile)
        assertTrue(persisted.file.readBytes().take(12).let {
            it.take(4).toByteArray().contentEquals("RIFF".toByteArray()) &&
                it.drop(8).take(4).toByteArray().contentEquals("WAVE".toByteArray())
        })
        assertEquals(generated.durationMs, persisted.durationMs)
        assertEquals(generated.waveform, persisted.waveform)
    }
}
