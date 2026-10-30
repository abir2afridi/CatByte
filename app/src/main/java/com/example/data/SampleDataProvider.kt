package com.example.data

import com.example.R
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.ClipAnimation
import com.example.model.EffectType
import com.example.model.MediaClip
import com.example.model.StickerOverlay
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TimelineEffect
import com.example.model.TransitionType
import com.example.model.VideoFilter
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
        AudioClip(id = "sfx_pencil_write", title = "✏️ 2B Pencil Scribble", artist = "Sketch ASMR", durationMs = 1400L, waveform = listOf(0.4f, 0.8f, 0.95f, 0.6f, 0.85f, 0.7f)),
        AudioClip(id = "sfx_page_turn", title = "📄 Parchment Page Flip", artist = "Sketch ASMR", durationMs = 1600L, waveform = listOf(0.3f, 0.7f, 0.9f, 0.8f, 0.4f, 0.2f)),
        AudioClip(id = "sfx_eraser", title = "🧼 Eraser Rub & Crumb", artist = "Sketch ASMR", durationMs = 1100L, waveform = listOf(0.5f, 0.8f, 0.7f, 0.6f, 0.4f, 0.2f)),
        AudioClip(id = "sfx_ruler", title = "📐 Drafting Ruler Snap", artist = "Sketch ASMR", durationMs = 650L, waveform = listOf(0.9f, 0.95f, 0.5f, 0.2f)),
        AudioClip(id = "sfx_scissors", title = "✂️ Paper Scissors Cut", artist = "Sketch ASMR", durationMs = 950L, waveform = listOf(0.7f, 0.9f, 0.6f, 0.3f)),
        AudioClip(id = "sfx_tape_tear", title = "🏷️ Washi Tape Tear", artist = "Sketch ASMR", durationMs = 1250L, waveform = listOf(0.4f, 0.85f, 0.9f, 0.5f, 0.2f))
    )

    val doodleStickers = listOf(
        "✏️", "📐", "✂️", "📌", "📎", "🏷️",
        "⭐️", "✨", "⚡️", "💬", "💡", "❤️",
        "☕️", "🎯", "🔥", "✍️", "🚀", "📖",
        "🎬", "🎨", "👀", "💯", "🎈", "🕶️"
    )

    fun getInitialProjects(): List<VideoProject> {
        val p1 = VideoProject(
            id = "proj_cyberpunk_reel",
            title = "Tokyo Neon Reel ⚡",
            aspectRatio = AspectRatioType.RATIO_9_16,
            clips = listOf(
                MediaClip(
                    id = "clip_01",
                    title = "Neon District Intro",
                    gradientColors = listOf(0xFF0F0F23, 0xFF1A1A4A, 0xFF0D0D1A),
                    durationMs = 3500L,
                    speed = 1.0f,
                    filter = VideoFilter.CYBERPUNK,
                    transition = TransitionType.WHIP_PAN
                ),
                MediaClip(
                    id = "clip_02",
                    title = "Speed Ramp Drop",
                    gradientColors = listOf(0xFF1A0A2E, 0xFF16213E, 0xFF0F3460),
                    durationMs = 2800L,
                    speed = 1.4f,
                    filter = VideoFilter.GLITCH_ART,
                    transition = TransitionType.FLASH_WHITE
                ),
                MediaClip(
                    id = "clip_03",
                    title = "Sunset Coast View",
                    gradientColors = listOf(0xFF2D1B3E, 0xFF8B3A3A, 0xFFE07B39),
                    durationMs = 3200L,
                    speed = 0.9f,
                    filter = VideoFilter.TEAL_ORANGE
                )
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "aud_01",
                    title = "Midnight Phonk (Speed Up)",
                    artist = "K-Phonk",
                    startTimelineMs = 0L,
                    durationMs = 9500L,
                    waveform = listOf(0.4f, 0.7f, 0.9f, 0.8f, 0.95f, 0.7f, 0.85f, 0.9f, 0.6f, 0.8f, 0.5f)
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "txt_01",
                    text = "TOKYO 2077 ⚡",
                    startTimelineMs = 500L,
                    durationMs = 2500L,
                    fontStyle = TextFontStyle.CAPCUT_BOLD,
                    color = 0xFF00E5FF,
                    animation = TextAnimationType.POP_UP,
                    posX = 0.5f,
                    posY = 0.35f
                ),
                TextOverlay(
                    id = "txt_02",
                    text = "DROP THE BEAT",
                    startTimelineMs = 3500L,
                    durationMs = 2800L,
                    fontStyle = TextFontStyle.NEON_GLOW,
                    color = 0xFFFF3358,
                    animation = TextAnimationType.TYPEWRITER,
                    posX = 0.5f,
                    posY = 0.65f
                )
            ),
            stickers = listOf(
                StickerOverlay(
                    id = "stk_01",
                    symbol = "🔥",
                    startTimelineMs = 3500L,
                    durationMs = 2000L,
                    posX = 0.5f,
                    posY = 0.5f,
                    scale = 1.3f
                )
            ),
            effects = listOf(
                TimelineEffect(
                    id = "eff_01",
                    name = "RGB Split Glitch",
                    effectType = EffectType.RGB_SPLIT,
                    startTimelineMs = 3500L,
                    durationMs = 1500L
                )
            ),
            lastModified = System.currentTimeMillis() - 1000 * 60 * 42 // 42 mins ago
        )

        val p2 = VideoProject(
            id = "proj_summer_vlog",
            title = "Summer Roadtrip Vlog 🌴",
            aspectRatio = AspectRatioType.RATIO_9_16,
            clips = listOf(
                MediaClip(
                    id = "c_vlog_01",
                    title = "Coastal Palms",
                    gradientColors = listOf(0xFF1B4D3E, 0xFF2D8D4C, 0xFF4CAF50),
                    durationMs = 4500L,
                    filter = VideoFilter.GOLDEN_HOUR,
                    transition = TransitionType.DISSOLVE
                ),
                MediaClip(
                    id = "c_vlog_02",
                    title = "Sunset Waves",
                    gradientColors = listOf(0xFF3E2723, 0xFF5D4037, 0xFF8D6E63),
                    durationMs = 4000L,
                    filter = VideoFilter.VINTAGE
                )
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "aud_vlog_01",
                    title = "Golden Hour Glow",
                    artist = "Aesthetic Beats",
                    startTimelineMs = 0L,
                    durationMs = 8500L
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "txt_vlog_01",
                    text = "Summer Memories ✨",
                    startTimelineMs = 800L,
                    durationMs = 3500L,
                    fontStyle = TextFontStyle.CINEMATIC_SERIF,
                    color = 0xFFFFFFFF,
                    animation = TextAnimationType.FADE_IN,
                    posX = 0.5f,
                    posY = 0.45f
                )
            ),
            lastModified = System.currentTimeMillis() - 1000 * 60 * 60 * 5 // 5 hours ago
        )

        val p3 = VideoProject(
            id = "proj_sketch_blueprint",
            title = "Architect's Blueprint 📐",
            aspectRatio = AspectRatioType.RATIO_16_9,
            isFavorite = true,
            paperStyle = "Drafting Grid",
            clips = listOf(
                MediaClip(
                    id = "c_blue_01",
                    title = "Blueprint Structural Draft",
                    gradientColors = listOf(0xFF001F3F, 0xFF003366, 0xFF0074D9),
                    durationMs = 4000L,
                    filter = VideoFilter.BLUEPRINT,
                    transition = TransitionType.PAGE_FLIP
                ),
                MediaClip(
                    id = "c_blue_02",
                    title = "Graphite Elevation Sketch",
                    gradientColors = listOf(0xFF1A1A1A, 0xFF2D2D2D, 0xFF3D3D3D),
                    durationMs = 3800L,
                    filter = VideoFilter.SKETCH_GRAPHITE
                )
            ),
            audioTracks = listOf(
                sketchbookFoleyList[0].copy(
                    id = "aud_foley_01",
                    startTimelineMs = 0L,
                    durationMs = 7800L
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "txt_arch_01",
                    text = "DRAFT NO. 42 // SCALE 1:50",
                    startTimelineMs = 500L,
                    durationMs = 3200L,
                    fontStyle = TextFontStyle.CAPCUT_BOLD,
                    color = 0xFF38BDF8,
                    animation = TextAnimationType.TYPEWRITER,
                    posX = 0.5f,
                    posY = 0.25f
                )
            ),
            stickers = listOf(
                StickerOverlay(
                    id = "stk_arch_01",
                    symbol = "📐",
                    startTimelineMs = 1000L,
                    durationMs = 2800L,
                    posX = 0.85f,
                    posY = 0.2f,
                    scale = 1.2f
                )
            ),
            lastModified = System.currentTimeMillis() - 1000 * 60 * 15 // 15 mins ago
        )

        return listOf(p1.copy(isFavorite = true), p2, p3)
    }
}
