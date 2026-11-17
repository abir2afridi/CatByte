package com.example.viewmodel.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.data.ClipTiming
import com.example.data.MediaDurationFallback
import com.example.data.MediaMetadata
import com.example.data.SampleAudioGenerator
import com.example.engine.AudioTiming
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Audio-track import, extraction, microphone recording and TTS voice-over. */
internal object AudioFeature {

    private const val IMPORTED_AUDIO_DIRECTORY = "imported_audio"
    private const val SAMPLE_AUDIO_DIRECTORY = "sample_audio"

    /** A source that can safely be written into a persisted project. */
    internal data class DurableAudioSource(
        val uriString: String,
        val metadataUri: Uri,
        val displayName: String,
        /** True when this is still a content URI backed by a persisted grant. */
        val requiresPersistedReadPermission: Boolean = false
    )

    /** Metadata that is expensive to obtain and therefore read on IO. */
    internal data class AudioImportMedia(
        val durationMs: Long,
        val waveform: List<Float> = emptyList()
    )

    /** A generated library sample persisted in app-private storage. */
    internal data class GeneratedSampleAudio(
        val file: File,
        val durationMs: Long,
        val waveform: List<Float>
    )

    /** Injectable metadata reader so import timing can be tested deterministically. */
    internal fun interface AudioImportProbe {
        suspend fun probe(context: Context, uri: Uri): AudioImportMedia
    }

    private val DefaultAudioImportProbe = AudioImportProbe { context, uri ->
        AudioImportMedia(
            durationMs = MediaMetadata.durationMs(context, uri),
            waveform = MediaMetadata.waveform(context, uri, 48).orEmpty()
        )
    }

    /**
     * Adds an audio track from a URI (real duration + waveform).
     *
     * The playhead is captured synchronously at import initiation. Content URIs
     * are copied into app-private storage before the project is changed, so a
     * successful import never depends on a temporary document-provider grant.
     * [onComplete] receives true only after the durable track was added.
     */
    fun addAudioTrackFromUri(
        vm: EditorViewModel,
        uriString: String,
        title: String,
        artist: String = "Unknown",
        onComplete: (Boolean) -> Unit = {},
        probe: AudioImportProbe = DefaultAudioImportProbe,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        // This must happen before any asynchronous metadata/waveform work: the
        // user can move the playhead while a large file is being copied.
        val startTimelineMs = vm._currentPositionMs.value
        val ctx = vm.context
        val projectId = vm._currentProject.value?.id
        if (ctx == null || projectId == null || uriString.isBlank()) {
            onComplete(false)
            return
        }

        val uri = runCatching { Uri.parse(uriString) }.getOrElse {
            onComplete(false)
            return
        }
        // Persistable access must be requested while the picker grant is still
        // active. A successful copy does not depend on it, but it is the only
        // legal fallback when copying is impossible.
        val persistableReadGranted = requestPersistableReadPermission(ctx, uri)

        vm.viewModelScope.launch {
            val succeeded = try {
                val source = withContext(ioDispatcher) {
                    resolveDurableAudioSource(ctx, uri, persistableReadGranted)
                } ?: throw IOException("Audio source is not durable and has no persisted read grant")
                val media = withContext(ioDispatcher) {
                    probe.probe(ctx, source.metadataUri)
                }
                if (vm._currentProject.value?.id != projectId) {
                    throw IOException("The open project changed while the audio was importing")
                }
                if (source.requiresPersistedReadPermission &&
                    !hasPersistedReadPermission(ctx, source.metadataUri)
                ) {
                    throw IOException("The persisted read grant is no longer available")
                }

                val sourceDurationMs = if (media.durationMs > 0L) media.durationMs else 6000L
                val realTitle = title.takeIf { it.isNotBlank() }
                    ?: source.displayName.takeIf { it.isNotBlank() }
                    ?: "Imported Audio"
                val audio = AudioClip(
                    id = UUID.randomUUID().toString(),
                    title = realTitle,
                    artist = artist,
                    uriString = source.uriString,
                    startTimelineMs = startTimelineMs,
                    durationMs = sourceDurationMs,
                    originalDurationMs = sourceDurationMs,
                    waveform = media.waveform.ifEmpty { emptyList() }
                )
                vm.updateProject { proj ->
                    proj.copy(audioTracks = proj.audioTracks + audio)
                }
                // Selection is a single source of truth: selecting the imported
                // audio must also clear any text/video selection.
                SelectionFeature.selectAudioClip(vm, audio.id)
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppLog.error("EditorViewModel", "Failed to import audio from $uriString", error)
                false
            }
            onComplete(succeeded)
        }
    }

