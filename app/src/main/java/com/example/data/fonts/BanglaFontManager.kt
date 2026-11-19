package com.example.data.fonts

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.annotation.Keep
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages loading and caching of bundled Bangla fonts from assets.
 * Fonts are copied to app's files directory for Typeface.createFromFile.
 */
@Keep
object BanglaFontManager {

    private const val TAG = "BanglaFontManager"
    private const val FONTS_DIR = "fonts/bangla/"

    // Bundled OFL Bangla fonts (must be placed in assets/fonts/bangla/)
    private val FONT_FILES = mapOf(
        "NotoSansBengali-Regular" to "NotoSansBengali-Regular.ttf",
        "NotoSansBengali-Bold" to "NotoSansBengali-Bold.ttf",
        "HindSiliguri-Regular" to "HindSiliguri-Regular.ttf",
        "HindSiliguri-Bold" to "HindSiliguri-Bold.ttf",
        "TiroBangla-Regular" to "TiroBangla-Regular.ttf",
        "TiroBangla-Bold" to "TiroBangla-Bold.ttf",
        "AnekBangla-Regular" to "AnekBangla-Regular.ttf",
        "AnekBangla-Bold" to "AnekBangla-Bold.ttf"
    )

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    private var initialized = false
    private var contextRef: Context? = null

    /**
     * Initializes the font manager. Must be called once at app startup.
     */
    fun initialize(context: Context) {
        if (initialized) return
        contextRef = context.applicationContext
        initialized = true

        // Pre-copy fonts to files directory for faster Typeface.createFromFile
        copyFontsToFilesDir()
    }

    private fun copyFontsToFilesDir() {
        val context = contextRef ?: return
        val filesDir = File(context.filesDir, "bangla_fonts")
        if (!filesDir.exists()) filesDir.mkdirs()

        for ((fontName, assetPath) in FONT_FILES) {
            val destFile = File(filesDir, assetPath)
            if (!destFile.exists()) {
                copyAssetToFile(context, FONTS_DIR + assetPath, destFile)
            }
        }
    }

    private fun copyAssetToFile(context: Context, assetPath: String, destFile: File) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = context.assets.open(assetPath)
            outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(8192)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                outputStream.write(buffer, 0, len)
            }
            outputStream.flush()
            Log.d(TAG, "Copied font: $assetPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy font $assetPath", e)
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    /**
     * Gets a Typeface for the given font family and weight.
     * Falls back to system font if bundled font not found.
     */
    fun getTypeface(fontFamily: String, weight: Int): Typeface {
        val cacheKey = "${fontFamily}_${weight}"
        return typefaceCache.computeIfAbsent(cacheKey) {
            createTypeface(fontFamily, weight)
        }
    }

    private fun createTypeface(fontFamily: String, weight: Int): Typeface {
        val context = contextRef ?: return Typeface.DEFAULT

        // Map font family to bundled font
        val fontFile = when (fontFamily.lowercase()) {
            "noto sans bengali", "noto sans bengali ui" -> if (weight >= 600) "NotoSansBengali-Bold.ttf" else "NotoSansBengali-Regular.ttf"
            "hind siliguri" -> if (weight >= 600) "HindSiliguri-Bold.ttf" else "HindSiliguri-Regular.ttf"
            "tiro bangla" -> if (weight >= 600) "TiroBangla-Bold.ttf" else "TiroBangla-Regular.ttf"
            "anek bangla" -> if (weight >= 600) "AnekBangla-Bold.ttf" else "AnekBangla-Regular.ttf"
            else -> null
        }

        fontFile?.let { fileName ->
            val file = File(context.filesDir, "bangla_fonts/$fileName")
            if (file.exists()) {
                return Typeface.createFromFile(file)
            }
        }

        // Fallback: try system font
        return try {
            Typeface.create(fontFamily, weight)
        } catch (e: Exception) {
            Log.w(TAG, "Font $fontFamily not found, using default")
            Typeface.DEFAULT
        }
    }

    /**
     * Gets the best available Bangla font for the given weight.
     * Priority: Noto Sans Bengali > Hind Siliguri > Tiro Bangla > Anek Bangla
     */
    fun getBestBanglaTypeface(weight: Int): Typeface {
        val families = listOf("Noto Sans Bengali", "Hind Siliguri", "Tiro Bangla", "Anek Bangla")
        for (family in families) {
            val typeface = getTypeface(family, weight)
            if (typeface != Typeface.DEFAULT) return typeface
        }
        return Typeface.DEFAULT
    }

    /**
     * Checks if Bangla fonts are available.
     */
    fun areFontsAvailable(): Boolean {
        val context = contextRef ?: return false
        return FONT_FILES.values.any { fileName ->
            File(context.filesDir, "bangla_fonts/$fileName").exists()
        }
    }

    /**
     * Preloads all font variants into cache.
     */
    fun preloadAll() {
        val weights = intArrayOf(400, 500, 600, 700)
        val families = listOf("Noto Sans Bengali", "Hind Siliguri", "Tiro Bangla", "Anek Bangla")
        for (family in families) {
            for (weight in weights) {
                getTypeface(family, weight)
            }
        }
    }
}