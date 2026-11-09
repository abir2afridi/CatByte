package com.example.engine

/**
 * Multi-Camera Audio Waveform Synchronization Engine.
 */
object AudioSyncEngine {

    /**
     * Calculates the time offset (in milliseconds) needed to align [targetWaveform]
     * with [referenceWaveform] using peak correlation.
     */
    fun calculateSyncOffsetMs(referenceWaveform: List<Float>, targetWaveform: List<Float>): Long {
        if (referenceWaveform.isEmpty() || targetWaveform.isEmpty()) return 0L

        // Simple cross-correlation peak alignment
        var bestShift = 0
        var maxScore = Float.MIN_VALUE

        for (shift in -10..10) {
            var score = 0f
            for (i in referenceWaveform.indices) {
                val targetIdx = i + shift
                if (targetIdx in targetWaveform.indices) {
                    score += referenceWaveform[i] * targetWaveform[targetIdx]
                }
            }
            if (score > maxScore) {
                maxScore = score
                bestShift = shift
            }
        }

        // Each index step equals roughly 100ms
        return (bestShift * 100L)
    }
}