    /**
     * Generates a built-in library sample as a real WAV file and adds it to
     * the project only after the file has been written successfully.
     *
     * The sample metadata intentionally has no URI. Generating and persisting
     * the source here prevents a null URI from reaching preview/export, where
     * it would look like a successful track but could not be rendered.
     */
    fun addSampleAudioTrack(
        vm: EditorViewModel,
        sample: AudioClip,
        onComplete: (Boolean) -> Unit = {},
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        generator: (String, Long) -> SampleAudioGenerator.GeneratedAudio = SampleAudioGenerator::generate
    ) {
        // Capture the insertion point before generation. A long sample must not
        // jump to wherever the user moved the playhead while it was prepared.
        val startTimelineMs = vm._currentPositionMs.value
        val ctx = vm.context
        val projectId = vm._currentProject.value?.id
        if (ctx == null || projectId == null || sample.id.isBlank()) {
            onComplete(false)
            return
        }

        vm.viewModelScope.launch {
            val succeeded = try {
                val generated = withContext(ioDispatcher) {
                    val audio = generator(sample.id, sample.durationMs)
                    persistGeneratedSample(ctx, audio)
                }
                if (vm._currentProject.value?.id != projectId) {
                    throw IOException("The open project changed while the sample was being prepared")
                }
                if (!generated.file.isFile || generated.file.length() <= 44L) {
                    throw IOException("The generated sample audio is missing or empty")
                }

                val track = AudioClip(
                    id = UUID.randomUUID().toString(),
                    title = sample.title,
                    artist = sample.artist,
                    uriString = generated.file.absolutePath,
                    startTimelineMs = startTimelineMs,
                    durationMs = generated.durationMs,
                    originalDurationMs = generated.durationMs,
                    waveform = generated.waveform
                )
                vm.updateProject { project ->
                    project.copy(audioTracks = project.audioTracks + track)
                }
                SelectionFeature.selectAudioClip(vm, track.id)
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppLog.error(
                    "EditorViewModel",
                    "Failed to generate sample audio '${sample.id}'",
                    error
                )
                false
            }
            onComplete(succeeded)
        }
    }

