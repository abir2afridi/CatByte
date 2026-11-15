package com.example.viewmodel.features

import com.example.model.StickerOverlay
import com.example.viewmodel.EditorViewModel
import java.util.UUID

/** Emoji/sticker overlays. */
internal object StickerFeature {

    fun addSticker(vm: EditorViewModel, symbol: String) {
        val sticker = StickerOverlay(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            startTimelineMs = vm._currentPositionMs.value,
            durationMs = 2500L,
            posX = 0.5f,
            posY = 0.4f,
            scale = 1.0f
        )
        vm.updateProject { proj ->
            proj.copy(stickers = proj.stickers + sticker)
        }
    }

    fun deleteSticker(vm: EditorViewModel, id: String) {
        vm.updateProject { proj ->
            proj.copy(stickers = proj.stickers.filterNot { it.id == id })
        }
    }
}
