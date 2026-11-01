package com.example.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Foreground service that performs video export in the background.
 * Shows a persistent notification with progress updates.
 * Survives app being swiped away from recent apps.
 */
class ExportForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "catbyte_export_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.catbyte.EXPORT_START"
        const val ACTION_CANCEL = "com.example.catbyte.EXPORT_CANCEL"
        const val EXTRA_PROJECT_JSON = "project_json"
        const val EXTRA_RESOLUTION = "resolution"
        const val EXTRA_FPS = "fps"
        const val EXTRA_WATERMARK = "watermark"
        const val EXTRA_PROJECT_ID = "project_id"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var exportJob: Job? = null
    private var exportManager: ExportManager? = null
    private var currentProjectId: String = ""
    private var notificationManager: NotificationManager? = null
    private var progressJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        exportManager = ExportManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancelExport()
            stopSelf()
            return START_NOT_STICKY
        }

        val projectJson = intent?.getStringExtra(EXTRA_PROJECT_JSON) ?: return START_NOT_STICKY
        val resolution = intent?.getStringExtra(EXTRA_RESOLUTION) ?: "1080P"
        val fps = intent?.getIntExtra(EXTRA_FPS, 30) ?: 30
        val watermark = intent?.getBooleanExtra(EXTRA_WATERMARK, false) ?: false
        currentProjectId = intent?.getStringExtra(EXTRA_PROJECT_ID) ?: ""

        val project = com.example.data.db.ProjectJson.fromJson(projectJson) ?: return START_NOT_STICKY

        // Never run two encoders at once: a second ACTION_START while a job
        // is still active would overwrite the job reference and keep both
        // transformers competing for the encoder.
        if (exportJob?.isActive == true) {
            AppLog.warn("ExportForegroundService", "Export already running; ignoring a second start request")
            return START_NOT_STICKY
        }

        // Start foreground before any heavy work, with the Android-version
        // correct foreground service type (see startForegroundTyped).
        val notification = buildNotificationBuilder(0, "Preparing export...", false).build()
        startForegroundTyped(notification)

        exportJob = scope.launch {
            try {
                var lastProgress = 0f

                // Monitor progress (notifications can be posted from any thread)
                progressJob = scope.launch {
                    while (true) {
                        val progress = exportManager?.progress?.value
                        if (progress != null && progress.progress != lastProgress && progress.progress > 0f) {
                            lastProgress = progress.progress
                            val notif = buildNotificationBuilder(
                                (progress.progress * 100).toInt(),
                                progress.stage,
                                progress.isDone
                            ).build()
                            notificationManager?.notify(NOTIFICATION_ID, notif)
                        }
                        if (progress?.isDone == true || progress?.error != null) {
                            this@ExportForegroundService.progressJob?.cancel()
                        }
                        delay(500)
                    }
                }

                val result = exportManager?.export(project, resolution, fps, watermark)

                progressJob?.cancel()

                if (result != null) {
                    val builder = buildNotificationBuilder(100, "Export complete! Tap to open.", true)
                        .setContentIntent(createOpenIntent(result.file))
                        .setAutoCancel(true)
                    val finalNotif = builder.build()
                    notificationManager?.notify(NOTIFICATION_ID, finalNotif)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    val builder = buildNotificationBuilder(0, "Export failed: ${exportManager?.progress?.value?.error ?: "Unknown error"}", true)
                        .setAutoCancel(true)
                    val errorNotif = builder.build()
                    notificationManager?.notify(NOTIFICATION_ID, errorNotif)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            } catch (e: Throwable) {
                AppLog.error("ExportForegroundService", "Background export failed", e)
                val builder = buildNotificationBuilder(0, "Export failed: ${e.message}", true)
                    .setAutoCancel(true)
                val errorNotif = builder.build()
                notificationManager?.notify(NOTIFICATION_ID, errorNotif)
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }

        return START_STICKY
    }

    /**
     * Calls startForeground with the right foreground service type for the
     * running Android version. `mediaProcessing` only exists on API 35+; the
     * manifest also declares `dataSync` so API 34 devices keep a valid type.
     */
    @Suppress("DEPRECATION")
    private fun startForegroundTyped(notification: Notification) {
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM ->
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                    )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )

                else -> startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure { error ->
            AppLog.error("ExportForegroundService", "startForeground failed, falling back", error)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelExport() {
        exportJob?.cancel()
        progressJob?.cancel()
        exportManager?.cancel()
        val builder = buildNotificationBuilder(0, "Export cancelled", true)
            .setAutoCancel(true)
        val errorNotif = builder.build()
        notificationManager?.notify(NOTIFICATION_ID, errorNotif)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CatByte Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video export progress"
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotificationBuilder(
        progress: Int,
        text: String,
        isComplete: Boolean
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_export)
            .setContentTitle("CatByte Export")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setOngoing(!isComplete)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(if (isComplete) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)

        if (isComplete) {
            builder.setProgress(0, 0, false)
        }
        return builder
    }

    private fun createOpenIntent(file: File): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    this@ExportForegroundService,
                    "${packageName}.fileprovider",
                    file
                ),
                "video/mp4"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        exportJob?.cancel()
        progressJob?.cancel()
        exportManager?.cancel()
        super.onDestroy()
    }
}

/**
 * Scheduler to start/stop the export foreground service.
 */
object ExportServiceScheduler {

    fun startExport(
        context: Context,
        project: com.example.model.VideoProject,
        resolution: String,
        fps: Int,
        watermark: Boolean
    ) {
        val intent = Intent(context, ExportForegroundService::class.java).apply {
            action = ExportForegroundService.ACTION_START
            putExtra(ExportForegroundService.EXTRA_PROJECT_JSON, com.example.data.db.ProjectJson.toJson(project))
            putExtra(ExportForegroundService.EXTRA_RESOLUTION, resolution)
            putExtra(ExportForegroundService.EXTRA_FPS, fps)
            putExtra(ExportForegroundService.EXTRA_WATERMARK, watermark)
            putExtra(ExportForegroundService.EXTRA_PROJECT_ID, project.id)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelExport(context: Context) {
        val intent = Intent(context, ExportForegroundService::class.java).apply {
            action = ExportForegroundService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    fun isExportRunning(context: Context): Boolean {
        // getRunningServices is deprecated but there is no public replacement
        // for checking our own started service state.
        @Suppress("DEPRECATION")
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(100).any { it.service.className == ExportForegroundService::class.java.name }
    }
}