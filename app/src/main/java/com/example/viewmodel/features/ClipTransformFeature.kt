package com.example.viewmodel.features

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.AppLog
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.withUpdatedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Chroma key and direct canvas transforms for the selected clip. */
internal object ClipTransformFeature {

    /** Updates chroma key (green/blue screen removal) for a clip. */
    fun setClipChromaKey(
        vm: EditorViewModel,
        clipId: String,
        chromaData: com.example.model.ChromaKeyData
    ) {
        vm.updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { if (it.id == clipId) it.copy(chromaKey = chromaData) else it },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(chromaKey = chromaData) else it })
                }
            )
        }
    }

    /** Updates the selected clip's transform directly from canvas gestures. */
    fun updateSelectedClipTransform(
        vm: EditorViewModel,
        scaleMultiplier: Float,
        rotationDelta: Float,
        panDeltaX: Float,
        panDeltaY: Float
    ) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "clipTransform:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val newScale = (clip.scale * scaleMultiplier).coerceIn(0.1f, 5.0f)
                val newRotation = (clip.rotation + rotationDelta) % 360f
                val newPosX = (clip.posX + panDeltaX).coerceIn(-0.5f, 1.5f)
                val newPosY = (clip.posY + panDeltaY).coerceIn(-0.5f, 1.5f)
                clip.copy(
                    scale = newScale,
                    rotation = if (newRotation < 0f) newRotation + 360f else newRotation,
                    posX = newPosX,
                    posY = newPosY
                )
            }
        }
    }

    /** Resets the selected clip's scale, position, and rotation to default. */
    fun resetSelectedClipTransform(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    scale = 1.0f,
                    rotation = 0f,
                    posX = 0.5f,
                    posY = 0.5f
                )
            }
        }
    }

    /** Samples video frame pixel color at normalized coordinates for Chroma Key. */
    fun sampleChromaColorAt(vm: EditorViewModel, context: Context, normX: Float, normY: Float) {
        val clipId = vm._selectedClipId.value ?: return
        val clip = vm._currentProject.value?.allVideoClips?.find { it.id == clipId } ?: return
        val uriStr = clip.uriString
        if (uriStr != null) {
            vm.viewModelScope.launch(Dispatchers.IO) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, Uri.parse(uriStr))
                    val timeUs = (vm._currentPositionMs.value * 1000L).coerceAtLeast(0L)
                    val frameBitmap = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (frameBitmap != null) {
                        val px = (normX.coerceIn(0f, 1f) * (frameBitmap.width - 1)).toInt()
                        val py = (normY.coerceIn(0f, 1f) * (frameBitmap.height - 1)).toInt()
                        val pixel = frameBitmap.getPixel(px, py)
                        val colorHex = (pixel.toLong() and 0xFFFFFFFFL) or 0xFF000000L
                        frameBitmap.recycle()
                        retriever.release()
                        withContext(Dispatchers.Main) {
                            val curr = clip.chromaKey
                            setClipChromaKey(vm, clipId, curr.copy(isEnabled = true, keyColorHex = colorHex))
                        }
                    } else {
                        retriever.release()
                    }
                } catch (e: Exception) {
                    AppLog.error("EditorViewModel", "Failed to sample chroma color at $normX, $normY", e)
                }
            }
        }
    }
}
