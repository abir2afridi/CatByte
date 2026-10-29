package com.example.data

import android.content.Context

/**
 * Small manual dependency container. Keeping construction in one place makes the
 * ViewModel easy to construct in tests and avoids a heavier DI framework.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val projectRepository = ProjectRepository()
    val settingsRepository = SettingsRepository(appContext)
    val cacheManager = CacheManager(appContext)
}
