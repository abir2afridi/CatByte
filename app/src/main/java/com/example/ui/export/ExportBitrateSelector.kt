package com.example.ui.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Export bitrate selector (5 - 100 Mbps) for the AI UHD dropdown.
 * The value here is what `ExportSettings.bitrateMbps` stores and what the
 * renderer multiplies into the video bitrate passed to the encoder.
 */
internal object ExportBitrateOptions {
    val values = listOf(5, 10, 20, 50, 100)
    val labels = listOf("5", "10", "20", "50", "100")

    fun indexOf(bitrateMbps: Int): Int = values.indexOf(bitrateMbps).coerceAtLeast(values.indexOf(10))
}

@Composable
internal fun ExportBitrateSelector(
    selectedBitrateMbps: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val hint = when {
            selectedBitrateMbps <= 5 -> "Small file - fine for chat apps"
            selectedBitrateMbps <= 10 -> "Recommended for this video(10)"
            selectedBitrateMbps <= 20 -> "High quality - larger file"
            else -> "Maximum quality - very large file"
        }
        ExportFeatureHeader(label = "Bitrate (Mbps)", hint = hint)
        Spacer(modifier = Modifier.height(6.dp))
        ExportSelectorTrack(
            labels = ExportBitrateOptions.labels,
            selectedIndex = ExportBitrateOptions.indexOf(selectedBitrateMbps),
            onSelect = { index -> onSelect(ExportBitrateOptions.values[index]) }
        )
    }
}
