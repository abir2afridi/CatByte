package com.example.data.db

import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.StickerOverlay
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round-trips the whole editing model through JSON and compares field by
 * field. Also covers the recovery paths: corrupt payloads must be rejected
 * (returning null) and unknown enum names fall back to safe defaults.
 */
class ProjectJsonTest {

    private fun fullProject(): VideoProject {
        val clip = MediaClip(
            id = "clip_1",
            title = "Clip",
            uriString = "content://media/video/1",
            startTimelineMs = 100L,
            durationMs = 2400L,
            originalDurationMs = 5000L,
            trimStartMs = 200L,
            trimEndMs = 2600L,
            speed = 1.25f,
            volume = 0.8f,
            filter = VideoFilter.CYBERPUNK,
            transition = com.example.model.TransitionType.DISSOLVE,
            transitionDurationMs = 500L,
            animationIn = com.example.model.ClipAnimation.FADE_IN,
            brightness = 0.1f,
            contrast = 1.2f,
            saturation = 1.1f,
            vignette = 0.25f,
            rotation = 90f,
            isMirrored = true,
            waveform = listOf(0.1f, 0.5f, 0.9f),
            blendMode = BlendMode.SCREEN,
            keyframes = listOf(
                Keyframe(id = "kf_1", timestampMs = 0L, property = KeyframeProperty.SCALE, value = 1.0f),
                Keyframe(
                    id = "kf_2",
                    timestampMs = 800L,
                    property = KeyframeProperty.SCALE,
                    value = 2.0f,
                    easing = EasingCurve.EASE_OUT
                )
            ),
            mask = MaskData(type = MaskType.CIRCLE, width = 0.5f, isInverted = true),
            hslAdjustments = listOf(
                com.example.model.HslAdjustment(
                    channel = com.example.model.HslChannel.BLUE,
                    hue = 12f,
                    saturation = -30f,
                    luminance = 5f
                )
            ),
            scale = 1.4f,
            posX = 0.25f,
            posY = 0.75f,
            opacity = 0.9f
        )
        val overlayClip = clip.copy(id = "pip_1", title = "PiP")
        return VideoProject(
            id = "proj_original",
            title = "Round Trip",
            aspectRatio = AspectRatioType.RATIO_16_9,
            clips = listOf(clip),
            overlayTracks = listOf(
                VideoTrack(name = "PIP Track 1", isMainTrack = false, clips = listOf(overlayClip))
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "aud_1",
                    title = "Music",
                    artist = "Test",
                    uriString = "content://media/audio/7",
                    startTimelineMs = 400L,
                    durationMs = 9000L,
                    volume = 0.5f,
                    fadeInMs = 300L,
                    fadeOutMs = 250L,
                    isMuted = false,
                    waveform = listOf(0.2f, 0.4f)
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "text_1",
                    text = "Hello CatByte",
                    startTimelineMs = 0L,
                    durationMs = 1200L,
                    fontStyle = TextFontStyle.NEON_GLOW,
                    color = 0xFFFF00FF,
                    backgroundColor = 0x88000000,
                    animation = com.example.model.TextAnimationType.TYPEWRITER,
                    posX = 0.4f,
                    posY = 0.6f,
                    scale = 1.1f
                )
            ),
            stickers = listOf(
                StickerOverlay(
                    id = "sticker_1",
                    symbol = "⭐",
                    startTimelineMs = 200L,
                    durationMs = 800L,
                    posX = 0.2f,
                    posY = 0.3f,
                    scale = 0.8f
                )
            ),
            effects = listOf(
                TimelineEffect(
                    id = "fx_1",
                    name = "Test FX",
                    effectType = EffectType.RGB_SPLIT,
                    startTimelineMs = 100L,
                    durationMs = 700L
                )
            ),
            resolution = "4K",
            fps = 60,
            isFavorite = true,
            paperStyle = "Blueprint"
        )
    }

    @Test
    fun `round trip preserves every field`() {
        val project = fullProject()
        val json = ProjectJson.toJson(project)
        val restored = ProjectJson.fromJson(json)
        assertNotNull(restored)
        assertEquals(project, restored)
    }

    @Test
    fun `corrupt json returns null instead of crashing`() {
        assertNull(ProjectJson.fromJson("this is not json"))
        assertNull(ProjectJson.fromJson("{\"clips\": 42"))
    }

    @Test
    fun `unknown enum names fall back to safe defaults`() {
        val json = JSONObject()
            .put("id", "proj_broken")
            .put("title", "Broken browser")
            .put("aspectRatio", "NOT_A_RATIO")
            .put("clips", org.json.JSONArray().put(JSONObject().put("id", "clip").put("filter", "BANANA")))
            .toString()

        val project = ProjectJson.fromJson(json)

        assertNotNull(project)
        assertEquals(AspectRatioType.RATIO_9_16, project!!.aspectRatio)
        assertEquals(VideoFilter.NONE, project.clips.first().filter)
    }

    @Test
    fun `missing fields use documented fallbacks`() {
        val project = ProjectJson.fromJson("{}")

        assertNotNull(project)
        assertEquals("Untitled", project!!.title)
        assertEquals(0, project.clips.size)
        assertEquals(30, project.fps)
        assertEquals("1080P", project.resolution)
        assertFalse(project.isFavorite)
    }

    @Test
    fun `null uri strings survive the round trip`() {
        val project = fullProject().copy(
            clips = fullProject().clips.map { it.copy(uriString = null) },
            audioTracks = fullProject().audioTracks.map { it.copy(uriString = null) }
        )
        val restored = ProjectJson.fromJson(ProjectJson.toJson(project))
        assertNotNull(restored)
        assertNull(restored!!.clips.first().uriString)
        assertNull(restored.audioTracks.first().uriString)
        assertEquals(project, restored)
    }

    @Test
    fun `thumbnail res id null and non null both survive`() {
        val base = fullProject().copy(thumbnailResId = null)
        val restoredNull = ProjectJson.fromJson(ProjectJson.toJson(base))
        assertNotNull(restoredNull)
        assertNull(restoredNull!!.thumbnailResId)

        val withThumb = base.copy(thumbnailResId = 42)
        val restored = ProjectJson.fromJson(ProjectJson.toJson(withThumb))
        assertEquals(42, restored!!.thumbnailResId)
    }

    @Test
    fun `bengali text survives the round trip`() {
        val base = fullProject().copy(
            textOverlays = listOf(
                fullProject().textOverlays.first().copy(text = "বাংলা টেক্সট — যুক্তাক্ষর পরীক্ষা")
            )
        )
        val restored = ProjectJson.fromJson(ProjectJson.toJson(base))
        assertEquals(
            "বাংলা টেক্সট — যুক্তাক্ষর পরীক্ষা",
            restored!!.textOverlays.first().text
        )
    }

    @Test
    fun `empty project round trips`() {
        val empty = VideoProject(id = "proj_empty", title = "It is empty")
        val restored = ProjectJson.fromJson(ProjectJson.toJson(empty))
        assertEquals(empty, restored)
    }

    @Test
    fun `toJson output parses as valid null-safe json`() {
        val json = JSONObject(ProjectJson.toJson(fullProject()))
        assertTrue(json.has("clips"))
        assertTrue(json.has("audioTracks"))
        assertTrue(json.has("textOverlays"))
        assertTrue(json.has("stickers"))
        assertTrue(json.has("effects"))
        assertTrue(json.has("overlayTracks"))
    }
}
