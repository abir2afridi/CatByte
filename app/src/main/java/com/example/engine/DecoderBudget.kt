package com.example.engine

import kotlin.math.max

/** What a decode slot is being used for. Preview wins every fight. */
enum class WorkPriority { PREVIEW, EXPORT, PROXY, THUMBNAIL }

data class DecoderRequest(
    val clipId: String,
    val priority: WorkPriority,
    val lastUsedAtMs: Long,
    val estimatedMb: Int = 48
)

data class DecoderLease(
    val clipId: String,
    val priority: WorkPriority
)

data class BudgetPlan(
    val granted: List<DecoderLease>,
    val deferred: List<String>,
    val evicted: List<String>,
    val usedMb: Int
)

/**
 * Decoder budget: MediaCodec instances are a scarce, global resource. Exceeding
 * them makes preview stall and export fail with "codec init" errors.
 *
 * The policy is deterministic so it can be unit tested: preview requests are
 * granted first, then export, then proxies, then thumbnails; within a priority
 * the least recently used request is evicted first, and the memory ceiling
 * always wins over the instance ceiling.
 */
class DecoderBudget(
    private val maxInstances: Int = 4,
    private val maxMemoryMb: Int = 640
) {

    fun plan(requests: List<DecoderRequest>, exclude: Set<String> = emptySet()): BudgetPlan {
        val ordered = requests
            .filter { it.clipId !in exclude }
            .sortedWith(compareBy({ it.priority.ordinal }, { it.lastUsedAtMs }))

        val granted = mutableListOf<DecoderLease>()
        val deferred = mutableListOf<String>()
        val evicted = mutableListOf<String>()
        var memory = 0

        for (request in ordered) {
            val fitsInstances = granted.size < maxInstances
            val fitsMemory = memory + request.estimatedMb <= maxMemoryMb
            if (fitsInstances && fitsMemory) {
                granted += DecoderLease(request.clipId, request.priority)
                memory += request.estimatedMb
            } else {
                deferred += request.clipId
                // A deferred request that outranks something already granted
                // means the plan has to evict rather than just defer.
                if (granted.any { it.priority.ordinal >= request.priority.ordinal } && granted.isNotEmpty()) {
                    evicted += request.clipId
                }
            }
        }
        return BudgetPlan(granted, deferred, evicted, memory)
    }

    /** Free slots left for the export path after preview takes what it needs. */
    fun spareForExport(requests: List<DecoderRequest>): Int =
        (maxInstances - plan(requests).granted.size).coerceAtLeast(0)

    companion object {
        fun forTier(tier: String): DecoderBudget = when (tier.uppercase()) {
            "LOW" -> DecoderBudget(maxInstances = 2, maxMemoryMb = 320)
            "MID" -> DecoderBudget(maxInstances = 3, maxMemoryMb = 512)
            else -> DecoderBudget(maxInstances = 4, maxMemoryMb = 768)
        }
    }
}

/** Everything the proxy policy needs to know about one source. */
data class ProxyInput(
    val clipId: String,
    val width: Int,
    val height: Int,
    val codec: String,
    val gopMs: Long,
    val isVariableFrameRate: Boolean,
    val bitDepth: Int = 8
)

enum class ProxyReason { NOT_NEEDED, HIGH_RESOLUTION, HEAVY_CODEC, LONG_GOP, VARIABLE_FRAME_RATE, TEN_BIT }

data class ProxyDecision(
    val required: Boolean,
    val reason: ProxyReason,
    val targetHeight: Int,
    val estimatedMb: Int
) {
    /** Export must always read originals, never the proxy. */
    val exportReadsOriginal: Boolean get() = true
}

/**
 * Proxy policy (CapCut's "smooth preview"): decide once at import whether a
 * source needs a low-res, short-GOP stand-in for the timeline.
 */
object ProxyPolicy {

    fun decide(input: ProxyInput, tier: String = "HIGH"): ProxyDecision {
        val tierFactor = when (tier.uppercase()) {
            "LOW" -> 0.6
            "MID" -> 0.85
            else -> 1.0
        }
        val shortSide = minOf(input.width, input.height)
        val targetHeight = when {
            shortSide >= 2160 -> 720
            shortSide >= 1440 -> 720
            shortSide >= 1080 -> 540
            else -> 480
        }
        val target = (targetHeight * tierFactor).toInt().coerceAtLeast(360)
        val heavyCodec = input.codec.lowercase() in setOf("hevc", "h265", "av1", "prores")
        val reason = when {
            input.bitDepth >= 10 -> ProxyReason.TEN_BIT
            shortSide >= 2160 * tierFactor -> ProxyReason.HIGH_RESOLUTION
            input.isVariableFrameRate -> ProxyReason.VARIABLE_FRAME_RATE
            input.gopMs >= 2_000L -> ProxyReason.LONG_GOP
            heavyCodec && shortSide >= 1080 -> ProxyReason.HEAVY_CODEC
            else -> ProxyReason.NOT_NEEDED
        }
        val required = reason != ProxyReason.NOT_NEEDED
        val estimatedMb = if (!required) 0 else estimateMb(target, input.gopMs.coerceAtLeast(1_000L))
        return ProxyDecision(required, reason, if (required) target else 0, estimatedMb)
    }

    /** Rough H.264 baseline estimate for a 2 Mbit/s proxy. */
    fun estimateMb(targetHeight: Int, sourceDurationMs: Long): Int {
        val bitrateKbps = max(600, targetHeight * 3)
        val seconds = sourceDurationMs / 1_000.0
        return max(1, (bitrateKbps * seconds / 8.0 / 1_024.0).toInt())
    }

    /** Thumbnail stride so a 10 minute clip does not decode 18k frames. */
    fun thumbnailStrideMs(durationMs: Long, thumbnails: Int = 12): Long {
        if (durationMs <= 0L || thumbnails <= 0) return 1_000L
        return max(250L, durationMs / thumbnails)
    }
}
