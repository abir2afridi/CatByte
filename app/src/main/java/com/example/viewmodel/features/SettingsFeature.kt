package com.example.viewmodel.features

import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

/** Editor settings: theme, language, defaults, grid, watermark and cache. */
internal object SettingsFeature {

    fun setEyedropperActive(vm: EditorViewModel, active: Boolean) {
        vm._isEyedropperActive.value = active
    }

    fun setAppThemeMode(vm: EditorViewModel, mode: com.example.ui.theme.AppThemeMode) {
        vm._appThemeMode.value = mode
        vm.viewModelScope.launch { vm.settingsRepository?.setThemeMode(mode) }
    }

    fun setAppLanguage(vm: EditorViewModel, language: AppLanguage) {
        vm._appLanguage.value = language
        vm.viewModelScope.launch { vm.settingsRepository?.setLanguage(language) }
    }

    /**
     * Saves the OpenAI-compatible endpoint the editor AI features use. The
     * in-memory config updates immediately so the next AI tap uses the new
     * endpoint without restarting the editor.
     */
    fun saveAiConfig(
        vm: EditorViewModel,
        baseUrl: String,
        apiKey: String,
        model: String,
        imageModel: String
    ) {
        vm._aiConfig.value = com.example.data.ai.AiConfig(
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            model = model.trim().ifBlank { "gpt-4o-mini" },
            imageModel = imageModel.trim().ifBlank { "dall-e-3" }
        )
        vm.viewModelScope.launch {
            vm.settingsRepository?.setAiConfig(baseUrl, apiKey, model, imageModel)
        }
    }

    fun openSettings(vm: EditorViewModel) {
        vm._isSettingsOpen.value = true
        refreshCacheSize(vm)
    }

    fun closeSettings(vm: EditorViewModel) {
        vm._isSettingsOpen.value = false
    }

    fun setDefaultResolution(vm: EditorViewModel, res: String) {
        vm._defaultResolution.value = res
        vm.viewModelScope.launch { vm.settingsRepository?.setDefaultResolution(res) }
    }

    fun setDefaultFps(vm: EditorViewModel, fps: Int) {
        vm._defaultFps.value = fps
        vm.viewModelScope.launch { vm.settingsRepository?.setDefaultFps(fps) }
    }

    fun togglePaperGrid(vm: EditorViewModel, enabled: Boolean) {
        vm._showPaperGrid.value = enabled
        vm.viewModelScope.launch { vm.settingsRepository?.setShowPaperGrid(enabled) }
    }

    fun toggleWatermark(vm: EditorViewModel, enabled: Boolean) {
        vm._watermarkEnabled.value = enabled
        vm.viewModelScope.launch { vm.settingsRepository?.setWatermarkEnabled(enabled) }
        // The watermark is burned into the same preview composition as export.
        vm.playerManager?.setWatermarkEnabled(enabled)
    }

    fun refreshCacheSize(vm: EditorViewModel) {
        val manager = vm.cacheManager ?: return
        vm.viewModelScope.launch {
            vm._cacheSizeMb.value = manager.cacheSizeMb()
        }
    }

    fun clearCache(vm: EditorViewModel) {
        val manager = vm.cacheManager
        if (manager == null) {
            vm._cacheSizeMb.value = 0
            return
        }
        vm.viewModelScope.launch {
            vm._cacheSizeMb.value = manager.clearCache()
        }
    }

    fun toggleMagneticSnap(vm: EditorViewModel) {
        vm._magneticSnap.value = !vm._magneticSnap.value
    }
}
