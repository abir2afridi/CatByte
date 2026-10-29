package com.example

import android.app.Application
import com.example.data.AppContainer

class CatByteApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
