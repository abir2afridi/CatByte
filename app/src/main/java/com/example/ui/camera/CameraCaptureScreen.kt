package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppLog

/**
 * The app's OWN camera: a CameraX viewfinder with a real shutter.
 *
 * - Live preview (front/back switchable) rendered through [PreviewView].
 * - Photo mode: still capture straight into the app's private camera dir.
 * - Video mode: in-app recording via the CameraX recorder (start/stop on the
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
    val controller = remember { CameraCaptureController() }

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
            controller.reportPermissionDenied(context)
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

    // Bind preview + capture use cases whenever the lens or permission changes.
    LaunchedEffect(hasCameraPermission, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect
        controller.bind(context, lifecycleOwner, previewView, lensFacing)
    }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .testTag("camera_capture_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Viewfinder
            CameraPreviewPane(
                previewView = previewView,
                capturedUri = controller.capturedUri,
                capturedIsVideo = controller.capturedIsVideo,
                hasCameraPermission = hasCameraPermission,
                statusMessage = controller.statusMessage,
                isRecording = controller.isRecording,
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            CameraTopBar(
                onBack = onBack,
                onSwitchLens = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                }
            )

            CameraCaptureControls(
                mode = mode,
                isRecording = controller.isRecording,
                cameraReady = controller.cameraReady,
                capturedUri = controller.capturedUri,
                onModeSelected = { mode = it },
                onShutter = {
                    when {
                        mode == CameraMode.VIDEO && controller.isRecording ->
                            controller.stopRecording()
                        mode == CameraMode.VIDEO ->
                            controller.startRecording(context, hasAudioPermission)
                        else -> controller.takePhoto(context)
                    }
                },
                onRetake = { controller.clearCapture() },
                onUse = {
                    val uri = controller.capturedUri
                    AppLog.info("Camera", "Use in project: $uri")
                    if (uri != null) onMediaCaptured(uri)
                }
            )
        }
    }
}
