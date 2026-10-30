package com.example

import android.app.Application
import com.example.data.AppContainer
import com.example.data.AppLog

class CatByteApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.info(
            tag = "App",
            message = "CatByte ${BuildConfig.VERSION_NAME} started (Android ${android.os.Build.VERSION.RELEASE}, SDK ${android.os.Build.VERSION.SDK_INT})"
        )
        container = AppContainer(this)
    }
}
