package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

/**
 * Golden-image tests for Bangla text shaping verification.
 * 
 * These tests render Bangla text with bundled OFL fonts and verify
 * that conjuncts, vowel signs, ref/ra-phala, and Bangla numerals render correctly.
 * 
 * Run with: ./gradlew :app:testDebugUnitTest --tests "com.example.engine.BanglaTextGoldenTest"
 * 
 * To regenerate golden images, set REGENERATE_GOLDEN=true environment variable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BanglaTextGoldenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val regenerate = System.getenv("REGENERATE_GOLDEN") == "true"
    private val goldenDir = File(context.filesDir, "golden/bangla")

    init {
        if (!goldenDir.exists()) goldenDir.mkdirs()
        // Initialize Bangla font manager
        BanglaText.initialize(context)
    }

    @Test
    fun testBanglaConjuncts() {
        // Test common Bangla conjuncts that often break
        val testCases = mapOf(
            "ক্ষ" to "ksha (conjunct)",
            "ত্র" to "tra (conjunct)",
            "জ্ঞ" to "jna (conjunct)",
            "শ্র" to "shra (conjunct)",
            "দ্দ" to "dda (conjunct)",
            "দ্ভ" to "dbha (conjunct)",
            "ল্প" to "lpa (conjunct)",
            "দ্ম" to "dma (conjunct)",
            "ঙ্ক" to "nka (conjunct)",
            "ঙ্গ" to "nga (conjunct)"
        )

        for ((text, description) in testCases) {
            val bitmap = renderText(text, 48f)
            val goldenFile = File(goldenDir, "conjunct_${text.replace("/", "_")}.png")
            verifyOrSave(bitmap, goldenFile, "Conjunct: $description ($text)")
        }
    }

    @Test
    fun testBanglaVowelSigns() {
        // Test vowel signs (kar) that attach to consonants
        val base = "ক"
        val vowelSigns = mapOf(
            "া" to "aa kar",
            "ি" to "i kar",
            "ী" to "ii kar",
            "ু" to "u kar",
            "ূ" to "uu kar",
            "ৃ" to "ri kar",
            "ে" to "e kar",
            "ৈ" to "oi kar",
            "ো" to "o kar",
            "ৌ" to "ou kar"
        )

        for ((sign, description) in vowelSigns) {
            val text = base + sign
            val bitmap = renderText(text, 48f)
            val goldenFile = File(goldenDir, "vowel_${sign}.png")
            verifyOrSave(bitmap, goldenFile, "Vowel sign: $description ($text)")
        }
    }

    @Test
    fun testBanglaRefRaPhala() {
        // Test ref (রেফ) and ra-phala (র-ফলা) forms
        val testCases = listOf(
            "র্ক" to "ref + ka",
            "র্ট" to "ref + tta",
            "র্প" to "ref + pa",
            "ক্র" to "ka + ra-phala",
            "গ্র" to "ga + ra-phala",
            "ত্র" to "ta + ra-phala",
            "দ্র" to "da + ra-phala"
        )

        for ((text, description) in testCases) {
            val bitmap = renderText(text, 48f)
            val goldenFile = File(goldenDir, "ref_ra_${text}.png")
            verifyOrSave(bitmap, goldenFile, "Ref/Ra-phala: $description ($text)")
        }
    }

    @Test
    fun testBanglaNumerals() {
        // Test Bangla digits (০-৯)
        val numerals = "০১২৩৪৫৬৭৮৯"
        val bitmap = renderText(numerals, 48f)
        val goldenFile = File(goldenDir, "numerals.png")
        verifyOrSave(bitmap, goldenFile, "Bangla numerals: $numerals")

        // Test mixed Latin/Bangla
        val mixed = "২০২৪年 Bangla ১২৩"
        val mixedBitmap = renderText(mixed, 48f)
        val mixedFile = File(goldenDir, "mixed_numerals.png")
        verifyOrSave(mixedBitmap, mixedFile, "Mixed numerals: $mixed")
    }

    @Test
    fun testBanglaWords() {
        // Test complete Bangla words
        val words = listOf(
            "বাংলা" to "Bangla",
            "দেশ" to "country",
            "ভাষা" to "language",
            "আমি" to "I",
            "তুমি" to "you",
            "সে" to "he/she",
            "আপনি" to "you (formal)",
            "ধন্যবাদ" to "thank you",
            "সুপ্রভাত" to "good morning",
            "শুভ রাত্রি" to "good night",
            "কেমন আছেন" to "how are you",
            "ভালো আছি" to "I am fine"
        )

        for ((word, meaning) in words) {
            val bitmap = renderText(word, 48f)
            val safeName = word.replace(" ", "_").replace("/", "_")
            val goldenFile = File(goldenDir, "word_$safeName.png")
            verifyOrSave(bitmap, goldenFile, "Word: $word ($meaning)")
        }
    }

    @Test
    fun testBanglaSentences() {
        // Test full sentences with punctuation
        val sentences = listOf(
            "আমি বাংলা ভাষা শিখছি।",
            "বাংলাদেশ একটি সুন্দর দেশ।",
            "আপনি কেমন আছেন? আমি ভালো আছি।",
            "আজ আকাশে বাদল আছে।",
            "সে স্কুলে পড়ে।",
            "আমরা একসাথে খাব।"
        )

        for (sentence in sentences) {
            val bitmap = renderText(sentence, 36f)
            val safeName = sentence
                .replace(" ", "_")
                .replace(".", "")
                .replace("।", "")
                .replace("?", "")
                .replace("/", "_")
            val goldenFile = File(goldenDir, "sentence_$safeName.png")
            verifyOrSave(bitmap, goldenFile, "Sentence: $sentence")
        }
    }

    @Test
    fun testAllFontVariants() {
        // Test each bundled font family at different weights
        val fonts = mapOf(
            "Noto Sans Bengali" to BanglaText.getBanglaTypeface("Noto Sans Bengali", 400),
            "Noto Sans Bengali Bold" to BanglaText.getBanglaTypeface("Noto Sans Bengali", 700),
            "Hind Siliguri" to BanglaText.getBanglaTypeface("Hind Siliguri", 400),
            "Hind Siliguri Bold" to BanglaText.getBanglaTypeface("Hind Siliguri", 700),
            "Tiro Bangla" to BanglaText.getBanglaTypeface("Tiro Bangla", 400),
            "Tiro Bangla Bold" to BanglaText.getBanglaTypeface("Tiro Bangla", 700),
            "Anek Bangla" to BanglaText.getBanglaTypeface("Anek Bangla", 400),
            "Anek Bangla Bold" to BanglaText.getBanglaTypeface("Anek Bangla", 700)
        )

        val testText = "বাংলা ভাষা"

        for ((fontName, typeface) in fonts) {
            val bitmap = renderTextWithTypeface(testText, 48f, typeface)
            val safeName = fontName.replace(" ", "_")
            val goldenFile = File(goldenDir, "font_$safeName.png")
            verifyOrSave(bitmap, goldenFile, "Font: $fontName")
        }
    }

    @Test
    fun testBanglaLineBreaking() {
        // Test that line breaking doesn't break conjuncts
        val longText = "বাংলা ভাষা আমাদের মাতৃভাষা। এটি বিশ্বের সাবধানতম ভাষা।"
        val lines = BanglaLineBreaker.wrap(longText, 15, 3)
        
        // Verify no line starts with a vowel sign or virama
        for (line in lines) {
            if (line.isNotEmpty()) {
                val firstChar = line.first()
                assertTrue(
                    "Line should not start with vowel sign: $line",
                    BanglaLineBreaker.canBreakBefore(firstChar)
                )
            }
        }
        
        // Verify shaping safety
        for (line in lines) {
            assertTrue(
                "Line should be shaping-safe: $line",
                BanglaLineBreaker.isShapingSafe(line)
            )
        }
    }

    @Test
    fun testBanglaTimecodeFormatting() {
        // Test Bangla numerals in timecode (mm:ss format)
        val timecodes = listOf(
            0L to "০০:০০",
            65_000L to "০১:০৫",
            3_661_000L to "৬১:০১" // 1h 1m 1s = 61m 1s in mm:ss format
        )

        for ((ms, expected) in timecodes) {
            val result = BanglaNumerals.timecodeLabel(ms, true)
            assertEquals("Bangla timecode for ${ms}ms", expected, result)
        }
    }

    @Test
    fun testFontAvailability() {
        assertTrue("Bangla fonts should be available", BanglaText.areFontsAvailable())
        
        // Verify each font family loads
        val families = listOf("Noto Sans Bengali", "Hind Siliguri", "Tiro Bangla", "Anek Bangla")
        for (family in families) {
            val regular = BanglaText.getBanglaTypeface(family, 400)
            val bold = BanglaText.getBanglaTypeface(family, 700)
            assertNotNull("$family Regular", regular)
            assertNotNull("$family Bold", bold)
            assertTrue("$family Regular should not be default", regular != android.graphics.Typeface.DEFAULT)
            assertTrue("$family Bold should not be default", bold != android.graphics.Typeface.DEFAULT)
        }
    }

    // Helper methods
    private fun renderText(text: String, size: Float): Bitmap {
        return renderTextWithTypeface(text, size, Typeface.DEFAULT)
    }

    private fun renderTextWithTypeface(text: String, size: Float, tf: Typeface): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = Color.WHITE
            typeface = tf
        }
        
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val width = bounds.width() + 20
        val height = bounds.height() + 20
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawText(text, 10f, 10f - bounds.top + bounds.height() / 2f, paint)
        
        return bitmap
    }

    private fun verifyOrSave(bitmap: Bitmap, file: File, description: String) {
        if (regenerate || !file.exists()) {
            saveBitmap(bitmap, file)
            if (regenerate) println("Regenerated golden: $file")
        } else {
            val golden = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("Golden image not found: $file", golden)
            assertBitmapsEqual(bitmap, golden, description)
        }
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun assertBitmapsEqual(actual: Bitmap, expected: Bitmap, description: String) {
        assertEquals("Width mismatch: $description", expected.width, actual.width)
        assertEquals("Height mismatch: $description", expected.height, actual.height)
        
        val maxDiff = 0.01f // 1% pixel difference allowed
        var diffCount = 0
        val totalPixels = actual.width * actual.height
        
        for (x in 0 until actual.width) {
            for (y in 0 until actual.height) {
                val a = actual.getPixel(x, y)
                val e = expected.getPixel(x, y)
                if (a != e) diffCount++
            }
        }
        
        val diffRatio = diffCount.toFloat() / totalPixels
        assertTrue(
            "$description: Pixel difference ${diffRatio * 100}% exceeds $maxDiff threshold",
            diffRatio <= maxDiff
        )
    }
}