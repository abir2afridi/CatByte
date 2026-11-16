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
    /** Preview-only safe-area and rule-of-thirds guides. */
    val showSafeAreaGuides: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /**
     * OpenAI-compatible AI endpoint. Empty means "no AI configured" and the
     * editor shows the AI tools as unavailable instead of pretending.
     * Example: https://api.openai.com  or  http://192.168.0.10:1234
     */
    val aiBaseUrl: String = "",
    val aiApiKey: String = "",
    /** Chat/completions model used by text features (remix, lyrics, translate). */
    val aiModel: String = "gpt-4o-mini",
    /** Images model used by the AI image feature. */
    val aiImageModel: String = "dall-e-3"
) {
    /** True when the user has pointed the app at an AI endpoint. */
    val hasAiEndpoint: Boolean get() = aiBaseUrl.isNotBlank()
}

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
        val SAFE_AREA_GUIDES = booleanPreferencesKey("safe_area_guides")
        val LANGUAGE = stringPreferencesKey("app_language")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_IMAGE_MODEL = stringPreferencesKey("ai_image_model")
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
                showSafeAreaGuides = prefs[Keys.SAFE_AREA_GUIDES] ?: false,
                language = prefs[Keys.LANGUAGE]
                    ?.let { value -> AppLanguage.entries.firstOrNull { it.name == value } }
                    ?: AppLanguage.SYSTEM,
                aiBaseUrl = prefs[Keys.AI_BASE_URL] ?: "",
                aiApiKey = prefs[Keys.AI_API_KEY] ?: "",
                aiModel = prefs[Keys.AI_MODEL] ?: "gpt-4o-mini",
                aiImageModel = prefs[Keys.AI_IMAGE_MODEL] ?: "dall-e-3"
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

    suspend fun setShowSafeAreaGuides(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SAFE_AREA_GUIDES] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
        LocaleUtils.persistLanguage(context, language)
    }

    /**
     * Stores the OpenAI-compatible endpoint used by the editor AI features.
     * An empty [baseUrl] disables AI again (the editor then explains that no
     * endpoint is configured instead of failing mid-edit).
     */
    suspend fun setAiConfig(baseUrl: String, apiKey: String, model: String, imageModel: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AI_BASE_URL] = baseUrl.trim()
            prefs[Keys.AI_API_KEY] = apiKey.trim()
            prefs[Keys.AI_MODEL] = model.trim().ifBlank { "gpt-4o-mini" }
            prefs[Keys.AI_IMAGE_MODEL] = imageModel.trim().ifBlank { "dall-e-3" }
        }
    }
}