    /** Writes a generated sample atomically into app-private storage. */
    internal fun persistGeneratedSample(
        context: Context,
        audio: SampleAudioGenerator.GeneratedAudio
    ): GeneratedSampleAudio {
        val directory = File(context.filesDir, SAMPLE_AUDIO_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create the private sample-audio directory")
        }
        val target = File(directory, "sample_${UUID.randomUUID()}.wav")
        val temporary = File(directory, ".${target.name}.part")
        return try {
            SampleAudioGenerator.writeWav(temporary, audio.samples)
            if (temporary.length() <= 44L) {
                throw IOException("The generated sample WAV is empty")
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            if (!target.isFile || target.length() <= 44L) {
                throw IOException("The generated sample WAV could not be persisted")
            }
            GeneratedSampleAudio(
                file = target,
                durationMs = audio.durationMs,
                waveform = audio.waveform
            )
        } finally {
            temporary.delete()
        }
    }

    fun addAudioTrack(vm: EditorViewModel, audioClip: AudioClip) {
        if (audioClip.uriString.isNullOrBlank()) {
            AppLog.warn(
                "EditorViewModel",
                "Cannot add '${audioClip.title}': the audio source is missing"
            )
            return
        }
        val id = UUID.randomUUID().toString()
        val startTimelineMs = vm._currentPositionMs.value
        val track = audioClip.copy(
            id = id,
            startTimelineMs = startTimelineMs,
            originalDurationMs = audioClip.originalDurationMs.takeIf { it > 0L }
                ?: audioClip.durationMs
        )
        vm.updateProject { proj ->
            proj.copy(audioTracks = proj.audioTracks + track)
        }
        SelectionFeature.selectAudioClip(vm, id)
    }

    /**
     * Resolves an import source to something that remains readable after the
     * process restarts. A content URI is copied into app-private storage; the
     * original URI is accepted only when a persisted read grant is confirmed.
     */
    internal fun resolveDurableAudioSource(
        context: Context,
        uri: Uri,
        persistableReadGranted: Boolean
    ): DurableAudioSource? {
        val displayName = queryDisplayName(context, uri)
        if (!uri.scheme.equals("content", ignoreCase = true)) {
            return DurableAudioSource(
                uriString = uri.toString(),
                metadataUri = uri,
                displayName = displayName
            )
        }

        val copiedFile = copyContentUriToPrivateStorage(context, uri, displayName)
        if (copiedFile != null) {
            return DurableAudioSource(
                uriString = copiedFile.absolutePath,
                metadataUri = Uri.fromFile(copiedFile),
                displayName = displayName
            )
        }

        // A provider may refuse a persistable grant. In that case its temporary
        // URI would be dead after a process restart and must not be stored as a
        // successful import.
        if (persistableReadGranted && hasPersistedReadPermission(context, uri)) {
            return DurableAudioSource(
                uriString = uri.toString(),
                metadataUri = uri,
                displayName = displayName,
                requiresPersistedReadPermission = true
            )
        }
        return null
    }

    /** Copies a content URI into app-private files storage. */
    internal fun copyContentUriToPrivateStorage(
        context: Context,
        uri: Uri,
        displayName: String
    ): File? {
        val directory = File(context.filesDir, IMPORTED_AUDIO_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            AppLog.warn("EditorViewModel", "Cannot create private audio import directory")
            return null
        }

        val extension = displayName.substringAfterLast('.', "")
            .lowercase()
            .takeIf { candidate ->
                candidate.length in 1..8 && candidate.all { it.isLetterOrDigit() }
            }
            ?: "audio"
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        val temporary = File(directory, ".${target.name}.part")
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("The selected audio cannot be opened")
            input.use { source ->
                temporary.outputStream().use { output -> source.copyTo(output) }
            }
            if (temporary.length() <= 0L) {
                throw IOException("The selected audio is empty")
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            AppLog.info("EditorViewModel", "Copied imported audio into private storage")
            target
        } catch (error: Exception) {
            temporary.delete()
            target.delete()
            AppLog.warn("EditorViewModel", "Cannot copy imported audio into private storage", error)
            null
        }
    }

    /** True only when Android reports an actual persisted read grant. */
    internal fun hasPersistedReadPermission(context: Context, uri: Uri): Boolean =
        runCatching {
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission
            }
        }.getOrDefault(false)

