package com.example.data

/**
 * Offline Template Store & Pack Manager.
 */
object TemplateStore {

    data class TemplatePack(val id: String, val title: String, val category: String)

    val availablePacks = listOf(
        TemplatePack("pack_cinematic", "Cinematic Film Pack", "Transitions"),
        TemplatePack("pack_cyberpunk", "Cyberpunk Neon Glow", "Effects"),
        TemplatePack("pack_vlog", "Daily Vlog Beats", "Music & Cut")
    )

    fun getPack(id: String): TemplatePack? {
        return availablePacks.find { it.id == id }
    }
}
