package com.example.viewmodel.features

import com.example.model.AspectRatioType
import com.example.viewmodel.EditorViewModel

/** Project canvas ratio and paper style. */
internal object CanvasFeature {

    fun setAspectRatio(vm: EditorViewModel, ratio: AspectRatioType) {
        vm.updateProject { proj ->
            proj.copy(aspectRatio = ratio)
        }
    }

    fun setPaperStyle(vm: EditorViewModel, style: String) {
        vm.updateProject { proj ->
            proj.copy(paperStyle = style)
        }
    }
}
