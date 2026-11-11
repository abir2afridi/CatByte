package com.example.ui.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Export frame rate selector (24 - 60 fps) for the AI UHD dropdown.
 * The value here is what `ExportSettings.fps` stores and what the renderer
 * passes to the Media3 Transformer.
 */
internal object ExportFrameRateOptions {
    val values = listOf(24, 25, 30, 50, 60)
    val labels = listOf("24", "25", "30", "50", "60")

    fun indexOf(fps: Int): Int = values.indexOf(fps).coerceAtLeast(values.indexOf(30))
}

@Composable
internal fun ExportFrameRateSelector(
    selectedFps: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExportFeatureHeader(label = "Frame rate", hint = "Smoother playback")
        Spacer(modifier = Modifier.height(6.dp))
        ExportSelectorTrack(
            labels = ExportFrameRateOptions.labels,
            selectedIndex = ExportFrameRateOptions.indexOf(selectedFps),
            onSelect = { index -> onSelect(ExportFrameRateOptions.values[index]) }
        )
    }
}
