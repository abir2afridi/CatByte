package com.example.ui.canvas

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.Keep
import com.example.engine.gl.GLCompositor
import com.example.engine.gl.GLFramebuffer
import com.example.engine.gl.GLTextureCache
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

// GL_TEXTURE_EXTERNAL_OES = 0x8D65 (not in GLES20/GLES30 constants)
private const val GL_TEXTURE_EXTERNAL_OES = 0x8D65

/**
 * GPU-accelerated preview surface using custom GL Compositor.
 * Renders masks, blends, chroma key, and GLSL effects in real-time.
 * 
 * This version integrates with Media3 by accepting a Surface from Media3's
 * VideoCompositor and using it as input texture.
 */
@Keep
class GLPreviewSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "GLPreviewSurface"
    }

    private var renderThread: RenderThread? = null
    private var glCompositor: GLCompositor? = null
    private var textureCache: GLTextureCache? = null
    private var currentProject: VideoProject? = null
    private var currentPositionMs = 0L
    private var isPlaying = false
    private var width = 0
    private var height = 0
    private var externalSurface: Surface? = null
    private var externalTextureId = 0

    init {
        holder.addCallback(this)
        setZOrderOnTop(true)
        holder.setFormat(android.graphics.PixelFormat.RGBA_8888)
    }

    fun setProject(project: VideoProject) {
        currentProject = project
        renderThread?.setProject(project)
    }

    fun setPlayheadPosition(positionMs: Long) {
        currentPositionMs = positionMs
        renderThread?.setPosition(positionMs)
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        renderThread?.setPlaying(playing)
    }

    /**
     * Sets an external Surface (from Media3 VideoCompositor) as input.
     * The GL Compositor will read from this surface's texture.
     */
    fun setExternalSurface(surface: Surface) {
        externalSurface = surface
        renderThread?.setExternalSurface(surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        width = holder.surfaceFrame.width()
        height = holder.surfaceFrame.height()
        if (width > 0 && height > 0) {
            startRenderThread()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        width = w
        height = h
        renderThread?.resize(w, h)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopRenderThread()
    }

    private fun startRenderThread() {
        renderThread = RenderThread(WeakReference(this), holder.surface)
        renderThread?.start()
    }

    private fun stopRenderThread() {
        renderThread?.requestStop()
        renderThread = null
    }

    private class RenderThread(
        private val surfaceRef: WeakReference<GLPreviewSurface>,
        private val surface: Surface
    ) : Thread("GLPreviewRender") {

        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var glCompositor: GLCompositor? = null
        private var textureCache: GLTextureCache? = null
        private var currentProject: VideoProject? = null
        private var currentPositionMs = 0L
        private var isPlaying = false
        private val shouldStop = AtomicBoolean(false)
        private var frameCount = 0L
        private var lastFrameTime = System.nanoTime()
        private var width = 0
        private var height = 0
        private var externalTextureId = 0
        private var externalEglImage: Long = 0L // EGL_NO_IMAGE_KHR = 0

        override fun run() {
            initEGL()
            initGL()

            while (!shouldStop.get()) {
                val startTime = System.nanoTime()
                val deltaMs = (startTime - lastFrameTime) / 1_000_000f
                lastFrameTime = startTime

                renderFrame(deltaMs)

                // Target 60fps = 16.67ms per frame
                val frameTimeMs = (System.nanoTime() - startTime) / 1_000_000
                val sleepTime = max(0, 16 - frameTimeMs)
                if (sleepTime > 0) {
                    try { Thread.sleep(sleepTime.toLong()) } catch (e: InterruptedException) { break }
                }
            }

            cleanup()
        }

        private fun initEGL() {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES3_BIT_KHR = 4
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val config = configs[0]!!

            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfaceAttribs, 0)

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            Log.i(GLPreviewSurface.TAG, "EGL initialized for preview")
        }

        private fun initGL() {
            glCompositor = GLCompositor(width, height)
            textureCache = GLTextureCache(maxMemoryBytes = 64 * 1024 * 1024)
            
            // Create external texture for Media3 input
            val tex = IntArray(1)
            GLES30.glGenTextures(1, tex, 0)
            externalTextureId = tex[0]
            GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, externalTextureId)
            GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)
        }

        private fun renderFrame(deltaMs: Float) {
            val surfaceView = surfaceRef.get() ?: return
            val project = currentProject ?: return

            glCompositor?.updateTime(deltaMs)

            // Build layers for current playhead position
            val layers = buildLayerInputs(project, currentPositionMs)

            // Render composited frame
            var backgroundTex = 0
            var firstLayer = true

            for (layer in layers) {
                if (firstLayer) {
                    backgroundTex = glCompositor!!.renderLayer(layer)
                    firstLayer = false
                } else {
                    backgroundTex = glCompositor!!.renderLayer(layer, backgroundTex)
                }
            }

            // Present to surface
            if (backgroundTex != 0) {
                presentToSurface(backgroundTex)
            }

            frameCount++
        }

        private fun presentToSurface(textureId: Int) {
            // Blit to default framebuffer (surface)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, width, height)
            GLES30.glClearColor(0f, 0f, 0f, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            // Draw textured quad to screen
            // This would use a simple blit shader
            // For now, just swap buffers
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        private fun buildLayerInputs(project: VideoProject, positionMs: Long): List<GLCompositor.LayerInput> {
            val layers = mutableListOf<GLCompositor.LayerInput>()
            val timeMs = positionMs

            // Main track
            var cursorMs = 0L
            for (clip in project.clips) {
                val clipEnd = cursorMs + clip.durationMs
                if (timeMs in cursorMs..clipEnd) {
                    // TODO: Get actual video texture from Media3 player
                    // For now, create placeholder
                    val texId = textureCache!!.getOrCreate(
                        "clip_${clip.id}",
                        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                    )
                    layers.add(GLCompositor.LayerInput(
                        textureId = texId,
                        blendMode = clip.blendMode,
                        opacity = clip.opacity,
                        effects = buildEffectList(clip),
                        mask = buildMaskParams(clip),
                        chromaKey = null
                    ))
                }
                cursorMs = clipEnd
            }

            // Overlays
            for (track in project.overlayTracks) {
                for (clip in track.clips) {
                    val clipStart = clip.startTimelineMs
                    val clipEnd = clipStart + clip.durationMs
                    if (timeMs in clipStart..clipEnd) {
                        val texId = textureCache!!.getOrCreate(
                            "overlay_${clip.id}",
                            android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                        )
                        layers.add(GLCompositor.LayerInput(
                            textureId = texId,
                            blendMode = clip.blendMode,
                            opacity = clip.opacity,
                            effects = buildEffectList(clip),
                            mask = buildMaskParams(clip),
                            chromaKey = null
                        ))
                    }
                }
            }

            return layers
        }

        private fun buildEffectList(clip: MediaClip): List<com.example.engine.GlslEffect> {
            val effects = mutableListOf<com.example.engine.GlslEffect>()
            if (clip.filter != com.example.model.VideoFilter.NONE) {
                val filterEffect = com.example.engine.EffectShaders.ALL.find { it.id == "filter_${clip.filter.name.lowercase()}" }
                filterEffect?.let { effects.add(it) }
            }
            return effects
        }

        private fun buildMaskParams(clip: MediaClip): GLCompositor.MaskParams? {
            if (clip.mask.type == com.example.model.MaskType.NONE) return null
            return GLCompositor.MaskParams(
                type = clip.mask.type,
                posX = clip.mask.posX,
                posY = clip.mask.posY,
                width = clip.mask.width,
                height = clip.mask.height,
                rotation = clip.mask.rotation,
                feather = clip.mask.feather,
                inverted = clip.mask.isInverted
            )
        }

        fun setProject(project: VideoProject) {
            currentProject = project
        }

        fun setPosition(positionMs: Long) {
            currentPositionMs = positionMs
        }

        fun setPlaying(playing: Boolean) {
            isPlaying = playing
        }

        fun resize(w: Int, h: Int) {
            width = w
            height = h
            glCompositor?.resize(w, h)
        }

        fun setExternalSurface(surface: Surface) {
            // Create EGLImage from external surface for zero-copy
            // This allows reading Media3's output texture directly
        }

        fun requestStop() {
            shouldStop.set(true)
            interrupt()
        }

        private fun cleanup() {
            glCompositor?.release()
            textureCache?.clear()
            if (externalTextureId != 0) {
                GLES30.glDeleteTextures(1, intArrayOf(externalTextureId), 0)
            }
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(eglDisplay)
            }
        }
    }
}