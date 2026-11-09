package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnginePolicyTest {

    // ------------------------------------------------------------------
    // Device tiers
    // ------------------------------------------------------------------

    @Test
    fun `tier follows memory cores and low ram flag`() {
        assertEquals(DeviceTier.LOW, DeviceProfiles.tier(DeviceSpec(2_048, 4, 30, isLowRamDevice = true)))
        assertEquals(DeviceTier.LOW, DeviceProfiles.tier(DeviceSpec(2_048, 8, 33)))
        assertEquals(DeviceTier.MID, DeviceProfiles.tier(DeviceSpec(4_096, 8, 30)))
        assertEquals(DeviceTier.HIGH, DeviceProfiles.tier(DeviceSpec(8_192, 8, 33, supportsHevcEncode = true)))
    }

    @Test
    fun `thermal stress degrades the preview budget`() {
        val nominal = DeviceProfiles.previewBudget(DeviceTier.HIGH, ThermalLevel.NOMINAL)
        assertEquals(1_080, nominal.previewHeight)
        assertEquals(60, nominal.targetFps)

        val critical = DeviceProfiles.previewBudget(DeviceTier.HIGH, ThermalLevel.CRITICAL)
        assertEquals(480, critical.previewHeight)
        assertEquals(24, critical.targetFps)
        assertFalse(critical.allowRealtimeEffects)
        assertFalse(critical.allowOverlays)
    }

    @Test
    fun `low battery drops preview quality unless charging`() {
        val drained = DeviceProfiles.previewBudget(
            DeviceTier.HIGH, ThermalLevel.NOMINAL, batteryPercent = 10, isCharging = false
        )
        assertEquals(540, drained.previewHeight)
        val charging = DeviceProfiles.previewBudget(
            DeviceTier.HIGH, ThermalLevel.NOMINAL, batteryPercent = 10, isCharging = true
        )
        assertEquals(1_080, charging.previewHeight)
    }

    @Test
    fun `export budget respects the decoder limit`() {
        assertEquals(1_080, DeviceProfiles.exportBudget(DeviceTier.LOW, DeviceSpec(2_048, 4, 30)).maxHeight)
        val capped = DeviceProfiles.exportBudget(
            DeviceTier.HIGH,
            DeviceSpec(8_192, 8, 33, supportsHevcEncode = true, maxDecoderHeight = 720)
        )
        assertEquals(720, capped.maxHeight)
        assertTrue(capped.allowHevc)
        assertFalse(capped.allowTenBit)
    }

    @Test
    fun `proxy generation is skipped on battery saver`() {
        val budget = DeviceProfiles.previewBudget(DeviceTier.MID)
        assertTrue(DeviceProfiles.shouldGenerateProxies(budget, batterySaver = false))
        assertFalse(DeviceProfiles.shouldGenerateProxies(budget, batterySaver = true))
        assertEquals(2, DeviceProfiles.liveOverlayLimit(DeviceProfiles.previewBudget(DeviceTier.LOW)))
    }

    // ------------------------------------------------------------------
    // Decoder budget
    // ------------------------------------------------------------------

    @Test
    fun `preview decoders are granted before thumbnails`() {
        val plan = DecoderBudget(maxInstances = 2, maxMemoryMb = 256).plan(
            listOf(
                DecoderRequest("thumb", WorkPriority.THUMBNAIL, lastUsedAtMs = 1L),
                DecoderRequest("next", WorkPriority.PREVIEW, lastUsedAtMs = 5L),
                DecoderRequest("export", WorkPriority.EXPORT, lastUsedAtMs = 3L)
            )
        )
        assertEquals(2, plan.granted.size)
        assertEquals(WorkPriority.PREVIEW, plan.granted.first().priority)
        assertTrue(plan.deferred.contains("thumb"))
    }

    @Test
    fun `proxy policy flags 4k and heavy codecs`() {
        val uhd = ProxyPolicy.decide(
            ProxyInput("clip", 3_840, 2_160, "hevc", gopMs = 1_000L, isVariableFrameRate = false)
        )
        assertTrue(uhd.required)
        assertEquals(ProxyReason.HIGH_RESOLUTION, uhd.reason)
        assertEquals(720, uhd.targetHeight)
        assertTrue(uhd.exportReadsOriginal)

        val small = ProxyPolicy.decide(
            ProxyInput("clip2", 1_280, 720, "avc", gopMs = 500L, isVariableFrameRate = false)
        )
        assertFalse(small.required)
        assertEquals(ProxyReason.NOT_NEEDED, small.reason)
    }

    // ------------------------------------------------------------------
    // Auto reframe
    // ------------------------------------------------------------------

    @Test
    fun `center crop matches the target aspect`() {
        val crop = AutoReframe.centerCrop(sourceAspect = 16f / 9f, targetAspect = 9f / 16f)
        assertEquals(0.3164f, crop.width, 0.01f)
        assertEquals(1f, crop.height, 0.001f)
        assertEquals(0.5f, crop.centerX, 0.001f)
    }

    @Test
    fun `subject near the edge stays inside the frame`() {
        val crop = AutoReframe.cropWindow(
            subjects = listOf(SubjectBox(NormRect(0.8f, 0.4f, 0.95f, 0.7f))),
            sourceAspect = 16f / 9f,
            targetAspect = 9f / 16f
        )
        assertTrue(crop.left >= -0.0001f)
        assertTrue(crop.right <= 1.0001f)
        assertTrue(crop.top >= -0.0001f)
        assertTrue(crop.bottom <= 1.0001f)
    }

    @Test
    fun `smoothing interpolates and ken burns ends on target`() {
        val mid = AutoReframe.smooth(NormRect.FULL, NormRect.CENTER, 0.5f)
        assertEquals(0.125f, mid.left, 0.001f)
        assertEquals(0.875f, mid.right, 0.001f)

        assertEquals(1f, AutoReframe.kenBurns(NormRect.FULL, NormRect.CENTER, 0f).width, 0.001f)
        assertEquals(0.5f, AutoReframe.kenBurns(NormRect.FULL, NormRect.CENTER, 1f).width, 0.001f)
    }

    @Test
    fun `pixel crop is even sized and inside the frame`() {
        val crop = AutoReframe.toPixels(NormRect(0f, 0f, 0.5f, 1f), 1_000, 1_000)
        assertEquals(0, crop.x)
        assertEquals(500, crop.width)
        assertEquals(1_000, crop.height)
        assertEquals(0, crop.width % 2)
    }

    // ------------------------------------------------------------------
    // Silence detection
    // ------------------------------------------------------------------

    @Test
    fun `quiet middle section is detected`() {
        val peaks = List(20) { index -> if (index in 5..9) 0.02f else 0.5f }
        val silences = SilenceDetector.detect(peaks, durationMs = 10_000L, threshold = 0.08f, minSilenceMs = 800L)
        assertEquals(1, silences.size)
        assertEquals(2_500L, silences.single().startMs)
        assertEquals(5_000L, silences.single().endMs)
        assertEquals(2_500L, SilenceDetector.removedMs(silences))
    }

    @Test
    fun `short gaps are not cut`() {
        val peaks = List(20) { index -> if (index == 7) 0.0f else 0.5f }
        assertTrue(SilenceDetector.detect(peaks, 10_000L, minSilenceMs = 5_000L).isEmpty())
    }

    @Test
    fun `keep ranges is the inverse of the silence`() {
        val kept = SilenceDetector.keepRanges(listOf(TimeRangeMs(2_500L, 5_000L)), durationMs = 10_000L)
        assertEquals(2, kept.size)
        assertEquals(TimeRangeMs(0L, 2_500L), kept[0])
        assertEquals(TimeRangeMs(5_000L, 10_000L), kept[1])
        assertTrue(SilenceDetector.isMostlySilent(listOf(TimeRangeMs(0L, 8_000L)), 10_000L))
    }

    // ------------------------------------------------------------------
    // Export presets
    // ------------------------------------------------------------------

    @Test
    fun `bitrate scales with canvas and quality`() {
        val low = ExportPresets.videoBitrateKbps(1_080, 1_920, 30, ExportQuality.LOW)
        val high = ExportPresets.videoBitrateKbps(1_080, 1_920, 30, ExportQuality.HIGH)
        assertTrue(high > low)
        assertTrue(ExportPresets.videoBitrateKbps(1_080, 1_920, 60, ExportQuality.HIGH) >
            ExportPresets.videoBitrateKbps(1_080, 1_920, 30, ExportQuality.HIGH))
    }

    @Test
    fun `size estimate grows with duration`() {
        val preset = ExportPresets.portrait()
        val oneMinute = ExportPresets.estimateMb(preset, 60_000L)
        val thirty = ExportPresets.estimateMb(preset, 30_000L)
        assertTrue(oneMinute > thirty)
        assertTrue(oneMinute > 1.0)
    }

    @Test
    fun `preset is downgraded to the device ceiling`() {
        val preset = ExportPresets.portrait()
        val clamped = ExportPresets.clampToDevice(preset, maxHeight = 720, maxFps = 30)
        assertEquals(720, clamped.height)
        assertTrue(clamped.width < preset.width)
        assertEquals(0, clamped.width % 2)
    }

    @Test
    fun `audio only and gif presets exist`() {
        val audio = ExportPresets.audioOnly()
        assertTrue(audio.isAudioOnly)
        assertEquals(0, audio.videoBitrateKbps)
        assertTrue(ExportPresets.estimateMb(audio, 60_000L) > 0.0)

        assertEquals(ExportContainer.GIF, ExportPresets.gif().container)
        assertEquals(5, ExportPresets.forOrientation(portrait = true).size)
        assertEquals(5, ExportPresets.forOrientation(portrait = false).size)
    }
}
