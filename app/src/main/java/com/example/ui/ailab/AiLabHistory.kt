package com.example.ui.ailab

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** One completed AI Lab run, shown in the "Recent" list. */
data class AiLabRun(
    val toolId: String,
    val title: String,
    val summary: String,
    val atMs: Long
)

private val Context.aiLabDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "catbyte_ai_lab"
)

/**
 * Persists the AI Lab's recent runs.
 *
 * Only a handful of short strings are kept (the cap in [MAX_RUNS] bounds the
 * file), and they are stored as one JSON array under a single key so the
 * DataStore stays a single flat preference.
 */
class AiLabHistory(private val context: Context) {

    private object Keys {
        val RUNS = stringPreferencesKey("recent_runs")
    }

    val runs: Flow<List<AiLabRun>> = context.aiLabDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> decode(prefs[Keys.RUNS]) }

    /** Records a run, most recent first, dropping anything past [MAX_RUNS]. */
    suspend fun record(toolId: String, title: String, summary: String) {
        val entry = JSONObject()
            .put("id", toolId)
            .put("title", title)
            .put("summary", summary.take(120))
            .put("at", System.currentTimeMillis())
        context.aiLabDataStore.edit { prefs ->
            val existing = JSONArray(prefs[Keys.RUNS] ?: "[]")
            val merged = JSONArray()
            merged.put(entry)
            for (index in 0 until existing.length()) {
                if (merged.length() >= MAX_RUNS) break
                merged.put(existing.getJSONObject(index))
            }
            prefs[Keys.RUNS] = merged.toString()
        }
    }

    suspend fun clear() {
        context.aiLabDataStore.edit { it.remove(Keys.RUNS) }
    }

    private fun decode(raw: String?): List<AiLabRun> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching {
                    val item = array.getJSONObject(index)
                    AiLabRun(
                        toolId = item.optString("id"),
                        title = item.optString("title"),
                        summary = item.optString("summary"),
                        atMs = item.optLong("at", 0L)
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val MAX_RUNS = 8
    }
}
