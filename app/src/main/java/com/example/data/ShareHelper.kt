package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    /** Target social apps with their package names for direct sharing. */
    data class ShareTarget(val label: String, val packageName: String, val icon: String)

    val socialTargets = listOf(
        ShareTarget("TikTok", "com.zhiliaoapp.musically", "🎵"),
        ShareTarget("Instagram", "com.instagram.android", "📸"),
        ShareTarget("YouTube", "com.google.android.youtube", "▶️"),
        ShareTarget("WhatsApp", "com.whatsapp", "💬"),
        ShareTarget("Facebook", "com.facebook.katana", "👍"),
        ShareTarget("X / Twitter", "com.twitter.android", "🐦")
    )

    /** Builds a share intent for an exported video file. */
    fun buildShareIntent(context: Context, videoFile: File, target: ShareTarget? = null): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            videoFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (target != null) {
                setPackage(target.packageName)
            }
        }
        return intent
    }

    /** Returns targets that are actually installed on the device. */
    fun installedTargets(context: Context): List<ShareTarget> {
        val pm = context.packageManager
        return socialTargets.filter { target ->
            runCatching { pm.getPackageInfo(target.packageName, 0) }.isSuccess
        }
    }
}
