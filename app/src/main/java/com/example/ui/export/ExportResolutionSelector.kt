package com.example.ui.export

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Export resolution selector (480P - 4K) for the AI UHD dropdown.
 *
 * Owns the option values AND the pixel mapping contract: the string here is
 * what `ExportSettings.resolution` stores and what
 * `com.example.data.export.ExportResolution` turns into output pixels.
 */
internal object ExportResolutionOptions {
    val values = listOf("480P", "720P", "1080P", "2K", "4K")
    val labels = listOf("480p", "720p", "1080p", "2k", "4k")

    /** CapCut disables the first stop: below the minimum quality bar. */
    const val minimumIndex = 1

    fun indexOf(resolution: String): Int =
        values.indexOf(resolution).coerceAtLeast(minimumIndex)
}

@Composable
internal fun ExportResolutionSelector(
    selectedResolution: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        // The hint describes the currently selected stop.
        val hint = when (selectedResolution) {
            "480P" -> "Data saver - small file, quick upload"
            "720P" -> "Standard definition - the resolution used for most TikTok videos"
            "1080P" -> "High definition - crisp on most phones"
            "2K" -> "2K - sharper, larger file"
            else -> "4K - maximum detail, largest file"
        }
        ExportFeatureHeader(label = "Resolution", hint = hint)
        Spacer(modifier = Modifier.height(6.dp))
        ExportSelectorTrack(
            labels = ExportResolutionOptions.labels,
            selectedIndex = ExportResolutionOptions.indexOf(selectedResolution),
            onSelect = { index -> onSelect(ExportResolutionOptions.values[index]) }
        )
    }
}
