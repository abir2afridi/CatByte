package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.data.AppContainer
import com.example.data.AppLog

class CatByteApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        // Pre-copies the bundled OFL Bangla fonts so the preview and the
        // export renderers can resolve BANGLA_* text styles to real Bangla
        // typefaces instead of system-font fallbacks (tofu on stock Android).
        com.example.data.fonts.BanglaFontManager.initialize(this)
        AppLog.info(
            tag = "App",
            message = "CatByte ${BuildConfig.VERSION_NAME} started (Android ${android.os.Build.VERSION.RELEASE}, SDK ${android.os.Build.VERSION.SDK_INT})"
        )
        container = AppContainer(this)
    }

    /**
     * Registers Coil's video-frame decoder so the timeline filmstrip can render
     * real frames from the imported video files (a plain ImageLoader fails on
     * video URIs, which is why the tiles used to stay empty).
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
