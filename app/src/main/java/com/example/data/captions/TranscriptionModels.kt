package com.example.data.captions

import android.content.Context
import com.example.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Download and storage of the on-device speech recognition models.
 *
 * The models are free-licence (Apache 2.0) Vosk builds. They are NOT shipped
 * in the APK - the user consents to the download once per language, the zip
 * lands in app-private storage, and afterwards everything runs offline.
 */
object TranscriptionModels {

    private const val TAG = "TranscriptionModels"

    /** English (small, ~40 MB) and Bengali (small, ~45 MB) Vosk models. */
    enum class Language(
        val displayName: String,
        val nativeName: String,
        val zipUrl: String,
        val folderPrefix: String
    ) {
        ENGLISH(
            "English",
            "English",
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
            "vosk-model-small-en-us"
        ),
        BENGALI(
            "Bengali",
            "বাংলা",
            "https://alphacephei.com/vosk/models/vosk-model-small-bn-0.3.zip",
            "vosk-model-small-bn"
        );

        companion object {
            /** Best model for a BCP-47-ish language tag ("bn", "bn-BD", "en-IN"...). */
            fun forTag(tag: String?): Language =
                if (tag?.startsWith("bn", ignoreCase = true) == true) BENGALI else ENGLISH
        }
    }

    data class ModelState(
        val language: Language,
        val isDownloaded: Boolean,
        val folder: File?
    )

    fun modelsDir(context: Context): File = File(context.filesDir, "speech-models")

    /** The downloaded model folder for [language], or null when not present. */
    fun installedFolder(context: Context, language: Language): File? {
        val dir = modelsDir(context)
        if (!dir.isDirectory) return null
        val exact = dir.listFiles { f -> f.isDirectory && f.name.startsWith(language.folderPrefix) }
            ?.maxByOrNull { it.name }
            ?: return null
        // A valid model folder holds the recogniser config files.
        return if (File(exact, "am/final.mdl").exists() || File(exact, "conf/model.conf").exists()) {
            exact
        } else {
            null
        }
    }

    fun isDownloaded(context: Context, language: Language): Boolean =
        installedFolder(context, language) != null

    /**
     * Downloads [language] into app storage on the IO dispatcher, reporting
     * progress as 0..1. Safe to call twice: an existing install short-circuits.
     *
     * @throws java.io.IOException on network or extraction failure - callers own
     *   the user-facing error message.
     */
    suspend fun download(
        context: Context,
        language: Language,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        installedFolder(context, language)?.let { return@withContext it }

        val dir = modelsDir(context).apply { mkdirs() }
        val zip = File(dir, "${language.folderPrefix}.zip")
        val connection = URL(language.zipUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.connect()
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            connection.inputStream.use { input ->
                zip.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val target = File(dir, language.folderPrefix)
        target.deleteRecursively() // clean half-extracted leftovers
        unzip(zip, target)
        if (!File(target, "conf/model.conf").exists() && !File(target, "am/final.mdl").exists()) {
            target.deleteRecursively()
            throw java.io.IOException("Model archive did not contain a Vosk model")
        }
        zip.delete()
        AppLog.info(TAG, "Model '${language.displayName}' installed at ${target.path}")
        target
    }

    /** Zip-slip-safe extraction of the model archive. */
    private fun unzip(zip: File, target: File) {
        val canonicalTarget = target.canonicalFile
        java.util.zip.ZipInputStream(zip.inputStream().buffered()).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                val out = File(target, entry.name)
                if (!out.canonicalPath.startsWith(canonicalTarget.canonicalPath)) {
                    throw java.io.IOException("Blocked zip entry outside target: ${entry.name}")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { stream.copyTo(it) }
                }
                stream.closeEntry()
            }
        }
    }

    /** Removes a downloaded model (e.g. when the user frees up storage). */
    fun delete(context: Context, language: Language) {
        installedFolder(context, language)?.deleteRecursively()
    }
}
