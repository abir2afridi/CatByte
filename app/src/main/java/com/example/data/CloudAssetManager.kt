package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CloudAsset(
    val id: String,
    val name: String,
    val type: String, // BGM | SFX | STICKER
    val url: String,
    val isCached: Boolean = false
)

private val Context.assetsDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_assets_cache")

/**
 * Manages cloud SFX, BGM and sticker assets with offline DataStore caching.
 */
class CloudAssetManager(private val context: Context) {

    private object Keys {
        val CACHED_ASSETS = stringPreferencesKey("cached_assets_ids")
    }

    /** Sample cloud asset library (royalty-free SFX, BGM, stickers). */
    val assetLibrary: List<CloudAsset> = listOf(
        CloudAsset("bgm_1", "Upbeat Pop Loop", "BGM", "https://cdn.catbyte.app/bgm/upbeat_pop.mp3"),
        CloudAsset("bgm_2", "Cinematic Trailer", "BGM", "https://cdn.catbyte.app/bgm/cinematic.mp3"),
        CloudAsset("sfx_1", "Whoosh Transition", "SFX", "https://cdn.catbyte.app/sfx/whoosh.mp3"),
        CloudAsset("sfx_2", "Applause Crowd", "SFX", "https://cdn.catbyte.app/sfx/applause.mp3"),
        CloudAsset("sticker_1", "🔥 Fire Emoji", "STICKER", "https://cdn.catbyte.app/stickers/fire.png"),
        CloudAsset("sticker_2", "💯 Hundred Points", "STICKER", "https://cdn.catbyte.app/stickers/100.png")
    )

    /** Flow of cached asset IDs for offline mode. */
    val cachedAssetIds: Flow<Set<String>> = context.assetsDataStore.data.map { prefs ->
        prefs[Keys.CACHED_ASSETS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun cacheAsset(assetId: String) {
        context.assetsDataStore.edit { prefs ->
            val current = prefs[Keys.CACHED_ASSETS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(assetId)
            prefs[Keys.CACHED_ASSETS] = current.joinToString(",")
        }
    }

    suspend fun clearCache() {
        context.assetsDataStore.edit { prefs ->
            prefs.remove(Keys.CACHED_ASSETS)
        }
    }
}
