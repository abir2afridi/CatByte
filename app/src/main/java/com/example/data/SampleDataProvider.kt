package com.example.data

import com.example.model.AudioClip
import com.example.model.VideoProject

object SampleDataProvider {

    val trendingAudioList = listOf(
        AudioClip(
            id = "aud_phonk",
            title = "Midnight Phonk (Speed Up)",
            artist = "K-Phonk Beats",
            durationMs = 12000L,
            waveform = listOf(0.4f, 0.8f, 0.95f, 0.7f, 0.9f, 0.85f, 0.6f, 0.9f, 0.75f, 0.8f, 0.95f, 0.5f)
        ),
        AudioClip(
            id = "aud_golden",
            title = "Golden Hour Glow",
            artist = "Aesthetic Beats",
            durationMs = 15000L,
            waveform = listOf(0.3f, 0.5f, 0.7f, 0.8f, 0.6f, 0.75f, 0.85f, 0.9f, 0.7f, 0.6f, 0.5f, 0.3f)
        ),
        AudioClip(
            id = "aud_cyber",
            title = "Neon Runner 2077",
            artist = "SynthWave Lab",
            durationMs = 10000L,
            waveform = listOf(0.5f, 0.9f, 0.85f, 0.7f, 0.95f, 0.6f, 0.8f, 0.9f, 0.85f, 0.7f, 0.9f, 0.4f)
        ),
        AudioClip(
            id = "aud_lofi",
            title = "Coffee & Memories",
            artist = "Chilled Cow",
            durationMs = 14000L,
            waveform = listOf(0.2f, 0.4f, 0.6f, 0.5f, 0.7f, 0.65f, 0.5f, 0.6f, 0.7f, 0.55f, 0.4f, 0.3f)
        ),
        AudioClip(
            id = "aud_bassdrop",
            title = "Trap Glitch & Drop",
            artist = "Bassline Crew",
            durationMs = 9000L,
            waveform = listOf(0.3f, 0.4f, 0.6f, 0.8f, 0.95f, 1.0f, 0.9f, 0.95f, 0.8f, 0.7f, 0.5f, 0.2f)
        )
    )

    val soundEffectsList = listOf(
        AudioClip(id = "sfx_whoosh", title = "Fast Whoosh Transition", artist = "SFX Pro", durationMs = 1200L, waveform = listOf(0.2f, 0.6f, 0.9f, 0.7f, 0.3f)),
        AudioClip(id = "sfx_glitch", title = "Cyber Glitch Static", artist = "SFX Pro", durationMs = 1500L, waveform = listOf(0.8f, 0.3f, 0.9f, 0.4f, 0.95f, 0.2f)),
        AudioClip(id = "sfx_camera", title = "SLR Camera Shutter", artist = "SFX Pro", durationMs = 800L, waveform = listOf(0.9f, 0.4f, 0.7f, 0.2f)),
        AudioClip(id = "sfx_pop", title = "Bubble Pop Accent", artist = "SFX Pro", durationMs = 600L, waveform = listOf(0.95f, 0.3f, 0.1f)),
        AudioClip(id = "sfx_ding", title = "Bell Chime Success", artist = "SFX Pro", durationMs = 1800L, waveform = listOf(0.95f, 0.7f, 0.5f, 0.3f, 0.1f))
    )

    val sketchbookFoleyList = listOf(
        AudioClip(id = "sfx_pencil_write", title = "Pencil Scribble", artist = "Sketch ASMR", durationMs = 1400L, waveform = listOf(0.4f, 0.8f, 0.95f, 0.6f, 0.85f, 0.7f)),
        AudioClip(id = "sfx_page_turn", title = "Page Flip", artist = "Sketch ASMR", durationMs = 1600L, waveform = listOf(0.3f, 0.7f, 0.9f, 0.8f, 0.4f, 0.2f)),
        AudioClip(id = "sfx_eraser", title = "Eraser Rub", artist = "Sketch ASMR", durationMs = 1100L, waveform = listOf(0.5f, 0.8f, 0.7f, 0.6f, 0.4f, 0.2f)),
        AudioClip(id = "sfx_ruler", title = "Ruler Snap", artist = "Sketch ASMR", durationMs = 650L, waveform = listOf(0.9f, 0.95f, 0.5f, 0.2f)),
        AudioClip(id = "sfx_scissors", title = "Scissors Cut", artist = "Sketch ASMR", durationMs = 950L, waveform = listOf(0.7f, 0.9f, 0.6f, 0.3f)),
        AudioClip(id = "sfx_tape_tear", title = "Tape Tear", artist = "Sketch ASMR", durationMs = 1250L, waveform = listOf(0.4f, 0.85f, 0.9f, 0.5f, 0.2f))
    )

    val doodleStickers = listOf(
        "✏️", "📐", "✂️", "📌", "📎", "🏷️",
        "⭐️", "✨", "⚡️", "💬", "💡", "❤️",
        "☕️", "🎯", "🔥", "✍️", "🚀", "📖",
        "🎬", "🎨", "👀", "💯", "🎈", "🕶️"
    )

    fun getInitialProjects(): List<VideoProject> {
        return emptyList()
    }
}