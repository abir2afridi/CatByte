package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppLog
import java.io.File

private enum class CameraMode { PHOTO, VIDEO }

/**
 * The app's OWN camera: a CameraX viewfinder with a real shutter.
 *
 * - Live preview (front/back switchable) rendered through [PreviewView].
 * - Photo mode: still capture straight into the app's private camera dir.
 * - Video mode: in-app recording via CameraX [Recorder] (start/stop on the
 *   shutter) - no hand-off to the system camera app.
 * - The capture is confirmed in-app ("Use in project" / "Retake") and the URI
 *   is handed back so the caller can import it into a new project.
 */
@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onMediaCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var mode by remember { mutableStateOf(CameraMode.PHOTO) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedIsVideo by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var cameraReady by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        AppLog.info("Camera", "Permission result: $grants")
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasAudioPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            statusMessage = context.getString(R.string.camera_permission_needed)
        }
    }

    LaunchedEffect(Unit) {
        AppLog.info("Camera", "CameraCaptureScreen opened (in-app CameraX)")
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Bind preview + capture use cases whenever the lens or permission changes.
    LaunchedEffect(hasCameraPermission, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect
        runCatching {
            val provider = cameraProviderFuture.get()
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

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recording?.stop() }
            recording = null
        }
    }

    fun takePhoto() {
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

    fun startRecording() {
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .testTag("camera_capture_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Viewfinder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF181A20))
                    .border(1.dp, Color(0xFF262832), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (capturedUri != null && !capturedIsVideo) {
                    AsyncImage(
                        model = capturedUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (capturedUri != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF24252A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFF26E5D7),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.camera_video_ready),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (!hasCameraPermission) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.camera_permission_needed),
                                color = Color(0xFFFFB020),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.camera_grant_permission),
                                color = Color(0xFF26E5D7),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else if (statusMessage != null) {
                        Text(
                            text = statusMessage!!,
                            color = Color(0xFFFFB020),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF4D4D))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.camera_recording),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("camera_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.camera_title),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.camera_in_app),
                        color = Color(0xFFA5A8B2),
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier.testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.camera_switch),
                        tint = Color.White
                    )
                }
            }

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (capturedUri == null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF24252A))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CameraModeChip(
                            label = stringResource(R.string.camera_take_photo),
                            selected = mode == CameraMode.PHOTO,
                            onClick = { mode = CameraMode.PHOTO }
                        )
                        CameraModeChip(
                            label = stringResource(R.string.camera_record_video),
                            selected = mode == CameraMode.VIDEO,
                            onClick = { mode = CameraMode.VIDEO }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable(enabled = cameraReady && !isRecording) {
                                when {
                                    mode == CameraMode.VIDEO && isRecording -> stopRecording()
                                    mode == CameraMode.VIDEO -> startRecording()
                                    else -> takePhoto()
                                }
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isRecording) 30.dp else 58.dp)
                                .clip(if (isRecording) RoundedCornerShape(6.dp) else CircleShape)
                                .background(Color(0xFFFF4D4D))
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFF3D404D), RoundedCornerShape(24.dp))
                                .clickable {
                                    capturedUri = null
                                    capturedIsVideo = false
                                    statusMessage = null
                                }
                                .testTag("camera_retake_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.camera_retake),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF26E5D7))
                                .clickable {
                                    val uri = capturedUri
                                    AppLog.info("Camera", "Use in project: $uri")
                                    if (uri != null) onMediaCaptured(uri)
                                }
                                .testTag("camera_use_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF041E21),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.camera_use_media),
                                color = Color(0xFF041E21),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF3A3C46) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFA5A8B2),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** Private app file shared with the rest of the app through the FileProvider. */
private fun newCaptureFile(context: Context, extension: String): File {
    val dir = File(context.filesDir, "camera").apply { mkdirs() }
    return File(dir, "capture_${System.currentTimeMillis()}.$extension")
}

private fun uriForFile(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
