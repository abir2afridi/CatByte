package com.example.ui.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.example.model.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The mixer sheet must expose real per-track actions, not decorative controls. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioMixerSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val audio = AudioClip(
        id = "music",
        title = "Music",
        artist = "Test",
        uriString = "content://audio/music",
        durationMs = 4_000L,
        originalDurationMs = 4_000L
    )

    @Test
    fun `mute reset and delete report the selected track`() {
        val muted = mutableListOf<Pair<String, Boolean>>()
        val reset = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { id, value -> muted += id to value },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = { reset += it },
                onDelete = { deleted += it },
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_mute").performClick()
        compose.onNodeWithTag("audio_mixer_reset").performScrollTo().performClick()
        compose.onNodeWithTag("audio_mixer_delete").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("music" to true), muted)
        assertEquals(listOf("music"), reset)
        assertEquals(listOf("music"), deleted)
        assertTrue(compose.onAllNodesWithTag("audio_mixer_sheet").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `solo reports the selected track and the next state`() {
        val soloed = mutableListOf<Pair<String, Boolean>>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio.copy(isSoloed = true)),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onSoloChanged = { id, value -> soloed += id to value },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_solo").performClick()
        compose.waitForIdle()

        // The track is already soloed, so the button asks for "not soloed".
        assertEquals(listOf("music" to false), soloed)
    }

    @Test
    fun `the pan slider reports the new balance of the selected track`() {
        val pans = mutableListOf<Pair<String, Float>>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio.copy(pan = -0.5f)),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onPanChanged = { id, value -> pans += id to value },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        val pan = compose.onNodeWithTag("audio_mixer_pan").performScrollTo()
        pan.performTouchInput {
            down(center)
            moveTo(Offset(center.x * 1.5f, center.y))
        }
        compose.waitForIdle()
        // The gesture only moves the local draft.
        assertTrue(pans.isEmpty())

        pan.performTouchInput { up() }
        compose.waitForIdle()

        assertEquals(1, pans.size)
        assertEquals("music", pans.single().first)
        assertTrue("the balance should move towards the right", pans.single().second > -0.5f)
    }

    @Test
    fun `auto volume stays disabled while the waveform is not decoded`() {
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onNormaliseVolume = { error("normalise must not fire without a waveform") },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_auto_volume").assertIsNotEnabled()
    }

    @Test
    fun `auto volume reports the selected track when the waveform is decoded`() {
        val normalised = mutableListOf<String>()
        val withData = audio.copy(waveform = List(48) { 0.1f })
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(withData),
                selectedAudioId = withData.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onNormaliseVolume = { normalised += it },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_auto_volume").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("music"), normalised)
    }

    @Test
    fun `slider gestures keep local drafts and commit once when the gesture ends`() {
        var currentAudio by mutableStateOf(audio)
        val volumes = mutableListOf<Pair<String, Float>>()
        val fades = mutableListOf<Triple<String, Long, Long>>()
        val speeds = mutableListOf<Pair<String, Float>>()
        val trims = mutableListOf<Triple<String, Long, Long>>()
        val starts = mutableListOf<Pair<String, Long>>()

        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(currentAudio),
                selectedAudioId = currentAudio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { id, value -> volumes += id to value },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { id, fadeIn, fadeOut ->
                    fades += Triple(id, fadeIn, fadeOut)
                },
                onSpeedChanged = { id, value -> speeds += id to value },
                onTrimChanged = { id, start, end -> trims += Triple(id, start, end) },
                onTimelineStartChanged = { id, value -> starts += id to value },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        val volume = compose.onNodeWithTag("audio_mixer_volume").performScrollTo()
        volume.performTouchInput {
            down(center)
            moveTo(Offset(center.x * 1.2f, center.y))
            moveTo(Offset(center.x * 1.4f, center.y))
            moveTo(Offset(center.x * 1.6f, center.y))
        }
        compose.waitForIdle()

        // Pointer movement only updates the local draft. No model/history work
        // is allowed until the finger is released.
        assertTrue(volumes.isEmpty())
        val draft = volume.fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
            .current
        assertTrue("the slider should show its local draft", draft > 1f)

        // An external update does not overwrite an active gesture. Once the
        // gesture ends, the authoritative model value is reflected again.
        currentAudio = currentAudio.copy(volume = 0.5f)
        compose.waitForIdle()
        assertTrue(volumes.isEmpty())

        volume.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(1, volumes.size)
        assertEquals("music", volumes.single().first)
        assertEquals(draft, volumes.single().second, 0.0001f)

        val reflected = volume.fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
            .current
        assertEquals(0.5f, reflected, 0.0001f)

        dragSlider("audio_mixer_fade_in", towardEnd = true)
        dragSlider("audio_mixer_fade_out", towardEnd = true)
        dragSlider("audio_mixer_speed", towardEnd = true)
        dragSlider("audio_mixer_trim_in", towardEnd = true)
        dragSlider("audio_mixer_trim_out", towardEnd = false)
        dragSlider("audio_mixer_start", towardEnd = true)

        assertEquals(2, fades.count { it.first == "music" })
        assertEquals(2, fades.size)
        assertEquals(1, speeds.size)
        assertEquals(2, trims.size)
        assertEquals(1, starts.size)
    }

    @Test
    fun `speed slider uses the canonical editor range`() {
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        val range = compose.onNodeWithTag("audio_mixer_speed")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
            .range
        assertEquals(0.1f, range.start, 0f)
        assertEquals(16f, range.endInclusive, 0f)
    }

    @Test
    fun `timeline placement excludes the moving clip duration from the range`() {
        val starts = mutableListOf<Pair<String, Long>>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { id, value -> starts += id to value },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        val start = compose.onNodeWithTag("audio_mixer_start").performScrollTo()
        val range = start.fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
            .range
        assertEquals(0f, range.start, 0f)
        assertEquals(6_000f, range.endInclusive, 0f)

        start.performTouchInput {
            down(center)
            moveTo(Offset(center.x * 2f - 1f, center.y))
            up()
        }
        compose.waitForIdle()

        assertEquals(1, starts.size)
        assertTrue(starts.single().second in 0L..6_000L)
    }

    @Test
    fun `zero-width timing edits preserve their exact bounds`() {
        val tinyClip = audio.copy(durationMs = 100L, originalDurationMs = 100L)
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(tinyClip),
                selectedAudioId = tinyClip.id,
                maxTimelineStartMs = 100L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        mapOf(
            "audio_mixer_fade_in" to (0f to 100f),
            "audio_mixer_fade_out" to (0f to 100f),
            "audio_mixer_trim_in" to (0f to 0f),
            "audio_mixer_trim_out" to (100f to 100f),
            "audio_mixer_start" to (0f to 0f)
        ).forEach { (tag, expected) ->
            val range = compose.onNodeWithTag(tag)
                .fetchSemanticsNode()
                .config[SemanticsProperties.ProgressBarRangeInfo]
                .range
            assertEquals("$tag start", expected.first, range.start, 0f)
            assertEquals("$tag end", expected.second, range.endInclusive, 0f)
        }
    }

    @Test
    fun `loop buttons report the selected track and the repeat count`() {
        val loops = mutableListOf<Pair<String, Int>>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onLoopCountChanged = { id, repeats -> loops += id to repeats },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_loop_4").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("music" to 4), loops)
    }

    @Test
    fun `effects and split actions report the selected track`() {
        val effects = mutableListOf<String>()
        val splits = mutableListOf<String>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onEffects = { effects += it },
                onSplit = { splits += it },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_effects").performScrollTo().performClick()
        compose.onNodeWithTag("audio_mixer_split").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("music"), effects)
        assertEquals(listOf("music"), splits)
    }

    @Test
    fun `the per-track export button reports the selected track`() {
        val exported = mutableListOf<String>()
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onExportTrack = { exported += it },
                onReset = {},
                onDelete = {},
                onClose = {}
            )
        }

        compose.onNodeWithTag("audio_mixer_export_track").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("music"), exported)
    }

    @Test
    fun `the output meter is shown with the live levels`() {
        compose.setContent {
            AudioMixerSheet(
                audioTracks = listOf(audio),
                selectedAudioId = audio.id,
                maxTimelineStartMs = 10_000L,
                onSelectAudio = {},
                onVolumeChanged = { _, _ -> },
                onMuteChanged = { _, _ -> },
                onFadesChanged = { _, _, _ -> },
                onSpeedChanged = { _, _ -> },
                onTrimChanged = { _, _, _ -> },
                onTimelineStartChanged = { _, _ -> },
                onReset = {},
                onDelete = {},
                onClose = {},
                leftLevel = 0.8f,
                rightLevel = 0.4f
            )
        }

        assertTrue(
            compose.onAllNodesWithTag("audio_mixer_output_meter")
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
    }

    private fun dragSlider(tag: String, towardEnd: Boolean) {
        val slider = compose.onNodeWithTag(tag).performScrollTo()
        slider.performTouchInput {
            down(center)
            if (towardEnd) {
                moveTo(Offset(center.x * 1.2f, center.y))
                moveTo(Offset(center.x * 1.4f, center.y))
                moveTo(Offset(center.x * 1.6f, center.y))
            } else {
                moveTo(Offset(center.x * 0.8f, center.y))
                moveTo(Offset(center.x * 0.6f, center.y))
                moveTo(Offset(center.x * 0.4f, center.y))
            }
            up()
        }
        compose.waitForIdle()
    }
}
