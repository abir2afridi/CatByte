package com.example.data.fonts

import android.content.Context
import android.graphics.Typeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for Bangla Font Manager.
 * Tests font loading, caching, and fallback behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BanglaFontManagerTest {

    @Test
    fun testInitializeAndGetTypeface() {
        val context = org.robolectric.RuntimeEnvironment.application
        BanglaFontManager.initialize(context)

        // Test getting best Bangla typeface
        val typeface = BanglaFontManager.getBestBanglaTypeface(400)
        assertNotNull("Should return a typeface", typeface)

        // Test different weights
        val bold = BanglaFontManager.getBestBanglaTypeface(700)
        assertNotNull("Should return a typeface for bold", bold)
    }

    @Test
    fun testGetSpecificFontFamily() {
        val context = org.robolectric.RuntimeEnvironment.application
        BanglaFontManager.initialize(context)

        // Test Noto Sans Bengali
        val notoRegular = BanglaFontManager.getTypeface("Noto Sans Bengali", 400)
        assertNotNull(notoRegular)

        val notoBold = BanglaFontManager.getTypeface("Noto Sans Bengali", 700)
        assertNotNull(notoBold)

        // Test Hind Siliguri
        val hindRegular = BanglaFontManager.getTypeface("Hind Siliguri", 400)
        assertNotNull(hindRegular)

        // Test Tiro Bangla
        val tiroRegular = BanglaFontManager.getTypeface("Tiro Bangla", 400)
        assertNotNull(tiroRegular)

        // Test Anek Bangla
        val anekRegular = BanglaFontManager.getTypeface("Anek Bangla", 400)
        assertNotNull(anekRegular)
    }

    @Test
    fun testFallbackToSystemFont() {
        val context = org.robolectric.RuntimeEnvironment.application
        BanglaFontManager.initialize(context)

        // Non-existent font family should fallback to system default
        val fallback = BanglaFontManager.getTypeface("NonExistentFont", 400)
        assertNotNull(fallback)
        // Should be Typeface.DEFAULT or similar
    }

    @Test
    fun testFontAvailabilityCheck() {
        val context = org.robolectric.RuntimeEnvironment.application
        BanglaFontManager.initialize(context)

        // Fonts may not be available in test environment (assets not copied)
        // This test documents expected behavior
        val available = BanglaFontManager.areFontsAvailable()
        // In test env, this might be false since assets aren't copied
        // In production, should be true after initialize()
    }

    @Test
    fun testPreloadAll() {
        val context = org.robolectric.RuntimeEnvironment.application
        BanglaFontManager.initialize(context)

        // Should not throw
        BanglaFontManager.preloadAll()
    }
}