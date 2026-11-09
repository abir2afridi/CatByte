package com.example.engine

/** Coarse device class used to pick preview and export budgets. */
enum class DeviceTier { LOW, MID, HIGH }

/** Mirrors Android's PowerManager thermal status without needing the framework. */
enum class ThermalLevel { NOMINAL, LIGHT, MODERATE, SEVERE, CRITICAL }

/**
 * Everything the capability probe reads once at first launch. Filled from
 * ActivityManager, MediaCodecList and Build in the Android layer, kept as plain
 * data here so the policy is JVM-testable.
 */
data class DeviceSpec(
    val totalRamMb: Int,
    val cpuCores: Int,
    val sdkInt: Int,
    val isLowRamDevice: Boolean = false,
    val supportsHevcEncode: Boolean = false,
    val maxDecoderHeight: Int = 1080,
    val supportsTenBitDecode: Boolean = false
)

data class PreviewBudget(
    val tier: DeviceTier,
    val previewHeight: Int,
    val targetFps: Int,
    val allowRealtimeEffects: Boolean,
    val allowOverlays: Boolean,
    val allowProxyGeneration: Boolean
)

data class ExportBudget(
    val tier: DeviceTier,
    val maxHeight: Int,
    val maxFps: Int,
    val allowHevc: Boolean,
    val allowTenBit: Boolean
)

/**
 * Graceful degradation policy: the same project must open on a 3 GB phone and
 * still edit smoothly, just with a smaller preview and fewer live effects.
 */
object DeviceProfiles {

    fun tier(spec: DeviceSpec): DeviceTier = when {
        spec.isLowRamDevice || spec.totalRamMb <= 3_072 || spec.cpuCores <= 4 -> DeviceTier.LOW
        spec.totalRamMb >= 6_144 && spec.cpuCores >= 8 && spec.sdkInt >= 31 -> DeviceTier.HIGH
        else -> DeviceTier.MID
    }

    fun previewBudget(
        tier: DeviceTier,
        thermal: ThermalLevel = ThermalLevel.NOMINAL,
        batteryPercent: Int = 100,
        isCharging: Boolean = false
    ): PreviewBudget {
        val base = when (tier) {
            DeviceTier.LOW -> PreviewBudget(tier, 540, 30, allowRealtimeEffects = false, allowOverlays = true, allowProxyGeneration = true)
            DeviceTier.MID -> PreviewBudget(tier, 720, 60, allowRealtimeEffects = true, allowOverlays = true, allowProxyGeneration = true)
            DeviceTier.HIGH -> PreviewBudget(tier, 1080, 60, allowRealtimeEffects = true, allowOverlays = true, allowProxyGeneration = true)
        }
        val thermalBudget = when (thermal) {
            ThermalLevel.NOMINAL -> base
            ThermalLevel.LIGHT -> base.copy(targetFps = base.targetFps.coerceAtMost(60))
            ThermalLevel.MODERATE -> base.copy(previewHeight = base.previewHeight.coerceAtMost(720), targetFps = base.targetFps.coerceAtMost(30))
            ThermalLevel.SEVERE -> base.copy(previewHeight = base.previewHeight.coerceAtMost(540), targetFps = 30, allowRealtimeEffects = false)
            ThermalLevel.CRITICAL -> base.copy(previewHeight = 480, targetFps = 24, allowRealtimeEffects = false, allowOverlays = false)
        }
        val batteryBudget = if (!isCharging && batteryPercent in 0..15) {
            thermalBudget.copy(previewHeight = thermalBudget.previewHeight.coerceAtMost(540), targetFps = thermalBudget.targetFps.coerceAtMost(30))
        } else {
            thermalBudget
        }
        return batteryBudget
    }

    fun exportBudget(tier: DeviceTier, spec: DeviceSpec): ExportBudget {
        val height = when (tier) {
            DeviceTier.LOW -> 1_080
            DeviceTier.MID -> 1_440
            DeviceTier.HIGH -> 2_160
        }
        return ExportBudget(
            tier = tier,
            maxHeight = minOf(height, spec.maxDecoderHeight.coerceAtLeast(720)),
            maxFps = if (tier == DeviceTier.LOW) 30 else 60,
            allowHevc = spec.supportsHevcEncode,
            allowTenBit = spec.supportsTenBitDecode && tier == DeviceTier.HIGH
        )
    }

    /** Proxy generation is skipped on battery saver to protect export time. */
    fun shouldGenerateProxies(budget: PreviewBudget, batterySaver: Boolean): Boolean =
        budget.allowProxyGeneration && !batterySaver

    /** How many overlay tracks the preview will composite live. */
    fun liveOverlayLimit(budget: PreviewBudget): Int = when {
        !budget.allowOverlays -> 1
        budget.tier == DeviceTier.LOW -> 2
        budget.tier == DeviceTier.MID -> 3
        else -> 5
    }
}
