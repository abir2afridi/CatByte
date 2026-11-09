package com.example.engine

/**
 * A colour grade as neutral-offset offsets (0 = untouched), so a filter can be
 * layered, keyframed and intensity-blended without touching pixels directly.
 */
data class ColorGrade(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val fade: Float = 0f,
) {
    fun coerceIn(): ColorGrade = ColorGrade(
        brightness = brightness.coerceIn(-1f, 1f),
        contrast = contrast.coerceIn(-1f, 1f),
        saturation = saturation.coerceIn(-1f, 1f),
        temperature = temperature.coerceIn(-1f, 1f),
        tint = tint.coerceIn(-1f, 1f),
        vignette = vignette.coerceIn(0f, 1f),
        grain = grain.coerceIn(0f, 1f),
        fade = fade.coerceIn(0f, 1f),
    )
}

/**
 * Built-in look library. Twenty original grades replace the 13-value enum, and
 * every one is a [ColorGrade] so the compositor can blend two looks (or one look
 * at 40% intensity) with the same triangle maths.
 */
object FilterPresets {

    data class Preset(
        val id: String,
        val nameEn: String,
        val nameBn: String,
        val grade: ColorGrade,
    )

    val presets: List<Preset> = listOf(
        Preset("cinematic", "Cinematic", "সিনেম্যাটিক", ColorGrade(contrast = 0.12f, saturation = -0.06f, fade = 0.06f, vignette = 0.18f)),
        Preset("teal_orange", "Teal Orange", "টিল অরেঞ্জ", ColorGrade(contrast = 0.1f, saturation = 0.18f, temperature = 0.16f, tint = -0.1f)),
        Preset("warm_film", "Warm Film", "ওয়ার্ম ফিল্ম", ColorGrade(brightness = 0.05f, contrast = -0.04f, temperature = 0.22f, grain = 0.12f, fade = 0.1f)),
        Preset("cool_fade", "Cool Fade", "কুল ফেড", ColorGrade(saturation = -0.12f, temperature = -0.2f, fade = 0.16f)),
        Preset("bangla_gold", "Bangla Gold", "বাংলা গোল্ড", ColorGrade(brightness = 0.06f, saturation = 0.14f, temperature = 0.24f, vignette = 0.1f)),
        Preset("monsoon", "Monsoon", "মৌসুমী", ColorGrade(contrast = 0.08f, saturation = -0.05f, temperature = -0.12f, tint = 0.08f, grain = 0.08f)),
        Preset("vintage", "Vintage", "ভিন্টেজ", ColorGrade(contrast = -0.1f, saturation = -0.2f, temperature = 0.18f, fade = 0.22f, grain = 0.18f)),
        Preset("noir", "Noir", "নয়্যার", ColorGrade(contrast = 0.28f, saturation = -1f, vignette = 0.28f)),
        Preset("noir_punch", "Noir Punch", "নয়্যার পাঞ্চ", ColorGrade(contrast = 0.34f, saturation = -0.8f, brightness = -0.04f, vignette = 0.34f)),
        Preset("pastel", "Pastel", "প্যাস্টেল", ColorGrade(brightness = 0.1f, contrast = -0.12f, saturation = -0.1f, fade = 0.2f)),
        Preset("vivid_pop", "Vivid Pop", "ভিভিড পপ", ColorGrade(contrast = 0.16f, saturation = 0.42f)),
        Preset("sunset", "Sunset", "সূর্যাস্ত", ColorGrade(saturation = 0.2f, temperature = 0.3f, vignette = 0.14f)),
        Preset("forest", "Forest", "অরণ্য", ColorGrade(contrast = 0.08f, saturation = 0.12f, temperature = -0.16f, tint = -0.14f)),
        Preset("cyberpunk", "Cyberpunk", "সাইবারপাঙ্ক", ColorGrade(contrast = 0.2f, saturation = 0.34f, temperature = -0.24f, tint = 0.26f, vignette = 0.22f)),
        Preset("retro_vhs", "Retro VHS", "রেট্রো ভিএইচএস", ColorGrade(saturation = 0.3f, contrast = -0.06f, grain = 0.3f, fade = 0.18f)),
        Preset("clean_skin", "Clean Skin", "ক্লিন স্কিন", ColorGrade(brightness = 0.08f, contrast = -0.02f, saturation = -0.04f, temperature = 0.08f)),
        Preset("foodie", "Foodie", "ফুডি", ColorGrade(brightness = 0.05f, contrast = 0.1f, saturation = 0.3f, temperature = 0.12f)),
        Preset("snow", "Snow", "স্নো", ColorGrade(brightness = 0.12f, saturation = -0.16f, temperature = -0.22f)),
        Preset("midnight", "Midnight", "মিডনাইট", ColorGrade(brightness = -0.1f, contrast = 0.2f, temperature = -0.3f, vignette = 0.26f)),
        Preset("clay", "Clay", "ক্লে", ColorGrade(contrast = 0.06f, saturation = -0.14f, temperature = 0.14f, fade = 0.14f)),
    )

    val neutral: ColorGrade = ColorGrade()

    fun byId(id: String): Preset? = presets.firstOrNull { it.id == id }

    /** Linear blend; `t = 0` is [a], `t = 1` is [b]. */
    fun blend(a: ColorGrade, b: ColorGrade, t: Float): ColorGrade {
        val k = t.coerceIn(0f, 1f)
        fun mix(from: Float, to: Float) = from + (to - from) * k
        return ColorGrade(
            brightness = mix(a.brightness, b.brightness),
            contrast = mix(a.contrast, b.contrast),
            saturation = mix(a.saturation, b.saturation),
            temperature = mix(a.temperature, b.temperature),
            tint = mix(a.tint, b.tint),
            vignette = mix(a.vignette, b.vignette),
            grain = mix(a.grain, b.grain),
            fade = mix(a.fade, b.fade),
        ).coerceIn()
    }

    /** The intensity slider: neutral at 0, the full preset at 1. */
    fun atIntensity(preset: Preset, intensity: Float): ColorGrade =
        blend(neutral, preset.grade, intensity)
}
