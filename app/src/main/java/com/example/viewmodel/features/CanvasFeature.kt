package com.example.viewmodel.features

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.MediaMetadata
import com.example.engine.AspectMath
import com.example.model.AspectRatioType
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.withUpdatedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /**
     * Applies the "Original" canvas ratio: the ratio of the imported footage.
     *
     * When the source size is already known the ratio is applied right away
     * and returned. Otherwise the media file is probed in the background and
     * the ratio is applied as soon as the size is known - in that case `null`
     * is returned.
     */
    fun applyOriginalAspectRatio(vm: EditorViewModel): AspectRatioType? {
        val project = vm._currentProject.value ?: return null
        val known = project.allVideoClips.firstOrNull { it.sourceWidth > 0 && it.sourceHeight > 0 }
        if (known != null) {
            val ratio = AspectMath.fromVideo(known.sourceWidth, known.sourceHeight)
            setAspectRatio(vm, ratio)
            return ratio
        }

        val source = project.allVideoClips.firstOrNull { !it.uriString.isNullOrBlank() } ?: return null
        val uriString = source.uriString ?: return null
        val ctx = vm.context ?: return null
        vm.viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                runCatching { MediaMetadata.videoInfo(ctx, Uri.parse(uriString)) }.getOrNull()
            } ?: return@launch
            if (info.width <= 0 || info.height <= 0) return@launch
            vm.updateProject { proj ->
                proj.withUpdatedClip(source.id) {
                    it.copy(sourceWidth = info.width, sourceHeight = info.height)
                }
            }
            setAspectRatio(vm, AspectMath.fromVideo(info.width, info.height, info.rotation))
        }
        return null
    }
}
