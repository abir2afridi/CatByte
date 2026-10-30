package com.example.data

import android.content.Context

/**
 * Small manual dependency container. Keeping construction in one place makes the
 * ViewModel easy to construct in tests and avoids a heavier DI framework.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Application context, used by the view models for media and file access. */
    val context: Context get() = appContext

    val projectRepository = ProjectRepository(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val cacheManager = CacheManager(appContext)
    val playerManager = PlayerManager(appContext)
    val exportManager = ExportManager(appContext)
    val cloudAssetManager = CloudAssetManager(appContext)
}
