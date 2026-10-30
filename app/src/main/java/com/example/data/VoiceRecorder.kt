package com.example.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records a real voice-over with the device microphone using [MediaRecorder].
 * The recording is stored as an AAC encoded m4a file inside the app folder.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    /** Starts recording. Returns true when the recorder is running. */
    fun start(): Boolean {
        if (recorder != null) return false
        val directory = File(context.filesDir, "voiceovers").apply { mkdirs() }
        val file = File(directory, "voice_${System.currentTimeMillis()}.m4a")

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(128_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            outputFile = file
            AppLog.info("VoiceRecorder", "Started recording a voice-over")
            true
        } catch (error: Throwable) {
            runCatching { mediaRecorder.release() }
            runCatching { file.delete() }
            AppLog.error("VoiceRecorder", "Could not start the recording", error)
            false
        }
    }

    /** Stops the recording and returns the recorded file. */
    fun stop(): File? {
        val active = recorder ?: return null
        val file: File? = outputFile
        return try {
            active.stop()
            active.release()
            recorder = null
            outputFile = null
            AppLog.info("VoiceRecorder", "Finished the voice-over recording")
            file
        } catch (error: Throwable) {
            runCatching { active.release() }
            recorder = null
            outputFile = null
            runCatching { file?.delete() }
            AppLog.error("VoiceRecorder", "Could not finish the recording", error)
            null
        }
    }

    /** Stops and discards the recording. */
    fun cancel() {
        val active = recorder ?: return
        runCatching { active.stop() }
        runCatching { active.release() }
        recorder = null
        runCatching { outputFile?.delete() }
        outputFile = null
    }
}
