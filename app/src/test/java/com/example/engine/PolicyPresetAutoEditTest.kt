package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyPresetAutoEditTest {

    private fun dark() = IntArray(400) { 0xFF000000.toInt() }
    private fun bright() = IntArray(400) { 0xFFFFFFFF.toInt() }

    @Test
    fun `platform preset picks the vertical frame`() {
        val target = PlatformPresets.target(PlatformPresets.Platform.TIKTOK, 1920, 1080)
        assertEquals(1080, target.width)
        assertEquals(1920, target.height)
        assertEquals(30, target.fps)
    }

    @Test
    fun `platform preset never upscales the source`() {
        val from720 = PlatformPresets.target(PlatformPresets.Platform.TIKTOK, 1280, 720)
        assertEquals(720, from720.width)
        assertEquals(1280, from720.height)

        val from360 = PlatformPresets.target(PlatformPresets.Platform.TIKTOK, 640, 360)
        assertEquals(360, from360.width)
        assertEquals(640, from360.height)
    }

    @Test
    fun `youtube 4k falls back to the source resolution`() {
        val target = PlatformPresets.target(PlatformPresets.Platform.YOUTUBE_4K, 1920, 1080)
        assertEquals(1920, target.width)
        assertEquals(1080, target.height)
    }

    @Test
    fun `whatsapp caps at 720p`() {
        val target = PlatformPresets.target(PlatformPresets.Platform.WHATSAPP, 1920, 1080)
        assertEquals(1280, target.width)
        assertEquals(720, target.height)
    }

    @Test
    fun `every platform target has even dimensions`() {
        val targets = PlatformPresets.all()
        assertFalse(targets.isEmpty())
        assertTrue(targets.all { it.width % 2 == 0 && it.height % 2 == 0 })
        assertTrue(targets.all { it.bitrateKbps in 1_500..45_000 })
    }

    @Test
    fun `shortcuts resolve the usual editing keys`() {
        assertEquals(Shortcuts.Command.PLAY_PAUSE, Shortcuts.resolve(Shortcuts.Key.SPACE))
        assertEquals(Shortcuts.Command.UNDO, Shortcuts.resolve(Shortcuts.Key.Z, ctrl = true))
        assertEquals(Shortcuts.Command.REDO, Shortcuts.resolve(Shortcuts.Key.Z, ctrl = true, shift = true))
        assertEquals(Shortcuts.Command.REDO, Shortcuts.resolve(Shortcuts.Key.Y, ctrl = true))
        assertEquals(Shortcuts.Command.SPLIT, Shortcuts.resolve(Shortcuts.Key.S))
        assertEquals(Shortcuts.Command.DELETE, Shortcuts.resolve(Shortcuts.Key.DELETE))
        assertEquals(Shortcuts.Command.ZOOM_IN, Shortcuts.resolve(Shortcuts.Key.PLUS))
        assertEquals(Shortcuts.Command.PREV_FRAME, Shortcuts.resolve(Shortcuts.Key.LEFT))
        assertNull(Shortcuts.resolve(Shortcuts.Key.ESCAPE))
        assertNull(Shortcuts.resolve(Shortcuts.Key.PLUS, ctrl = true))
    }

    @Test
    fun `identical frames produce no cut`() {
        val frames = List(8) { dark() }
        assertTrue(SceneMath.detectCuts(frames).isEmpty())
        assertEquals(0f, SceneMath.distance(SceneMath.histogram(dark()), SceneMath.histogram(dark())), 0.0001f)
    }

    @Test
    fun `a hard luminance change is a cut`() {
        val frames = List(6) { dark() } + List(6) { bright() }
        val cuts = SceneMath.detectCuts(frames, threshold = 0.35f, minGapFrames = 3)
        assertEquals(listOf(6), cuts)
    }

    @Test
    fun `beat grid estimates tempo from onsets`() {
        val energy = FloatArray(25)
        for (i in listOf(0, 5, 10, 15, 20)) energy[i] = 1f
        val onsets = BeatGrid.onsets(energy, hopMs = 100)
        assertEquals(5, onsets.size)
        assertEquals(1_000L, onsets[2].atMs)

        val grid = BeatGrid.grid(onsets, durationMs = 3_000)
        assertNotNull(grid)
        assertEquals(120.0, grid!!.bpm, 0.01)
        assertEquals(500L, BeatGrid.nearestBeat(grid, 260))
    }

    @Test
    fun `beat grid needs enough onsets`() {
        assertNull(BeatGrid.grid(emptyList(), 3_000))
        assertNull(BeatGrid.grid(listOf(BeatGrid.Onset(0, 1f), BeatGrid.Onset(500, 1f)), 3_000))
    }

    @Test
    fun `source beats map through timeline start, trim and speed`() {
        val sourceBeats = listOf(0L, 500L, 1_000L, 2_000L, 3_000L)
        assertEquals(
            listOf(2_000L, 2_500L),
            BeatGrid.timelineBeats(
                sourceBeats = sourceBeats,
                startTimelineMs = 2_000L,
                trimStartMs = 1_000L,
                speed = 2f,
                durationMs = 1_000L
            )
        )
    }

    @Test
    fun `half-speed audio spreads beats across the timeline`() {
        assertEquals(
            listOf(0L, 2_000L, 4_000L),
            BeatGrid.timelineBeats(
                sourceBeats = listOf(0L, 1_000L, 2_000L, 5_000L),
                startTimelineMs = 0L,
                trimStartMs = 0L,
                speed = 0.5f,
                durationMs = 4_500L
            )
        )
    }

    @Test
    fun `beat mapping drops empty and zero-length inputs`() {
        assertTrue(BeatGrid.timelineBeats(emptyList(), 0L, 0L, 1f, 1_000L).isEmpty())
        assertTrue(BeatGrid.timelineBeats(listOf(100L), 0L, 0L, 1f, 0L).isEmpty())
    }

    @Test
    fun `cut offsets keep beats inside and away from the edges`() {
        assertEquals(
            listOf(1_000L, 2_500L),
            BeatGrid.cutOffsets(
                durationMs = 4_000L,
                beatOffsetsMs = listOf(50L, 1_000L, 2_500L, 3_950L),
                minSegmentMs = 100L
            )
        )
    }

    @Test
    fun `dense beat lists abort instead of shredding a clip`() {
        val beats = (1..20).map { it * 200L }
        assertTrue(BeatGrid.cutOffsets(4_000L, beats, maxSegments = 5L).isEmpty())
    }

    @Test
    fun `subject smoothing follows the target gradually`() {
        val previous = SubjectTrack.Box(0.5f, 0.5f, 0.2f, 0.2f)
        val next = SubjectTrack.Box(0.9f, 0.5f, 0.2f, 0.2f)
        val smoothed = SubjectTrack.smooth(previous, next, smoothing = 0.5f, deadband = 0.01f)
        assertEquals(0.7f, smoothed.cx, 0.001f)
        assertEquals(0.5f, smoothed.cy, 0.001f)
    }

    @Test
    fun `deadband ignores sub-pixel jitter`() {
        val previous = SubjectTrack.Box(0.5f, 0.5f, 0.2f, 0.2f)
        val next = SubjectTrack.Box(0.505f, 0.5f, 0.2f, 0.2f)
        val smoothed = SubjectTrack.smooth(previous, next, smoothing = 0.5f, deadband = 0.01f)
        assertEquals(0.5f, smoothed.cx, 0.0001f)
    }

    @Test
    fun `subject box is clamped inside the frame`() {
        val clamped = SubjectTrack.clamp(SubjectTrack.Box(0.99f, 0.5f, 0.4f, 0.4f))
        assertEquals(0.8f, clamped.cx, 0.001f)
    }

    @Test
    fun `crop window keeps the requested aspect`() {
        val window = SubjectTrack.cropWindow(SubjectTrack.Box(0.5f, 0.5f, 0.2f, 0.2f), aspect = 9f / 16f)
        assertEquals(0.24f, window.w, 0.001f)
        assertEquals(0.4267f, window.h, 0.001f)
        assertEquals(0.5625f, window.w / window.h, 0.001f)
    }
}
