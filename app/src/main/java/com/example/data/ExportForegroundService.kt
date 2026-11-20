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
import com.example.data.db.CatByteDatabase
import com.example.data.db.ExportJobDao
import com.example.data.db.ExportJobEntity
import com.example.data.db.ProjectJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Foreground service that drains the export queue in the background.
 *
 * Every job is a row in `export_jobs` carrying the full project payload, so a
 * queued export survives app restarts, can be retried, and shows up in the
 * history even while the editor is busy. Jobs run strictly one at a time (a
 * phone's hardware encoder does not parallelise well) and report progress
 * through a persistent notification.
 */
class ExportForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "catbyte_export_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.catbyte.EXPORT_START"
        const val ACTION_CANCEL = "com.example.catbyte.EXPORT_CANCEL"
        const val ACTION_DRAIN = "com.example.catbyte.EXPORT_DRAIN"
        const val EXTRA_PROJECT_JSON = "project_json"
        const val EXTRA_RESOLUTION = "resolution"
        const val EXTRA_FPS = "fps"
        const val EXTRA_WATERMARK = "watermark"
        const val EXTRA_BITRATE_MBPS = "bitrate_mbps"
        const val EXTRA_AUDIO_BOOST = "audio_boost"
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_TITLE = "title"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var drainJob: Job? = null
    private var progressJob: Job? = null
    private var exportManager: ExportManager? = null
    private var jobDao: ExportJobDao? = null
    private var notificationManager: NotificationManager? = null
    private var currentJobId: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        exportManager = ExportManager(this)
        jobDao = CatByteDatabase.getInstance(this).exportJobDao()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelCurrent()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                // A background export request: park it in the queue and let
                // the drain loop handle it (and anything queued before it).
                val projectJson = intent.getStringExtra(EXTRA_PROJECT_JSON)
                if (projectJson != null) {
                    val project = ProjectJson.fromJson(projectJson)
                    if (project != null) {
                        scope.launch {
                            jobDao?.insert(
                                ExportJobEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = intent.getStringExtra(EXTRA_TITLE)
                                        ?: project.title,
                                    payload = projectJson,
                                    resolution = intent.getStringExtra(EXTRA_RESOLUTION)
                                        ?: "1080P",
                                    fps = intent.getIntExtra(EXTRA_FPS, 30),
                                    watermark = intent.getBooleanExtra(EXTRA_WATERMARK, false),
                                    bitrateMbps = intent.getIntExtra(EXTRA_BITRATE_MBPS, 10),
                                    audioBoost = intent.getFloatExtra(EXTRA_AUDIO_BOOST, 1f),
                                    state = ExportJobEntity.STATE_QUEUED,
                                    progress = 0f,
                                    outputPath = null,
                                    error = null,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            ensureDrain()
                        }
                        // Foreground must start promptly on API 26+; park the
                        // service in the foreground while the row is written.
                        startForegroundTyped(
                            buildNotificationBuilder(0, getString(R.string.export_notification_preparing), false).build()
                        )
                        return START_STICKY
                    }
                }
                return START_NOT_STICKY
            }
            ACTION_DRAIN -> ensureDrain()
        }
        return START_STICKY
    }

    /** Starts the single drain loop when it is not already running. */
    private fun ensureDrain() {
        if (drainJob?.isActive == true) return
        drainJob = scope.launch { drain() }
    }

    /**
     * FIFO loop: oldest QUEUED row out, encode, record, repeat. The service
     * stops itself when the queue runs dry.
     */
    private suspend fun drain() {
        val dao = jobDao ?: return
        val manager = exportManager ?: return
        try {
            while (true) {
                val job = dao.nextQueued() ?: break
                currentJobId = job.id
                dao.updateResult(job.id, ExportJobEntity.STATE_RUNNING, 0f, null, null)

                val notification = buildNotificationBuilder(
                    0,
                    getString(R.string.export_notification_preparing),
                    false
                ).setContentText("Exporting ${job.title}").build()
                startForegroundTyped(notification)

                AppLog.info(
                    "ExportForegroundService",
                    "Queue export '${job.title}' (${job.resolution}, ${job.fps} fps)"
                )

                var lastPersisted = 0f
                val monitor = scope.launch {
                    while (true) {
                        val progress = manager.progress.value
                        if (progress != null && progress.progress > 0f &&
                            progress.progress - lastPersisted >= 0.02f
                        ) {
                            lastPersisted = progress.progress
                            dao.updateProgress(job.id, progress.progress)
                            notificationManager?.notify(
                                NOTIFICATION_ID,
                                buildNotificationBuilder(
                                    (progress.progress * 100).toInt(),
                                    progress.stage,
                                    progress.isDone
                                ).setContentText("Exporting ${job.title}").build()
                            )
                        }
                        if (progress?.isDone == true || progress?.error != null) break
                        delay(500)
                    }
                }

                val result = try {
                    manager.export(
                        ProjectJson.fromJson(job.payload) ?: break,
                        job.resolution,
                        job.fps,
                        job.watermark,
                        job.bitrateMbps.coerceIn(1, 100) * 1_000_000,
                        job.audioBoost
                    )
                } catch (error: Throwable) {
                    AppLog.error("ExportForegroundService", "Queued export failed", error)
                    dao.updateResult(
                        job.id, ExportJobEntity.STATE_FAILED, 0f, null,
                        error.message ?: "Unknown error"
                    )
                    showFinishedNotification(
                        getString(R.string.export_notification_failed, error.message ?: "Unknown error")
                    )
                    monitor.cancel()
                    currentJobId = null
                    continue
                }
                monitor.cancel()

                if (result != null) {
                    dao.updateResult(job.id, ExportJobEntity.STATE_DONE, 1f, result.file.path, null)
                    val builder = buildNotificationBuilder(
                        100,
                        getString(R.string.export_notification_tap_to_open),
                        true
                    )
                        .setContentText("Exported ${job.title}")
                        .setContentIntent(createOpenIntent(result.file))
                        .setAutoCancel(true)
                    notificationManager?.notify(NOTIFICATION_ID, builder.build())
                } else {
                    val errorMsg = manager.progress.value?.error ?: "Unknown error"
                    dao.updateResult(job.id, ExportJobEntity.STATE_FAILED, 0f, null, errorMsg)
                    showFinishedNotification(
                        getString(R.string.export_notification_failed, errorMsg)
                    )
                }
                currentJobId = null
            }
            // Queue empty: leave the foreground cleanly.
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (error: Throwable) {
            // The drain itself died (not just one job): record and stop.
            AppLog.error("ExportForegroundService", "Export drain failed", error)
            currentJobId?.let { id ->
                jobDao?.updateResult(id, ExportJobEntity.STATE_FAILED, 0f, null, error.message)
            }
            currentJobId = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun showFinishedNotification(text: String) {
        val builder = buildNotificationBuilder(0, text, true).setAutoCancel(true)
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
        stopForeground(STOP_FOREGROUND_REMOVE)
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

    /** Cancels the in-flight job (queued jobs stay queued) and keeps draining. */
    private fun cancelCurrent() {
        val cancelledId = currentJobId
        exportManager?.cancel()
        progressJob?.cancel()
        if (cancelledId != null) {
            scope.launch {
                jobDao?.updateResult(
                    cancelledId, ExportJobEntity.STATE_CANCELLED, 0f, null, "Cancelled"
                )
                currentJobId = null
                // The drain loop sees the cancelled encoder and moves on.
                ensureDrain()
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        val builder = buildNotificationBuilder(0, getString(R.string.export_cancelled), true)
            .setAutoCancel(true)
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.export_notification_title),
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
            .setContentTitle(getString(R.string.export_notification_title))
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
        drainJob?.cancel()
        progressJob?.cancel()
        exportManager?.cancel()
        super.onDestroy()
    }
}
