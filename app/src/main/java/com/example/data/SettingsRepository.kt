package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val defaultResolution: String = "1080P",
    val defaultFps: Int = 30,
    val showPaperGrid: Boolean = true,
    val watermarkEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "catbyte_settings"
)

/**
 * Persists user preferences with DataStore. The chosen language is mirrored to
 * SharedPreferences so [LocaleUtils] can apply it before the activity inflates.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val RESOLUTION = stringPreferencesKey("default_resolution")
        val FPS = intPreferencesKey("default_fps")
        val PAPER_GRID = booleanPreferencesKey("show_paper_grid")
        val WATERMARK = booleanPreferencesKey("watermark_enabled")
        val LANGUAGE = stringPreferencesKey("app_language")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME]
                    ?.let { value -> runCatching { AppThemeMode.valueOf(value) }.getOrNull() }
                    ?: AppThemeMode.LIGHT,
                defaultResolution = prefs[Keys.RESOLUTION] ?: "1080P",
                defaultFps = prefs[Keys.FPS] ?: 30,
                showPaperGrid = prefs[Keys.PAPER_GRID] ?: true,
                watermarkEnabled = prefs[Keys.WATERMARK] ?: false,
                language = prefs[Keys.LANGUAGE]
                    ?.let { value -> AppLanguage.entries.firstOrNull { it.name == value } }
                    ?: AppLanguage.SYSTEM
            )
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setDefaultResolution(resolution: String) {
        context.settingsDataStore.edit { it[Keys.RESOLUTION] = resolution }
    }

    suspend fun setDefaultFps(fps: Int) {
        context.settingsDataStore.edit { it[Keys.FPS] = fps }
    }

    suspend fun setShowPaperGrid(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PAPER_GRID] = enabled }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.WATERMARK] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
        LocaleUtils.persistLanguage(context, language)
    }
}
