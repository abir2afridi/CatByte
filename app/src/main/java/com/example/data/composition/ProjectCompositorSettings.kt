package com.example.data.composition

import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.StaticOverlaySettings
import com.example.data.KeyframeInterpolator
import com.example.model.MediaClip

/**
 * Dynamic compositor settings: input 0 is the main video, inputs 1..N are PIP
 * clips. The overlay position, scale, rotation and alpha are evaluated for every
 * frame, so keyframed PIP motion is rendered identically in preview and export.
 */
@UnstableApi
internal class ProjectCompositorSettings(
    private val width: Int,
    private val height: Int,
    private val overlayInputs: Map<Int, MediaClip>,
    /**
     * When a blurred background copy occupies input 0, the sharp main video
     * becomes [mainInputId] and must be contain-fitted instead of stretched
     * over the background. `null` keeps the default full-frame mapping.
     */
    private val mainInputId: Int = 0,
    private val mainScale: Float? = null
) : VideoCompositorSettings {

    private val defaultSettings: OverlaySettings = StaticOverlaySettings.Builder()
        .setAlphaScale(1f)
        .build()

    override fun getOutputSize(inputSizes: List<Size>): Size = Size(width, height)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        // Main video sitting above a blurred background input: contain-fit it
        // so the blur shows in the bars (CapCut "Background -> Blur").
        if (mainScale != null && inputId == mainInputId) {
            return StaticOverlaySettings.Builder()
                .setScale(mainScale, mainScale)
                .setAlphaScale(1f)
                .build()
        }
        val clip = overlayInputs[inputId] ?: return defaultSettings
        val timeMs = presentationTimeUs / 1000L
        val localMs = timeMs - clip.startTimelineMs
        if (localMs < 0L || localMs > clip.durationMs) {
            return StaticOverlaySettings.Builder().setAlphaScale(0f).build()
        }

        val posX = KeyframeInterpolator.getPosX(clip, localMs).coerceIn(0f, 1f)
        val posY = KeyframeInterpolator.getPosY(clip, localMs).coerceIn(0f, 1f)
        val scale = KeyframeInterpolator.getScale(clip, localMs).coerceAtLeast(0.02f)
        val rotation = KeyframeInterpolator.getRotation(clip, localMs)
        val opacity = KeyframeInterpolator.getOpacity(clip, localMs).coerceIn(0f, 1f)

        return StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(
                (posX * 2f - 1f).coerceIn(-1f, 1f),
                (1f - posY * 2f).coerceIn(-1f, 1f)
            )
            .setOverlayFrameAnchor(0f, 0f)
            .setScale(scale, scale)
            // Android/Compose rotation is clockwise; OverlaySettings is CCW.
            .setRotationDegrees(-rotation)
            .setAlphaScale(opacity)
            .build()
    }
}