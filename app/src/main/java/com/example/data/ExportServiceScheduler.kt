package com.example.data

import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Scheduler to start/stop the export foreground service.
 */
object ExportServiceScheduler {

    fun startExport(
        context: Context,
        project: com.example.model.VideoProject,
        resolution: String,
        fps: Int,
        watermark: Boolean,
        bitrateMbps: Int = 10,
        /** Gain applied to the exported mix (1 = untouched). */
        audioBoost: Float = 1f
    ) {
        val intent = Intent(context, ExportForegroundService::class.java).apply {
            action = ExportForegroundService.ACTION_START
            putExtra(ExportForegroundService.EXTRA_PROJECT_JSON, com.example.data.db.ProjectJson.toJson(project))
            putExtra(ExportForegroundService.EXTRA_RESOLUTION, resolution)
            putExtra(ExportForegroundService.EXTRA_FPS, fps)
            putExtra(ExportForegroundService.EXTRA_WATERMARK, watermark)
            putExtra(ExportForegroundService.EXTRA_BITRATE_MBPS, bitrateMbps)
            putExtra(ExportForegroundService.EXTRA_AUDIO_BOOST, audioBoost)
            putExtra(ExportForegroundService.EXTRA_PROJECT_ID, project.id)
            putExtra(ExportForegroundService.EXTRA_TITLE, project.title)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /** Nudges the service to (re)start draining the queue. */
    fun startDrain(context: Context) {
        val intent = Intent(context, ExportForegroundService::class.java).apply {
            action = ExportForegroundService.ACTION_DRAIN
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
