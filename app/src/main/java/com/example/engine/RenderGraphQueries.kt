package com.example.engine

import kotlin.math.roundToLong

/**
 * Canvas background for a project's paper style. Shared by the preview canvas
 * and the render graph so the swatch in the Background panel is the same
 * colour the rest of the pipeline reads.
 */
fun backgroundArgbFor(paperStyle: String): Long = when (paperStyle.lowercase()) {
    "blueprint", "architect blueprint" -> 0xFF0B1E33
    "sepia", "parchment sketchbook" -> 0xFF2A2118
    "charcoal", "dark slate chalkboard" -> 0xFF141414
    "drafting grid" -> 0xFF1A1A1E
    else -> 0xFF000000
}

/** Convenience: sample the graph at a timeline position. */
fun RenderGraph.layersAt(positionUs: Long): List<LayerSpec> =
    layers.filter { positionUs >= it.timelineInUs && positionUs < it.timelineOutUs }

fun RenderGraph.textsAt(positionUs: Long): List<TextSpec> =
    texts.filter { positionUs >= it.timelineInUs && positionUs < it.timelineOutUs }

/** Frame index of a presentation time, used by the golden-frame harness. */
fun RenderGraph.frameAt(positionUs: Long): Long = TimeUs(positionUs).toFrame(fps)

/** Rounds a duration up to whole frames (export needs an exact frame count). */
fun RenderGraph.totalFrames(): Long = TimeUs(durationUs).toFrame(fps).coerceAtLeast(1L)

/** Milliseconds of a frame count, for UI labels. */
fun RenderGraph.frameCountToMs(frames: Long): Long =
    (TimeUs.fromFrames(frames, fps).value / 1_000.0).roundToLong()