    /** Requests and verifies a persistable read grant while the picker grant lives. */
    private fun requestPersistableReadPermission(context: Context, uri: Uri): Boolean {
        if (!uri.scheme.equals("content", ignoreCase = true)) return false
        if (hasPersistedReadPermission(context, uri)) return true
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            hasPersistedReadPermission(context, uri)
        } catch (error: Exception) {
            AppLog.warn("EditorViewModel", "Provider did not grant persistable audio access", error)
            false
        }
    }

    /** Reads the provider's display name without changing the stored source. */
    private fun queryDisplayName(context: Context, uri: Uri): String {
        val queried = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
        }.getOrNull()
        return queried?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() }
            ?: "Imported Audio"
    }

    fun deleteAudioTrack(vm: EditorViewModel, id: String) {
        vm.updateProject { proj ->
            proj.copy(audioTracks = proj.audioTracks.filterNot { it.id == id })
        }
        if (vm._selectedAudioId.value == id) {
            vm._selectedAudioId.value = null
        }
    }

    /**
     * Extracts the selected clip's audio as a real timeline audio track. The
     * source trim and speed are carried over, so the extracted track lines up
     * with the picture in both preview and export instead of starting from the
     * beginning of the original recording.
     */
    fun extractAudioFromSelectedClip(vm: EditorViewModel) {
        val ctx = vm.context ?: return
        val project = vm._currentProject.value ?: return
        val selectedId = vm._selectedClipId.value

        var found: MediaClip? = null
        var startMs = 0L
        var cursor = 0L
        for (clip in project.clips) {
            if (clip.id == selectedId) {
                found = clip
                startMs = cursor
                break
            }
            cursor += clip.durationMs
        }
        if (found == null) {
            project.overlayTracks.forEach { track ->
                track.clips.firstOrNull { it.id == selectedId }?.let { clip ->
                    found = clip
                    startMs = clip.startTimelineMs
                }
            }
        }

        val clip = found ?: return
        val uriString = clip.uriString
        if (uriString.isNullOrBlank()) {
            AppLog.warn(
                "EditorViewModel",
                "Cannot extract audio: '${clip.title}' has no media source"
            )
            return
        }

        val sourceLength = ClipTiming.sourceLengthMs(clip)
        val trimStart = clip.trimStartMs.coerceAtLeast(0L)
        val trimEnd = if (clip.trimEndMs > trimStart) clip.trimEndMs else trimStart + sourceLength
        val effectiveSpeed = if (ClipTiming.hasSpeedCurve(clip)) {
            AudioTiming.normalizedSpeed(
                sourceLength.toFloat() / clip.durationMs.coerceAtLeast(1L)
            )
        } else {
            AudioTiming.normalizedSpeed(clip.speed)
        }

        vm.viewModelScope.launch {
            val waveform = withContext(Dispatchers.IO) {
                MediaMetadata.waveform(ctx, Uri.parse(uriString), 48)
            } ?: emptyList()
            val audio = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "${clip.title} (Audio)",
                artist = "Extracted",
                uriString = uriString,
                startTimelineMs = startMs,
                durationMs = clip.durationMs.coerceAtLeast(300L),
                originalDurationMs = clip.originalDurationMs.takeIf { it > 0L } ?: sourceLength,
                trimStartMs = trimStart,
                trimEndMs = trimEnd,
                speed = effectiveSpeed,
                volume = clip.volume.coerceIn(0f, 2f),
                waveform = waveform
            )
            vm.updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
            AppLog.info(
                "EditorViewModel",
                "Extracted audio from '${clip.title}': start=${startMs}ms, duration=${audio.durationMs}ms"
            )
        }
    }

    /**
     * Detaches (unlinks) the selected clip's audio into its own audio track -
     * CapCut's "Unlink": the video keeps playing silently and the sound becomes
     * an independent clip you can move, trim, fade or delete. Both halves are
     * written in ONE project update, so a single undo restores the original
     * clip with its sound.
     *
     * @return false when there is nothing to detach (no selection, no media
     *   source, or the audio was already detached) so the UI can explain why
     *   instead of silently doing nothing.
     */
    fun detachAudioFromSelectedClip(vm: EditorViewModel): Boolean {
        val project = vm._currentProject.value ?: return false
        val selectedId = vm._selectedClipId.value ?: return false
        val clip = project.allVideoClips.firstOrNull { it.id == selectedId } ?: return false

        val uriString = clip.uriString
        if (uriString.isNullOrBlank()) {
            AppLog.warn(
                "EditorViewModel",
                "Cannot detach audio: '${clip.title}' has no media source"
            )
            return false
        }
        if (clip.isMuted) {
            AppLog.info("EditorViewModel", "Audio of '${clip.title}' is already detached")
            return false
        }

        val startMs = timelineStartMsOf(project, clip)
        val sourceLength = ClipTiming.sourceLengthMs(clip)
        val trimStart = clip.trimStartMs.coerceAtLeast(0L)
        val trimEnd = if (clip.trimEndMs > trimStart) clip.trimEndMs else trimStart + sourceLength
        val effectiveSpeed = if (ClipTiming.hasSpeedCurve(clip)) {
            AudioTiming.normalizedSpeed(
                sourceLength.toFloat() / clip.durationMs.coerceAtLeast(1L)
            )
        } else {
            AudioTiming.normalizedSpeed(clip.speed)
        }

        val detached = AudioClip(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (Audio)",
            artist = "Detached",
            uriString = uriString,
            startTimelineMs = startMs,
            durationMs = clip.durationMs.coerceAtLeast(300L),
            originalDurationMs = clip.originalDurationMs.takeIf { it > 0L } ?: sourceLength,
            trimStartMs = trimStart,
            trimEndMs = trimEnd,
            speed = effectiveSpeed,
            volume = clip.volume.coerceIn(0f, 2f),
            waveform = clip.waveform
        )

        vm.updateProject { proj ->
            proj.copy(
                audioTracks = proj.audioTracks + detached,
                clips = proj.clips.map { if (it.id == clip.id) it.copy(isMuted = true) else it },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map {
                            if (it.id == clip.id) it.copy(isMuted = true) else it
                        }
                    )
                }
            )
        }
        AppLog.info(
            "EditorViewModel",
            "Detached audio from '${clip.title}': start=${startMs}ms, duration=${detached.durationMs}ms"
        )
        return true
    }

    /** Absolute timeline start of a clip (main track = sum of the clips before it). */
    private fun timelineStartMsOf(
        project: com.example.model.VideoProject,
        clip: MediaClip
    ): Long {
        var cursor = 0L
        for (main in project.clips) {
            if (main.id == clip.id) return cursor
            cursor += main.durationMs
        }
        return clip.startTimelineMs
    }

    /** Starts a real microphone recording. */
    fun startVoiceRecording(vm: EditorViewModel) {
        val recorder = vm.voiceRecorder ?: return
        val ok = recorder.start()
        if (ok) {
            vm._isRecordingVoice.value = true
            vm._voiceRecordingDurationMs.value = 0L
            vm.voiceRecordJob?.cancel()
            vm.voiceRecordJob = vm.viewModelScope.launch {
                while (isActive && vm._isRecordingVoice.value) {
                    delay(100L)
                    vm._voiceRecordingDurationMs.value += 100L
                }
            }
        }
    }

    /** Stops the recording and adds the clip to the timeline. */
    fun stopAndSaveVoiceRecording(vm: EditorViewModel) {
        val recorder = vm.voiceRecorder ?: return
        val file = recorder.stop()
        vm._isRecordingVoice.value = false
        vm.voiceRecordJob?.cancel()
        vm.voiceRecordJob = null

        if (file != null && file.exists()) {
            val duration = vm._voiceRecordingDurationMs.value
            val audioId = UUID.randomUUID().toString()
            val audio = AudioClip(
                id = audioId,
                title = "Voiceover",
                artist = "You",
                uriString = file.absolutePath,
                startTimelineMs = vm._currentPositionMs.value,
                durationMs = duration.coerceAtLeast(500L),
                originalDurationMs = duration.coerceAtLeast(500L),
                isVoiceover = true,
                waveform = emptyList()
            )
            vm.updateProject { proj ->
                proj.copy(audioTracks = proj.audioTracks + audio)
            }
            // The recording is a real local file: decode its envelope so the
            // timeline shows the actual waveform instead of an empty row.
            attachWaveformAsync(vm, audioId, file)
        }
    }

    fun cancelVoiceRecording(vm: EditorViewModel) {
        vm.voiceRecorder?.cancel()
        vm._isRecordingVoice.value = false
        vm._voiceRecordingDurationMs.value = 0L
        vm.voiceRecordJob?.cancel()
        vm.voiceRecordJob = null
    }

    /** Generates a voice-over from text using the device TTS engine. */
    fun startTtsVoiceover(vm: EditorViewModel, text: String) {
        val tts = vm.speechSynthesizer ?: return
        vm._activeSheet.value = ActiveToolSheet.TTS_VOICEOVER
        tts.synthesize(text) { file, error ->
            if (file != null) {
                val durationMs = MediaDurationFallback.durationMs(vm.context!!, Uri.fromFile(file))
                    .coerceAtLeast(500L)
                val audioId = UUID.randomUUID().toString()
                val audio = AudioClip(
                    id = audioId,
                    title = "AI Voiceover",
                    artist = "CatByte TTS",
                    uriString = file.absolutePath,
                    startTimelineMs = vm._currentPositionMs.value,
                    durationMs = durationMs,
                    originalDurationMs = durationMs,
                    isVoiceover = true,
                    waveform = emptyList()
                )
                vm.updateProject { proj ->
                    proj.copy(audioTracks = proj.audioTracks + audio)
                }
                // TTS output is a real file too; refine its waveform once the
                // decoder has read it.
                attachWaveformAsync(vm, audioId, file)
            }
            // The UI can show error if needed
        }
    }

    /**
     * Decodes the peak envelope of a freshly created voice-over/TTS file and
     * attaches it in a follow-up update. The clip appears immediately with an
     * honest "no data" waveform; the envelope arrives once decoding finishes.
     */
    private fun attachWaveformAsync(vm: EditorViewModel, audioId: String, file: File) {
        val ctx = vm.context ?: return
        vm.viewModelScope.launch {
            val waveform = withContext(Dispatchers.IO) {
                runCatching { MediaMetadata.waveform(ctx, Uri.fromFile(file), 48) }.getOrNull()
            } ?: return@launch
            if (waveform.isEmpty()) return@launch
            vm.updateProject("audio-waveform:$audioId") { proj ->
                proj.copy(
                    audioTracks = proj.audioTracks.map {
                        if (it.id == audioId) it.copy(waveform = waveform) else it
                    }
                )
            }
        }
    }
}
