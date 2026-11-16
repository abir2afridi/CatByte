package com.example.ui.camera

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.R
import com.example.data.AppLog

/**
 * Owns the CameraX use cases and the photo/video capture lifecycle for
 * [CameraCaptureScreen]. Remembered per screen instance so rotation does not
 * share capture state between two camera sessions.
 */
internal class CameraCaptureController {

    var isRecording by mutableStateOf(false)
        private set
    var capturedUri by mutableStateOf<Uri?>(null)
        private set
    var capturedIsVideo by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var cameraReady by mutableStateOf(false)
        private set

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    /** Binds preview + photo + video use cases for the given lens. */
    fun bind(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int
    ) {
        runCatching {
            val provider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.fromOrderedList(
                        listOf(Quality.FHD, Quality.HD, Quality.SD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                .build()
            val video = VideoCapture.withOutput(recorder)
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture, video)
            capture to video
        }.onSuccess { (capture, video) ->
            imageCapture = capture
            videoCapture = video
            cameraReady = true
            statusMessage = null
            AppLog.info("Camera", "Camera bound: lensFacing=$lensFacing")
        }.onFailure { error ->
            cameraReady = false
            AppLog.error("Camera", "Camera binding failed", error)
            statusMessage = context.getString(R.string.camera_unavailable)
        }
    }

    fun takePhoto(context: Context) {
        val capture = imageCapture
        if (capture == null) {
            statusMessage = context.getString(R.string.camera_unavailable)
            return
        }
        val file = newCaptureFile(context, "jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        AppLog.info("Camera", "Taking photo -> ${file.name}")
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    AppLog.info("Camera", "Photo saved: ${file.length()} bytes")
                    capturedUri = uriForFile(context, file)
                    capturedIsVideo = false
                    statusMessage = null
                }

                override fun onError(exception: ImageCaptureException) {
                    AppLog.error("Camera", "Photo capture failed", exception)
                    statusMessage = context.getString(R.string.camera_capture_failed)
                    Toast.makeText(context, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    fun startRecording(context: Context, hasAudioPermission: Boolean) {
        val video = videoCapture
        if (video == null) {
            statusMessage = context.getString(R.string.camera_unavailable)
            return
        }
        val file = newCaptureFile(context, "mp4")
        var pending = video.output.prepareRecording(
            context.applicationContext,
            FileOutputOptions.Builder(file).build()
        )
        if (hasAudioPermission) {
            pending = runCatching { pending.withAudioEnabled() }.getOrDefault(pending)
        }
        AppLog.info("Camera", "Recording started -> ${file.name}")
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    statusMessage = null
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    recording = null
                    if (event.hasError()) {
                        AppLog.error("Camera", "Recording failed (code ${event.error})")
                        statusMessage = context.getString(R.string.camera_capture_failed)
                    } else {
                        AppLog.info("Camera", "Video saved: ${file.length()} bytes")
                        capturedUri = uriForFile(context, file)
                        capturedIsVideo = true
                        statusMessage = null
                    }
                }
            }
        }
    }

    fun stopRecording() {
        AppLog.info("Camera", "Recording stopped")
        recording?.stop()
        recording = null
    }

    fun clearCapture() {
        capturedUri = null
        capturedIsVideo = false
        statusMessage = null
    }

    fun reportPermissionDenied(context: Context) {
        statusMessage = context.getString(R.string.camera_permission_needed)
    }

    fun release() {
        runCatching { recording?.stop() }
        recording = null
    }
}
