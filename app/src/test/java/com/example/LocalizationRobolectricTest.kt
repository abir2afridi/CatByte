package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Device-independent Android resource tests. `sdk = [34]` pins the Robolectric
 * Android-14 runtime (the jar cached in the test sandbox); newer levels would
 * need matching `android-all-instrumented` jars.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalizationRobolectricTest {

    @Test
    fun `english resources carry the app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("CatByte", context.getString(R.string.app_name))
    }

    @Test
    fun `bengali resources are actually translated`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bengaliContext = context.createConfigurationContext(
            Configuration().apply { setLocale(Locale("bn", "BD")) }
        )
        // settings_title used to be an untranslated English copy — guard it.
        assertEquals("সেটিংস", bengaliContext.getString(R.string.settings_title))
        // Menu and tool labels are user-facing; they must be Bengali too.
        assertEquals("সব", bengaliContext.getString(R.string.filter_all))
        assertEquals("টেক্সট", bengaliContext.getString(R.string.tool_text))
        assertEquals("আনডু", bengaliContext.getString(R.string.undo))
        // Placeholders survive translation.
        assertEquals(
            bengaliContext.getString(R.string.fps_label, 30),
            "30 fps"
        )
    }
}
