package com.example.data.db

import com.example.engine.AudioTiming
import com.example.engine.ClipAnimationMath
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.AudioCompressor
import com.example.model.AudioEqualizer
import com.example.model.BezierPoint
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.model.StickerOverlay
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.AudioEffectSettings
import com.example.model.TimelineEffect
import com.example.model.TimelineMarker
import com.example.model.VideoFilter
import com.example.model.VideoProject
import com.example.model.VideoTrack
import com.example.model.VoiceEffectType
import org.json.JSONArray
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
            speedCurve = SpeedCurve(
                preset = SpeedCurvePreset.CUSTOM,
                points = listOf(BezierPoint(0f, 0.8f), BezierPoint(0.4f, 2.2f), BezierPoint(1f, 1f))
            ),
            volume = 0.8f,
            filter = VideoFilter.CYBERPUNK,
            transition = com.example.model.TransitionType.DISSOLVE,
            transitionDurationMs = 500L,
            animationIn = com.example.model.ClipAnimation.FADE_IN,
            animationDurationMs = 1_200L,
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
            opacity = 0.9f,
            audioEffects = AudioEffectSettings(
                type = VoiceEffectType.ROBOT,
                intensity = 0.7f,
                enhanceVoice = true,
                reduceNoise = true
            ),
            audioFadeInMs = 250L,
            audioFadeOutMs = 400L
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
                    durationMs = 22_695L,
                    originalDurationMs = 10000L,
                    trimStartMs = 150L,
                    trimEndMs = 8850L,
                    speed = 1.15f,
                    volume = 0.5f,
                    pan = -0.35f,
                    audioEffects = AudioEffectSettings(
                        type = VoiceEffectType.REVERB,
                        intensity = 0.5f,
                        enhanceVoice = true
                    ),
                    equalizer = AudioEqualizer(
                        lowGainDb = 5.5f,
                        midGainDb = -2.5f,
                        highGainDb = 3f
                    ),
                    compressor = AudioCompressor(
                        enabled = true,
                        thresholdDb = -22f,
                        ratio = 6f,
                        makeupGainDb = 2.5f,
                        attackMs = 4f,
                        releaseMs = 220f
                    ),
                    loopCount = 3,
                    fadeInMs = 300L,
                    fadeOutMs = 250L,
                    isMuted = false,
                    isSoloed = true,
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
            paperStyle = "Blueprint",
            manualBeatsMs = listOf(250L, 1_250L, 2_500L)
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
    fun `background mode and image round trip`() {
        val project = fullProject().copy(
            backgroundImageUri = "/data/bg.jpg",
            backgroundMode = "blur"
        )
        val restored = ProjectJson.fromJson(ProjectJson.toJson(project))
        assertNotNull(restored)
        assertEquals("blur", restored!!.backgroundMode)
        assertEquals("/data/bg.jpg", restored.backgroundImageUri)
        // Unknown mode strings survive as written (no crash on future modes).
        val mutated = JSONObject(ProjectJson.toJson(project))
        mutated.put("backgroundMode", "neon")
        val legacy = ProjectJson.fromJson(mutated.toString())
        assertEquals("neon", legacy!!.backgroundMode)
    }


    @Test
    fun `effect intensity scale and clip motion blur round trip`() {
        val project = fullProject().copy(
            effects = listOf(
                com.example.model.TimelineEffect(
                    id = "fx_1",
                    name = "Test",
                    effectType = EffectType.RGB_SPLIT,
                    startTimelineMs = 100L,
                    durationMs = 2_000L,
                    intensityScale = 1.4f
                )
            ),
            clips = listOf(
                fullProject().clips.first().copy(motionBlur = true)
            )
        )
        val restored = ProjectJson.fromJson(ProjectJson.toJson(project))
        assertNotNull(restored)
        assertEquals(1.4f, restored!!.effects.single().intensityScale, 0.001f)
        assertTrue(restored.clips.single().motionBlur)
        // Defaults survive for older projects without the new fields.
        val legacyJson = JSONObject(ProjectJson.toJson(project))
        legacyJson.optJSONArray("effects")!!.getJSONObject(0).remove("intensityScale")
        legacyJson.optJSONArray("clips")!!.getJSONObject(0).remove("motionBlur")
        val legacy = ProjectJson.fromJson(legacyJson.toString())
        assertEquals(1f, legacy!!.effects.single().intensityScale, 0.001f)
        assertTrue(!legacy.clips.single().motionBlur)
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
    fun `malformed audio effects fall back to safe defaults`() {
        val malformed = JSONObject()
            .put("type", "BANANA")
            .put("intensity", "not-a-number")
            .put("enhanceVoice", true)
        val applied = audioEffectsFromJson(malformed)
        assertEquals(VoiceEffectType.NONE, applied.type)
        assertEquals(0.6f, applied.intensity, 0.0001f)
        assertTrue(applied.enhanceVoice)

        // Missing object entirely: the documented defaults.
        val missing = audioEffectsFromJson(null)
        assertTrue(missing.isNoOp)
        assertFalse(missing.enhanceVoice)
        assertFalse(missing.reduceNoise)
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

    @Test
    fun `track mute lock solo and clip mute survive the round trip`() {
        val clip = MediaClip(
            id = "clip_1",
            title = "Clip",
            durationMs = 2_000L,
            originalDurationMs = 2_000L,
            isMuted = true
        )
        val overlay = MediaClip(
            id = "ov_1",
            title = "Overlay",
            startTimelineMs = 500L,
            durationMs = 1_000L,
            originalDurationMs = 1_000L
        )
        val project = VideoProject(
            id = "proj_tracks",
            title = "Tracks",
            clips = listOf(clip),
            overlayTracks = listOf(
                VideoTrack(
                    id = "track_1",
                    name = "Overlay",
                    isMainTrack = false,
                    clips = listOf(overlay),
                    isMuted = true,
                    isLocked = true,
                    isSolo = true
                )
            ),
            isMainTrackMuted = true,
            isMainTrackLocked = true,
            isMainTrackSolo = false,
            autoDuckEnabled = false,
            autoDuckStrength = 0.6f
        )
        val restored = ProjectJson.fromJson(ProjectJson.toJson(project))
        assertEquals(project, restored)
    }

    @Test
    fun `malformed audio timing is normalized during json decode`() {
        val json = JSONObject()
            .put("id", "malformed_audio")
            .put("title", "Audio")
            .put(
                "audioTracks",
                JSONArray().put(
                    JSONObject()
                        .put("id", "broken")
                        .put("title", "Broken")
                        .put("artist", "Test")
                        .put("uriString", "content://audio/broken")
                        .put("startTimelineMs", -100L)
                        .put("durationMs", 1_000L)
                        .put("originalDurationMs", 1_000L)
                        .put("trimStartMs", 5_000L)
                        .put("trimEndMs", 6_000L)
                        .put("speed", 2.0)
                        .put("volume", 1.0)
                        .put("fadeInMs", Long.MAX_VALUE)
                        .put("fadeOutMs", Long.MAX_VALUE)
                )
            )

        val audio = ProjectJson.fromJson(json.toString())!!.audioTracks.single()

        assertTrue(audio.startTimelineMs >= 0L)
        assertTrue(audio.trimStartMs <= audio.trimEndMs)
        assertTrue(audio.trimEndMs <= audio.originalDurationMs)
        assertEquals(AudioTiming.presentationDurationMs(audio), audio.durationMs)
        assertTrue(audio.fadeInMs + audio.fadeOutMs <= audio.durationMs)
    }

    @Test
    fun `malformed manual beats are dropped instead of becoming beat zero`() {
        val json = JSONObject()
            .put("id", "manual_beats")
            .put("title", "Beats")
            .put(
                "manualBeatsMs",
                JSONArray()
                    .put(1_200L)
                    .put(-400L)
                    .put("not a beat")
                    .put(1_200L)
                    .put(300L)
            )

        assertEquals(
            listOf(300L, 1_200L),
            ProjectJson.fromJson(json.toString())!!.manualBeatsMs
        )
    }

    @Test
    fun `malformed equalizer gains are clamped during json decode`() {
        val json = JSONObject()
            .put("id", "malformed_eq")
            .put("title", "EQ")
            .put(
                "audioTracks",
                JSONArray().put(
                    JSONObject()
                        .put("id", "broken_eq")
                        .put("title", "Broken EQ")
                        .put("artist", "Test")
                        .put("uriString", "content://audio/broken")
                        .put("durationMs", 1_000L)
                        .put("originalDurationMs", 1_000L)
                        .put(
                            "equalizer",
                            JSONObject()
                                .put("lowGainDb", 400.0)
                                .put("midGainDb", "not a number")
                                .put("highGainDb", -80.0)
                        )
                )
            )

        val restored = ProjectJson.fromJson(json.toString())!!
            .audioTracks
            .single()
            .equalizer

        assertEquals(AudioEqualizer.MAX_GAIN_DB, restored.lowGainDb, 0f)
        assertEquals(0f, restored.midGainDb, 0f)
        assertEquals(AudioEqualizer.MIN_GAIN_DB, restored.highGainDb, 0f)
    }

    @Test
    fun `legacy audio without an equalizer decodes as flat`() {
        val legacy = JSONObject()
            .put("id", "legacy_eq")
            .put("title", "Legacy EQ")
            .put("durationMs", 1_000L)
            .put("originalDurationMs", 1_000L)

        assertEquals(AudioEqualizer(), audioFromJson(legacy).equalizer)
    }

    @Test
    fun `malformed compressor settings are clamped during json decode`() {
        val json = JSONObject()
            .put("id", "malformed_comp")
            .put("title", "Compressor")
            .put(
                "audioTracks",
                JSONArray().put(
                    JSONObject()
                        .put("id", "broken_comp")
                        .put("title", "Broken compressor")
                        .put("artist", "Test")
                        .put("uriString", "content://audio/broken")
                        .put("durationMs", 1_000L)
                        .put("originalDurationMs", 1_000L)
                        .put(
                            "compressor",
                            JSONObject()
                                .put("enabled", true)
                                .put("thresholdDb", 90.0)
                                .put("ratio", "not a number")
                                .put("makeupGainDb", -30.0)
                                .put("attackMs", 0.0)
                                .put("releaseMs", 99_999.0)
                        )
                )
            )

        val restored = ProjectJson.fromJson(json.toString())!!
            .audioTracks
            .single()
            .compressor

        assertTrue(restored.enabled)
        assertEquals(AudioCompressor.MAX_THRESHOLD_DB, restored.thresholdDb, 0f)
        assertEquals(AudioCompressor.DEFAULT_RATIO, restored.ratio, 0f)
        assertEquals(AudioCompressor.MIN_MAKEUP_GAIN_DB, restored.makeupGainDb, 0f)
        assertEquals(AudioCompressor.MIN_ATTACK_MS, restored.attackMs, 0f)
        assertEquals(AudioCompressor.MAX_RELEASE_MS, restored.releaseMs, 0f)
    }

    @Test
    fun `legacy audio without a compressor decodes as disabled`() {
        val legacy = JSONObject()
            .put("id", "legacy_comp")
            .put("title", "Legacy compressor")
            .put("durationMs", 1_000L)
            .put("originalDurationMs", 1_000L)

        assertEquals(AudioCompressor(), audioFromJson(legacy).compressor)
    }

    @Test
    fun `the animation duration survives the round trip`() {
        val clip = MediaClip(
            id = "anim",
            title = "Animated",
            durationMs = 2_000L,
            originalDurationMs = 2_000L,
            animationIn = ClipAnimation.ZOOM_IN,
            animationDurationMs = 1_500L
        )

        val restored = clipFromJson(clipToJson(clip))

        assertEquals(1_500L, restored.animationDurationMs)
    }

    @Test
    fun `a malformed animation duration is clamped during json decode`() {
        val clip = JSONObject()
            .put("id", "anim")
            .put("title", "Animated")
            .put("durationMs", 2_000L)
            .put("originalDurationMs", 2_000L)
            .put("animationDurationMs", 900_000L)

        assertEquals(ClipAnimationMath.MAX_DURATION_MS, clipFromJson(clip).animationDurationMs)
    }

    @Test
    fun `the transition length survives the round trip`() {
        val clip = MediaClip(
            id = "trans",
            title = "Transition",
            durationMs = 2_000L,
            originalDurationMs = 2_000L,
            transition = com.example.model.TransitionType.PAGE_FLIP,
            transitionDurationMs = 1_200L
        )

        val restored = clipFromJson(clipToJson(clip))

        assertEquals(1_200L, restored.transitionDurationMs)
        assertEquals(com.example.model.TransitionType.PAGE_FLIP, restored.transition)
    }

    @Test
    fun `a malformed transition length is clamped during json decode`() {
        val clip = JSONObject()
            .put("id", "trans")
            .put("title", "Transition")
            .put("durationMs", 2_000L)
            .put("originalDurationMs", 2_000L)
            .put("transitionDurationMs", 900_000L)

        assertEquals(
            com.example.model.TransitionType.MAX_DURATION_MS,
            clipFromJson(clip).transitionDurationMs
        )
    }

    @Test
    fun `a legacy clip without a transition length decodes with the default`() {
        val legacy = JSONObject()
            .put("id", "legacy_trans")
            .put("title", "Legacy")
            .put("durationMs", 2_000L)
            .put("originalDurationMs", 2_000L)

        assertEquals(400L, clipFromJson(legacy).transitionDurationMs)
    }

    @Test
    fun `a legacy clip without an animation duration decodes with the default`() {
        val legacy = JSONObject()
            .put("id", "legacy_anim")
            .put("title", "Legacy")
            .put("durationMs", 2_000L)
            .put("originalDurationMs", 2_000L)

        assertEquals(ClipAnimationMath.DURATION_MS, clipFromJson(legacy).animationDurationMs)
    }

    @Test
    fun `malformed marker ids are normalized so the marker sheet stays usable`() {
        val json = JSONObject()
            .put("id", "malformed_markers")
            .put("title", "Markers")
            .put(
                "markers",
                JSONArray()
                    .put(JSONObject().put("id", "same").put("atMs", 100).put("label", "One"))
                    .put(JSONObject().put("id", "same").put("atMs", 200).put("label", "Two"))
                    .put(JSONObject().put("id", " ").put("atMs", -50).put("label", "x".repeat(40)))
            )

        val markers = ProjectJson.fromJson(json.toString())!!.markers

        assertEquals(3, markers.size)
        assertEquals(3, markers.map { it.id }.distinct().size)
        assertTrue(markers.all { it.id.isNotBlank() })
        assertTrue(markers.all { it.atMs >= 0L })
        assertTrue(markers.all { it.label.length <= 24 })
    }

    @Test
    fun `markers survive the round trip`() {
        val marker = TimelineMarker(
            id = "marker_1",
            atMs = 1_500L,
            label = "Drop",
            colorArgb = 0xFF00FF00
        )
        val project = VideoProject(
            id = "proj_markers",
            title = "Markers",
            markers = listOf(marker)
        )
        val restored = ProjectJson.fromJson(ProjectJson.toJson(project))
        assertEquals(project, restored)
    }
}
